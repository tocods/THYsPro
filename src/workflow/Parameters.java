package workflow;

import org.zeromq.ZMQ;
import org.zeromq.ZContext;

public class Parameters {
    public enum JobAllocationAlgorithm {
        BEST_FIT, DOT_PROD, RANDOM, PACKING, RR, FGD
    }

    public static JobAllocationAlgorithm allocationAlgorithm = JobAllocationAlgorithm.RR;

    public static int maxTime = 3;

    public static double duration = Double.MAX_VALUE;

    public static int engineID = -1;

    public static ZContext context = new ZContext(1);
    public static ZMQ.Socket socket = context.createSocket(ZMQ.REQ);
}
