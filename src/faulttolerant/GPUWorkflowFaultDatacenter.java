package faulttolerant;

import cloudsim.*;
import cloudsim.core.CloudSim;
import cloudsim.core.CloudSimTags;
import cloudsim.core.SimEvent;
import cloudsim.core.predicates.PredicateType;
import faulttolerant.faultGenerator.FaultGenerator;
import faulttolerant.faultInject.BasicInjector;
import faulttolerant.faultInject.FaultInjector;
import gpu.GpuCloudlet;
import gpu.GpuHost;
import gpu.GpuTask;
import gpu.ResGpuTask;
import gpu.core.GpuCloudSimTags;
import workflow.GPUWorkflowDatacenter;
import workflow.GpuJob;
import workflow.Parameters;
import workflow.WorkflowSimTags;
import workflow.jobAllocation.BasicAllocation;
import workflow.jobAllocation.JobAllocationInterface;
import workflow.jobAllocation.RandomAllocation;
import workflow.taskCluster.BasicClustering;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

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

    public void setHostFaultGenerator(Host host, FaultGenerator generator) {
        if(!isFaultToleranceEnabled())
            return;
        getFaultInjector().initHostGenerator(host, generator);
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
        }
    }

    /**
     * 在这个函数中，我们需要对集群中的所有物理节点进行错误注入
     */
    protected void processFaultCheck(SimEvent ev) {
        //Log.printLine("当前时间:" + CloudSim.clock());
        Host failHost = (Host) ev.getData();
        if(failHost != null)
            hostFail(failHost);
        double lastTime = Double.MAX_VALUE;
        Host toFailHost = null;
        for(Host h: getHostList()) {
            FaultInjector faultInjector = getFaultInjector();
            double nextFailTime = faultInjector.nextHostFailTime(h, CloudSim.clock());
            if(nextFailTime < lastTime) {
                lastTime = nextFailTime;
                toFailHost = h;
            }
        }
        if(lastTime != Double.MAX_VALUE) {
            Log.printLine("下一次宕机：" + lastTime);
            nextFailTime = Math.min(lastTime, nextFailTime);
            schedule(getId(), lastTime - CloudSim.clock(), FaultTolerantTags.FAULT_DATACENTER_EVENT, toFailHost);
        }
    }

    /**
     * 对发生错误的主机进行处理
     * @param h 错误主机
     */
    private void hostFail(Host h) {
        CloudSim.cancelAll(getId(), new PredicateType(CloudSimTags.VM_DATACENTER_EVENT));
        DecimalFormat format = new DecimalFormat("###.##");
        Log.errPrintLine(format.format(CloudSim.clock()) + ": " + h.getName() + "宕机");
        //TODO: 主机宕机要多久修复？目前是直接将任务重新分配到其他主机上，默认主机修复时间忽略不计
        h.setFailed(true);
        startMigrateTaskInFailedHost(h);
        sendNow(getId(), CloudSimTags.VM_DATACENTER_EVENT);
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
    private void startMigrateTaskInFailedHost(Host h){
        List<Host> filterHosts = new ArrayList<>();
        // 排除掉宕机的主机
        for(Host host: getHostList()) {
            if(host.getId() == h.getId())
                continue;
            filterHosts.add(host);
        }
        JobAllocationInterface jobAllocation = getAllocation();
        jobAllocation.setHosts(filterHosts);
        List<ResCloudlet> cloudletList = h.getCloudletScheduler().getCloudletExecList();
        cloudletList.addAll(h.getCloudletScheduler().getCloudletPausedList());
        cloudletList.addAll(h.getCloudletScheduler().getCloudletWaitingList());
        List<GpuJob> job2Migrate = new ArrayList<>();
        for(ResCloudlet cl: cloudletList) {
            cl.finalizeCloudlet();
            GpuCloudlet gpuCloudlet = (GpuCloudlet)cl.getCloudlet();
            job2Migrate.add((GpuJob) gpuCloudlet);
        }
        Log.printLine(CloudSim.clock() + ": " + job2Migrate.size());
        jobAllocation.setJobs(job2Migrate);
        try {
            jobAllocation.run();
            for(GpuJob job: jobAllocation.getJobs()){
                sendNow(getId(), CloudSimTags.CLOUDLET_SUBMIT, job);
            }
            DecimalFormat format = new DecimalFormat("###.##");
            ((GpuHost)h).hostFail();
            FaultRecord record = new FaultRecord();
            record.ifFalseAlarm = "True";
            record.fault = h.getName();
            record.time = format.format(CloudSim.clock());
            faulttolerant.Parameters.faultRecordList.add(record);
        }catch (Exception e) {
            DecimalFormat format = new DecimalFormat("###.##");
            ((GpuHost)h).hostFail();
            FaultRecord record = new FaultRecord();
            record.ifFalseAlarm = "False";
            record.fault = h.getName();
            record.time = format.format(CloudSim.clock());
            faulttolerant.Parameters.faultRecordList.add(record);
            Log.printLine(e.getMessage());
        }
    }


    /**
     * 对于执行结束的内核，检查任务在执行过程中是否发生错误
     */
    @Override
    protected void processGpuCloudletReturn(SimEvent ev) {
        GpuCloudlet cl = (GpuCloudlet) ev.getData();
        //Log.printLine(cl.getGpuTask().getName() + "执行完成");
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
            job.setCloudletStatus(Cloudlet.SUCCESS);
            sendNow(job.getUserId(), CloudSimTags.CLOUDLET_RETURN, job);
        }catch (Exception e) {
            Log.printLine("设置任务完成状态时发生了意外。。。。但理论上是不可能的");
            e.printStackTrace();
        }
    }


}
