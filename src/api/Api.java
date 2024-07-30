package api;

import api.service.Service;
import api.util.ParseUtil;
import gpu.GpuCloudlet;
import gpu.power.PowerGpuHost;
import cloudsim.ParameterException;
import workflow.Parameters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Api {
    /**
     * 输入文件解析器
     */
    private ParseUtil parser;

    private Result doSchema(File file) {
        Result ret = Result.success(null);
        return ret;
    }



    /**
     *  设置集群中的物理节点
     */

    private List<PowerGpuHost> hostList;
    public void setHosts(List<PowerGpuHost> hosts) {
        hostList = new ArrayList<>(hosts);
    }

    public List<PowerGpuHost> getHosts() {
        return hostList;
    }

    /**
     *  设置集群中的任务(pod、container...)
     */

    private List<GpuCloudlet> taskList;

    public void setTasks(List<GpuCloudlet> tasks) {
        taskList = new ArrayList<>(tasks);
    }

    public List<GpuCloudlet> getTasks() {
        return taskList;
    }

    /**
     * 仿真
     */
    public Result start(Parameters.JobAllocationAlgorithm algorithm, String outputPath) {
        Service service = new Service(getHosts(), getTasks());
        String ret = "";
        service.setAlgorithm(algorithm);
        try {
            ret = service.start(outputPath);
        }catch (Exception e) {
            return Result.error(e.getMessage(), "仿真失败");
        }
        return Result.success(ret);
    }
}
