package org.sega.viewer.services;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sega.viewer.models.DatabaseConfiguration;
import org.sega.viewer.models.ProcessInstance;
import org.sega.viewer.services.support.AttributeType;
import org.sega.viewer.utils.Base64Util;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Raysmond<i@raysmond.com>
 */
@Service
public class EdbService {

    private Map<String, List<AttributeType>> mapping = new HashMap<>();
    private Map<String, String> tableMap = new HashMap<>();
    private Connection connection;

    private static final String MYSQL_DRIVER = "com.mysql.jdbc.Driver";
    private static final String MYSQL_CONNECTION_URL = "jdbc:mysql://%s:%s/%s?useUnicode=true&characterEncoding=UTF-8";

    /**
     * Synchronize SeGA process instance to edb
     */
    public void sync(ProcessInstance instance) throws SQLException, ClassNotFoundException, UnSupportEdbException {
        mapping = new HashMap<>();
        tableMap = new HashMap<>();

        JSONObject entity = new JSONObject(instance.getEntity());
        JSONObject entitySchema = new JSONArray(instance.getProcess().getEntityJSON()).getJSONObject(0);

        getEntityInfo(entitySchema);

        connection = createEdbConnection(instance.getProcess().getDbconfig());

        // travel and sync entity
        syncEntity(entity, getMainArtifactId(entitySchema));

        closeEdbConnection();
    }

    /**
     * Synchronize an artifact entity recursively to edb
     * @param entity
     * @param artifactId
     */
    private void syncEntity(JSONObject entity, String artifactId) {
        if (!entity.has(artifactId))
            return;

        JSONObject artifact = entity.getJSONObject(artifactId);

        Map<String, Object> columns = new HashMap<>();
        String key = null;
        Long keyValue = null;

        for (AttributeType attribute : mapping.get(artifactId)) {
            String id = attribute.getId();

            if (!artifact.has(id)) {
                continue;
            }

            // one-to-one
            if (attribute.getType().equals("artifact") && artifact.optJSONObject(id) != null) {
                syncEntity(artifact.getJSONObject(id), id);
            }

            // one-to-many
            if (attribute.getType().equals("artifact_n") && artifact.optJSONArray(id) != null) {
                JSONArray entities = artifact.getJSONArray(id);
                for (int i = 0; i < entities.length(); ++i) {
                    syncEntity(entities.getJSONObject(i), id);
                }
            }

            if (attribute.getType().equals("key")) {
                key = id;
                String val = artifact.optString(id);
                if (val == null || val.equalsIgnoreCase("NEW")) {
                    keyValue = null;
                } else {
                    keyValue = Long.valueOf(artifact.getString(id));
                }

            }

            // normal attribute
            // TODO
            if (attribute.getType().equals("attribute")) {
                switch (attribute.getValueType()) {
                    case LONG:
                        columns.put(id, artifact.getLong(id));
                        break;
                    case INTEGER:
                        columns.put(id, artifact.getInt(id));
                        break;
                    case STRING:
                        columns.put(id, artifact.getString(id));
                        break;
                    default:
                        columns.put(id, artifact.getString(id));
                }
            }

            // group
            if (attribute.getType().equals("group")) {
                // TODO
            }
        }

        // save entity to edb
        Table table = new Table(connection, tableMap.get(artifactId), key, keyValue, columns);
        table.save();
    }

    private void getEntityInfo(JSONObject entitySchema) {
        String artifactId = entitySchema.getString("id");

        tableMap.put(artifactId, entitySchema.getJSONObject("data").getString("mapTo"));
        mapping.put(artifactId, new ArrayList<>());

        JSONArray children = entitySchema.getJSONArray("children");

        for (int i = 0; i < children.length(); ++i) {
            JSONObject child = children.getJSONObject(i);

            switch (child.getString("type")) {
                case "artifact":
                    getEntityInfo(child);
                    break;
                case "key":
                case "attribute":
                    AttributeType attribute = new AttributeType(
                            child.getString("id"),
                            child.getString("text"),
                            child.getString("type"),
                            child.getString("value_type"),
                            child.getBoolean("isMapped"),
                            child.getString("mapped_type")
                    );

                    mapping.get(artifactId).add(attribute);
                    break;
                case "group":
                    // TODO
                    break;
                case "artifact_n":
                    getEntityInfo(child);
                    break;
                default:
                    System.out.println("Unrecognized entityJson child: " + child.toString());
            }
        }
    }

    private String getMainArtifactId(JSONObject entitySchema) {
        return entitySchema.getString("id");
    }


    private Connection createEdbConnection(DatabaseConfiguration edb) throws ClassNotFoundException, SQLException, UnSupportEdbException {
        // only support mysql temporarily
        if (edb == null || !edb.getType().equals("mysql")) {
            throw new UnSupportEdbException();
        }

        String dbUrl = String.format(MYSQL_CONNECTION_URL, edb.getHost(), edb.getPort(), edb.getDatabase_name());
        String user = edb.getUsername();
        String password = edb.getPassword();

        Class.forName(MYSQL_DRIVER);
        return DriverManager.getConnection(dbUrl, user, password);
    }

    private void closeEdbConnection() throws SQLException {
        connection.close();
        connection = null;
    }
}

class UnSupportEdbException extends Exception {

}


class Table {
    private Connection connection;
    private String name;
    private String key;
    private Long keyValue;
    private Map<String, Object> columns = new HashMap<>();

    public Table(Connection connection, String name, String key, Long keyValue, Map<String, Object> columns) {
        this.connection = connection;
        this.name = name;
        this.key = key;
        this.columns = columns;
        this.keyValue = keyValue;
    }

    /**
     * Insert or update a table record
     */
    public void save() {
        PreparedStatement statement;
        String sql;

        if (!this.exists()) {
            sql = "INSERT INTO " + name + " (" + getColumnsSql() + ") VALUES (" + getValuesPlaceholder() + ")";
        } else {
            sql = "UPDATE " + name + " SET " + getUpdateColumnsSql() + " WHERE " + key + "=?";
        }

        try {
            statement = connection.prepareStatement(sql);

            // set statement parameters
            int count = 1;
            for (String column : columns.keySet()) {
                Object value = columns.get(column);

                if (value instanceof String) {
                    statement.setString(count++, value.toString());
                } else if (value instanceof Long) {
                    statement.setLong(count++, (Long) value);
                } else if (value instanceof Integer) {
                    statement.setLong(count++, (Integer) value);
                } else {
                    // TODO others
                    statement.setString(count++, value.toString());
                }
            }

            // UPDATE SQL
            if (this.exists()) {
                statement.setLong(count, keyValue);
            }

            System.out.println("sync SQL : " + statement.toString());

            // execute sync SQL
            statement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // TODO
    public String toSql() {
        return null;
    }

    private boolean exists() {
        return keyValue != null;
    }

    private String getColumnsSql() {
        StringBuilder sql = new StringBuilder();
        for (String column : columns.keySet()) {
            sql.append(column).append(",");
        }

        sql.deleteCharAt(sql.length() - 1);

        return sql.toString();
    }

    private String getValuesPlaceholder() {
        StringBuilder placeholder = new StringBuilder();

        for (int i = 0; i < columns.size(); ++i) {
            placeholder.append("?,");
        }

        placeholder.deleteCharAt(placeholder.length() - 1);

        return placeholder.toString();
    }

    private String getUpdateColumnsSql() {
        StringBuilder sql = new StringBuilder();

        for (String column : columns.keySet()) {
            sql.append(column).append("=").append("?").append(",");
        }

        sql.deleteCharAt(sql.length() - 1);

        return sql.toString();
    }
}