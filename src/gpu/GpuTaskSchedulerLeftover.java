package gpu;

import api.service.Parameters;
import cloudsim.Consts;
import cloudsim.Log;
import cloudsim.core.CloudSim;
import cloudsim.util.MathUtil;
import org.apache.commons.math3.analysis.function.Max;
import workflow.GpuJob;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GpuTaskSchedulerLeftover implements a policy of scheduling performed by a
 * {@link Vgpu} to run its {@link GpuTask GpuTasks}. The implemented policy is
 * leftover. A GpuTask occupies GPU resources until the end of its execution.
 * The simultaneous execution of other tasks is only possible if enough
 * resources are left unused; otherwise they will be in a waiting list. Even
 * though tasks may wait for GPU, their data transfer happens as soon as there
 * will be enough memory available in the GPU.
 * 
 * @author Ahmad Siavashi
 */
public class GpuTaskSchedulerLeftover extends GpuTaskScheduler {
	/** The number of used PEs. It holds virtual PE ids. */
	protected List<Integer> usedBlocks;

	private double usedGddram = 0.0;

	protected Integer cores;

	protected Integer coresPerSM;

	protected Integer maxBlock;

	protected Integer smNum;

	protected Integer usedsmNum = 0;

	private List<ResGpuTask> memoryTransferTask = new ArrayList<>();

	private List<ResGpuTask> waitingMemoryTransferTask = new ArrayList<>();

	/**
	 * Creates a new GpuTaskSchedulerLeftover object
	 */
	public GpuTaskSchedulerLeftover() {
		super();
		setUsedBlocks(new ArrayList<Integer>());
	}

	/**
	 * 初始化GPU中的流处理器(SM)
	 * @param cores GPU总核数
	 * @param coresPerSM 每个SM的核数
	 * @param maxBlock 每个SM最大线程块数
	 */
	public void initSMs(Integer cores, Integer coresPerSM, Integer maxBlock) {
		this.cores = cores;
		this.coresPerSM = coresPerSM;
		this.maxBlock = maxBlock;
		this.smNum = this.cores / this.coresPerSM;
	}

	public double getGPUUtil() {
		//if(!getUsedBlocks().isEmpty())
			//Log.printLine(getUsedBlocks().size() + " + " + getCurrentRequestedMips().size());
		//Log.printLine("GPU Util: " + (double) getUsedBlocks().size() + " "  + getCurrentRequestedMips().size());
		if(getCurrentRequestedMips().isEmpty())
			return 0.0;
		return (double) getUsedBlocks().size() / getCurrentRequestedMips().size();
	}

