package workflow.jobAllocation;

import workflow.GpuJob;

import java.util.List;

public interface JobAllocationInterface {

    public void setJobs(List<GpuJob> list);

    public void setHosts(List list);

    public List<GpuJob> getJobs();

    public abstract void run() throws Exception;
}
