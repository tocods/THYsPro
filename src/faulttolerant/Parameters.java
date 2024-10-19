package faulttolerant;

import cloudsim.Host;
import faulttolerant.faultGenerator.FaultGenerator;
import faulttolerant.faultInject.FaultInjector;
import org.apache.commons.collections.list.AbstractLinkedList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Parameters {

    public static Map<Host, FaultGenerator> host2FaultInject = new HashMap<>();

    public static Map<String, FaultGenerator> job2FaultInject = new HashMap<>();

    public static FaultRecord record = null;

    public static List<FaultRecord> faultRecordList = new ArrayList<>();

    public static FaultInjector injector = null;

    public static int jobTotalRebuildNum = 0;

    public static int jobTotalRebuildSuccessTime = 0;
    public enum DistributionFamily {

        NONE, LOGNORMAL, GAMMA, WEIBULL, NORMAL
    }

    public enum FailureGeneratorMode {
        NONE, FAILURE_ALL, FAILURE_JOB, FAILURE_HOST
    }

    public enum JobFaultTolerationMode {
        NONE, RETRY, DC
    }

    public enum HostFaultTolerationMode {
        NONE, MIGRATE, DC
    }
}
