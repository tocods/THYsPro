package api.info;

import cloudsim.UtilizationModel;
import cloudsim.UtilizationModelFull;
import gpu.GpuTask;

import java.util.ArrayList;
import java.util.List;

public class GPUTaskInfo {
    public long taskInputSize;
    public long taskOutputSize;
    public long requestedGddramSize;

    public List<Kernel> kernels;

    public static class Kernel {

        public Integer blockNum;
        public Integer threadNum;
        public long threadLength;
    }

    /**
     *
     * @param cloudletId 所属总任务的ID
     * @return
     */
    public List<GpuTask> tran2GpuTask(int cloudletId, int idStart) {
        if(kernels.isEmpty())
            return null;
        List<GpuTask> gpuTasks = new ArrayList<>();
        int id = 0;
        for(Kernel kernel: kernels) {
            long taskLength = kernel.threadLength * kernel.threadNum;
            UtilizationModel gpuUtilizationModel = new UtilizationModelFull();
            UtilizationModel gddramUtilizationModel = new UtilizationModelFull();
            UtilizationModel gddramBwUtilizationModel = new UtilizationModelFull();
            if(kernels.size() == 1) {
                GpuTask gpuTask = new GpuTask(id + idStart, taskLength, kernel.blockNum, taskInputSize, taskOutputSize,
                        requestedGddramSize, gpuUtilizationModel, gddramUtilizationModel, gddramBwUtilizationModel);
                gpuTask.setName("Kernel_" + cloudletId + "_" + id);
                gpuTasks.add(gpuTask);
                break;
            }
            if(id == 0) {
                GpuTask gpuTask = new GpuTask(id + idStart, taskLength, kernel.blockNum, taskInputSize, 0,
                        requestedGddramSize, gpuUtilizationModel, gddramUtilizationModel, gddramBwUtilizationModel);
                gpuTask.setName("Kernel_" + cloudletId + "_" + id);
                gpuTasks.add(gpuTask);
            } else if(id == kernels.size() - 1) {
                GpuTask gpuTask = new GpuTask(id + idStart, taskLength, kernel.blockNum, 0, taskOutputSize,
                        requestedGddramSize, gpuUtilizationModel, gddramUtilizationModel, gddramBwUtilizationModel);
                gpuTask.setName("Kernel_" + cloudletId + "_" + id);
                gpuTasks.add(gpuTask);
            } else {
                GpuTask gpuTask = new GpuTask(id + idStart, taskLength, kernel.blockNum, 0, 0,
                        requestedGddramSize, gpuUtilizationModel, gddramUtilizationModel, gddramBwUtilizationModel);
                gpuTask.setName("Kernel_" + cloudletId + "_" + id);
                gpuTasks.add(gpuTask);
            }
            id ++;
        }
        return gpuTasks;
    }
}
