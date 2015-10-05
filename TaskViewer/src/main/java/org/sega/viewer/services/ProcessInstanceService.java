package org.sega.viewer.services;

import org.json.JSONObject;
import org.sega.viewer.models.Artifact;
import org.sega.viewer.models.ProcessInstance;
import org.sega.viewer.models.Process;
import org.sega.viewer.repositories.ProcessInstanceRepository;
import org.sega.viewer.services.support.TaskType;
import org.sega.viewer.services.support.TasksResolver;
import org.sega.viewer.utils.Base64Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

/**
 * @author Raysmond<jiankunlei@gmail.com>.
 */
@Service
public class ProcessInstanceService {
    @Autowired
    private ProcessInstanceRepository instanceRepository;

    @Autowired
    private ArtifactService artifactService;

    // todo
    public JSONObject readEntity(ProcessInstance processInstance, String taskId){
        JSONObject entity = null;
        TasksResolver tasksResolver = getTaskResolver(processInstance.getProcess().getBindingJson());

        if (tasksResolver == null)
            return null;

        TaskType taskType = tasksResolver.getTask(taskId);

        return entity;
    }

    // todo
    public JSONObject writeEntity(ProcessInstance processInstance, String taskId){
        JSONObject entity = null;
        TasksResolver tasksResolver = getTaskResolver(processInstance.getProcess().getBindingJson());

        if (tasksResolver == null)
            return null;

        TaskType taskType = tasksResolver.getTask(taskId);

        return entity;
    }


    public ProcessInstance createProcessInstance(Process process){
        ProcessInstance instance = new ProcessInstance(process);
//        artifactService.createMainArtifact(instance);

        // TODO
        instance.setEntity(createEntity(process));

        return instance;
    }

    private String createEntity(Process process) {
        try {
            return Base64Util.decode(process.getEntityJSON());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    private TasksResolver getTaskResolver(String bingResultJson){
        try {
            return new TasksResolver(bingResultJson);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }


    public JSONObject getEntity(ProcessInstance processInstance, String taskId){
        JSONObject entity = new JSONObject();
        // { j1_1: 11, j1_5: { j1_6: val } }

        TasksResolver tasksResolver = getTaskResolver(processInstance.getProcess().getBindingJson());

        if (tasksResolver == null)
            return null;

        TaskType taskType = tasksResolver.getTask(taskId);

        return entity;
    }

    public JSONObject updateEntity(JSONObject entity, String taskId, ProcessInstance processInstance){

        return entity;
    }
}
