package gpu.power;

import gpu.GpuDatacenterBroker;
import gpu.core.GpuCloudSimTags;
import cloudsim.core.CloudSim;
import cloudsim.core.predicates.PredicateType;

/**
 * {@link PowerGpuDatacenterBroker} extends {@link GpuDatacenterBroker} to
 * handle extra power-events that occur in the simulation.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class PowerGpuDatacenterBroker extends GpuDatacenterBroker {

	/**
	 * @see GpuDatacenterBroker#GpuDatacenterBroker(String)
	 */
	public PowerGpuDatacenterBroker(String name) throws Exception {
		super(name);
	}

	@Override
	protected void finishExecution() {
		for (Integer datacenterId : getDatacenterIdsList()) {
			CloudSim.cancelAll(datacenterId.intValue(),
					new PredicateType(GpuCloudSimTags.GPU_VM_DATACENTER_POWER_EVENT));
		}
		super.finishExecution();
	}

}
