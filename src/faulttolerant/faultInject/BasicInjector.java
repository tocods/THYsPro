package faulttolerant.faultInject;

import api.service.Service;
import cloudsim.Host;
import cloudsim.Log;
import cloudsim.core.CloudSim;
import faulttolerant.FaultTolerantTags;
import faulttolerant.faultGenerator.FaultGenerator;
import faulttolerant.faultGenerator.NormalGenerator;
import gpu.GpuCloudlet;
import gpu.GpuTask;
import workflow.GpuJob;
import workflow.taskCluster.BasicClustering;

import java.util.HashMap;
import java.util.Map;

public class BasicInjector implements FaultInjector{
    private final Map<String, FaultGenerator> task2Generator;
    private Map<Integer, FaultGenerator> host2Generator;

    public BasicInjector() {
        task2Generator = new HashMap<>();
        host2Generator = new HashMap<>();
    }


    @Override
    public void initHostGenerator(Host host, FaultGenerator faultGenerator) {
        host2Generator.put(host.getId(), faultGenerator);
    }

    @Override
    public void initJobGenerator(String job, FaultGenerator faultGenerator) {
        task2Generator.put(job, faultGenerator);
    }

    @Override
    public boolean ifJobFail(GpuJob task) {
        boolean ifFail = false;
        FaultGenerator faultGenerator = task2Generator.get(task.getName());
        if(faultGenerator == null) {
            if(FaultTolerantTags.IF_TEST) {
                faultGenerator = new NormalGenerator();
                faultGenerator.initSamples(100, 100);
            }else {
                //Log.printLine(task.getName() + "无错误注入");
                return false;
            }
        }
        double[] samples = faultGenerator.getCumulativeSamples();
        // 如果错误时间点的最后一个都小于任务的开始时间，我们生成一系列新的错误时间点
        //Log.printLine("开始时间: " + task.getExecStartTime() + " 结束时间: " + CloudSim.clock());
        while(samples[samples.length - 1] < task.getExecStartTime()){
            faultGenerator.extendSample();
            samples = faultGenerator.getCumulativeSamples();
        }
        for (double sample : samples) {
            if (CloudSim.clock() < sample) {
                //如果第一个错误时间点都在任务执行结束之后，任务当然执行成功
                break;
            }
            if (task.getExecStartTime() <= sample) {
                //有错误
                faultGenerator.getNextSample();
                ifFail = true;
                break;
            }
        }
        task2Generator.put(task.getName(), faultGenerator);
        return ifFail;
    }

    @Override
    public double nextHostFailTime(Host host, double currentTime) {
        double nextTime = Double.MAX_VALUE;
        FaultGenerator faultGenerator = host2Generator.get(host.getId());
        if(faultGenerator == null) {
            if(FaultTolerantTags.IF_TEST) {
                faultGenerator = new NormalGenerator();
                faultGenerator.initSamples(100, 100);
            }else
                return Double.MAX_VALUE;
        }
        double[] samples = faultGenerator.getCumulativeSamples();
        // 如果错误时间点的最后一个都小于当前时间，我们生成一系列新的错误时间点
        if(samples[samples.length - 1] < currentTime){
            faultGenerator.extendSample();
            samples = faultGenerator.getCumulativeSamples();
        }
        for (double sample : samples) {
            //Log.printLine(sample);
            if (currentTime < sample) {
                //有错误
                nextTime = sample;
                break;
            }
        }
        host2Generator.put(host.getId(), faultGenerator);
        return nextTime;
    }

    public static void main(String[] args) {
        FaultTolerantTags.IF_TEST = true;
        BasicInjector injector = new BasicInjector();
        BasicClustering clustering = new BasicClustering();
        GpuCloudlet t = Service.createGpuCloudlet(0, 0, 0, 100, 1, 2, 1,clustering);
        t.setExecStartTime(100.0);
        t.setFinishTime(200.0);
        System.out.println(injector.ifJobFail((GpuJob) t));
        Host h = Service.createHosts(0);
        System.out.println(injector.nextHostFailTime(h, 100.0));
    }
}
