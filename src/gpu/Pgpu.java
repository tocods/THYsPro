/**
 * 
 */
package gpu;

import gpu.provisioners.GpuBwProvisioner;
import gpu.provisioners.GpuGddramProvisioner;
import cloudsim.Pe;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Represents a physical GPU inside a video card.
 * 
 * @author Ahmad Siavashi
 *
 */
public class Pgpu {

	/**
	 * Pgpu的Id
	 */
	private int id;
	
	/**
	 * Pgpu的类型
	 */
	private String type;

	/**
	 * Pgpu的PE列表
	 */
	private List<Pe> peList;
	private List<Double> mips;

	private List<Double> imips;

	private List<Double> mmips;

	private Integer maxBlockPerSM;

	private Integer corePerSM;

	/**
	 * 任务调度器负责调度Pgpu上的任务 {@link GpuTask GpuTasks}.
	 */
	private GpuTaskScheduler gpuTaskScheduler;

	/**
	 * GPU'的GDDRAM负责人
	 */
	private GpuGddramProvisioner gddramProvisioner;
	/**
	 * GPU的GDDRAM带宽负责人
	 */
	private GpuBwProvisioner bwProvisioner;

	/**
	 * @param id
	 *            Pgpu id
	 * @param pes
	 *            list of Pgpu's processing elements
	 */
	public Pgpu(int id, String type, List<Pe> pes, GpuGddramProvisioner gddramProvisioner, GpuBwProvisioner bwProvisioner) {
		super();
		setId(id);
		setType(type);
		setPeList(pes);
		setGddramProvisioner(gddramProvisioner);
		setBwProvisioner(bwProvisioner);
		initialMips();
	}

	public void setCorePerSM(Integer corePerSM) {
		this.corePerSM = corePerSM;
	}

	public Integer getCorePerSM() {
		return corePerSM;
	}

	public Integer getMaxBlockPerSM() {
		return maxBlockPerSM;
	}

	public void setMaxBlockPerSM(Integer maxBlockPerSM) {
		this.maxBlockPerSM = maxBlockPerSM;
	}

	private void initialMips() {
		mips = new ArrayList<>();
		for (Pe pe : peList) {
			mips.add((double) pe.getMips());
		}
		imips = new ArrayList<>();
		for (Pe pe : peList) {
			imips.add((double) pe.getIMips());
		}
		mmips = new ArrayList<>();
		for (Pe pe : peList) {
			mmips.add((double) pe.getMMips());
		}
	}

	/**
	 * 在GPU上设置任务的调度器
	 * @param gpuTaskScheduler 要设置的GPU任务调度器
	 */
	public void setGpuTaskScheduler(GpuTaskScheduler gpuTaskScheduler) {
		this.gpuTaskScheduler = gpuTaskScheduler;
	}

	/**
	 * 获取GPU上的任务调度器
	 * @return GPU上的任务调度器
	 */
	public GpuTaskScheduler getGpuTaskScheduler() {
		return gpuTaskScheduler;
	}

	public double updateGpuTaskProcessing(double currentTime) {
		return getGpuTaskScheduler().updateGpuTaskProcessing(currentTime, mips, imips, mmips);
	}

	public int getId() {
		return id;
	}

	protected void setId(int id) {
		this.id = id;
	}

	public List<Pe> getPeList() {
		return peList;
	}

	protected void setPeList(List<Pe> peList) {
		this.peList = peList;
	}

	/**
	 * @return the gddramProvisioner
	 */
	public GpuGddramProvisioner getGddramProvisioner() {
		return gddramProvisioner;
	}

	/**
	 * @param gddramProvisioner
	 *            the gddramProvisioner to set
	 */
	public void setGddramProvisioner(GpuGddramProvisioner gddramProvisioner) {
		this.gddramProvisioner = gddramProvisioner;
	}

	/**
	 * @return the bwProvisioner
	 */
	public GpuBwProvisioner getBwProvisioner() {
		return bwProvisioner;
	}

	/**
	 * @param bwProvisioner
	 *            the bwProvisioner to set
	 */
	public void setBwProvisioner(GpuBwProvisioner bwProvisioner) {
		this.bwProvisioner = bwProvisioner;
	}

	public String getType() {
		return type;
	}

	protected void setType(String type) {
		this.type = type;
	}

}
