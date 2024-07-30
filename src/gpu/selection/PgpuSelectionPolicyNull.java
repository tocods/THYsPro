package gpu.selection;

import gpu.Pgpu;
import gpu.Vgpu;
import gpu.VgpuScheduler;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

public class PgpuSelectionPolicyNull implements PgpuSelectionPolicy {

	public PgpuSelectionPolicyNull() {
	}

	@Override
	public Pgpu selectPgpu(Vgpu vgpu, VgpuScheduler scheduler, List<Pgpu> pgpuList) {
		throw new NotImplementedException("");
	}

}
