package api.service;

import api.info.JobInfo;
import api.info.JobRunningInfo;
import api.info.TaskRunInfo;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import faulttolerant.FaultRecord;
import faulttolerant.GPUWorkflowFaultDatacenter;
import faulttolerant.GPUWorkflowFaultEngine;
import faulttolerant.faultGenerator.FaultGenerator;
import faulttolerant.faultGenerator.NormalGenerator;
import gpu.*;
import gpu.allocation.VideoCardAllocationPolicy;
import gpu.allocation.VideoCardAllocationPolicyNull;
import gpu.hardware_assisted.grid.*;
import gpu.performance.models.PerformanceModel;
import gpu.performance.models.PerformanceModelGpuConstant;
import gpu.power.PowerGpuHost;
import gpu.power.PowerVideoCard;
import gpu.power.models.GpuHostPowerModelLinear;
import gpu.power.models.VideoCardPowerModel;
import gpu.provisioners.GpuBwProvisionerShared;
import gpu.provisioners.GpuGddramProvisionerSimple;
import gpu.provisioners.VideoCardBwProvisioner;
import gpu.provisioners.VideoCardBwProvisionerShared;
import gpu.selection.PgpuSelectionPolicy;
import gpu.selection.PgpuSelectionPolicyNull;
import cloudsim.*;
import cloudsim.core.CloudSim;
import cloudsim.power.models.PowerModel;
import cloudsim.provisioners.BwProvisionerSimple;
import cloudsim.provisioners.PeProvisionerSimple;
import cloudsim.provisioners.RamProvisionerSimple;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import workflow.GPUWorkflowDatacenter;
import workflow.GPUWorkflowEngine;
import workflow.GpuJob;
import workflow.Kernel;
import workflow.jobAllocation.JobAllocationInterface;
import workflow.taskCluster.BasicClustering;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.text.DecimalFormat;
import java.util.*;

public class Service {
    private List<PowerGpuHost> hosts;
    private List<GpuCloudlet> tasks;

    private GPUWorkflowFaultDatacenter datacenter;

    private GPUWorkflowFaultEngine engine;

    private workflow.Parameters.JobAllocationAlgorithm algorithm;

    public Service() {
        algorithm = workflow.Parameters.JobAllocationAlgorithm.RR;
    }

    public Service(List<PowerGpuHost> hosts, List<GpuCloudlet> tasks) {
        algorithm = workflow.Parameters.JobAllocationAlgorithm.RR;
        this.hosts = new ArrayList<>(hosts);
        this.tasks = new ArrayList<>(tasks);
    }

    public void setHosts(List<PowerGpuHost> hostList) {
        this.hosts = hostList;
    }

    public void setTasks(List<GpuCloudlet> taskList) {
        this.tasks = taskList;
    }

