package gpu.selection;

import gpu.Pgpu;
import gpu.Vgpu;
import gpu.VgpuScheduler;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * {@link PgpuSelectionPolicyLeastLoad} implements {@link PgpuSelectionPolicy}
 * and selects the Pgpu with the maximum available memory.
 * 
 * @author Ahmad Siavashi
 *
 */
public class PgpuSelectionPolicyLeastLoad implements PgpuSelectionPolicy {

	public PgpuSelectionPolicyLeastLoad() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cloudsim.gpu.selection.PgpuSelectionPolicy#selectPgpu(org.
	 * cloudbus.cloudsim.gpu.VgpuScheduler, java.util.List)
	 */
	@Override
	public Pgpu selectPgpu(Vgpu vgpu, VgpuScheduler scheduler, List<Pgpu> pgpuList) {
		if (pgpuList.isEmpty()) {
			return null;
		}
		return Collections.max(pgpuList, new Comparator<Pgpu>() {
			@Override
			public int compare(Pgpu pgpu1, Pgpu pgpu2) {
				int pgpu1AvailableMemory = pgpu1.getGddramProvisioner().getAvailableGddram();
				int pgpu2AvailableMemory = pgpu2.getGddramProvisioner().getAvailableGddram();
				return Integer.compare(pgpu1AvailableMemory, pgpu2AvailableMemory);
			}
		});
	}

}
