package gpu.performance;

import gpu.*;
import gpu.performance.models.PerformanceModel;
import gpu.selection.PgpuSelectionPolicy;

import java.util.List;

/**
 * * {@link PerformanceVgpuSchedulerFairShareEx} extends
 * {@link VgpuSchedulerFairShareEx VgpuSchedulerFairShareEx}
 * to add support for
 * {@link PerformanceModel
 * PerformanceModels}.
 * 
 * @author Ahmad Siavashi
 * 
 */
public class PerformanceVgpuSchedulerFairShareEx extends VgpuSchedulerFairShareEx implements PerformanceScheduler<Vgpu> {
	/** The performance model */
	private PerformanceModel<VgpuScheduler, Vgpu> performanceModel;

	/**
	 * @see VgpuSchedulerFairShare#VgpuSchedulerFairShare(int,
	 *      List, PgpuSelectionPolicy) VgpuSchedulerFairShare(int, List,
	 *      PgpuSelectionPolicy)
	 * 
	 * @param performanceModel
	 *            the performance model
	 */
	public PerformanceVgpuSchedulerFairShareEx(String videoCardType, List<Pgpu> pgpuList,
			PgpuSelectionPolicy pgpuSelectionPolicy, PerformanceModel<VgpuScheduler, Vgpu> performanceModel) {
		super(videoCardType, pgpuList, pgpuSelectionPolicy);
		this.performanceModel = performanceModel;
	}

	@Override
	public List<Double> getAvailableMips(Vgpu vgpu, List<Vgpu> vgpuList) {
		return this.performanceModel.getAvailableMips(this, vgpu, vgpuList);
	}
}
