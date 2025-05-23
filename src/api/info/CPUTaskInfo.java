package api.info;

import cloudsim.Log;
import cloudsim.UtilizationModelFull;
import com.alibaba.fastjson.annotation.JSONField;
import gpu.GpuCloudlet;
import gpu.GpuTask;

import java.util.List;

public class CPUTaskInfo {

    public long length;

    public double ram;

    public long fileSize;

    public long outputSize;

    public int pesNumber;

    public GpuCloudlet tran2Cloudlet(Integer id, GpuTask task) {
        Log.printLine(pesNumber);
        Log.printLine(length);
        GpuCloudlet gpuCloudlet = new GpuCloudlet(id, length, pesNumber, fileSize, outputSize,
                new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull(), task, false);
        gpuCloudlet.setName("Task_" + id);
        gpuCloudlet.setRam(ram);
        return gpuCloudlet;
    }
}
