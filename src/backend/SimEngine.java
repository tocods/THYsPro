package backend;

import api.Api;
import api.Result;
import api.util.JsonUtil;
import api.util.ParseUtil;
import api.info.*;
import cloudsim.*;
import cloudsim.power.models.PowerModel;
import cloudsim.provisioners.BwProvisionerSimple;
import cloudsim.provisioners.RamProvisionerSimple;
import faulttolerant.faultGenerator.FaultGenerator;
import gpu.*;
import gpu.allocation.VideoCardAllocationPolicy;
import gpu.allocation.VideoCardAllocationPolicyNull;
import gpu.power.PowerGpuHost;
import gpu.power.models.GpuHostPowerModelLinear;
import lombok.Setter;
import org.apache.commons.math3.util.Pair;
import workflow.GpuJob;
import workflow.Parameters;
import fncs.JNIfncs;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

import java.io.File;
import java.util.*;

public class SimEngine {
    private List<PowerGpuHost> hosts;

    private List<GpuCloudlet> jobs;

    @Setter
    private Parameters.JobAllocationAlgorithm algorithm;

    private Api api;

    private ParseUtil parser;

    private JsonUtil jsonParser;

    private int cloudletId;

    private int taskId;

    private int gpuId;

    public SimEngine() {
        api = new Api();
        algorithm = Parameters.JobAllocationAlgorithm.RR;
        parser = new ParseUtil();
        jsonParser = new JsonUtil();
        cloudletId = 0;
        taskId = 0;
        gpuId = 0;
        this.hosts = new ArrayList<>();
        this.jobs = new ArrayList<>();
        resetParamForSim();
    }

    public void resetParamForSim() {
        // 此项为本次仿真持续时间，解析输入文件前设置为无上限
        Parameters.duration = Double.MAX_VALUE;
        // 此项为各个主机和任务的错误生成器，解析输入文件前不存在
        faulttolerant.Parameters.host2FaultInject = new HashMap<>();
        faulttolerant.Parameters.job2FaultInject = new HashMap<>();
        faulttolerant.Parameters.faultRecordList = new ArrayList<>();
    }

    public Result parseXmlOfHost(String path) {
        Log.printLine(String.join("", Collections.nCopies(100, "-")));
        Log.printLine("解析主机信息文件 " + path);
        Result ret = null;
        String mess = parser.parseHostXml(new File(path));
        for(HostInfo hostInfo: parser.getHostInfos()) {
            ret = addHost(hostInfo.videoCardInfos, hostInfo.cpuInfos, hostInfo.ram, hostInfo.name);
            if(ret.ifError()) {
                Log.printLine(ret.getMessage());
                hosts = new ArrayList<>();
                return ret;
            }
        }
        return Result.success(mess);
    }

    public Result parseJsonOfHost(String path) {
        Log.printLine(String.join("", Collections.nCopies(100, "-")));
        Log.printLine("解析主机信息文件 " + path);
        Result ret = null;
        List<HostInfo> hostInfos = jsonParser.parseHosts(path);
        for(HostInfo hostInfo: hostInfos) {
            ret = addHost(hostInfo.videoCardInfos, hostInfo.cpuInfos, hostInfo.ram, hostInfo.name);
            if(ret.ifError()) {
                Log.printLine(ret.getMessage());
                hosts = new ArrayList<>();
                return ret;
            }
        }
        return Result.success("");
    }


    public Result parseXmlOfJob(String path) {
        Log.printLine(String.join("", Collections.nCopies(100, "-")));
        Log.printLine("解析任务信息文件 " + path);
        GpuJob j = null;
        String mes = parser.parseJobXml(new File(path));
        for(JobInfo jobInfo: parser.getJobInfos()) {
            j = addJob(jobInfo);
            if(jobInfo.generator != null) {
                GpuJob job = (GpuJob) jobs.get(jobs.size() - 1);
                faulttolerant.Parameters.job2FaultInject.put(job.getName(), jobInfo.generator);
                for(GpuCloudlet cl: job.getTasks()) {
                    faulttolerant.Parameters.job2FaultInject.put(cl.getName(), jobInfo.generator);
                }
            }
        }
        return Result.success(mes);
    }