	@Override
	public double updateGpuTaskProcessing(double currentTime, List<Double> mipsShare, List<Double> imipsShare, List<Double> mmipsShare) {
		//Log.printLine("aaaasasa");
		setCurrentMipsShare(mipsShare);
		double timeSpan = currentTime - getPreviousTime(); // time since last
		// 如果不开启GPU共享，执行任务队列中只会有1个任务
		for (ResGpuTask rcl : getTaskExecList()) {
			double cap = getTotalCurrentAvailableMipsForTask(rcl, mipsShare);
			if(rcl.getGpuTask().calcuType == 1)
				cap = getTotalCurrentAvailableMipsForTask(rcl, imipsShare);
			if(rcl.getGpuTask().calcuType == 2)
				cap = getTotalCurrentAvailableMipsForTask(rcl, mmipsShare);
			rcl.updateTaskFinishedSoFar((long) (cap
					* rcl.getGpuTask().getUtilizationOfGpu(currentTime) * timeSpan * Consts.MILLION));
		}
		//Log.printLine(getTaskExecList().size() + "个任务在执行");
		// 没有任务在执行
		if (getTaskExecList().isEmpty() && getTaskWaitingList().isEmpty()) {
			setPreviousTime(currentTime);
			//Log.printLine("222");
			return 0.0;
		}

		List<ResGpuTask> toRemove = new ArrayList<ResGpuTask>();
		for (ResGpuTask rcl : getTaskExecList()) {
			// finished anyway, rounding issue...
			if (rcl.getRemainingTaskLength() == 0) {
				toRemove.add(rcl);
				//Log.printLine(CloudSim.clock() + " : 结束");
				taskFinish(rcl);
				Log.printLine(rcl.getGpuTask().getName() + " 执行结束");
			}
		}
		getTaskExecList().removeAll(toRemove);

		// 开启了GPU共享时，尝试将等待队列中的内核加入执行队列
		// 未开启GPU共享时，只有在执行队列为空时才将等待中的内核加入执行队列

		if (getTaskExecList().isEmpty() || Parameters.enableGPUShare) {
			toRemove.clear();
			for (ResGpuTask rcl : getTaskWaitingList()) {
				int numberOfCurrentAvailableBlocks = getCurrentMipsShare().size() - getUsedBlocks().size();
				if (numberOfCurrentAvailableBlocks > 0 && (gddram - usedGddram) >= rcl.getGpuTask().getRequestedGddramSize()) {
					rcl.setTaskStatus(GpuTask.INEXEC);
					int numberOfAllocatedPes = 0;
					for (int k = 0; k < mipsShare.size(); k++) {
						if (!getUsedBlocks().contains(k)) {
							rcl.setPeId(k);
							getUsedBlocks().add(k);
							numberOfAllocatedPes += 1;
							if (numberOfAllocatedPes == rcl.getGpuTask().getPesLimit()) {
								break;
							}
						}
					}
					//Log.printLine(rcl.getGpuTask().getName() + "加入待执行队列");
					getTaskExecList().add(rcl);
					toRemove.add(rcl);
					usedGddram += rcl.getGpuTask().getRequestedGddramSize();
					// 不开启GPU共享时，1次只将1个内核加入执行队列
					if(!Parameters.enableGPUShare)
						break;
				}
			}
			getTaskWaitingList().removeAll(toRemove);
		}

		// estimate finish time of tasks in the execution queue
		double nextEvent = Double.MAX_VALUE;
		for (ResGpuTask rcl : getTaskExecList()) {
			double estimatedFinishTime = currentTime + getEstimatedFinishTime(rcl);
			if (estimatedFinishTime < nextEvent) {
				nextEvent = estimatedFinishTime;
			}
		}
		setPreviousTime(currentTime);
		return nextEvent;
	}

	// TODO: Test it
	@Override
	public GpuTask taskCancel(int taskId) {
		// First, looks in the finished queue
		for (ResGpuTask rcl : getTaskFinishedList()) {
			if (rcl.getTaskId() == taskId) {
				getTaskFinishedList().remove(rcl);
				return rcl.getGpuTask();
			}
		}

		// Then searches in the exec list
		for (ResGpuTask rcl : getTaskExecList()) {
			if (rcl.getTaskId() == taskId) {
				getTaskExecList().remove(rcl);
				if (rcl.getRemainingTaskLength() == 0) {
					taskFinish(rcl);
				} else {
					rcl.setTaskStatus(GpuTask.CANCELED);
				}
				return rcl.getGpuTask();
			}
		}

		// Now, looks in the paused queue
		for (ResGpuTask rcl : getTaskPausedList()) {
			if (rcl.getTaskId() == taskId) {
				getTaskPausedList().remove(rcl);
				return rcl.getGpuTask();
			}
		}

		// Finally, looks in the waiting list
		for (ResGpuTask rcl : getTaskWaitingList()) {
			if (rcl.getTaskId() == taskId) {
				rcl.setTaskStatus(GpuTask.CANCELED);
				getTaskWaitingList().remove(rcl);
				return rcl.getGpuTask();
			}
		}

		return null;

	}

