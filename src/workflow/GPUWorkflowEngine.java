package workflow;

import api.service.Parameters;
import faulttolerant.faultGenerator.FaultGenerator;
import gpu.GpuCloudlet;
import gpu.GpuDatacenterBroker;
import gpu.core.GpuCloudSimTags;
import cloudsim.Cloudlet;
import cloudsim.DatacenterCharacteristics;
import cloudsim.Host;
import cloudsim.Log;
import cloudsim.core.CloudSim;
import cloudsim.core.CloudSimTags;
import cloudsim.core.SimEvent;
import workflow.jobAllocation.BasicAllocation;
import workflow.jobAllocation.JobAllocationInterface;
import workflow.jobAllocation.RandomAllocation;
import workflow.taskCluster.BasicClustering;
import workflow.taskCluster.ClusteringInterface;

import java.util.*;


public class GPUWorkflowEngine extends GpuDatacenterBroker {
    protected Map<Integer, Integer> host2Datacenter;

    protected Map<String, Integer> jobRepeat;

    protected JobAllocationInterface jobAllocationInterface;

    protected int jobNeedRepeat = 0;

    protected Boolean haveJobRepeat = false;
    /**
     * @param name 用于DEBUG
     * @see GpuDatacenterBroker
     */
    public GPUWorkflowEngine(String name) throws Exception {
        super(name);
        host2Datacenter = new HashMap<>();
        Parameters.endTime = 0.0;
        jobRepeat = new HashMap<>();
        //initClusteringInterface();
        //initJobAllocationInterface();
    }

