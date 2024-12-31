package faulttolerant;

import cloudsim.*;
import cloudsim.core.CloudSim;
import cloudsim.core.CloudSimTags;
import cloudsim.core.SimEvent;
import cloudsim.core.predicates.PredicateType;
import faulttolerant.faultGenerator.FaultGenerator;
import faulttolerant.faultInject.BasicInjector;
import faulttolerant.faultInject.FaultInjector;
import gpu.*;
import gpu.core.GpuCloudSimTags;
import gpu.power.PowerGpuHost;
import workflow.GPUWorkflowDatacenter;
import workflow.GpuJob;
import workflow.Parameters;
import workflow.WorkflowSimTags;
import workflow.jobAllocation.BasicAllocation;
import workflow.jobAllocation.FaultAllocation;
import workflow.jobAllocation.JobAllocationInterface;
import workflow.jobAllocation.RandomAllocation;
import workflow.taskCluster.BasicClustering;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GPUWorkflowFaultDatacenter extends GPUWorkflowDatacenter {
    private boolean faultToleranceEnabled;
    private FaultInjector faultInjector;

    private double nextFailTime = Double.MAX_VALUE;

    public GPUWorkflowFaultDatacenter(String name, DatacenterCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList, double schedulingInterval) throws Exception {
        super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
       setFaultToleranceDisabled();
       setFaultInjector();
    }

    private FaultInjector getFaultInjector() {
        return faultInjector;
    }


    /**
     * 根据给定的数学分布设定错误注入器
     */
    private void setFaultInjector() {
        faultInjector = new BasicInjector();
        faulttolerant.Parameters.injector = faultInjector;
    }

    /**
     * 允许错误注入
     */
    public void setFaultToleranceEnabled() {
        this.faultToleranceEnabled = true;
    }

    /**
     * 取消错误注入
     */
    public void setFaultToleranceDisabled() {
        this.faultToleranceEnabled = false;
    }

    /**
     * 错误注入是否已开启
     * @return 错误注入是否已经开启
     */
    public boolean isFaultToleranceEnabled() {
        return faultToleranceEnabled;
    }

    public void setHostFaultGenerator(Host host, List<FaultGenerator> generators) {
        if(!isFaultToleranceEnabled())
            return;
        getFaultInjector().initHostGenerator(host, generators);
    }

    public void setJobFaultGenerator(String job, FaultGenerator generator) {
        if(!isFaultToleranceEnabled())
            return;
        getFaultInjector().initJobGenerator(job, generator);
    }

    @Override
    public void startEntity() {
        if(isFaultToleranceEnabled())
            schedule(getId(), getSchedulingInterval(), FaultTolerantTags.FAULT_DATACENTER_EVENT);
        super.startEntity();
    }

    /**
     * 处理容错模块的事件
     * @param ev information about the event just happened
     *
     */
    @Override
    protected void processOtherEvent(SimEvent ev) {
        if(ifFinish())
            return;
        super.processOtherEvent(ev);
        // 如果没有开启错误注入，容错模块的数据中心并不需要处理特殊事件
        if(!isFaultToleranceEnabled())
            return;
        switch (ev.getTag()) {
            case FaultTolerantTags.FAULT_DATACENTER_EVENT:
                processFaultCheck(ev);
                break;
            case FaultTolerantTags.FAULT_HOST_REPAIR:
                processHostRepair(ev);
                break;
            case FaultTolerantTags.RECORD_FAULT_RECORD:
                FaultRecord record = (FaultRecord)ev.getData();
                faulttolerant.Parameters.faultRecordList.add(record);
                Topo topo = new Topo();
                topo.hosts = new ArrayList<>();
                DecimalFormat format = new DecimalFormat("##.##");
                topo.time = format.format(CloudSim.clock());
                for(Host h : getHostList()) {
                    List<ResCloudlet> cloudletList = h.getCloudletScheduler().getCloudletExecList();
                    cloudletList.addAll(h.getCloudletScheduler().getCloudletPausedList());
                    cloudletList.addAll(h.getCloudletScheduler().getCloudletWaitingList());
                    Topo.TopoHost host = topo.new TopoHost();
                    host.hostName = h.getName();
                    host.softwares = new ArrayList<>();
                    for(ResCloudlet cl: cloudletList) {
                        ResGpuCloudlet rgcl = (ResGpuCloudlet) cl;
                        if(!rgcl.ifFromGpu) {
                            host.softwares.add(((GpuCloudlet)rgcl.getCloudlet()).getName());
                        }
                    }
                    topo.hosts.add(host);
                }
                faulttolerant.Parameters.topos.add(topo);
        }
    }



    /**
     * 恢复故障主机
     * @param ev 故障主机
     */
    protected void processHostRepair(SimEvent ev) {
        Host h = (Host) ev.getData();
        h.getCloudletScheduler().hostRepair(CloudSim.clock());
        h.setFailed(false, "");
        Log.printLine(CloudSim.clock() + ": " + h.getName() + "恢复运行");
        ((GpuHost)h).addFailTime();
        Log.printLine(new DecimalFormat("##.##").format(CloudSim.clock()) + ": " + h.getName() + "故障恢复");
        sendNow(getId(), FaultTolerantTags.FAULT_DATACENTER_EVENT, null);
    }

    /**
     * 在这个函数中，我们需要对集群中的物理节点进行错误注入
     */
    protected void processFaultCheck(SimEvent ev) {
        Host failHost = (Host) ev.getData();
        // 对该主机进行错误注入
        if(failHost != null)
            hostFail(failHost);
        double lastTime = Double.MAX_VALUE;
        Host toFailHost = null;
        // 遍历所有未发生故障的主机，进行错误注入
        for(Host h: getHostList()) {
            if(h.isFailed())
                continue;
            FaultInjector faultInjector = getFaultInjector();
            double nextFailTime = faultInjector.nextHostFailTime(h, CloudSim.clock());
            if(nextFailTime < lastTime) {
                lastTime = nextFailTime;
                toFailHost = h;
            }
        }
        if(lastTime != Double.MAX_VALUE) {
            //Log.printLine("下一次宕机：" + lastTime);
            nextFailTime = Math.min(lastTime, nextFailTime);
            schedule(getId(), lastTime - CloudSim.clock(), FaultTolerantTags.FAULT_DATACENTER_EVENT, toFailHost);
        }
    }




    /**
     * 对发生错误的主机进行处理
     * @param h 错误主机
     */
    private void hostFail(Host h) {
        if(h.isFailed())
            return;
        DecimalFormat format = new DecimalFormat("###.##");
        Log.printLine(format.format(CloudSim.clock()) + ": " + h.getName() + "宕机");
        Integer redundancyBefore = 0;
        h.setFailed(true, getFaultInjector().lastFaultType(h));
        for(Host host : getHostList())
            if(!host.ifHostFailed())
                redundancyBefore ++;
        Double redundancyAfter = getRedundancyOfCluster();
        FaultRecord record = new FaultRecord();
        record.type = FaultRecord.FaultType.REBUILD;
        record.redundancyBefore = redundancyBefore;
        record.redundancyAfter = redundancyAfter;
        record.faultType = faultInjector.lastFaultType(h);
        record.host2Cals = host2Cals;
        for(Map.Entry<String, List<String>> s: host2Cals.entrySet()) {
            Log.printLine(s.getKey() + ": ");
            for(String ss : s.getValue())
                Log.printLine("     " + ss);
        }
        // 将故障主机上的任务迁移到其他主机上
        startMigrateTaskInFailedHost(h, record);
        //sendNow(getId(), CloudSimTags.VM_DATACENTER_EVENT);
        send(getId(), getFaultInjector().hostRepairTime(h), FaultTolerantTags.FAULT_HOST_REPAIR, h);
    }

    @Override
    protected void updateCloudletProcessing() {
       if(Math.abs(CloudSim.clock() - nextFailTime) < 1) {
           send(getId(), 1, CloudSimTags.VM_DATACENTER_EVENT);
           return;
       }
       super.updateCloudletProcessing();
    }


    private JobAllocationInterface getAllocation() {
        switch (workflow.Parameters.allocationAlgorithm) {
            case RR:
                return new BasicAllocation();
            case RANDOM:
                return new RandomAllocation();
            default:
                return new BasicAllocation();
        }
    }


    /**
     * 将失败的主机中的任务重新分配到其他主机上
     */
    private void startMigrateTaskInFailedHost(Host h, FaultRecord record){
        List<Host> filterHosts = new ArrayList<>();
        // 排除掉宕机的主机
        for(Host host: getHostList()) {
            if(host.getId() == h.getId())
                continue;
            filterHosts.add(host);
        }
        JobAllocationInterface jobAllocation = new FaultAllocation();
        jobAllocation.setHosts(filterHosts);
        List<ResCloudlet> cloudletList = h.getCloudletScheduler().getCloudletPausedList();
        List<GpuJob> job2Migrate = new ArrayList<>();
        Boolean ifGpu = getFaultInjector().lastFaultType(h).equals("gpu");
        Integer i = 0;
        for(ResCloudlet cl: cloudletList) {
            ResGpuCloudlet rgcl = (ResGpuCloudlet)cl;
            if(rgcl.ifFromGpu)
                continue;
            GpuCloudlet gpuCloudlet = (GpuCloudlet)cl.getCloudlet();
            Log.printLine("检查下" + gpuCloudlet.getName() + ((GpuJob)gpuCloudlet).ifHasKernel());
            if(ifGpu && !((GpuJob)gpuCloudlet).ifHasKernel())
                continue;
            Log.printLine(gpuCloudlet.getName() + "需要迁移 " + i);
            i++;
            rgcl.finalizeCloudlet();
            gpuCloudlet = (GpuCloudlet)cl.getCloudlet();
            job2Migrate.add((GpuJob) gpuCloudlet);
        }
        ((GpuHost)h).hostFail(getFaultInjector().lastFaultType(h).equals("gpu"));
        DecimalFormat format = new DecimalFormat("###.##");
        if(job2Migrate.isEmpty()) {
            record.ifFalseAlarm = "True";
            record.fault = h.getName();
            record.time = format.format(CloudSim.clock());
            record.ifEmpty = "True";
            faulttolerant.Parameters.faultRecordList.add(record);
            return;
        }
        Log.printLine(CloudSim.clock() + ": " + job2Migrate.size() + " 个任务待迁移");
        jobAllocation.setJobs(job2Migrate);
        faulttolerant.Parameters.jobTotalRebuildNum += job2Migrate.size();
        try {
            jobAllocation.run();
            record.faultJobNum = jobAllocation.getJobs().size();
            faulttolerant.Parameters.jobTotalRebuildSuccessTime += record.faultJobNum;
            if(jobAllocation.getJobs().size() < job2Migrate.size()) {
                record.ifFalseAlarm = "False";
                record.ifSuccessRebuild = "False";
            }else
                record.ifFalseAlarm = "True";
            record.fault = h.getName();
            record.time = format.format(CloudSim.clock());
            if(!jobAllocation.getJobs().isEmpty()) {
                for (GpuJob job : jobAllocation.getJobs()) {
                    job.setRecord(record);
                    Log.printLine("将任务 " + job.getName() + " 迁移至 " + job.getHost().getName());
                    sendNow(getId(), CloudSimTags.CLOUDLET_SUBMIT, job);
                }
            }else {
                record.redundancyAfter = 0.0;
                faulttolerant.Parameters.faultRecordList.add(record);
            }
        }catch (Exception e) {
            // 此时网络重构失败
            //((GpuHost)h).hostFail();
            record.ifFalseAlarm = "False";
            record.ifSuccessRebuild = "False";
            record.fault = h.getName();
            record.time = format.format(CloudSim.clock());
            faulttolerant.Parameters.faultRecordList.add(record);
            Log.printLine(e.getMessage());
            // e.printStackTrace();
        }
    }


    /**
     * 对于执行结束的内核，检查任务在执行过程中是否发生错误
     */
    @Override
    protected void processGpuCloudletReturn(SimEvent ev) {
        GpuCloudlet cl = (GpuCloudlet) ev.getData();
        Log.printLine(cl.getGpuTask().getName() + "执行完成");
        try {
                cl.setCloudletStatus(Cloudlet.SUCCESS);
                notifyGpuTaskCompletion(cl.getGpuTask());
                sendNow(getId(), CloudSimTags.VM_DATACENTER_EVENT);
            }catch (Exception e) {
                e.printStackTrace();
            }
    }

    /**
     * 对于执行结束的任务，检查有无错误发生
     */
    @Override
    protected void processCloudletReturn(SimEvent ev) {
        GpuJob job = (GpuJob) ev.getData();
        try {
            Log.printLine(CloudSim.clock() + ": 将" + job.getName() + "返回给引擎");
            job.setCloudletStatus(Cloudlet.SUCCESS);
            sendNow(Parameters.engineID, CloudSimTags.CLOUDLET_RETURN, job);
        }catch (Exception e) {
            Log.printLine("设置任务完成状态时发生了意外。。。。但理论上是不可能的");
            e.printStackTrace();
        }
    }


}
