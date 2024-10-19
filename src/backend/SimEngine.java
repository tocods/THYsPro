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
import gpu.*;
import gpu.allocation.VideoCardAllocationPolicy;
import gpu.allocation.VideoCardAllocationPolicyNull;
import gpu.power.PowerGpuHost;
import gpu.power.models.GpuHostPowerModelLinear;
import workflow.GpuJob;
import workflow.Parameters;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class SimEngine {
    private List<PowerGpuHost> hosts;

    private List<GpuCloudlet> jobs;

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
            if(hostInfo.generator != null)
                faulttolerant.Parameters.host2FaultInject.put(hosts.get(hosts.size() - 1), hostInfo.generator);
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
            if(hostInfo.generator != null)
                faulttolerant.Parameters.host2FaultInject.put(hosts.get(hosts.size() - 1), hostInfo.generator);
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
        Result ret = null;
        String mes = parser.parseJobXml(new File(path));
        for(JobInfo jobInfo: parser.getJobInfos()) {
            ret = addJob(jobInfo);
            if(jobInfo.generator != null) {
                GpuJob job = (GpuJob) jobs.get(jobs.size() - 1);
                faulttolerant.Parameters.job2FaultInject.put(job.getName(), jobInfo.generator);
                for(GpuCloudlet cl: job.getTasks()) {
                    faulttolerant.Parameters.job2FaultInject.put(cl.getName(), jobInfo.generator);
                }
            }
            if(ret.ifError()) {
                Log.printLine(ret.getMessage());
                jobs = new ArrayList<>();
                return ret;
            }
        }
        return Result.success(mes);
    }

    public Result parseJsonOfJob(String path) {
        Log.printLine(String.join("", Collections.nCopies(100, "-")));
        Log.printLine("解析任务信息文件 " + path);
        Result ret = null;
        List<JobInfo> jobInfos = jsonParser.parseJobs(path);
        for(JobInfo jobInfo: jobInfos) {
            ret = addJob(jobInfo);
            if(jobInfo.generator != null) {
                GpuJob job = (GpuJob) jobs.get(jobs.size() - 1);
                faulttolerant.Parameters.job2FaultInject.put(job.getName(), jobInfo.generator);
                for(GpuCloudlet cl: job.getTasks()) {
                    faulttolerant.Parameters.job2FaultInject.put(cl.getName(), jobInfo.generator);
                }
            }
            if(ret.ifError()) {
                Log.printLine(ret.getMessage());
                jobs = new ArrayList<>();
                return ret;
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
            if(h != null)
                faulttolerant.Parameters.host2FaultInject.put(h, faultInfo.tran2Generator());
        }
        return Result.success("");
    }

    public void setAlgorithm(Parameters.JobAllocationAlgorithm algorithm) {
        this.algorithm = algorithm;
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
        newHost.setCloudletScheduler(cloudletSchedulerTimeShared);

        newHost.setName(name);
        hosts.add(newHost);
        return Result.success(null);
    }

    /**
     * 创建1个新的任务

     */
    public Result addJob(JobInfo jobInfo) {
        GpuJob job = jobInfo.tran2Job(cloudletId, taskId, gpuId);
        Log.printLine(job.getName());
        jobs.add(job);
        cloudletId ++;
        taskId += job.getTasks().size();
        gpuId += job.getTasks().size();
        return Result.success(null);
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
        SimEngine engine = new SimEngine();
//        String outPath = System.getProperty("user.dir") + "\\OutputFiles";
        String outPath = args[0];
        String jobPath = args[2];
        String hostPath = args[1];
        String faultPath = args[3];
        Integer algorithm = Integer.parseInt(args[4]);
        Double duration = Double.parseDouble(args[5]);
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