    @Override
    public void startEntity() {
        if(workflow.Parameters.duration != Double.MAX_VALUE)
            send(getId(), workflow.Parameters.duration, WorkflowSimTags.WORKFLOW_CLOUDLET_END_SIM);
        super.startEntity();
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
     * 设置任务调度算法
     */
    public void initJobAllocationInterface(List<? extends Host> hosts, workflow.Parameters.JobAllocationAlgorithm jobAllocationAlgorithm) {
        workflow.Parameters.allocationAlgorithm = jobAllocationAlgorithm;
        jobAllocationInterface = getAllocation();
        jobAllocationInterface.setHosts(hosts);
    }

    @Override
    protected void processOtherEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case WorkflowSimTags.WORKFLOW_CLOUDLET_NEXT_PERIOD:
                GpuJob gpuJob = (GpuJob) ev.getData();
                submitRepeatJob(gpuJob);
                break;
            case WorkflowSimTags.WORKFLOW_CLOUDLET_END_SIM:
                doComplete();
                break;
            default:
                super.processOtherEvent(ev);
                break;
        }
    }

    /**
     * 这个函数基本就是GPUWorkflowEngine会第一个调用的函数，在这个函数里我们要开启任务的下发
     * @param ev a SimEvent object
     */
    @Override
    protected void processResourceCharacteristics(SimEvent ev) {
        //Log.printLine("===============");
        DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();
        getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);
        for(Host h: characteristics.getHostList()) {
            host2Datacenter.put(h.getId(), characteristics.getId());
        }
        send(characteristics.getId(), CloudSim.getMinTimeBetweenEvents(), GpuCloudSimTags.VGPU_DATACENTER_EVENT);
        // 完成了Datacenter和Engine之间的初始化同步
        if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
            setDatacenterRequestedIdsList(new ArrayList<Integer>());
            try {
                // 不需要原本的创建虚拟机这一步骤，直接分发任务
                doTaskDeliver();
            }catch (Exception e) {
                Log.printLine(e.getMessage());
            }
        }
    }

    private void submitRepeatJob(GpuJob job) {
        Log.printLine(CloudSim.clock() + ": " + job.getName() + "需要进入下一周期");
        int datacenterId = host2Datacenter.get(job.getVmId());
        BasicClustering clustering = new BasicClustering();
        List<GpuCloudlet> cloudlets = new ArrayList<>();
        for(GpuCloudlet cl: job.getTasks()) {
            cloudlets.add(cl.getNewCloudlet());
        }
        GpuJob jobRepeat = clustering.addTasks2Job(cloudlets);
        jobRepeat.setHost(job.getHost());
        jobRepeat.setVmId(job.getVmId());
        jobRepeat.setExecTime(job.getExecTime() + 1);
        jobRepeat.setName(job.getName());
        jobRepeat.setUserId(job.getUserId());
        jobRepeat.setPeriod(job.getPeriod());
        double period = job.getPeriod();
        double delay = job.getPeriod();
        //Log.printLine(jobRepeat.getExecTime());
        if(this.jobRepeat.get(job.getName()) == 0) {
            Log.printLine(job.getName() + "超时，在" + job.getHost().getName() + "上");
            sendNow(datacenterId, WorkflowSimTags.WORKFLOW_CLOUDLET_OUT, job);
        }
        if(period == 0)
            return;
        if(job.getExecTime() == workflow.Parameters.maxTime - 1 && workflow.Parameters.duration == Double.MAX_VALUE) {
            return;
        }
        send(getId(), delay, WorkflowSimTags.WORKFLOW_CLOUDLET_NEXT_PERIOD, jobRepeat);
        sendNow(datacenterId, CloudSimTags.CLOUDLET_SUBMIT, jobRepeat);
        cloudletsSubmitted ++;
        getCloudletSubmittedList().add(job);
    }

    /**
     * {@link api.service.Service} 将任务队列上传至此
     * @param list 任务队列
     */
    @Override
    public void submitCloudletList(List<? extends Cloudlet> list) {
        boolean ifStart = getCloudletList().isEmpty();
        getCloudletList().addAll(list);
        if(ifStart)
            return;
        try {
            doTaskDeliver();
        }catch (Exception e) {
            Log.printLine(e.getMessage());
        }
    }

    private List<GpuJob> doSchedule(List<GpuJob> jobs) throws Exception {
        Log.printLine(String.join("", Collections.nCopies(100, "-")));
        jobAllocationInterface.setJobs(jobs);
        jobAllocationInterface.run();
        Log.printLine(String.join("", Collections.nCopies(100, "-")));
        return jobAllocationInterface.getJobs();
    }

    /**
     * 下发任务给 {@link GPUWorkflowDatacenter}
     * 触发时机：
     * 1. 运行开始时
     * 2. 有执行完成的任务返回
     * 3. 有新上传的任务
     */
    protected void doTaskDeliver() throws Exception {
        List<GpuJob> tasks2Submit = new ArrayList<>();
        for(Cloudlet c: getCloudletList()) {
            GpuJob job = (GpuJob) c;
            boolean ifAllParentFinish = true;
            for(Cloudlet parent: job.getParent()){
                if(!getCloudletReceivedList().contains(parent)){
                    ifAllParentFinish = false;
                    break;
                }
            }
            if(ifAllParentFinish)
                tasks2Submit.add(job);
        }
        getCloudletList().removeAll(tasks2Submit);
        List<GpuJob> jobs = doSchedule(tasks2Submit);
        for(GpuJob job: jobs) {
            if(job.getPeriod() != 0 && job.getExecTime() == 0) {
                jobNeedRepeat++;
                haveJobRepeat = true;
                if(!jobRepeat.containsKey(job.getName())) {
                    jobRepeat.put(job.getName(), 0);
                    send(getId(), job.getPeriod(), WorkflowSimTags.WORKFLOW_CLOUDLET_NEXT_PERIOD, job);
                }
            }
            int datacenterId = host2Datacenter.get(job.getVmId());
            sendNow(datacenterId, CloudSimTags.CLOUDLET_SUBMIT, job);
            cloudletsSubmitted ++;
            getCloudletSubmittedList().add(job);
        }
    }


    /**
     * 判断仿真是否结束
     * 判断标准：任务全部执行完成
     * @return True 如果任务全部执行完成
     */
    protected boolean ifFinish() {
        if(workflow.Parameters.duration == Double.MAX_VALUE || !haveJobRepeat)
            return getCloudletList().isEmpty() && cloudletsSubmitted == 0 && jobNeedRepeat == 0;
        return CloudSim.clock() > workflow.Parameters.duration;
    }

    /**
     * 仿真结束
     */
    protected void doComplete() {
        // 记录仿真结束时间
        Parameters.endTime = CloudSim.clock();
        // 这个函数的作用是清除数据中心的所有虚拟机，但我们的仿真不存在虚拟机
        clearDatacenters();
        // 通知数据中心仿真结束
        finishExecution();

        CloudSim.abruptallyTerminate();
    }

    /**
     * 仿真未结束，执行下一步操作
     */
    protected void doNext(GpuJob job) {
//        BasicClustering clustering = new BasicClustering();
//        List<GpuCloudlet> cloudlets = new ArrayList<>();
//        for(GpuCloudlet cl: job.getTasks()) {
//            cloudlets.add(cl.getNewCloudlet());
//        }
//        GpuJob jobRepeat = clustering.addTasks2Job(cloudlets);
//        jobRepeat.setHost(job.getHost());
//        jobRepeat.setVmId(job.getVmId());
//        jobRepeat.setExecTime(job.getExecTime() + 1);
//        jobRepeat.setName(job.getName());
//        jobRepeat.setUserId(job.getUserId());
//        jobRepeat.setPeriod(job.getPeriod());
//        double period = job.getPeriod();
//        double start = job.getExecStartTime();
//        double delay = start + period - CloudSim.clock();
//        //Log.printLine(jobRepeat.getExecTime());
//        if(period == 0 || (jobRepeat.getExecTime() > workflow.Parameters.maxTime && workflow.Parameters.duration == Double.MAX_VALUE))
//            return;
//        send(getId(), Math.max(delay, 0), WorkflowSimTags.WORKFLOW_CLOUDLET_NEXT_PERIOD, jobRepeat);
        jobRepeat.put(job.getName(), 1);
    }

    /**
     * {@link GPUWorkflowDatacenter} 会将执行完成的任务传回，此函数将被调用
     * @param ev a SimEvent object
     */
    @Override
    protected void processCloudletReturn(SimEvent ev) {
        GpuCloudlet task = (GpuCloudlet) ev.getData();
        getCloudletReceivedList().add(task);
        cloudletsSubmitted--;
        if(((GpuJob)task).getExecTime() == workflow.Parameters.maxTime - 1 && ((GpuJob)task).getPeriod() != 0)
            jobNeedRepeat --;
        if(jobRepeat.containsKey(((GpuJob)task).getName()) && task.getCloudletStatus() == Cloudlet.SUCCESS) {
            if(jobRepeat.get(((GpuJob)task).getName()) == 0) {
                doNext((GpuJob) task);
            }
        }
        if(ifFinish()) {
            Log.printLine("仿真结束");
            // 任务全部执行完成，仿真结束
            doComplete();
        }
    }
}
