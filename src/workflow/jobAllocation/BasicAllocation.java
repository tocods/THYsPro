package workflow.jobAllocation;

import cloudsim.Host;
import cloudsim.Log;
import workflow.GpuJob;

import java.util.ArrayList;
import java.util.List;

public class BasicAllocation implements JobAllocationInterface{
    protected List<GpuJob> jobs;

    protected List<GpuJob> schedJobs;
    protected List<?extends Host> hosts;

    private Integer lastId;
    @Override
    public void setJobs(List<GpuJob> list) {
        jobs = list;
        schedJobs = new ArrayList<>();
        lastId = -1;
    }

    @Override
    public void setHosts(List list) {
        hosts = list;
    }


    @Override
    public List<GpuJob> getJobs() {
        return schedJobs;
    }


    @Override
    public void run() throws Exception {
        //Log.printLine("开始调度");
        for(GpuJob job: jobs) {
            //Log.printLine("job to schedule: " + job.getCloudletId());
            lastId = (lastId + 1) % hosts.size();
            Log.printLine("RoundRobin调度器：将" + job.getName() + "分配到" + hosts.get(lastId).getName());
            job.setVmId(hosts.get(lastId).getId());
            job.setHost(hosts.get(lastId));
            //Log.printLine("finish schedule");
            schedJobs.add(job);
        }
    }
}
