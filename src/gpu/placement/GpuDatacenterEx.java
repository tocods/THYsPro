package gpu.placement;

import gpu.GpuVm;
import gpu.GpuVmAllocationPolicy;
import gpu.Vgpu;
import gpu.VideoCard;
import gpu.core.GpuCloudSimTags;
import gpu.power.PowerGpuDatacenter;
import cloudsim.DatacenterCharacteristics;
import cloudsim.Log;
import cloudsim.Storage;
import cloudsim.VmAllocationPolicy;
import cloudsim.core.CloudSim;
import cloudsim.core.CloudSimTags;
import cloudsim.core.SimEvent;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * 
 * This class extends {@link PowerGpuDatacenter} to support placement window and
 * remote vGPUs. It must be used along with {@link GpuDatacenterBrokerEx} or its
 * subclasses.
 * 
 * @author Ahmad Siavashi
 *
 */
public class GpuDatacenterEx extends PowerGpuDatacenter {

	/**
	 * List of newly arrived VMs.
	 */
	private List<Entry<GpuVm, Boolean>> newVms;

	/**
	 * Denotes the size of aggregation window for placement.
	 */
	private double placementWindow;

	public GpuDatacenterEx(String name, DatacenterCharacteristics characteristics,
			VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList, double schedulingInterval,
			double placementWindow) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
		setNewVms(new ArrayList<Entry<GpuVm, Boolean>>());
		setPlacementWindow(placementWindow);
	}

	@Override
	protected void processVmCreate(SimEvent ev, boolean ack) {
		Entry<GpuVm, Boolean> newVm = new SimpleEntry<GpuVm, Boolean>((GpuVm) ev.getData(), ack);
		getNewVms().add(newVm);
	}
	
	@Override
	public void startEntity() {
		schedule(getId(), getSchedulingInterval(), GpuCloudSimTags.GPU_VM_DATACENTER_PLACEMENT);
		super.startEntity();
	}

	@Override
	protected void processOtherEvent(SimEvent ev) {
		super.processOtherEvent(ev);
		switch (ev.getTag()) {
		case GpuCloudSimTags.GPU_VM_DATACENTER_PLACEMENT:
			runPlacement(getNewVms());
			schedule(getId(), getPlacementWindow(), GpuCloudSimTags.GPU_VM_DATACENTER_PLACEMENT);
			break;
		}
	}

	protected void runPlacement(List<Entry<GpuVm, Boolean>> newVmList) {
		// Guard
		if (newVmList.isEmpty()) {
			return;
		}
		long startTime = System.nanoTime();
		Map<GpuVm, Boolean> results = ((GpuVmAllocationPolicy) getVmAllocationPolicy())
				.allocateHostForVms(newVmList.stream().map(x -> x.getKey()).collect(Collectors.toList()));
		long endTime = System.nanoTime();
		long durationMs = (endTime - startTime) / 1000000;
		System.out.println(
				"{'clock': " + CloudSim.clock() + ", 'type': 'placement duration', 'duration': " + durationMs + "}");
		for (Entry<GpuVm, Boolean> result : results.entrySet()) {
			processVmCreate(result.getKey(), true, result.getValue());
		}
		getNewVms().clear();
	}

	protected void processVmCreate(GpuVm vm, boolean ack, boolean result) {
		Log.printLine(CloudSim.clock() + ": Trying to Create VM #" + vm.getId() + " in " + getName());

		if (ack) {
			int[] data = new int[3];
			data[0] = getId();
			data[1] = vm.getId();

			if (result) {
				data[2] = CloudSimTags.TRUE;
			} else {
				data[2] = CloudSimTags.FALSE;
			}
			send(vm.getUserId(), CloudSim.getMinTimeBetweenEvents(), CloudSimTags.VM_CREATE_ACK, data);
		}

		if (result) {
			getVmList().add(vm);
			GpuVm gpuVm = (GpuVm) vm;
			Vgpu vgpu = gpuVm.getVgpu();

			if (vm.isBeingInstantiated()) {
				vm.setBeingInstantiated(false);
			}

			vm.updateVmProcessing(CloudSim.clock(),
					getVmAllocationPolicy().getHost(vm).getVmScheduler().getAllocatedMipsForVm(vm));

			if (vgpu != null) {
				if (vgpu.isBeingInstantiated()) {
					vgpu.setBeingInstantiated(false);
				}

				VideoCard videoCard = vgpu.getVideoCard();
				vgpu.updateGpuTaskProcessing(CloudSim.clock(),
						videoCard.getVgpuScheduler().getAllocatedMipsForVgpu(vgpu), null, null);
			}
		}

	}

	protected List<Entry<GpuVm, Boolean>> getNewVms() {
		return newVms;
	}

	protected void setNewVms(List<Entry<GpuVm, Boolean>> newVms) {
		this.newVms = newVms;
	}

	protected double getPlacementWindow() {
		return placementWindow;
	}

	public void setPlacementWindow(double placementWindow) {
		this.placementWindow = placementWindow;
	}

}