    public Result parseJsonOfJob(String path) {
        Log.printLine(String.join("", Collections.nCopies(100, "-")));
        Log.printLine("解析任务信息文件 " + path);
        GpuJob j = null;
        List<JobInfo> jobInfos = jsonParser.parseJobs(path);
        Map<String, HashSet<GpuCloudlet>> c2p = new HashMap<>();
        for(JobInfo jobInfo: jobInfos) {
            j = addJob(jobInfo);
            for(ChildInfo c: jobInfo.children) {
                if(c2p.containsKey(c.child)) {
                    c2p.get(c.child).add(j);
                }else{
                    HashSet<GpuCloudlet> tmp = new HashSet<>();
                    tmp.add(j);
                    c2p.put(c.child, tmp);
                }
            }
            if(jobInfo.generator != null) {
                GpuJob job = (GpuJob) jobs.get(jobs.size() - 1);
                faulttolerant.Parameters.job2FaultInject.put(job.getName(), jobInfo.generator);
                for(GpuCloudlet cl: job.getTasks()) {
                    faulttolerant.Parameters.job2FaultInject.put(cl.getName(), jobInfo.generator);
                }
            }
        }
        for(GpuCloudlet jo: jobs){
            GpuJob job = (GpuJob) jo;
            if(c2p.containsKey(job.getName())) {
                HashSet<GpuCloudlet> ps = c2p.get(job.getName());
                for(GpuCloudlet p: ps)
                    job.addParent(p);
            }
        }

        return Result.success("");
    }

    public Result parseJsonOfFault(String path) {
        Log.printLine(String.join("", Collections.nCopies(100, "-")));
        Log.printLine("解析错误注入信息文件 " + path);
        Result ret = null;
        List<FaultInfo> faultInfos = jsonParser.parseFaults(path);
        for(FaultInfo faultInfo: faultInfos) {
            Host h = null;
            for(Host host: hosts)
                if(host.getName().equals(faultInfo.aim))
                    h = host;
            if(h != null) {
                if(faulttolerant.Parameters.host2FaultInject.containsKey(h))
                    faulttolerant.Parameters.host2FaultInject.get(h).add(faultInfo.tran2Generator());
                else {
                    List<FaultGenerator> generators = new ArrayList<>();
                    generators.add(faultInfo.tran2Generator());
                    faulttolerant.Parameters.host2FaultInject.put(h, generators);
                }
            }
        }
        return Result.success("");
    }

    public Result start(String outputPath) {
        api.setHosts(hosts);
        api.setTasks(jobs);
        return api.start(algorithm, outputPath);
    }


