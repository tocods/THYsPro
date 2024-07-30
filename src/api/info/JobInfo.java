package api.info;

import cloudsim.Log;
import cloudsim.UtilizationModelFull;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import faulttolerant.faultGenerator.FaultGenerator;
import gpu.GpuCloudlet;
import gpu.GpuTask;
import workflow.GpuJob;
import workflow.taskCluster.BasicClustering;
import workflow.taskCluster.ClusteringInterface;

import java.util.ArrayList;
import java.util.List;

public class JobInfo {
    public double period = 0.0;

    public String name;
    public TaskInfo cpuTask;

    public TaskInfo gpuTask;

    public FaultGenerator generator = null;
    public static class TaskInfo {
        public CPUTaskInfo cpuTaskInfo;
        public GPUTaskInfo gpuTaskInfo;
    }

    public GpuJob tran2Job(int id, int taskIdStart, int gpuIdStart) {
        if(gpuTask == null || gpuTask.gpuTaskInfo == null || gpuTask.gpuTaskInfo.kernels.isEmpty()) {
            List<GpuCloudlet> tasks = new ArrayList<>();
            GpuCloudlet gpuCloudlet = cpuTask.cpuTaskInfo.tran2Cloudlet(taskIdStart, null);
            tasks.add(gpuCloudlet);
            BasicClustering clustering = new BasicClustering();
            GpuJob job = clustering.addTasks2Job(tasks);
            job.setCloudletId(id);
            return job;
        }
        List<GpuCloudlet> tasks = new ArrayList<>();
        List<GpuTask> kernels = gpuTask.gpuTaskInfo.tran2GpuTask(id, gpuIdStart);
        int kernelNum = kernels.size();
        for(int i = 0; i < kernelNum; i++) {
            if(i == 0) {
                GpuCloudlet gpuCloudlet = cpuTask.cpuTaskInfo.tran2Cloudlet(taskIdStart, kernels.get(i));
                tasks.add(gpuCloudlet);
            }else {
                int taskId = taskIdStart + i;
                GpuCloudlet gpuCloudlet = new GpuCloudlet(taskId, 0, 0, 0, 0,
                        new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull(), kernels.get(i), false);
                gpuCloudlet.setName("Task_" + taskId + "_" + i);
                gpuCloudlet.setRam(0);
                tasks.add(gpuCloudlet);
            }
        }
        BasicClustering clustering = new BasicClustering();
        GpuJob job = clustering.addTasks2Job(tasks);
        job.setCloudletId(id);
        job.setPeriod(period);
        job.setName(name);
        return job;
    }

    public String print() {
        AsciiTable at = new AsciiTable();
        at.addRule();
        at.addRow("name", "period", "memory", "");
        at.addRule();
        at.addRow(name, period, cpuTask.cpuTaskInfo.ram, "");
        at.addRule();
        if(generator != null) {
            at.addRow("faultGenerator", "scale", "shape", "");
            at.addRule();
            at.addRow(generator.getType(), generator.getScale(), generator.getShape(), "");
            at.addRule();
        }
        at.addRow("Device", "cores", "mi", "");
        at.addRule();
        at.addRow("CPU", cpuTask.cpuTaskInfo.pesNumber, cpuTask.cpuTaskInfo.length, "");
        at.addRule();
        if(gpuTask != null && !gpuTask.gpuTaskInfo.kernels.isEmpty()) {
            int i = 0;
            at.addRow("Kernel", "blockNum", "threadPerBlock", "FLOP");
            at.addRule();
            for(GPUTaskInfo.Kernel kernel: gpuTask.gpuTaskInfo.kernels) {
                at.addRow(i, kernel.blockNum, kernel.blockNum, kernel.threadLength);
                at.addRule();
                i++;
            }
        }
        at.setTextAlignment(TextAlignment.CENTER);
        Log.printLine(at.render());
        return at.render();
    }
}
