package gpu.provisioners;

import gpu.GpuTask;
import gpu.Pgpu;
import gpu.ResGpuCloudlet;
import gpu.ResGpuTask;
import javafx.util.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VideoCardBwProvisioner is an abstract class that represents the provisioning
 * policy used by a VideoCard to allocate PCIe bandwidth (bw) to Pgpus inside
 * it.
 * 
 * @author Ahmad Siavashi
 *
 */
public abstract class VideoCardBwProvisioner {

	/**
	 * The total PCIe bandwidth capacity from the video card that the provisioner
	 * can allocate to Pgpus.
	 */
	private long bw;

	protected double lastTime;

	/** videoCard-bw mapping */
	private Map<Pgpu, Long> pgpuBwMap;

	/** videoCard-requested bw mapping */
	private Map<Pgpu, Long> pgpuRequestedBwMap;

	protected List<RK> h2d;

	protected List<RK> d2h;

	public List<GpuTask> h2dFinal;

	public List<GpuTask> d2hFinal;

	public class RK {
		public GpuTask kernel;
		public long remain;
		public RK(GpuTask kernel, long remain) {
			this.kernel = kernel;
			this.remain = remain;
		}

	}

	/**
	 * Creates the new VideoCardBwProvisioner.
	 * 
	 * @param bw
	 *            The total PCIe bandwidth capacity from the video card that the
	 *            provisioner can allocate to Pgpus.
	 * 
	 * @pre bw >= 0
	 * @post $none
	 */
	public VideoCardBwProvisioner(long bw) {
		setBw(bw);
		setPgpuBwMap(new HashMap<Pgpu, Long>());
		setPgpuRequestedBwMap(new HashMap<Pgpu, Long>());
		h2d = new ArrayList<>();
		d2h = new ArrayList<>();
		h2dFinal = new ArrayList<>();
		d2hFinal = new ArrayList<>();
	}

	/**
	 * Allocates PCIe BW for a given Pgpu.
	 * 
	 * @param pgpu
	 *            the pgpu for which the PCIe bw is being allocated
	 * @param bw
	 *            the PCIe bw to be allocated to the pgpu
	 * 
	 * @return $true if the PCIe bw could be allocated; $false otherwise
	 * 
	 * @pre $none
	 * @post $none
	 */
	public abstract boolean allocateBwForPgpu(Pgpu pgpu, long bw);

	/**
	 * Gets the allocated PCIe BW for Pgpu.
	 * 
	 * @param pgpu
	 *            the pgpu
	 * 
	 * @return the allocated PCIe BW for the pgpu
	 */
	public long getAllocatedBwForPgpu(Pgpu Pgpu) {
		return getPgpuBwMap().get(Pgpu).longValue();
	}

	/**
	 * Releases PCIe BW used by a pgpu
	 * 
	 * @param pgpu
	 *            the pgpu
	 * 
	 * @pre $none
	 * @post none
	 */
	public void deallocateBwForPgpu(Pgpu pgpu) {
		getPgpuBwMap().remove(pgpu);
	}

	/**
	 * Releases PCIe BW used by all Pgpus.
	 * 
	 * @pre $none
	 * @post none
	 */
	public void deallocateBwForAllPgpus() {
		getPgpuBwMap().clear();
	}

	/**
	 * Gets the PCIe bw capacity.
	 * 
	 * @return the PCIe bw capacity
	 */
	public long getBw() {
		return bw;
	}

	/**
	 * Sets the PCIe bw capacity.
	 * 
	 * @param bw
	 *            the new PCIe bw capacity
	 */
	protected void setBw(long bw) {
		this.bw = bw;
	}

	/**
	 * Gets the available PCIe BW in the Pgpu.
	 * 
	 * @return available PCIe bw
	 * 
	 * @pre $none
	 * @post $none
	 */
	public abstract long getAvailableBw();

	/**
	 * @return the pgpuBwMap
	 */
	public Map<Pgpu, Long> getPgpuBwMap() {
		return pgpuBwMap;
	}

	/**
	 * @param pgpuBwMap
	 *            the pgpuBwMap to set
	 */
	public void setPgpuBwMap(Map<Pgpu, Long> pgpuBwMap) {
		this.pgpuBwMap = pgpuBwMap;
	}

	/**
	 * @return the pgpuRequestedBwMap
	 */
	public Map<Pgpu, Long> getPgpuRequestedBwMap() {
		return pgpuRequestedBwMap;
	}

	/**
	 * @param pgpuRequestedBwMap
	 *            the pgpuRequestedBwMap to set
	 */
	public void setPgpuRequestedBwMap(Map<Pgpu, Long> pgpuRequestedBwMap) {
		this.pgpuRequestedBwMap = pgpuRequestedBwMap;
	}

	public Double submitH2D(GpuTask kernel) {
		long input = kernel.getTaskInputSize();
		if(input == 0)
			return 0.0;
		ResGpuTask resGpuTask = new ResGpuTask(kernel);
		h2d.add(new RK(kernel, kernel.getTaskInputSize()));
		return (double)input / bw;
	}

	public Double submitD2H(GpuTask kernel) {
		long output = kernel.getTaskOutputSize();
		if(output == 0)
			return 0.0;
		ResGpuTask resGpuTask = new ResGpuTask(kernel);
		d2h.add(new RK(kernel, kernel.getTaskOutputSize()));
		return (double)output / bw;
	}

	public void  processTransfer(double current) {
		double timeSpam = current - lastTime;
		if(timeSpam <= 0)
			return;
		int h2dnum = h2d.size();
		int d2hnum = d2h.size();
		long bw = getBw();
		long h2dbw = getBw() / h2dnum;
		long d2hbw = getBw() / d2hnum;
		List<RK> toRemove = new ArrayList<>();
		for(RK pair: h2d) {
			long input = pair.remain;
			long remain = (long) (input - h2dbw * timeSpam);
			if(remain <= 0) {
				toRemove.add(pair);
			}else {
				pair.remain = remain;
			}
		}
		h2d.removeAll(toRemove);
		for(RK pair: toRemove)
			h2dFinal.add(pair.kernel);
		toRemove.clear();
		for(RK pair: d2h) {
			long output = pair.remain;
			long remain = (long) (output - d2hbw * timeSpam);
			if(remain <= 0) {
				toRemove.add(pair);
			}else {
				pair.remain = remain;
			}
		}
		d2h.removeAll(toRemove);
		for(RK pair: toRemove)
			d2hFinal.add(pair.kernel);
	}

}
