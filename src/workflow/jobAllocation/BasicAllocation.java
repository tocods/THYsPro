package workflow.jobAllocation;

import cloudsim.Host;
import cloudsim.Log;
import gpu.GpuCloudlet;
import gpu.GpuHost;
import gpu.power.PowerGpuHost;
import workflow.GpuJob;

import java.util.ArrayList;
import java.util.List;

public class BasicAllocation implements JobAllocationInterface{
    protected List<GpuJob> jobs;

    protected List<GpuJob> schedJobs;
    protected List<?extends Host> hosts;
    protected List<GpuJob> remainJobs;

    protected List<Host> filteredHost;

    private Integer lastId;
    @Override
    public void setJobs(List<GpuJob> list) {
        remainJobs = new ArrayList<>();
        jobs = list;
        schedJobs = new ArrayList<>();
        lastId = -1;
        filteredHost = new ArrayList<>();
    }

    @Override
    public void setHosts(List list) {
        hosts = list;
    }


    @Override
    public List<GpuJob> getJobs() {
        return schedJobs;
    }

    protected void doSchedule(List<Host> filterHosts) throws Exception {
        if(filterHosts.isEmpty())
            throw new Exception("所有主机都已宕机");
        List<GpuJob> noKernelJob = new ArrayList<>();
        List<GpuJob> kernelJob = new ArrayList<>();
        // 首先排除指定了主机的任务
        List<GpuJob> toRemove = new ArrayList<>();
        for(GpuJob job: jobs) {
            if(job.getHost() != null) {
                if(job.getHost().getRam() < job.getRam())
                    continue;
                toRemove.add(job);
                schedJobs.add(job);
                Log.printLine("指定调度:将" + job.getName() + "分配到" + job.getHost().getName());
            }
        }
        if(!toRemove.isEmpty())
            jobs.removeAll(toRemove);
        // 为了便于调度，将任务区分为GPU任务和非GPU任务
        for(GpuJob job: jobs) {
            boolean ifKernel = false;
            for(GpuCloudlet gpuCloudlet: job.getTasks()) {
                if(gpuCloudlet.getGpuTask() != null) {
                    ifKernel = true;
                    break;
                }
            }
            if(ifKernel)
                kernelJob.add(job);
            else
                noKernelJob.add(job);
        }
        Boolean ifHasBasicHost;
        for(GpuJob job: noKernelJob) {
            int times = 0;
            ifHasBasicHost = false;
            while (true) {
                // 此时说明所有节点都不满足要求
                if(times == filterHosts.size()) {
                    // 此时表明所有节点都不具备承载任务的基本条件
                    if(!ifHasBasicHost)
                        remainJobs.add(job);
                    // 有的节点虽然此时可用资源不够，但随着节点上任务完成资源被释放，该任务可以运行
                    else
                        schedJobs.add(job);
                    break;
                }
                lastId = (lastId + 1) % filterHosts.size();
                double availableRam = (filterHosts.get(lastId)).getRam() * (1 - ((PowerGpuHost) (filterHosts.get(lastId))).getCurrentRamUtilization());
                if(filterHosts.get(lastId).getRam() >= job.getRam()) {
                    ifHasBasicHost = true;
                    job.setVmId(filterHosts.get(lastId).getId());
                    job.setHost(filterHosts.get(lastId));
                }
                if(availableRam < job.getRam()) {
                    times ++;
                    continue;
                }
                Log.printLine("调度器：将" + job.getName() + "分配到" + filterHosts.get(lastId).getName());
                job.setVmId(filterHosts.get(lastId).getId());
                job.setHost(filterHosts.get(lastId));
                //Log.printLine("finish schedule");
                schedJobs.add(job);
                break;
            }
        }
        for(GpuJob job: kernelJob) {
            int times = 0;
            ifHasBasicHost = false;
            while (true) {
                // 此时说明所有节点都不满足要求
                if(times == filterHosts.size()) {
                    if(!ifHasBasicHost)
                        remainJobs.add(job);
                    else
                        schedJobs.add(job);
                    break;
                }
                lastId = (lastId + 1) % filterHosts.size();
                double availableRam = (filterHosts.get(lastId)).getRam() * (1 - ((PowerGpuHost) (filterHosts.get(lastId))).getCurrentRamUtilization());
                if(filterHosts.get(lastId).getRam() >= job.getRam() && !filterHosts.get(lastId).ifGpuFailed()) {
                    ifHasBasicHost = true;
                    job.setVmId(filterHosts.get(lastId).getId());
                    job.setHost(filterHosts.get(lastId));
                }
                if(availableRam < job.getRam() || filterHosts.get(lastId).ifGpuFailed() || !((GpuHost)(filterHosts.get(lastId))).isGpuEquipped()) {
                    times ++;
                    continue;
                }
                Log.printLine("调度器：将" + job.getName() + "分配到" + filterHosts.get(lastId).getName());
                job.setVmId(filterHosts.get(lastId).getId());
                job.setHost(filterHosts.get(lastId));
                //Log.printLine("finish schedule");
                schedJobs.add(job);
                break;
            }
        }
    }

    protected List<Host> doFilter() {
        List<Host> ret = new ArrayList<>();
        filteredHost = new ArrayList<>();
        for(Host h: hosts) {
            if(h.ifHostFailed()) {
                filteredHost.add(h);
                continue;
            }
            ret.add(h);
        }
        return ret;
    }
    @Override
    public void run() throws Exception {
        doSchedule(doFilter());
    }

    @Override
    public List<GpuJob> getRemainJobs() {
        return remainJobs;
    }

    @Override
    public void clearRemainJobs() {
        remainJobs.clear();
    }
}
