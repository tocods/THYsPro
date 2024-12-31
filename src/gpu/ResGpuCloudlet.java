package gpu;

import cloudsim.Log;
import cloudsim.ResCloudlet;
import cloudsim.UtilizationModelFull;
import workflow.GpuJob;

import java.util.ArrayList;
import java.util.List;

public class ResGpuCloudlet extends ResCloudlet {

	public Boolean ifGPU = false;

	public Boolean ifFromGpu = false;

	public String type = "";

	private final List<GpuTask> gpuTasks;
	private final List<GpuTask> remainGpuTasks;

	private final GpuTask gpuTask;

	public ResGpuCloudlet(GpuCloudlet cloudlet) {
		super(cloudlet);
		//Log.printLine("red: " + cloudlet.getName());
		this.gpuTask = cloudlet.getGpuTask();
		this.gpuTasks = new ArrayList<>();
		this.remainGpuTasks = new ArrayList<>();
		for(GpuCloudlet cl: ((GpuJob)cloudlet).getTasks()) {
			if(cl.getGpuTask() == null)
				continue;
			this.gpuTasks.add(cl.getGpuTask());
			this.remainGpuTasks.add(cl.getGpuTask());
			Log.printLine("将" + cl.getGpuTask().getName() + "归属于" + cloudlet.getName() + "其ID为：" + cloudlet.getCloudletId() );
			cl.getGpuTask().setResId(getCloudletId());
		}
		ifGPU = ((GpuJob) cloudlet).ifHasKernel();
		//this.gpuTasks = new ArrayList<>();
		//gpuTasks.add(gpuTask);
	}

	public ResGpuCloudlet(GpuTask task, GpuCloudlet cl) {
		super(cl);
		//Log.printLine("set: " + task.getThreadsPerBlock());
		this.gpuTask = task;
		this.remainGpuTasks = new ArrayList<>();
		this.gpuTasks = new ArrayList<>();
		this.ifFromGpu = true;
    }

	public ResGpuCloudlet(GpuCloudlet cloudlet, long startTime, int duration, int reservID) {
		super(cloudlet, startTime, duration, reservID);
		this.gpuTask = cloudlet.getGpuTask();
		this.gpuTasks = new ArrayList<>();
		this.remainGpuTasks = new ArrayList<>();
		for(GpuCloudlet cl: ((GpuJob)cloudlet).getTasks()) {
			if(cl.getGpuTask() == null)
				continue;
			this.gpuTasks.add(cl.getGpuTask());
			this.remainGpuTasks.add(cl.getGpuTask());
			cl.getGpuTask().setResId(getCloudletId());
		}
		ifGPU = !this.gpuTasks.isEmpty();
	}
	
	public GpuCloudlet finishCloudlet() {
		setCloudletStatus(GpuCloudlet.SUCCESS);
		finalizeCloudlet();
		return (GpuCloudlet) getCloudlet();
	}

	public GpuTask getGpuTask() {
		return gpuTask;
	}

	public List<GpuTask> getGpuTasks() {
		return gpuTasks;
	}

	public List<GpuTask> getRemainGpuTasks() {
		return remainGpuTasks;
	}

	public boolean finishGpuTasks(GpuTask t) {
		//Log.printLine("aaa");
		for(GpuTask task: getRemainGpuTasks()) {
			if(task.getTaskId() == t.getTaskId()) {
				remainGpuTasks.remove(task);
				//Log.printLine(((GpuJob)getCloudlet()).getName() + "还剩" + remainGpuTasks.size() + "个内核待执行");
				break;
			}
		}
		return remainGpuTasks.isEmpty();
	}

	public boolean hasGpuTask() {
//		if (getGpuTask() != null) {
//			return true;
//		}
//		return false;
		return !gpuTasks.isEmpty();
	}

}
