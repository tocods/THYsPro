package workflow.jobAllocation;

import cloudsim.Host;
import cloudsim.Log;
import workflow.GpuJob;

import java.util.List;
import java.util.Random;

public class RandomAllocation extends BasicAllocation{

    @Override
    protected void doSchedule(List<Host> filterHosts) throws Exception {
        //Log.printLine("开始调度");
        if(filterHosts.isEmpty())
            throw new Exception();
        Random random = new Random();
        int hostNum = filterHosts.size();
        for(GpuJob job: jobs) {
            int id = random.nextInt(hostNum);
            Log.printLine("Random调度器：将" + job.getName() + "分配到" + filterHosts.get(id).getName());
            job.setVmId(filterHosts.get(id).getId());
            job.setHost(filterHosts.get(id));
            //Log.printLine("finish schedule");
            schedJobs.add(job);
        }
    }
}
