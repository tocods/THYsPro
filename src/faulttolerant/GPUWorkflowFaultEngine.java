package faulttolerant;

import cloudsim.Cloudlet;
import cloudsim.Log;
import cloudsim.core.CloudSim;
import cloudsim.core.SimEvent;
import gpu.GpuCloudlet;
import gpu.GpuHost;
import workflow.GPUWorkflowEngine;
import workflow.GpuJob;
import workflow.Parameters;

import java.text.DecimalFormat;

public class GPUWorkflowFaultEngine extends GPUWorkflowEngine {

    public GPUWorkflowFaultEngine(String name) throws Exception {
        super(name);
    }

    @Override
    protected void processCloudletReturn(SimEvent ev) {
        GpuCloudlet task = (GpuCloudlet) ev.getData();

        Log.printLine("任务返回： " + task.getName());
        if(task.getCloudletStatus() == Cloudlet.FAILED) {
            //Log.printLine("执行失败" + ((GpuJob) task).getExecTime());
            getCloudletReceivedList().add(task);
            return;
        }
        cloudletsSubmitted--;
        if(((GpuJob)task).getExecTime() == workflow.Parameters.maxTime - 1 && ((GpuJob)task).getPeriod() != 0) {
            jobNeedRepeat--;
        }
        if(jobRepeat.containsKey(((GpuJob)task).getName()) && task.getCloudletStatus() == Cloudlet.SUCCESS) {
            if(jobRepeat.get(((GpuJob)task).getName()) == 0) {
                doNext((GpuJob) task);
            }
        }
        if(task.getCloudletStatus() == Cloudlet.CANCELED) {
            DecimalFormat format = new DecimalFormat("###.##");
            FaultRecord record = new FaultRecord();
            record.ifFalseAlarm = "False";
            record.fault = task.getName();
            record.time = format.format(CloudSim.clock());
            faulttolerant.Parameters.faultRecordList.add(record);
            task.setExecStartTime(CloudSim.clock() - ((GpuJob) task).getPeriod());
            Log.printLine("执行超时" + ((GpuJob) task).getExecTime());
            getCloudletReceivedList().add(task);
        }else {
            Log.printLine("执行完成" + ((GpuJob) task).getExecTime());
            getCloudletReceivedList().add(task);
        }
        if(ifFinish()) {
            Log.printLine("仿真结束");
            // 任务全部执行完成，仿真结束
            doComplete();
        }
    }
}