	// TODO: Test it
	@Override
	public boolean taskPause(int taskId) {
		boolean found = false;
		int position = 0;

		// first, looks for the task in the exec list
		for (ResGpuTask rcl : getTaskExecList()) {
			if (rcl.getTaskId() == taskId) {
				found = true;
				break;
			}
			position++;
		}

		if (found) {
			// moves to the paused list
			ResGpuTask rgl = getTaskExecList().remove(position);
			if (rgl.getRemainingTaskLength() == 0) {
				taskFinish(rgl);
			} else {
				rgl.setTaskStatus(GpuTask.PAUSED);
				getTaskPausedList().add(rgl);
			}
			return true;

		}

		// now, look for the task in the waiting list
		position = 0;
		found = false;
		for (ResGpuTask rcl : getTaskWaitingList()) {
			if (rcl.getTaskId() == taskId) {
				found = true;
				break;
			}
			position++;
		}

		if (found) {
			// moves to the paused list
			ResGpuTask rgl = getTaskWaitingList().remove(position);
			if (rgl.getRemainingTaskLength() == 0) {
				taskFinish(rgl);
			} else {
				rgl.setTaskStatus(GpuTask.PAUSED);
				getTaskPausedList().add(rgl);
			}
			return true;

		}

		return false;
	}

	@Override
	public void taskFinish(ResGpuTask rcl) {
		rcl.setTaskStatus(GpuTask.SUCCESS);
		rcl.finalizeTask();
		List<Integer> pesToRemove = new ArrayList<>();
		for (Integer peId : getUsedBlocks()) {
			pesToRemove.add(peId);
		}
		getUsedBlocks().removeAll(pesToRemove);
		getTaskFinishedList().add(rcl);
		usedGddram -= rcl.getGpuTask().getRequestedGddramSize();
		usedGddram = Math.max(0, usedGddram);
	}

	@Override
	public long jobFail(GpuJob job) {
		long ret = 0;
		Log.printLine(CloudSim.clock() + ": " + job.getName() + " 进入超时流程");
		List<ResGpuTask> toRemove = new ArrayList<>();
		for(ResGpuTask cl: getTaskExecList()) {
			GpuCloudlet cloudlet = cl.getGpuTask().getCloudlet();
			if(job.getTasks().contains(cloudlet)) {
				Log.printLine(cl.getGpuTask().getName() + "超时");
				toRemove.add(cl);
				ret += cl.getDoneLen();
				//taskFinish(cl);
				cl.finalizeTask();
				List<Integer> pesToRemove = new ArrayList<>();
				for (Integer peId : getUsedBlocks()) {
					pesToRemove.add(peId);
				}
				getUsedBlocks().removeAll(pesToRemove);
				getTaskFinishedList().add(cl);
				usedGddram -= cl.getGpuTask().getRequestedGddramSize();
				usedGddram = Math.max(0, usedGddram);
			}
		}
		getTaskExecList().removeAll(toRemove);
		toRemove = new ArrayList<>();
		for(ResGpuTask cl: getTaskWaitingList()) {
			GpuCloudlet cloudlet = cl.getGpuTask().getCloudlet();
			if(job.getTasks().contains(cloudlet)) {
				//Log.printLine(cl.getGpuTask().getName() + "超时");
				toRemove.add(cl);
				//taskFinish(cl);
			}
		}
		getTaskWaitingList().removeAll(toRemove);
		return ret;
	}

	@Override
	public void hostFail() {
		usedGddram = 0;
		getUsedBlocks().clear();
		super.hostFail();
	}

