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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasicInjector implements FaultInjector{
    private final Map<String, FaultGenerator> task2Generator;
    private Map<Integer, List<FaultGenerator>> host2Generator;

    private Map<Integer, List<FaultGenerator>> gpu2Generator;

    private Map<Integer, FaultGenerator> lastHost;

    public BasicInjector() {
        task2Generator = new HashMap<>();
        host2Generator = new HashMap<>();
        gpu2Generator = new HashMap<>();
        lastHost = new HashMap<>();
    }


    @Override
    public void initHostGenerator(Host host, List<FaultGenerator> faultGenerators) {
        List<FaultGenerator> host2s = new ArrayList<>();
        List<FaultGenerator> gpu2s = new ArrayList<>();
        for(FaultGenerator faultGenerator: faultGenerators) {
            if(faultGenerator.getFaultType().equals("gpu"))
                gpu2s.add(faultGenerator);
            else
                host2s.add(faultGenerator);
        }
        host2Generator.put(host.getId(), host2s);
        gpu2Generator.put(host.getId(), gpu2s);
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
        List<FaultGenerator> faultGenerators = host2Generator.get(host.getId());
        double minTime = Double.MAX_VALUE;
        FaultGenerator generator = null;
        if(faultGenerators != null && !faultGenerators.isEmpty())
        {
            for (FaultGenerator faultGenerator : faultGenerators) {
                if (faultGenerator == null) {
                    if (FaultTolerantTags.IF_TEST) {
                        faultGenerator = new NormalGenerator();
                        faultGenerator.initSamples(100, 100);
                    } else
                        return Double.MAX_VALUE;
                }
                double[] samples = faultGenerator.getCumulativeSamples();
                // 如果错误时间点的最后一个都小于当前时间，我们生成一系列新的错误时间点
                if (samples[samples.length - 1] < currentTime) {
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
                if (nextTime < minTime) {
                    minTime = nextTime;
                    generator = faultGenerator;
                }
                //host2Generator.put(host.getId(), faultGenerator);
            }
        }
        faultGenerators = gpu2Generator.get(host.getId());
        if(faultGenerators != null && !faultGenerators.isEmpty())
        {
            for (FaultGenerator faultGenerator : faultGenerators) {
                if (faultGenerator == null) {
                    if (FaultTolerantTags.IF_TEST) {
                        faultGenerator = new NormalGenerator();
                        faultGenerator.initSamples(100, 100);
                    } else
                        return Double.MAX_VALUE;
                }
                double[] samples = faultGenerator.getCumulativeSamples();
                // 如果错误时间点的最后一个都小于当前时间，我们生成一系列新的错误时间点
                if (samples[samples.length - 1] < currentTime) {
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
                if (nextTime < minTime) {
                    minTime = nextTime;
                    generator = faultGenerator;
                }
                //host2Generator.put(host.getId(), faultGenerator);
            }
        }
        if(generator != null) {
            Log.printLine("主机" + host.getName() + "的" + generator.getFaultType() + "发生故障, 时间为：" + nextTime);
            lastHost.put(host.getId(), generator);
        }
        return nextTime;
    }

    @Override
    public double hostRepairTime(Host host) {
        FaultGenerator faultGenerator = lastHost.get(host.getId());
        Log.printLine(host.getName() + "的" + faultGenerator.getFaultType() + "发生了故障，下一次恢复要再过" + faultGenerator.getRepairTime());
        return Math.max(0, faultGenerator.getRepairTime());
    }

    @Override
    public String lastFaultType(Host h) {
        if(lastHost.containsKey(h.getId()))
            return lastHost.get(h.getId()).getFaultType();
        return "";
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