    public void setAlgorithm(workflow.Parameters.JobAllocationAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * 根据{@link PowerGpuHost}队列创建数据中心
     * @return 数据中心
     */
    private GPUWorkflowFaultDatacenter createDatacenter() {
        String arch = "x86";
        String os = "Linux";
        String vmm = "Horizen";
        double time_zone = +3.5;
        double cost = 0.0;
        double costPerMem = 0.00;
        double costPerStorage = 0.000;
        double costPerBw = 0.0;
        LinkedList<Storage> storageList = new LinkedList<Storage>();

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(arch, os, vmm, hosts, time_zone,
                cost, costPerMem, costPerStorage, costPerBw);

        GPUWorkflowFaultDatacenter datacenter = null;
        try {
            datacenter = new GPUWorkflowFaultDatacenter("Datacenter", characteristics,
                    new GridGpuVmAllocationPolicyBreadthFirst(hosts), storageList, 1);
            if(!faulttolerant.Parameters.host2FaultInject.isEmpty()) {
                datacenter.setFaultToleranceEnabled();
                for(Map.Entry<Host, FaultGenerator> h2f: faulttolerant.Parameters.host2FaultInject.entrySet()) {
                    datacenter.setHostFaultGenerator(h2f.getKey(), h2f.getValue());
                }
            }
        } catch (Exception e) {
            return null;
        }
        return datacenter;
    }

    /**
     * 根据{@link GpuCloudlet}队列创建工作流引擎
     * @return 工作流引擎
     */
    private GPUWorkflowFaultEngine createBroker() {
        GPUWorkflowFaultEngine broker = null;
        try {
            broker = new GPUWorkflowFaultEngine("Broker");
        } catch (Exception e) {
            return null;
        }
        if(!faulttolerant.Parameters.job2FaultInject.isEmpty()) {
            datacenter.setFaultToleranceEnabled();
            for(Map.Entry<String, FaultGenerator> j2f: faulttolerant.Parameters.job2FaultInject.entrySet()) {
                datacenter.setJobFaultGenerator(j2f.getKey(), j2f.getValue());
            }
        }
        return broker;
    }


    public String start(String outputPath){
        Log.printLine(String.join("", Collections.nCopies(100, "-")));
        Log.printLine("开始仿真");
        try {
            int num_user = 1;
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = true;
            CloudSim.init(num_user, calendar, trace_flag);

            datacenter = createDatacenter();
            assert datacenter != null;
            //Log.printLine(datacenter.getId());
            engine = createBroker();
            assert engine != null;
            engine.submitVmList(new ArrayList<>());
            engine.initJobAllocationInterface(hosts, algorithm);
            int brokerId = engine.getId();
            for(GpuCloudlet t: tasks) {
                t.setUserId(brokerId);
                t.setVmId(hosts.get(0).getId());
            }
            engine.submitCloudletList(tasks);

            //Log.disable();
            CloudSim.startSimulation();
            CloudSim.stopSimulation();
            //Log.enable();
            outputXml(outputPath);
            return startShowResult(engine.getCloudletReceivedList(), outputPath);

        } catch (Exception e) {
            Log.printLine("仿真出现错误:\n");
            e.printStackTrace();
            return "仿真出现错误";
        }
    }

    private void outputXml(String path) throws IOException {
        Log.printLine("输出XML文件");
        String cpuPath = path + "\\hostUtils.xml";
        java.io.File file = new File(cpuPath);
        if(!file.exists()) {
            file.getParentFile().mkdir();
        }
        Element root = new Element("hostUtilization");
        Document doc = new Document(root);
        Element r = null;
        int i = 0;
        DecimalFormat dft = new DecimalFormat("###.##");
        while(true) {
            if(i >= Parameters.endTime.intValue())
                break;
            r = new Element("Util");
            r.setAttribute("time", String.valueOf(i));
            for(PowerGpuHost h: hosts) {
                //Log.printLine(h.getName());
                Element t = new Element("Host");
                t.setAttribute("name", h.getName());
                t.setAttribute("cpuUtilization", dft.format(datacenter.getCpuUtilOfHost(h).get(i)));
                t.setAttribute("ramUtilization", dft.format(datacenter.getRamUtilOfHost(h).get(i)));
                for(Map.Entry<Integer, List<Double>> entry: datacenter.getGpuUtilOfHost(h).entrySet()) {
                    Element gpu = new Element("gpuUtilization");
                    gpu.setAttribute("id", entry.getKey().toString());
                    gpu.setAttribute("gpu", dft.format(entry.getValue().get(i)));
                    t.addContent(gpu);
                }
                r.addContent(t);
            }
            doc.getRootElement().addContent(r);
            i++;
        }
        XMLOutputter xmlOutput = new XMLOutputter();
        Format f = Format.getRawFormat();
        f.setIndent("  "); // 文本缩进
        f.setTextMode(Format.TextMode.TRIM_FULL_WHITE);
        xmlOutput.setFormat(f);

        // 把xml文件输出到指定的位置
        xmlOutput.output(doc, new FileOutputStream(file));

        String faultRecordPath = path + "\\faultRecords.xml";
        file = new File(faultRecordPath);
        if(!file.exists()) {
            file.getParentFile().mkdir();
        }
        root = new Element("faultRecord");
        doc = new Document(root);
        r = null;
        for(FaultRecord record: faulttolerant.Parameters.faultRecordList) {
            r = new Element("faultRecord");
            r.setAttribute("time", record.time);
            r.setAttribute("object", record.fault);
            r.setAttribute("isFalseAlarm", record.ifFalseAlarm);
            doc.getRootElement().addContent(r);
            i++;
        }
        xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(f);

        // 把xml文件输出到指定的位置
        xmlOutput.output(doc, new FileOutputStream(file));
        Log.printLine("文件成功输入至 " + path);
        Log.printLine(String.join("", Collections.nCopies(100, "-")));
    }

    /**
     * 在终端显示仿真结果
     * @param jobs 任务队列
     */
    private String startShowResult(List<Cloudlet> jobs, String path) throws IOException {
        String cpuPath = path + "\\jobRun.xml";
        java.io.File file = new File(cpuPath);
        if(!file.exists()) {
            file.getParentFile().mkdir();
        }
        Element root = new Element("jobRun");
        Document doc = new Document(root);
        Element r = null;
        Map<String, List<JobRunningInfo>> jobRecord = new HashMap<>();
        Log.printLine(String.join("", Collections.nCopies(100, "-")));
        Log.printLine("展示结果");
        DecimalFormat dft = new DecimalFormat("###.##");
        DecimalFormat dftTransfer = new DecimalFormat("###.####");
        AsciiTable at = new AsciiTable();
        for(Cloudlet cl: jobs) {
            GpuJob gpuJob = (GpuJob) cl;
            JobRunningInfo info = new JobRunningInfo();
            at.addRule();
            at.addRow("Job", "Status", "Host", "Duration", "Start", "End");
            at.addRule();
            String jobStatus = "Success";
            if(gpuJob.getStatus() == Cloudlet.FAILED)
                jobStatus = "Fail";
            if(gpuJob.getStatus() == Cloudlet.CANCELED)
                jobStatus = "Canceled";
            info.status = jobStatus;
            at.addRow(gpuJob.getName(), jobStatus, gpuJob.getHost().getName(),
                    dft.format(Math.max(-1, gpuJob.getActualCPUTime())),
                    dft.format(gpuJob.getExecStartTime()),
                    dft.format(gpuJob.getFinishTime()));
            info.duration =  dft.format(Math.max(-1, gpuJob.getActualCPUTime()));
            info.start = dft.format(gpuJob.getExecStartTime());
            info.end = dft.format(gpuJob.getFinishTime());
            info.host = gpuJob.getHost().getName();
            at.addRule();
            if (gpuJob.getTasks().get(0).getGpuTask() == null) {
                if(!jobRecord.containsKey(gpuJob.getName())) {
                    List<JobRunningInfo> records = new ArrayList<>();
                    records.add(info);
                    jobRecord.put(gpuJob.getName(), records);
                } else {
                    jobRecord.get(gpuJob.getName()).add(info);
                }
                continue;
            }
            at.addRow("Kernel", "H2D","D2H", "Duration", "Start", "End");
            //at.addRule();
            // 遍历任务的每一个内核
            for(GpuCloudlet gpuCloudlet : gpuJob.getTasks()) {
                TaskRunInfo taskRunInfo = new TaskRunInfo();
                GpuTask gpuTask = gpuCloudlet.getGpuTask();
                if (gpuTask == null) {
                    continue;
                }
                at.addRule();
                at.addRow(gpuTask.getName(), dftTransfer.format(gpuTask.getH2d()), dftTransfer.format(gpuTask.getD2h()),
                        dft.format(Math.max(-1, gpuTask.getActualGPUTime())),
                        dft.format(gpuTask.getExecStartTime()),
                        dft.format(gpuTask.getFinishTime()));
                taskRunInfo.d2h = dftTransfer.format(gpuTask.getD2h());
                taskRunInfo.h2d = dftTransfer.format(gpuTask.getH2d());
                taskRunInfo.end = dft.format(gpuTask.getFinishTime());
                taskRunInfo.start = dft.format(gpuTask.getExecStartTime());
                taskRunInfo.duration = dft.format(Math.max(-1, gpuTask.getActualGPUTime()));
                info.runInfos.add(taskRunInfo);
            }
            at.addRule();
            if(!jobRecord.containsKey(gpuJob.getName())) {
                List<JobRunningInfo> records = new ArrayList<>();
                records.add(info);
                jobRecord.put(gpuJob.getName(), records);
            } else {
                jobRecord.get(gpuJob.getName()).add(info);
            }
        }

        at.setTextAlignment(TextAlignment.CENTER);
        Log.printLine(at.render());
        Log.printLine("任务群总完成时间: " + dft.format(Parameters.endTime));
        Log.printLine(String.join("", Collections.nCopies(100, "-")));

        for(Map.Entry<String, List<JobRunningInfo>> record: jobRecord.entrySet()) {
            r = new Element("Job");
            r.setAttribute("name", record.getKey());
            for(JobRunningInfo info: record.getValue()) {
                Element t = new Element("RunningRecord");
                t.setAttribute("status", info.status);
                t.setAttribute("host", info.host);
                t.setAttribute("start", info.start);
                t.setAttribute("end", info.end);
                t.setAttribute("duration", info.duration);
                int id = 0;
                for(TaskRunInfo entry: info.runInfos) {
                    Element gpu = new Element("KernelRecord");
                    gpu.setAttribute("id", String.valueOf(id));
                    gpu.setAttribute("h2d", entry.h2d);
                    gpu.setAttribute("d2h", entry.d2h);
                    gpu.setAttribute("start", entry.start);
                    gpu.setAttribute("end", entry.end);
                    gpu.setAttribute("duration", entry.duration);
                    t.addContent(gpu);
                    id++;
                }
                r.addContent(t);
            }
            doc.getRootElement().addContent(r);
        }
        XMLOutputter xmlOutput = new XMLOutputter();
        Format f = Format.getRawFormat();
        f.setIndent("  "); // 文本缩进
        f.setTextMode(Format.TextMode.TRIM_FULL_WHITE);
        xmlOutput.setFormat(f);

        // 把xml文件输出到指定的位置
        xmlOutput.output(doc, new FileOutputStream(file));
        return at.render();
    }

    //*********************************************************************************
    //*                         以下内容用于调试 Service                                 *
    //*********************************************************************************

    public static void main(String[] args) {
        List<PowerGpuHost> hostList = new ArrayList<>();
        for(int i = 0; i < 1; i++)
            hostList.add(createHosts(i));
        Service s = new Service(hostList, createTasks());
        String outPath = System.getProperty("user.dir") + "\\OutputFiles";
        s.start(outPath);
    }
    public static GpuCloudlet createGpuCloudlet(int gpuCloudletId, int gpuTaskId, int brokerId, long len, double gpuPart, int numBlocks, int kernelNum, BasicClustering clustering) {
        List<GpuCloudlet> kernels = new ArrayList<>();
        // 任务cpu部分
        long length = (long) (len * (1 - gpuPart));
        long fileSize = 300;
        long outputSize = 300;
        int pesNumber = 1;


        // 任务gpu部分
        long taskLength = (long) (len * gpuPart / numBlocks);
        long taskInputSize = 128;
        long taskOutputSize = 128;
        long requestedGddramSize = 4 * 1024;
        int numberOfBlocks = numBlocks / kernelNum;

        for(int i = 0; i < kernelNum; i++) {
            Kernel gpuTask = null;
            long input = 0;
            if(i == 0)
                input = taskInputSize;
            long output = 0;
            if(i == kernelNum - 1)
                output = taskOutputSize;
            if (gpuPart > 0) {
                gpuTask = new Kernel(gpuTaskId, taskLength, numberOfBlocks, input, output,
                        requestedGddramSize, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());
                gpuTask.setThreadsPerBlock(100);
                gpuTask.setName("Kernel_" + gpuTaskId + "_" + i);
            }
            if(i == 0) {
                GpuCloudlet gpuCloudlet = new GpuCloudlet(gpuCloudletId, length, pesNumber, fileSize, outputSize,
                        new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull(), gpuTask, false);
                gpuCloudlet.setName("Task_" + gpuCloudletId + "_" + i);
                gpuCloudlet.setUserId(brokerId);
                gpuCloudlet.setRam(10000);
                kernels.add(gpuCloudlet);
            }else{
                GpuCloudlet gpuCloudlet = new GpuCloudlet(gpuCloudletId, 0, 0, 0, 0,
                        new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull(), gpuTask, false);
                gpuCloudlet.setName("Task_" + gpuCloudletId + "_" + i);
                gpuCloudlet.setUserId(brokerId);
                gpuCloudlet.setRam(0);
                kernels.add(gpuCloudlet);
            }
        }
        return clustering.addTasks2Job(kernels);
    }
    public static List<GpuCloudlet> createTasks() {
        BasicClustering clustering = new BasicClustering();
        long totalLength = 2000000;
        Parameters.enableGPUShare = true;
        List<GpuCloudlet> ret = new ArrayList<>();
        Random random = new Random();
        for(int i = 0; i < 100; i++) {
            ret.add(createGpuCloudlet(i, i, 0, totalLength, Math.random(), 20, random.nextInt(4) + 1,clustering));
        }
        return ret;
    }

    public static PowerGpuHost createHosts(Integer id) {

        /* Create 2 hosts, one is GPU-equipped */

        // Number of host's video cards
        int numVideoCards = 1;
        // To hold video cards
        List<VideoCard> videoCards = new ArrayList<VideoCard>(numVideoCards);
        for (int videoCardId = 0; videoCardId < numVideoCards; videoCardId++) {
            List<Pgpu> pgpus = new ArrayList<Pgpu>();
            // Adding an NVIDIA K1 Card
            double mips = 100;
            int gddram = GridVideoCardTags.NVIDIA_K1_CARD_GPU_MEM;
            long bw = GridVideoCardTags.NVIDIA_K1_CARD_BW_PER_BUS;
            for (int pgpuId = 0; pgpuId < 4; pgpuId++) {
                List<Pe> pes = new ArrayList<Pe>();
                for (int peId = 0; peId < 100; peId++) {
                    pes.add(new Pe(peId, new PeProvisionerSimple(mips * 10)));
                }
                Pgpu p = new Pgpu(pgpuId, GridVideoCardTags.NVIDIA_K1_GPU_TYPE, pes,
                        new GpuGddramProvisionerSimple(gddram), new GpuBwProvisionerShared(bw));
                GpuTaskSchedulerLeftover schedulerLeftover = new GpuTaskSchedulerLeftover();
                schedulerLeftover.initSMs(1000, 100, 10);
                p.setGpuTaskScheduler(schedulerLeftover);
                pgpus.add(p);
            }
            // Pgpu selection policy
            PgpuSelectionPolicy pgpuSelectionPolicy = new PgpuSelectionPolicyNull();
            // Performance Model
            double performanceLoss = 0.1;
            PerformanceModel<VgpuScheduler, Vgpu> performanceModel = new PerformanceModelGpuConstant(performanceLoss);
            // Scheduler
            GridPerformanceVgpuSchedulerFairShare vgpuScheduler = new GridPerformanceVgpuSchedulerFairShare(
                    GridVideoCardTags.NVIDIA_K1_CARD, pgpus, pgpuSelectionPolicy, performanceModel);
            // PCI Express Bus Bw Provisioner
            VideoCardBwProvisioner videoCardBwProvisioner = new VideoCardBwProvisionerShared(BusTags.PCI_E_3_X16_BW);
            // Video Card Power Model
            VideoCardPowerModel videoCardPowerModel = new GridVideoCardPowerModelK1(false);
            // Create a video card
            PowerVideoCard videoCard = new PowerVideoCard(videoCardId, GridVideoCardTags.NVIDIA_K1_CARD, vgpuScheduler,
                    videoCardBwProvisioner, videoCardPowerModel);
            videoCards.add(videoCard);
        }

        // A Machine contains one or more PEs or CPUs/Cores.
        List<Pe> peList = new ArrayList<Pe>();

        // PE's MIPS power
        double mips = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_PE_MIPS;

        for (int peId = 0; peId < 4; peId++) {
            // Create PEs and add these into a list.
            peList.add(new Pe(0, new PeProvisionerSimple(mips)));
        }

        // Create Host with its id and list of PEs and add them to the list of machines
        // host memory (MB)
        int ram = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_RAM;
        // host storage
        long storage = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_STORAGE;
        // host BW
        int bw = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_BW;
        // Set VM Scheduler
        VmScheduler vmScheduler = new VmSchedulerTimeShared(peList);
        // Host Power Model
        double hostMaxPower = 200;
        double hostStaticPowerPercent = 0.70;
        PowerModel powerModel = new GpuHostPowerModelLinear(hostMaxPower, hostStaticPowerPercent);
        // Video Card Selection Policy
        VideoCardAllocationPolicy videoCardAllocationPolicy = new VideoCardAllocationPolicyNull(videoCards);

        PowerGpuHost newHost = new PowerGpuHost(id, GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3,
                new RamProvisionerSimple(ram), new BwProvisionerSimple(bw), storage, peList, vmScheduler,
                videoCardAllocationPolicy, powerModel);
        newHost.setCloudletScheduler(new GpuCloudletSchedulerTimeShared());
        newHost.setName("Host" + id);
        return newHost;
    }
}
