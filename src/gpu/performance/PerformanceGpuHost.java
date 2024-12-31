package gpu.performance;

import gpu.GpuHost;
import gpu.Vgpu;
import gpu.allocation.VideoCardAllocationPolicy;
import cloudsim.Pe;
import cloudsim.VmScheduler;
import cloudsim.provisioners.BwProvisioner;
import cloudsim.provisioners.RamProvisioner;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link PerformanceGpuHost} extends {@link GpuHost} to add support for
 * schedulers that implement {@link PerformanceScheduler PerformanceScheduler}
 * interface.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class PerformanceGpuHost extends GpuHost {

	/**
	 * @see GpuHost#GpuHost GpuHost
	 */
	public PerformanceGpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner,
			long storage, List<? extends Pe> peList, VmScheduler vmScheduler,
			VideoCardAllocationPolicy videoCardAllocationPolicy) {
		super(id, type, ramProvisioner, bwProvisioner, storage, peList, vmScheduler, videoCardAllocationPolicy);
	}

	/**
	 * @see GpuHost#GpuHost GpuHost
	 */
	public PerformanceGpuHost(int id, String type, RamProvisioner ramProvisioner, BwProvisioner bwProvisioner,
			long storage, List<? extends Pe> peList, VmScheduler vmScheduler) {
		super(id, type, ramProvisioner, bwProvisioner, storage, peList, vmScheduler);
	}

	@Override
	public double updateVgpusProcessing(double currentTime) {
		double smallerTime = Double.MAX_VALUE;

		if (isGpuEquipped()) {
			List<Vgpu> runningVgpus = new ArrayList<Vgpu>();
			for (Vgpu vgpu : getVideoCardAllocationPolicy().getVgpuVideoCardMap().keySet()) {
				if (vgpu.getGpuTaskScheduler().runningTasks() > 0) {
					runningVgpus.add(vgpu);
				}
			}
			for (Vgpu vgpu : getVideoCardAllocationPolicy().getVgpuVideoCardMap().keySet()) {
				@SuppressWarnings("unchecked")
				PerformanceScheduler<Vgpu> vgpuScheduler = (PerformanceScheduler<Vgpu>) getVideoCardAllocationPolicy()
						.getVgpuVideoCardMap().get(vgpu).getVgpuScheduler();
				double time = vgpu.updateGpuTaskProcessing(currentTime,
						vgpuScheduler.getAvailableMips(vgpu, runningVgpus), null, null);
				if (time > 0.0 && time < smallerTime) {
					smallerTime = time;
				}
			}
		}

		return smallerTime;
	}

}
