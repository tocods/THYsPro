package gpu.core;

import gpu.GpuVm;
import gpu.power.PowerGpuDatacenter;
import gpu.remote.RemoteGpuDatacenterEx;

/**
 * Contains Gpu-related events in the simulator.
 * {@link cloudsim.core.CloudSimTags} cannot be extended due to its
 * private constructor.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class GpuCloudSimTags {

	/**
	 * Denotes an event to submit a GpuTask for execution.
	 */
	public final static int GPU_TASK_SUBMIT = 49;

	/**
	 * Denotes an internal event in the GpuDatacenter. Updates the progress of
	 * executions.
	 */
	public final static int VGPU_DATACENTER_EVENT = 50;

	/**
	 * Denotes an event to evaluate the power consumption of a
	 * {@link PowerGpuDatacenter
	 * PowerGpuDatacenter}.
	 */
	public final static int GPU_VM_DATACENTER_POWER_EVENT = 51;

	/**
	 * Denotes an event to perform a {@link GpuVm} placement in a
	 * {@link RemoteGpuDatacenterEx}.
	 */
	public final static int GPU_VM_DATACENTER_PLACEMENT = 52;

	/**
	 * Denotes an event to update GPU memory transfers.
	 */
	public final static int GPU_MEMORY_TRANSFER = 53;

	/**
	 * Denotes the return of a GpuCloudlet to the sender.
	 */
	public final static int GPU_CLOUDLET_RETURN = 54;

	public final static int GPU_TAG_LAST = 54;

}