	// TODO: Test it
	@Override
	public double taskResume(int taskId) {
		boolean found = false;
		int position = 0;

		// look for the task in the paused list
		for (ResGpuTask rcl : getTaskPausedList()) {
			if (rcl.getTaskId() == taskId) {
				found = true;
				break;
			}
			position++;
		}

		if (found) {
			ResGpuTask rcl = getTaskPausedList().remove(position);
			int numberOfCurrentAvailablePEs = getCurrentMipsShare().size() - getUsedBlocks().size();
			// it can go to the exec list
			if (numberOfCurrentAvailablePEs > 0) {
				rcl.setTaskStatus(GpuTask.INEXEC);
				for (int i = 0; i < rcl.getNumberOfBlocks() && i < getCurrentMipsShare().size(); i++) {
					if (!getUsedBlocks().contains(i)) {
						rcl.setPeId(i);
						getUsedBlocks().add(i);
					}
				}

				getTaskExecList().add(rcl);

				return getEstimatedFinishTime(rcl);

			} else {// no enough free PEs: go to the waiting queue
				rcl.setTaskStatus(GpuTask.QUEUED);

				getTaskWaitingList().add(rcl);
				return 0.0;
			}

		}

		// not found in the paused list: either it is in in the queue, executing
		// or not exist
		return 0.0;

	}

	/**
	 * Returns the estimated amount of time that it takes for this task to finish.
	 * 
	 * @param rcl
	 * @return finish time estimation of the task
	 */
	protected double getEstimatedFinishTime(ResGpuTask rcl) {
		List<Double> mipsShare = getCurrentMipsShare();
		double totalMips = getTotalCurrentAvailableMipsForTask(rcl, mipsShare);
		return rcl.getRemainingTaskLength() / totalMips;
	}

	/**
	 * 提交1个任务内核给GPU
	 */
	@Override
	public double taskSubmit(GpuTask task) {

		ResGpuTask rgt = new ResGpuTask(task);
//		if(getCurrentMipsShare() == null)
//			Log.printLine("1");
//		if(getUsedBlocks() == null)
//			Log.printLine("2");
		Log.printLine("GPU任务长度：" + task.getTaskTotalLength());
		// 获取GPU剩余线程块数
		int numberOfCurrentAvailablePEs = getCurrentMipsShare().size() - getUsedBlocks().size();
		//Log.printLine(gddram + " " + usedGddram);
		double sizeOfCurrentAvailableGddram = gddram - usedGddram;

		//Log.printLine(sizeOfCurrentAvailableGddram + " " + numberOfCurrentAvailablePEs + "   : " + task.getRequestedGddramSize());
		// 如果还有剩余且开启了GPU共享，将任务放入执行队列
		if (numberOfCurrentAvailablePEs > 0 && sizeOfCurrentAvailableGddram >= task.getRequestedGddramSize() && (Parameters.enableGPUShare || getTaskExecList().isEmpty())) {
			rgt.setTaskStatus(GpuTask.INEXEC);
			int numberOfAllocatedBlocks = 0;
			for (int i = 0; i < getCurrentMipsShare().size(); i++) {
				if (!getUsedBlocks().contains(i)) {
					rgt.setPeId(i);
					getUsedBlocks().add(i);
					numberOfAllocatedBlocks++;
					// 占用线程块数达到了任务需求
					if (numberOfAllocatedBlocks == rgt.getGpuTask().getPesLimit()) {
						break;
					}
				}
			}
			usedGddram += task.getRequestedGddramSize();
			getTaskExecList().add(rgt);
			//Log.printLine(getEstimatedFinishTime(rgt));
			return getEstimatedFinishTime(rgt);
		} else {// 没有剩余的核或者未开启GPU共享，任务内核进入等待队列
			rgt.setTaskStatus(GpuTask.QUEUED);
			if(numberOfCurrentAvailablePEs <= 0)
				rgt.failType = "GPU";
			else
				rgt.failType = "GDDRAM";
			getTaskWaitingList().add(rgt);
			return 0.0;
		}
	}

	@Override
	public double getTotalUtilizationOfGpu(double time) {
		final double totalMipsShare = MathUtil.sum(getCurrentMipsShare());
		double totalRequestedMips = 0.0;
		for (ResGpuTask gl : getTaskExecList()) {
			totalRequestedMips += gl.getGpuTask().getUtilizationOfGpu(time)
					* getTotalCurrentAllocatedMipsForTask(gl, time);
		}
		double totalUtilization = totalRequestedMips / totalMipsShare;
		return totalUtilization;
	}

