package workflow;

import cloudsim.UtilizationModel;
import gpu.GpuTask;

public class Kernel extends GpuTask {
    public Kernel(int taskId, long blockLength, int numberOfBlocks, long inputSize, long outputSize, long requestedGddramSize, UtilizationModel utilizationModelGpu, UtilizationModel utilizationModelGddram, UtilizationModel utilizationModelBw) {
        super(taskId, blockLength, numberOfBlocks, inputSize, outputSize, requestedGddramSize, utilizationModelGpu, utilizationModelGddram, utilizationModelBw);
    }
}
