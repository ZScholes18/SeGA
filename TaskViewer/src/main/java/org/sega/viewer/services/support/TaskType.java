package org.sega.viewer.services.support;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Raysmond<i@raysmond.com>.
 */
@Data
public class TaskType {
    private String id;
    private boolean autoGenerate;
    private boolean syncPoint;

    private List<String> reads = new ArrayList<>();
    private List<String> writes = new ArrayList<>();

}