	/**
	 * Returns the first task to migrate to another VM.
	 * 
	 * @return the first running task
	 * @pre $none
	 * @post $none
	 * 
	 * @TODO: it doesn't check if the list is empty
	 */
	// TODO: Test it
	@Override
	public GpuTask migrateTask() {
		ResGpuTask rcl = getTaskExecList().remove(0);
		rcl.finalizeTask();
		GpuTask cl = rcl.getGpuTask();
		List<Integer> pesToRemove = new ArrayList<>();
		for (Integer peId : getUsedBlocks()) {
			pesToRemove.add(peId);
		}
		getUsedBlocks().removeAll(pesToRemove);
		return cl;
	}

	@Override
	public List<Double> getCurrentRequestedMips() {
		List<Double> mipsShare = new ArrayList<Double>();
		if (getCurrentMipsShare() != null) {
			double totalGpuUtilization = getTotalUtilizationOfGpu(CloudSim.clock());
			for (Double mips : getCurrentMipsShare()) {
				mipsShare.add(mips * totalGpuUtilization);
			}
		}
		return mipsShare;
	}

	@Override
	public double getTotalCurrentAvailableMipsForTask(ResGpuTask rcl, List<Double> mipsShare) {
		int coresPerBlock = coresPerSM / maxBlock;
		double flopsPerBlock = mipsShare.get(0) * Math.max(coresPerBlock, rcl.getGpuTask().getThreadsPerBlock()) / coresPerBlock;
		return flopsPerBlock * rcl.getPeIdList().size();
	}

	@Override
	public double getTotalCurrentAllocatedMipsForTask(ResGpuTask rcl, double time) {
		double totalMips = 0.0;
		for (Double mips : getCurrentAllocatedMipsForTask(rcl, time)) {
			totalMips += mips;
		}
		return totalMips;
	}

	@Override
	public List<Double> getCurrentAllocatedMipsForTask(ResGpuTask rcl, double time) {
		List<Double> allocatedMips = new ArrayList<Double>();
		for (Integer peId : rcl.getPeIdList()) {
			allocatedMips.add(getCurrentMipsShare().get(peId));
		}
		return allocatedMips;
	}

	@Override
	public double getTotalCurrentRequestedMipsForTask(ResGpuTask rcl, double time) {
		return rcl.getGpuTask().getUtilizationOfGpu(time) * getTotalCurrentAllocatedMipsForTask(rcl, time);
	}

	@Override
	public double getCurrentRequestedUtilizationOfGddram() {
		double totalUtilization = 0;
		for (ResGpuTask gl : getTaskExecList()) {
			totalUtilization += gl.getGpuTask().getUtilizationOfGddram(CloudSim.clock());
		}
		if (totalUtilization > 1) {
			totalUtilization = 1;
		}
		return totalUtilization;
	}

	@Override
	public double getCurrentRequestedUtilizationOfBw() {
		double totalUtilization = 0;
		for (ResGpuTask gl : getTaskExecList()) {
			totalUtilization += gl.getGpuTask().getUtilizationOfBw(CloudSim.clock());
		}
		if (totalUtilization > 1) {
			totalUtilization = 1;
		}
		return totalUtilization;
	}

	/**
	 * @return the usedPes
	 */
	protected List<Integer> getUsedBlocks() {
		return usedBlocks;
	}

