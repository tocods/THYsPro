package faulttolerant.faultInject;

import cloudsim.Host;
import faulttolerant.faultGenerator.FaultGenerator;
import gpu.GpuTask;
import workflow.GpuJob;

import java.util.List;

public interface FaultInjector {
    void initHostGenerator(Host host, List<FaultGenerator> faultGenerator);

    void initJobGenerator(String job, FaultGenerator faultGenerator);
    boolean ifJobFail(GpuJob task);

    double nextHostFailTime(Host host, double currentTime);

    double hostRepairTime(Host host);

    String lastFaultType(Host h);
}
