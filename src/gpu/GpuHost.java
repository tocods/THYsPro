package gpu;

import cloudsim.core.CloudSim;
import gpu.allocation.VideoCardAllocationPolicy;
import cloudsim.*;
import cloudsim.provisioners.BwProvisioner;
import cloudsim.provisioners.RamProvisioner;
import org.apache.commons.compress.archivers.dump.DumpArchiveEntry;
import workflow.GpuJob;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 
 * {@link GpuHost} extends {@link Host} and supports {@link VideoCard}s through
 * a {@link VideoCardAllocationPolicy}.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuHost extends Host {

	/**
	 * type of the host
	 */
	public String type;

	/** video card allocation policy */
	private VideoCardAllocationPolicy videoCardAllocationPolicy;

	private Double failTime = 0.0;

	private List<Double> failTimes = new ArrayList<>();

	private Double lastFailTime = 0.0;

	/**
	 * 
	 * See {@link Host#Host}
	 * 
	 * @param type                      type of the host which is specified in
	 *                                  {@link GpuHostTags}.
	 * @param videoCardAllocationPolicy the policy in which the host allocates video
	 *                                  cards to vms
	 */
	public GpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner, long storage,
			List<? extends Pe> peList, VmScheduler vmScheduler, VideoCardAllocationPolicy videoCardAllocationPolicy) {
		super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler);
		setType(type);
		setVideoCardAllocationPolicy(videoCardAllocationPolicy);
	}

	/**
	 * 
	 * See {@link Host#Host}
	 * 
	 * @param type type of the host which is specified in {@link GpuHostTags}.
	 */
	public GpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner, long storage,
			List<? extends Pe> peList, VmScheduler vmScheduler) {
		super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler);
		setType(type);
		setVideoCardAllocationPolicy(null);
	}

	/**
	 * 遍历每张显卡上的每个 pGPU，调用 pGPU 上的任务调度器更新任务
	 * @param currentTime
	 * @return
	 */
	public double updatePgpuProcessing(double currentTime) {
		double smallerTime = Double.MAX_VALUE;
		if (isGpuEquipped()) {
			// 调用 pGPU 上的任务调度器更新任务
			for (VideoCard videoCard : getVideoCardAllocationPolicy().getVideoCards()) {
				// 遍历每张显卡上的每个 pGPU
				for(Pgpu pgpu : videoCard.getPgpuList()) {
					double time = pgpu.updateGpuTaskProcessing(currentTime);
					if (time > 0.0 && time < smallerTime) {
						smallerTime = time;
					}
				}
			}
		}
		return smallerTime;
	}

	public Double getFailTime() {
		return failTime;
	}

	public void addFailTime() {
		this.failTime += CloudSim.clock() - lastFailTime;
		this.failTimes.add(CloudSim.clock() - lastFailTime);
	}

	public List<Double> getFailTimes() {
		return failTimes;
	}

	public void hostFail(Boolean ifGpu) {
		lastFailTime = CloudSim.clock();
		getCloudletScheduler().hostFail(ifGpu);
		if(isGpuEquipped()) {
			for(VideoCard videoCard: getVideoCardAllocationPolicy().getVideoCards()) {
				for(Pgpu pgpu: videoCard.getPgpuList()) {
					pgpu.getGpuTaskScheduler().hostFail();
				}
			}
		}
	}

	public String jobFailType(GpuJob job) {
		String ret = getCloudletScheduler().jobFailType(job);
		if(isGpuEquipped()) {
			for(VideoCard videoCard: getVideoCardAllocationPolicy().getVideoCards()) {
				for(Pgpu pgpu: videoCard.getPgpuList()) {
					String type = pgpu.getGpuTaskScheduler().jobFailType(job);
					if(ret == "" && type != "") {
						ret = type;
					}
				}
			}
		}
		if(ret == "") {
			ret = "FAIL";
		}
		return ret;
	}

	public long jobFail(GpuJob job) {
		long rets = getCloudletScheduler().jobFail(job);
		if(isGpuEquipped()) {
			for(VideoCard videoCard: getVideoCardAllocationPolicy().getVideoCards()) {
				for(Pgpu pgpu: videoCard.getPgpuList()) {
					rets += pgpu.getGpuTaskScheduler().jobFail(job);
				}
			}
		}return rets;
	}
	public void notifyGpuTaskCompletion(GpuTask t) {
		//Log.printLine("aaasa");
		GpuCloudletScheduler cloudletScheduler = (GpuCloudletScheduler) getCloudletScheduler();
		cloudletScheduler.notifyGpuTaskCompletion(t);
	}

	public List<ResGpuTask> checkGpuTaskCompletion() {
		List<ResGpuTask> ret = new ArrayList<>();
		for (VideoCard videoCard : getVideoCardAllocationPolicy().getVideoCards()) {
			// 遍历每张显卡上的每个 pGPU
			for(Pgpu pgpu : videoCard.getPgpuList()) {
				while(pgpu.getGpuTaskScheduler().hasFinishedTasks()) {
					ret.add(pgpu.getGpuTaskScheduler().getNextFinishedTask());
				}
			}
		}
		return ret;
	}

	public boolean hasGpuTask(){
		return ((GpuCloudletScheduler)getCloudletScheduler()).hasGpuTask();
	}

	public GpuTask getNextGpuTask() {
		return ((GpuCloudletScheduler)getCloudletScheduler()).getNextGpuTask();
	}

	public double updateVgpusProcessing(double currentTime) {
		double smallerTime = Double.MAX_VALUE;
		if (isGpuEquipped()) {
			// Update resident vGPUs
			for (Vgpu vgpu : getVideoCardAllocationPolicy().getVgpuVideoCardMap().keySet()) {
				double time = vgpu.updateGpuTaskProcessing(currentTime, getVideoCardAllocationPolicy()
						.getVgpuVideoCardMap().get(vgpu).getVgpuScheduler().getAllocatedMipsForVgpu(vgpu), null, null);
				if (time > 0.0 && time < smallerTime) {
					smallerTime = time;
				}
			}
		}
		return smallerTime;
	}

	@Override
	public boolean isSuitableForVm(Vm vm) {
		boolean result = vmCreate(vm);
		if (result) {
			vmDestroy(vm);
		}
		return result;
	}

	/**
	 * @return the videoCardAllocationPolicy
	 */
	public VideoCardAllocationPolicy getVideoCardAllocationPolicy() {
		return videoCardAllocationPolicy;
	}

	/**
	 * @param videoCardAllocationPolicy the videoCardAllocationPolicy to set
	 */
	public void setVideoCardAllocationPolicy(VideoCardAllocationPolicy videoCardAllocationPolicy) {
		this.videoCardAllocationPolicy = videoCardAllocationPolicy;
	}

	/**
	 * Checks the existence of a given video card id in the host
	 * 
	 * @param videoCardId id of the video card
	 * @return
	 */
	public boolean hasVideoCard(int videoCardId) {
		if (!isGpuEquipped()) {
			return false;
		}
		for (VideoCard videoCard : getVideoCardAllocationPolicy().getVideoCards()) {
			if (videoCard.getId() == videoCardId) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks the existence of a given pgpu id in the host
	 * 
	 * @param pgpuId id of the video card
	 * @return
	 */
	public boolean hasPgpu(int pgpuId) {
		if (!isGpuEquipped()) {
			return false;
		}
		for (VideoCard videoCard : getVideoCardAllocationPolicy().getVideoCards()) {
			for (Pgpu pgpu : videoCard.getVgpuScheduler().getPgpuList()) {
				if (pgpu.getId() == pgpuId) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isGpuEquipped() {
		return getVideoCardAllocationPolicy() != null && !getVideoCardAllocationPolicy().getVideoCards().isEmpty();
	}

	public void vgpuDestroy(Vgpu vgpu) {
		if (vgpu != null) {
			getVideoCardAllocationPolicy().deallocate(vgpu);
		}
	}

	public boolean vgpuCreate(Vgpu vgpu) {
		return getVideoCardAllocationPolicy().allocate(vgpu, vgpu.getPCIeBw());
	}

	public boolean vgpuCreate(Vgpu vgpu, Pgpu pgpu) {
		return getVideoCardAllocationPolicy().allocate(pgpu, vgpu, vgpu.getPCIeBw());
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 */
	protected void setType(String type) {
		this.type = type;
	}

	public Set<Vgpu> getVgpuSet() {
		if (!isGpuEquipped()) {
			return null;
		}
		return getVideoCardAllocationPolicy().getVgpuVideoCardMap().keySet();
	}

	@Override
	public double submitJob(GpuJob job) {
//		// 部署了GPU，正常提交任务
//		if(isGpuEquipped()) {
//			return getCloudletScheduler().cloudletSubmit(job);
//		}
//		// 未部署GPU，本该由GPU执行的部分只能有CPU来执行
//		return ((GpuCloudletSchedulerTimeShared)getCloudletScheduler()).cloudletSubmitWithoutGPU(job, 0);
		return getCloudletScheduler().cloudletSubmit(job);
	}

	public boolean isIdle() {

		if (!getVmList().isEmpty()) {
			return false;
		} else if (getVgpuSet() != null && !getVgpuSet().isEmpty()) {
			return false;
		}

		return true;
	}
}
