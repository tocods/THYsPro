package api.info;

import java.util.ArrayList;
import java.util.List;

public class JobRunningInfo {
    public String host = "";

    public String start = "";

    public String duration = "";

    public String end = "";

    public String status = "";

    public List<TaskRunInfo> runInfos = new ArrayList<>();
}
