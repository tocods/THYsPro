package gpu.selection;

import gpu.Pgpu;
import gpu.Vgpu;
import gpu.VgpuScheduler;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * {@link PgpuSelectionPolicyBreadthFirst} implements
 * {@link PgpuSelectionPolicy} and selects the Pgpu with the fewest number of
 * allocated Vgpus.
 * 
 * @author Ahmad Siavashi
 *
 */
public class PgpuSelectionPolicyBreadthFirst implements PgpuSelectionPolicy {

	public PgpuSelectionPolicyBreadthFirst() {
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
		return Collections.min(pgpuList, new Comparator<Pgpu>() {
			@Override
			public int compare(Pgpu pgpu1, Pgpu pgpu2) {
				Integer numPgpu1Vgpus = scheduler.getPgpuVgpuMap().get(pgpu1).size();
				Integer numPgpu2Vgpus = scheduler.getPgpuVgpuMap().get(pgpu2).size();
				return Integer.compare(numPgpu1Vgpus, numPgpu2Vgpus);
			}
		});
	}

}