    /**
     * 创建1个新的物理节点
     * @param videoCardInfos 显卡信息
     * @param cpuInfos CPU信息
     * @param ram 内存大小
     */
    public Result addHost(List<VideoCardInfo> videoCardInfos, List<CPUInfo> cpuInfos, int ram, String name) {
        // 对GPU建模
        List<VideoCard> videoCards = new ArrayList<>(videoCardInfos.size());
        for(int videoCardId = 0; videoCardId < videoCardInfos.size(); videoCardId ++) {
            videoCards.add(videoCardInfos.get(videoCardId).tran2Entity(videoCardId));
        }
        VideoCardAllocationPolicy videoCardAllocationPolicy = new VideoCardAllocationPolicyNull(videoCards);

        // 对CPU建模
        List<Pe> peList = new ArrayList<>();
        for(CPUInfo cpuInfo: cpuInfos) {
           peList.addAll(cpuInfo.tran2Pes());
        }


        //以下为仿真中无需使用的参数
        long storage = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_STORAGE;
        int bw = GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3_BW;
        VmScheduler vmScheduler = new VmSchedulerTimeShared(peList);
        //以上为仿真中无需使用的参数

        // 主机能耗模型
        double hostMaxPower = 200;
        double hostStaticPowerPercent = 0.70;
        PowerModel powerModel = new GpuHostPowerModelLinear(hostMaxPower, hostStaticPowerPercent);


        // 对主机建模
        PowerGpuHost newHost = new PowerGpuHost(hosts.size(), GpuHostTags.DUAL_INTEL_XEON_E5_2620_V3,
                new RamProvisionerSimple(ram * 1024), new BwProvisionerSimple(bw), storage, peList, vmScheduler,
                videoCardAllocationPolicy, powerModel);
        GpuCloudletSchedulerTimeShared cloudletSchedulerTimeShared = new GpuCloudletSchedulerTimeShared();
        cloudletSchedulerTimeShared.setRam(ram * 1024);
        List<Double> mips = new ArrayList<>();
        for(Pe pe : peList)
            mips.add(Double.valueOf((Integer)(pe.getMips())));
        cloudletSchedulerTimeShared.updateJobProcessing(0, mips, mips, mips);
        newHost.setCloudletScheduler(cloudletSchedulerTimeShared);

        newHost.setName(name);
        hosts.add(newHost);
        return Result.success(null);
    }

    /**
     * 创建1个新的任务

     */
    public GpuJob addJob(JobInfo jobInfo) {
        GpuJob job = jobInfo.tran2Job(cloudletId, taskId, gpuId);
        Log.printLine(jobInfo.host);
        if(!jobInfo.host.equals("")) {
            for(Host host: hosts) {
                if(host.getName().equals(jobInfo.host)) {
                    job.setVmId(host.getId());
                    job.setHost(host);
                    Log.printLine("I found it");
                }
            }
        }

        jobs.add(job);
        cloudletId ++;
        taskId += job.getTasks().size();
        gpuId += job.getTasks().size();
        return job;
    }

    public Parameters.JobAllocationAlgorithm getAlgorithm(Integer i) {
        switch (i) {
            case 0:
                return Parameters.JobAllocationAlgorithm.RR;
            case 1:
                return Parameters.JobAllocationAlgorithm.RANDOM;
            default:
                return Parameters.JobAllocationAlgorithm.RR;
        }
    }

    public static void main(String[] args) {
        JNIfncs.initialize();
        SimEngine engine = new SimEngine();
//        String outPath = System.getProperty("user.dir") + "\\OutputFiles";
        String outPath = args[0];
        outPath = "/Users/davidt/Library/Mobile Documents/com~apple~CloudDocs/gpuworkflowsim/out";
        String jobPath = args[2];
        jobPath = "/Users/davidt/Library/Mobile Documents/com~apple~CloudDocs/gpuworkflowsim/input/jobs.json";
        String hostPath = args[1];
        hostPath = "/Users/davidt/Library/Mobile Documents/com~apple~CloudDocs/gpuworkflowsim/input/hosts.json";
        String faultPath = args[3];
        faultPath = "/Users/davidt/Library/Mobile Documents/com~apple~CloudDocs/gpuworkflowsim/input/faults.json";
        Integer algorithm = -1;
        Double duration = 100.0;
        if(duration > 0)
            Parameters.duration = duration;
//        engine.parseXmlOfHost(System.getProperty("user.dir") + "\\input\\Hosts.xml");
//        engine.parseXmlOfJob(System.getProperty("user.dir") + "\\input\\Jobs.xml");
//        engine.setAlgorithm(Parameters.JobAllocationAlgorithm.RANDOM);
//        engine.start(outPath);
//        engine.parseXmlOfHost(hostPath);
        engine.parseJsonOfHost(hostPath);
//        engine.parseXmlOfJob(jobPath);
        engine.parseJsonOfJob(jobPath);
        engine.parseJsonOfFault(faultPath);
        engine.setAlgorithm(engine.getAlgorithm(algorithm));
        engine.start(outPath);
    }
}
