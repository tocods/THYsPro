package workflow.jobAllocation;

import cloudsim.Host;
import cloudsim.Log;
import workflow.GpuJob;

import java.util.List;
import java.util.Random;

public class RandomAllocation extends BasicAllocation{

    @Override
    public void run() throws Exception {
        //Log.printLine("开始调度");
        Random random = new Random();
        int hostNum = hosts.size();
        for(GpuJob job: jobs) {
            int id = random.nextInt(hostNum);
            Log.printLine("Random调度器：将" + job.getName() + "分配到" + hosts.get(id).getName());
            job.setVmId(hosts.get(id).getId());
            job.setHost(hosts.get(id));
            //Log.printLine("finish schedule");
            schedJobs.add(job);
        }
    }
}
