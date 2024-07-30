package gpu;

import cloudsim.*;
import cloudsim.core.CloudSim;
import workflow.GpuJob;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link GpuCloudletSchedulerTimeShared} extends
 * {@link CloudletSchedulerTimeShared} to schedule {@link GpuCloudlet}s.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuCloudletSchedulerTimeShared extends CloudletSchedulerTimeShared implements GpuCloudletScheduler {

	private List<GpuTask> gpuTaskList;

	/**
	 * {@link CloudletSchedulerTimeShared} with GpuCloudlet support. Assumes all PEs have same MIPS
	 * capacity.
	 */
	public GpuCloudletSchedulerTimeShared() {
		super();
		setGpuTaskList(new ArrayList<GpuTask>());
	}

	@Override
	public double updateVmProcessing(double currentTime, List<Double> mipsShare) {
		setCurrentMipsShare(mipsShare);
		double timeSpam = currentTime - getPreviousTime();

		for (ResCloudlet rcl : getCloudletExecList()) {
			rcl.updateCloudletFinishedSoFar((long) (getCapacity(mipsShare) * timeSpam * rcl.getNumberOfPes() * Consts.MILLION));
		}

		if (getCloudletExecList().size() == 0) {
			setPreviousTime(currentTime);
			return 0.0;
		}

		// check finished cloudlets
		double nextEvent = Double.MAX_VALUE;
		List<ResCloudlet> toRemove = new ArrayList<ResCloudlet>();
		List<ResCloudlet> resCloudlets = getCloudletExecList();
		for (ResCloudlet rcl : resCloudlets) {
			long remainingLength = rcl.getRemainingCloudletLength();
			if (remainingLength == 0) {// finished: remove from the list
				toRemove.add(rcl);
				cloudletFinish(rcl);
			}
		}
		getCloudletExecList().removeAll(toRemove);

		// estimate finish time of cloudlets
		for (ResCloudlet rcl : getCloudletExecList()) {
			double estimatedFinishTime = currentTime
					+ (rcl.getRemainingCloudletLength() / (getCapacity(mipsShare) * rcl.getNumberOfPes()));
			if (estimatedFinishTime - currentTime < CloudSim.getMinTimeBetweenEvents()) {
				estimatedFinishTime = currentTime + CloudSim.getMinTimeBetweenEvents();
			}

			if (estimatedFinishTime < nextEvent) {
				nextEvent = estimatedFinishTime;
			}
		}

		setPreviousTime(currentTime);
		return nextEvent;
	}

	@Override
	public double cloudletSubmitWithoutGPU(Cloudlet cloudlet, double fileTransferTime) {
		ResGpuCloudlet rcl = new ResGpuCloudlet((GpuCloudlet) cloudlet);
		rcl.ifGPU = false;
		rcl.setCloudletStatus(Cloudlet.INEXEC);
		for (int i = 0; i < cloudlet.getNumberOfPes(); i++) {
			rcl.setMachineAndPeId(0, i);
		}
		long addLength = 0;
		for(GpuCloudlet gpuCloudlet: ((GpuJob)cloudlet).getTasks()) {
			if(gpuCloudlet.getGpuTask() != null) {
				addLength += gpuCloudlet.getGpuTask().getTaskTotalLength();
			}
		}
		rcl.addLength(addLength);
		getCloudletExecList().add(rcl);

		// use the current capacity to estimate the extra amount of
		// time to file transferring. It must be added to the cloudlet length
		double extraSize = getCapacity(getCurrentMipsShare()) * fileTransferTime;
		long length = (long) (cloudlet.getCloudletLength() + extraSize);
		cloudlet.setCloudletLength(length);


		return (length + addLength) / getCapacity(getCurrentMipsShare());
	}

	@Override
	public double cloudletSubmit(Cloudlet cloudlet, double fileTransferTime) {
		ResGpuCloudlet rcl = new ResGpuCloudlet((GpuCloudlet) cloudlet);
		//Log.printLine(((GpuCloudlet) cloudlet).getName() + "被提交到CPU的任务调度器上");
		rcl.setCloudletStatus(Cloudlet.INEXEC);
		for (int i = 0; i < cloudlet.getNumberOfPes(); i++) {
			rcl.setMachineAndPeId(0, i);
		}

		getCloudletExecList().add(rcl);

		// use the current capacity to estimate the extra amount of
		// time to file transferring. It must be added to the cloudlet length
		double extraSize = getCapacity(getCurrentMipsShare()) * fileTransferTime;
		long length = (long) (cloudlet.getCloudletLength() + extraSize);
		cloudlet.setCloudletLength(length);

		return cloudlet.getCloudletLength() / getCapacity(getCurrentMipsShare());
	}

	@Override
	public void cloudletFinish(ResCloudlet rcl) {
		ResGpuCloudlet rgcl = (ResGpuCloudlet) rcl;
		if (!rgcl.hasGpuTask() || !rgcl.ifGPU) {
			super.cloudletFinish(rcl);
			//Log.printLine(((GpuCloudlet)rcl.getCloudlet()).getName() + "不包含要GPU执行的内核");
		} else {
			List<GpuTask> gt = rgcl.getGpuTasks();
			getGpuTaskList().addAll(gt);
			//Log.printLine(((GpuCloudlet)rcl.getCloudlet()).getName() + "包含" + gt.size() + "个GPU执行的内核");
			try {
				rgcl.setCloudletStatus(GpuCloudlet.PAUSED);
				getCloudletPausedList().add(rgcl);
			} catch (Exception e) {
				e.printStackTrace();
				CloudSim.abruptallyTerminate();
			}
		}
	}

	protected List<GpuTask> getGpuTaskList() {
		return gpuTaskList;
	}

	protected void setGpuTaskList(List<GpuTask> gpuTaskList) {
		this.gpuTaskList = gpuTaskList;
	}

	@Override
	public boolean hasGpuTask() {
		//Log.printLine(getGpuTaskList().size());
		return !getGpuTaskList().isEmpty();
	}

	@Override
	public GpuTask getNextGpuTask() {
		//Log.printLine("getNextGpuTask");
		if (hasGpuTask()) {
			return getGpuTaskList().remove(0);
		}
		return null;
	}

	@Override
	public boolean notifyGpuTaskCompletion(GpuTask gt) {
		//Log.printLine("notifyGpuTaskCompletion: " + gt.getTaskId());
		ResGpuCloudlet toRemove = null;
		for (ResCloudlet rcl : getCloudletPausedList()) {
			ResGpuCloudlet rgcl = (ResGpuCloudlet) rcl;
			if (gt.isThisResCloudlet(rgcl)) {
				if(rgcl.finishGpuTasks(gt)) {
					//Log.printLine("a rescloudlet finish");
					//rgcl.setCloudletStatus(GpuCloudlet.SUCCESS);
					//rgcl.finalizeCloudlet();
					super.cloudletFinish(rgcl);
					toRemove = rgcl;
				}
			}
		}
		if(toRemove == null)
			return false;
		getCloudletPausedList().remove(toRemove);
		return true;
	}

}
