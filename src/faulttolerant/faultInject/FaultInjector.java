package faulttolerant.faultInject;

import cloudsim.Host;
import faulttolerant.faultGenerator.FaultGenerator;
import gpu.GpuTask;
import workflow.GpuJob;

public interface FaultInjector {
    void initHostGenerator(Host host, FaultGenerator faultGenerator);

    void initJobGenerator(String job, FaultGenerator faultGenerator);
    boolean ifJobFail(GpuJob task);

    double nextHostFailTime(Host host, double currentTime);
}
