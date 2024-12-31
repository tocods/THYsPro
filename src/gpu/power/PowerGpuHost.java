package gpu.power;

import cloudsim.Log;
import cloudsim.ResCloudlet;
import cloudsim.core.CloudSim;
import gpu.*;
import gpu.allocation.VideoCardAllocationPolicy;
import gpu.performance.PerformanceGpuHost;
import gpu.power.models.GpuHostPowerModelNull;
import cloudsim.Pe;
import cloudsim.VmScheduler;
import cloudsim.power.models.PowerModel;
import cloudsim.provisioners.BwProvisioner;
import cloudsim.provisioners.RamProvisioner;
import org.apache.commons.lang3.ObjectUtils;
import workflow.GpuJob;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link PowerGpuHost} extends {@link PerformanceGpuHost} to represent a
 * power-aware host.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class PowerGpuHost extends PerformanceGpuHost {

	/** The power model associated with this host (video cards excluded) */
	private PowerModel powerModel;

	/**
	 * 
	 * @see PerformanceGpuHost#PerformanceGpuHost(int, int, RamProvisioner,
	 *      BwProvisioner, long, List, VmScheduler, VideoCardAllocationPolicy)
	 *      erformanceGpuHost(int, int, RamProvisioner, BwProvisioner, long, List,
	 *      VmScheduler, VideoCardAllocationPolicy)
	 * @param powerModel
	 *            the power model associated with the host (video cards have their
	 *            own power models)
	 */
	public PowerGpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner, long storage,
			List<? extends Pe> peList, VmScheduler vmScheduler, VideoCardAllocationPolicy videoCardAllocationPolicy,
			PowerModel powerModel) {
		super(id, type, ramProvisioner, bwProvisioner, storage, peList, vmScheduler, videoCardAllocationPolicy);
		setPowerModel(powerModel);
	}
	
	/**
	 * 
	 * @see PerformanceGpuHost#PerformanceGpuHost(int, int, RamProvisioner,
	 *      BwProvisioner, long, List, VmScheduler, VideoCardAllocationPolicy)
	 *      erformanceGpuHost(int, int, RamProvisioner, BwProvisioner, long, List,
	 *      VmScheduler, VideoCardAllocationPolicy)
	 */
	public PowerGpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner, long storage,
			List<? extends Pe> peList, VmScheduler vmScheduler, VideoCardAllocationPolicy videoCardAllocationPolicy) {
		super(id, type, ramProvisioner, bwProvisioner, storage, peList, vmScheduler, videoCardAllocationPolicy);
		setPowerModel(new GpuHostPowerModelNull());
	}

	/**
	 * 
	 * @see PerformanceGpuHost#PerformanceGpuHost(int, int, RamProvisioner,
	 *      BwProvisioner, long, List, VmScheduler) erformanceGpuHost(int, int,
	 *      RamProvisioner, BwProvisioner, long, List, VmScheduler)
	 */
	public PowerGpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner, long storage,
			List<? extends Pe> peList, VmScheduler vmScheduler, PowerModel powerModel) {
		super(id, type, ramProvisioner, bwProvisioner, storage, peList, vmScheduler);
		setPowerModel(powerModel);
	}
	
	/**
	 * 
	 * @see PerformanceGpuHost#PerformanceGpuHost(int, int, RamProvisioner,
	 *      BwProvisioner, long, List, VmScheduler) erformanceGpuHost(int, int,
	 *      RamProvisioner, BwProvisioner, long, List, VmScheduler)
	 */
	public PowerGpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner, long storage,
			List<? extends Pe> peList, VmScheduler vmScheduler) {
		super(id, type, ramProvisioner, bwProvisioner, storage, peList, vmScheduler);
		setPowerModel(new GpuHostPowerModelNull());
	}

	/**
	 * Returns the current total utilization of host's CPUs.
	 * 
	 * @return total utilization of host CPUs
	 **/
	@SuppressWarnings("unchecked")
	public double getCurrentCpuUtilization() {
//		double totalRequestedMips = 0.0;
//		for (GpuVm vm : (List<GpuVm>) (List<?>) getVmList()) {
//			totalRequestedMips += vm.getCurrentRequestedTotalMips();
//		}
//		return totalRequestedMips / getTotalMips();
		Integer peInUse = 0;
		for(ResCloudlet cl: getCloudletScheduler().getCloudletExecList()) {
			peInUse += cl.getNumberOfPes();
			//Log.printLine(((GpuCloudlet)cl.getCloudlet()).getName() + " CPU:" + cl.getNumberOfPes());
		}
		if(peInUse > getNumberOfPes())
			return 1;
		return (double) peInUse / getNumberOfPes();
	}

	public double getCurrentRamUtilization() {
		double ramInUse = 0;
		for(ResCloudlet cl: getCloudletScheduler().getCloudletExecList()) {
			ramInUse += ((GpuCloudlet)cl.getCloudlet()).getRam();
			//Log.printLine(((GpuCloudlet)cl.getCloudlet()).getName() + " 内存:" + cl.getRam());
		}
		if(ramInUse > getRam())
			return 1;
		return ramInUse / getRam();
	}

	public List<Double> getCurrentGpuUtilization() {
		List<Double> ret = new ArrayList<>();
		if(!isGpuEquipped()) {
			//Log.printLine("nono");
			ret.add(Double.MAX_VALUE);
			return ret;
		}
		for(VideoCard videoCard: getVideoCardAllocationPolicy().getVideoCards()) {
			//Log.printLine("yeye");
			for(Pgpu pgpu: videoCard.getPgpuList()) {
				ret.add(((GpuTaskSchedulerLeftover)pgpu.getGpuTaskScheduler()).getGPUUtil());
			}
		}
		return ret;
	}


	/**
	 * Returns the current total power consumption of the host (CPUs + GPUs).
	 * 
	 * @return current total power consumption of the host
	 */
	public double getCurrentTotalPower() {
		double totalPower = 0;
		totalPower += getCurrentHostCpuPower();
		for (Double videoCardPower : getCurrentVideoCardsPower().values()) {
			totalPower += videoCardPower;
		}
		return totalPower;
	}

	/**
	 * Returns the current power consumption of host's CPUs
	 * 
	 * @return current power consumption of host's CPUs
	 */
	public double getCurrentHostCpuPower() {
		return getPowerModel().getPower(getCurrentCpuUtilization());
	}

	/**
	 * Returns the current power consumption of host's video cards
	 * 
	 * @return the current power consumption of host's video cards
	 */
	public Map<VideoCard, Double> getCurrentVideoCardsPower() {
		Map<VideoCard, Double> videoCardsPower = new HashMap<VideoCard, Double>();
		if (isGpuEquipped()) {
			for (VideoCard videoCard : getVideoCardAllocationPolicy().getVideoCards()) {
				PowerVideoCard powerVideoCard = (PowerVideoCard) videoCard;
				videoCardsPower.put(powerVideoCard, powerVideoCard.getPower());
			}
		}
		return videoCardsPower;
	}

	/**
	 * @return the powerModel
	 */
	public PowerModel getPowerModel() {
		return powerModel;
	}

	/**
	 * @param powerModel
	 *            the powerModel to set
	 */
	protected void setPowerModel(PowerModel powerModel) {
		this.powerModel = powerModel;
	}

}
