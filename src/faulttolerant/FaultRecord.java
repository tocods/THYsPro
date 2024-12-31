package faulttolerant;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaultRecord {
    public enum FaultType{
        TIME_OVER,
        REBUILD
    }

    public FaultType type;

    public String fault = "";

    public String time = "";

    public String ifFalseAlarm = "True";

    public String ifSuccessRebuild = "True";

    public Integer redundancyBefore = Integer.MIN_VALUE;

    public Double redundancyAfter = Double.MAX_VALUE;

    public Integer faultJobNum = 0;

    public String faultType = "";

    public String ifEmpty = "False";

    public String failReason = "";

    public Map<String, List<String>> host2Cals = new HashMap<>();
}