	/**
	 * @param usedBlocks the usedPes to set
	 */
	protected void setUsedBlocks(List<Integer> usedBlocks) {
		this.usedBlocks = usedBlocks;
	}

//	/**
//	 * Returns a gpu task with memory transfer to complete. The gpu task is selected
//	 * from a waiting list if there was no gpu task with memory transfer in
//	 * progress. If the waiting list was empty, null is returned.
//	 *
//	 * @param currentAllocatedGddram the memory that is allocated to the vgpu which
//	 *                               this scheduler is associated with
//	 * @return a gpu task with memory transfer to complete; $null otherwise
//	 */
//	protected ResGpuTask getTaskForMemoryTransfer(int currentAllocatedGddram) {
//		if (getMemoryTransferList().isEmpty() && getWaitingMemoryTransferList().isEmpty()) {
//			// 没有任务需要传输
//			return null;
//		}
//		if (getMemoryTransferList().isEmpty()) {
//			ResGpuTask rcl = getWaitingMemoryTransferList().get(0);
//			double requestedGddram = rcl.getGpuTask().getRequestedGddramSize();
//			// guards
//			if (requestedGddram > currentAllocatedGddram) {
//				Log.printConcatLine(
//						CloudSim.clock()
//								+ ": the requested memory is more than that of allocated for the Vgpu: GpuTaskId: #",
//						rcl.getGpuTask().getTaskId());
//				System.exit(-1);
//			} else if (rcl.getGpuTask().getTaskInputSize() > requestedGddram
//					|| rcl.getGpuTask().getTaskOutputSize() > requestedGddram) {
//				Log.printConcatLine(
//						CloudSim.clock() + ": task's H2D/D2H transfers are larger than requested memory: GpuTaskId: #",
//						rcl.getGpuTask().getTaskId());
//				System.exit(-1);
//			}
//			double currentAvailableGddram = currentAllocatedGddram - usedGddram;
//			if (requestedGddram <= currentAvailableGddram) {
//				getWaitingMemoryTransferList().remove(rcl);
//				getMemoryTransferList().add(rcl);
//				this.usedGddram += requestedGddram;
//				if (rcl.lastMemoryTransferTime == 0) {
//					rcl.setTaskMemoryTransferStartTime(CloudSim.clock());
//				}
//				return rcl;
//			}
//		} else {
//			ResGpuTask rcl = getMemoryTransferList().get(0);
//			if (rcl.lastMemoryTransferTime == 0) {
//				rcl.setTaskMemoryTransferStartTime(CloudSim.clock());
//			}
//			return rcl;
//		}
//		return null;
//	}
//
//	private List<ResGpuTask> getMemoryTransferList() {
//		return memoryTransferTask;
//	}
//
//	private List<ResGpuTask> getWaitingMemoryTransferList() {
//		return waitingMemoryTransferTask;
//	}
//
//	@Override
//	public double updateTaskMemoryTransfer(double currentTime, int currentAllocatedGddram, long allocatedBw) {
//		double time = Double.MAX_VALUE;
//		ResGpuTask rcl = getTaskForMemoryTransfer(currentAllocatedGddram);
//		if (rcl == null) {
//			return time;
//		}
//		double timeSpan = currentTime - rcl.lastMemoryTransferTime;
//		rcl.updateTaskMemoryTransfer(timeSpan * allocatedBw);
//		if (rcl.getRemainingTaskMemoryTransfer() == 0) {
//			taskMemoryTransferFinish(rcl);
//			rcl = getTaskForMemoryTransfer(currentAllocatedGddram);
//			if (rcl == null) {
//				return time;
//			}
//		}
//		rcl.lastMemoryTransferTime = currentTime;
//		time = rcl.getRemainingTaskMemoryTransfer() / allocatedBw;
//		return time;
//	}
//
//	@Override
//	public void taskMemoryTransferFinish(ResGpuTask rcl) {
//		if (rcl.state == ResGpuTask.TransferState.H2D) {
//			rcl.setTaskMemoryTransferEndTime(CloudSim.clock());
//			getMemoryTransferList().remove(rcl);
//			getMemoryTransferFinishedList().add(rcl);
//
//		} else if (rcl.state == ResGpuTask.TransferState.D2H) {
//			rcl.setTaskMemoryTransferEndTime(CloudSim.clock());
//			getMemoryTransferList().remove(rcl);
//			getMemoryTransferFinishedList().add(rcl);
//			usedGddram -= rcl.getGpuTask().getRequestedGddramSize();
//			rcl.finalizeMemoryTransfers();
//			getTaskFinishedList().add(rcl);
//		}
//		rcl.lastMemoryTransferTime = 0;
//	}

}
