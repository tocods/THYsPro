package faulttolerant;

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

    public Double redundancyBefore = Double.MIN_VALUE;

    public Double redundancyAfter = Double.MAX_VALUE;

    public Integer faultJobNum = 0;
}
