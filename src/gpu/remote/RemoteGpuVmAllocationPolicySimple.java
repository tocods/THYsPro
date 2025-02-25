package gpu.remote;

import gpu.GpuHost;
import gpu.GpuVm;
import gpu.GpuVmAllocationPolicySimple;
import gpu.Vgpu;
import cloudsim.Host;
import cloudsim.Vm;

import java.util.List;

/**
 * This class extends {@link RemoteGpuVmAllocationPolicy} and implements
 * first-fit allocation policy.
 * 
 * @author Ahmad Siavashi
 *
 */
public class RemoteGpuVmAllocationPolicySimple extends RemoteGpuVmAllocationPolicy {

	/**
	 * This class extends {@link RemoteGpuVmAllocationPolicy} and implements
	 * first-fit allocation policy.
	 * 
	 * @see {@link GpuVmAllocationPolicySimple}
	 */
	public RemoteGpuVmAllocationPolicySimple(List<? extends Host> list) {
		super(list);
	}

	@Override
	public boolean allocateHostForVm(Vm vm) {
		if (!getVmTable().containsKey(vm.getUid())) {
			Vgpu vgpu = ((GpuVm) vm).getVgpu();
			if (vgpu == null) {
				for (Host host : getHostList()) {
					boolean result = allocateHostForVm(vm, host);
					if (result) {
						return true;
					}
				}
			} else if (RemoteVgpuTags.isLocal(vgpu)) {
				for (GpuHost host : getGpuHostList()) {
					boolean result = allocateHostForVm(vm, host);
					if (result) {
						if (allocateGpuForVgpu(vgpu, host)) {
							return true;
						}
						deallocateHostForVm(vm);
					}
				}
			} else {
				// vGPU allocation
				boolean isVgpuAllocated = false;
				for (GpuHost gpuHost : getGpuHostList()) {
					if (allocateGpuForVgpu(vgpu, gpuHost)) {
						isVgpuAllocated = true;
						break;
					}
				}
				if (!isVgpuAllocated) {
					return false;
				}
				for (Host host : getHostList()) {
					if (allocateHostForVm(vm, host)) {
						return true;
					}
				}
				deallocateGpuForVgpu(vgpu);
			}
		}
		return false;
	}
}