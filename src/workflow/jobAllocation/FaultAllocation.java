package workflow.jobAllocation;

import cloudsim.Host;
import cloudsim.Log;
import cloudsim.core.CloudSim;
import gpu.GpuCloudlet;
import gpu.GpuHost;
import gpu.power.PowerGpuHost;
import workflow.GpuJob;

import java.util.ArrayList;
import java.util.List;

public class FaultAllocation extends BasicAllocation{

    @Override
    protected void doSchedule(List<Host> filterHosts) throws Exception {
        if(filterHosts.isEmpty())
            throw new Exception("所有主机都已经宕机");
        List<GpuJob> noKernelJob = new ArrayList<>();
        List<GpuJob> kernelJob = new ArrayList<>();
        // 为了便于调度，将任务区分为GPU任务和非GPU任务
        for(GpuJob job: jobs) {
            boolean ifKernel = false;
            for(GpuCloudlet gpuCloudlet: job.getTasks()) {
                if(gpuCloudlet.getGpuTask() != null) {
                    if(gpuCloudlet.getGpuTask().hardware == "gpu" || gpuCloudlet.getGpuTask().hardware == "GPU")
                    ifKernel = true;
                    break;
                }
            }
            if(ifKernel)
                kernelJob.add(job);
            else
                noKernelJob.add(job);
        }
        Log.printLine("普通任务: " + noKernelJob.size());
        Log.printLine("GPU任务: " + kernelJob.size());
        for(GpuJob job: noKernelJob) {
            int times = 0;
            GpuHost choseHost = null;
            while (true) {
                if(times == filterHosts.size()) {
                    if(choseHost == null) {
                        remainJobs.add(job);
                    }
                    else {
                        Log.printLine("调度器：将" + job.getName() + "分配到" + choseHost.getName());
                        job.setVmId(choseHost.getId());
                        job.setHost(choseHost);
                        //Log.printLine("finish schedule");
                        schedJobs.add(job);
                    }
                    break;
                }
                double availableRam = (filterHosts.get(times)).getRam() * (1 - ((PowerGpuHost) (filterHosts.get(times))).getCurrentRamUtilization());
                if(filterHosts.get(times).getRam() >= job.getRam()) {
                    job.setVmId(filterHosts.get(times).getId());
                    job.setHost(filterHosts.get(times));
                    if(choseHost == null)
                        choseHost = (GpuHost) filterHosts.get(times);
                }
                if(availableRam < job.getRam()) {
                    times ++;
                    continue;
                }
                if((choseHost.getFailTime() / CloudSim.clock()) > (((GpuHost)filterHosts.get(times)).getFailTime() / CloudSim.clock())) {
                    choseHost = (GpuHost) filterHosts.get(times);
                }
                times++;
            }
        }
        for(GpuJob job: kernelJob) {
            int times = 0;
            GpuHost choseHost = null;
            while (true) {
                // 此时说明所有节点都不满足要求
                if(times == filterHosts.size()) {
                    if(choseHost == null)
                        remainJobs.add(job);
                    else {
                        Log.printLine("调度器：将" + job.getName() + "分配到" + choseHost.getName());
                        job.setVmId(choseHost.getId());
                        job.setHost(choseHost);
                        //Log.printLine("finish schedule");
                        schedJobs.add(job);
                    }
                    break;
                }
                double availableRam = (filterHosts.get(times)).getRam() * (1 - ((PowerGpuHost) (filterHosts.get(times))).getCurrentRamUtilization());
                if(filterHosts.get(times).getRam() >= job.getRam() && !filterHosts.get(times).ifGpuFailed()) {
                    job.setVmId(filterHosts.get(times).getId());
                    job.setHost(filterHosts.get(times));
                    if(choseHost == null)
                        choseHost = (GpuHost) filterHosts.get(times);
                }
                if(availableRam < job.getRam() || filterHosts.get(times).ifGpuFailed() || !((GpuHost)(filterHosts.get(times))).isGpuEquipped()) {
                    times ++;
                    continue;
                }
                if((choseHost.getFailTime() / CloudSim.clock()) > (((GpuHost)filterHosts.get(times)).getFailTime() / CloudSim.clock())) {
                    choseHost = (GpuHost) filterHosts.get(times);
                }
                times++;
            }
        }
    }
}
