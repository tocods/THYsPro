package comm;

import cloudsim.Cloudlet;
import cloudsim.Log;
import cloudsim.core.CloudSim;
import cloudsim.core.SimEvent;
import faulttolerant.FaultRecord;
import faulttolerant.GPUWorkflowFaultEngine;
import gpu.GpuCloudlet;
import workflow.GpuJob;
import workflow.WorkflowSimTags;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommEngine extends GPUWorkflowFaultEngine {
    private Map<String, GpuJob> waittingJobs;
    public CommEngine(String name) throws Exception {
        super(name);
        waittingJobs = new HashMap<>();
    }

    @Override
    protected void processOtherEvent(SimEvent event){
        switch (event.getTag()) {
            case Api.RECEIVE_EVENT:

                String src_name = event.getData().toString();
                Log.printLine("从网络仿真器传来事件： 任务" + src_name + " 完成");
                GpuJob waittingJob = waittingJobs.get(src_name);
                waittingJob.receivePacket();
                if(waittingJob.ifReceiveAll())
                    getCloudletReceivedList().add(waittingJob);
                try{
                    doTaskDeliver();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                super.processOtherEvent(event);
                break;
        }
    }

    @Override
    protected boolean ifFinish() {
        Log.printLine(getCloudletList().isEmpty() + " / " + cloudletsSubmitted);
        return getCloudletList().isEmpty() && cloudletsSubmitted <= 0;
    }

    @Override
    protected void processCloudletReturn(SimEvent ev) {
        GpuCloudlet task = (GpuCloudlet) ev.getData();
        Log.printLine(CloudSim.clock() + ": " + task.getName() + "  返回");
        if(task.getCloudletStatus() == Cloudlet.FAILED) {
            getCloudletReceivedList().add(task);
            return;
        }
        cloudletsSubmitted--;
        if(task.getCloudletStatus() == Cloudlet.CANCELED) {
            DecimalFormat format = new DecimalFormat("###.##");
            FaultRecord record = new FaultRecord();
            record.type = FaultRecord.FaultType.TIME_OVER;
            record.ifFalseAlarm = "False";
            record.fault = task.getName();
            record.time = format.format(CloudSim.clock());
            record.redundancyAfter = -1.0;
            record.redundancyBefore = -1;
            record.failReason = ((GpuJob) task).type;
            faulttolerant.Parameters.faultRecordList.add(record);
        }
        GpuJob job = (GpuJob) task;

        if(!job.packets.isEmpty()) {
            for(Packet p: job.packets) {
                Log.printLine(job.getName() + " 发包 " + p.toString());
                Api.publish(Api.CommType.SEND_WITHOUT_REPLY, p.toString());
            }
            waittingJobs.put(job.getName(), job);
        }else {
            getCloudletReceivedList().add(job);
        }
        if(ifFinish()) {
            Log.printLine("仿真结束");
            // 任务全部执行完成，仿真结束
            Api.sendEnd();
            doComplete();
        }
    }
}
