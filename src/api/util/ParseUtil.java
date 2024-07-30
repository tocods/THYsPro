/**
 * Copyright 2019-2020 University Of Southern California
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.com.wfc.cloudsim/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package api.util;


import api.info.*;
import cloudsim.Log;
import faulttolerant.faultGenerator.*;
import gpu.GpuCloudlet;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import workflow.Parameters;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public final class ParseUtil {
    List<HostInfo> hostInfos;

    List<JobInfo> jobInfos;
    public ParseUtil() {
        hostInfos = new ArrayList<>();
        jobInfos = new ArrayList<>();
    }

    public List<HostInfo> getHostInfos() {
        return this.hostInfos;
    }

    public List<JobInfo> getJobInfos() {
        return this.jobInfos;
    }

    private VideoCardInfo.GPUInfo parseGpuInfo(Element gpu) {
        VideoCardInfo.GPUInfo gpuInfo = new VideoCardInfo.GPUInfo();
        for(Element property: gpu.getChildren()) {
            switch (property.getName()) {
                case "memory":
                    gpuInfo.gddram = Integer.parseInt(property.getValue());
                    break;
                case "cores":
                    gpuInfo.cores = Integer.parseInt(property.getValue());
                    break;
                case "coresPerSM":
                    gpuInfo.corePerSM = Integer.parseInt(property.getValue());
                    break;
                case "flops":
                    gpuInfo.flopsPerCore = Integer.parseInt(property.getValue());
                    break;
                case "maxBlockNum":
                    gpuInfo.maxBlockPerSM = Integer.parseInt(property.getValue());
                    break;

            }
        }
        return gpuInfo;
    }

    private VideoCardInfo parseVideoCardInfo(Element videoCard) {
        VideoCardInfo videoCardInfo = new VideoCardInfo();
        videoCardInfo.gpuInfos = new ArrayList<>();
        for(Element property: videoCard.getChildren()) {
            switch (property.getName()) {
                case "name":
                    videoCardInfo.name = property.getValue();
                    break;
                case "pcieBandwidth":
                    videoCardInfo.PCIeBW = Long.parseLong(property.getValue());
                    break;
                case "gpus":
                    int num = Integer.parseInt(property.getAttributeValue("num"));
                    for(int i = 0; i < num; i++){
                        videoCardInfo.gpuInfos.add(parseGpuInfo(property));
                    }
                    break;
            }
        }
        return videoCardInfo;
    }

    private CPUInfo parseCpuInfo(Element cpu) {
        CPUInfo cpuInfo = new CPUInfo();
        for(Element property: cpu.getChildren()) {
            switch (property.getName()) {
                case "name":
                    cpuInfo.name = property.getValue();
                    break;
                case "cores":
                    cpuInfo.cores = Integer.parseInt(property.getValue());
                    break;
                case "flops":
                    cpuInfo.mips = Integer.parseInt(property.getValue());
                    break;
            }
        }
        return cpuInfo;
    }

    private int parseRamInfo(Element ram) {
        int ret = -1;
        for(Element property: ram.getChildren()){
            if(property.getName().equals("size"))
                ret = Integer.parseInt(property.getValue());
        }
        return ret;
    }

    private FaultGenerator parseFaultGenerator(Element generator) {
        String type = generator.getAttributeValue("type");
        double scale = Double.MAX_VALUE, shape = Double.MAX_VALUE;
        for(Element property: generator.getChildren()) {
            switch (property.getName()) {
                case "scale":
                    scale = Double.parseDouble(property.getValue());
                    break;
                case "shape":
                    shape = Double.parseDouble(property.getValue());
                    break;
            }
        }
        if(shape == Double.MAX_VALUE || scale == Double.MAX_VALUE)
            return null;
        FaultGenerator ret = null;
        if(type == null || type.isEmpty())
            ret = new NormalGenerator();
        switch (Objects.requireNonNull(type)) {
            case "Gamma" :
                ret = new GammaGenerator();
                break;
            case "Weibull":
                ret = new WeibullGenerator();
                break;
            case "LogNormal":
                ret = new LogNormalGenerator();
                break;
            default:
                ret = new NormalGenerator();
        }
        ret.initSamples(scale, shape);
        return ret;
    }

    public String parseHostXml(File f) {
        try {
            StringBuilder ret = new StringBuilder();
            SAXBuilder builder = new SAXBuilder();
            Document dom = builder.build(f);
            Element root = dom.getRootElement();
            List<Element> list = root.getChildren();
            for (Element host : list) {
                if(!host.getName().equals("host")) {
                    Log.printLine("标签为" + host.getName() + "项，应为host");
                    continue;
                }
                HostInfo hostInfo = new HostInfo();
                String name = host.getAttributeValue("name");
                hostInfo.name = name;
                hostInfo.cpuInfos = new ArrayList<>();
                hostInfo.videoCardInfos = new ArrayList<>();
//                Boolean ifGPU = false;
                for(Element property: host.getChildren()) {
                    switch (property.getName()) {
                        case "videoCard":
//                            Log.printLine("解析节点"+name+"的显卡信息");
//                            ifGPU = true;
                            hostInfo.videoCardInfos.add(parseVideoCardInfo(property));
                            break;
                        case "cpus":
//                            Log.printLine("解析节点"+name+"CPU信息");
                            int num = Integer.parseInt(property.getAttributeValue("num"));
                            for(int i = 0; i < num; i++){
                                hostInfo.cpuInfos.add(parseCpuInfo(property));
                            }
                            break;
                        case "memory":
//                            Log.printLine("解析节点"+name+"内存信息");
                            hostInfo.ram = parseRamInfo(property);
                            break;
                        case "faultInjection":
                            hostInfo.generator = parseFaultGenerator(property);
                            break;
                        default:
                            Log.printLine("包含无法解析的字段：" + property.getName());
                    }
                }
//                Log.printLine(name + " | " + hostInfo.cpuInfos.get(0).cores + " | " + hostInfo.cpuInfos.get(0).mips);
//                Log.printLine(name + " | " + hostInfo.ram);
//                if(ifGPU) {
//                    Log.printLine(name + " | " + hostInfo.videoCardInfos.get(0).gpuInfos.get(0).cores + " | " + hostInfo.videoCardInfos.get(0).gpuInfos.get(0).corePerSM + " | " + hostInfo.videoCardInfos.get(0).gpuInfos.get(0).flopsPerCore + " | " + hostInfo.videoCardInfos.get(0).gpuInfos.get(0).gddram + " | " + hostInfo.videoCardInfos.get(0).gpuInfos.get(0).maxBlockPerSM);
//                }
                ret.append('\n');
                ret.append(hostInfo.print());
                hostInfos.add(hostInfo);
            }
            return ret.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public void parseHostJson(File f) {

    }

    private CPUTaskInfo parseCPUTaskInfo(Element cpuTask) {
        CPUTaskInfo cpuTaskInfo = new CPUTaskInfo();
        for(Element property: cpuTask.getChildren()) {
            switch (property.getName()) {
                case "cores":
                    cpuTaskInfo.pesNumber = Integer.parseInt(property.getValue());
                    break;
                case "flop":
                    cpuTaskInfo.length = Integer.parseInt(property.getValue());
                    break;
            }
        }
        cpuTaskInfo.fileSize = 0;
        cpuTaskInfo.outputSize = 0;
        return cpuTaskInfo;
    }

    private GPUTaskInfo parseGPUTaskInfo(Element gpuTask) {
        GPUTaskInfo gpuTaskInfo = new GPUTaskInfo();
        gpuTaskInfo.kernels = new ArrayList<>();
        for(Element property: gpuTask.getChildren()) {
            switch (property.getName()) {
                case "memory":
                    gpuTaskInfo.requestedGddramSize = Integer.parseInt(property.getValue());
                    break;
                case "inputSize":
                    gpuTaskInfo.taskInputSize = Integer.parseInt(property.getValue());
                    break;
                case "outputSize":
                    gpuTaskInfo.taskOutputSize = Integer.parseInt(property.getValue());
                    break;
                case "kernels":
                    for(Element kernel: property.getChildren()) {
                        if(!kernel.getName().equals("kernel"))
                            continue;
                        int blockNum = 0;
                        int threadNum = 0;
                        int flop = 0;
                        for(Element blockProperty: kernel.getChildren()) {
                            switch (blockProperty.getName()) {
                                case "blockNum":
                                    blockNum = Integer.parseInt(blockProperty.getValue());
                                    break;
                                case "threadNum":
                                    threadNum = Integer.parseInt(blockProperty.getValue());
                                    break;
                                case "flop":
                                    flop = Integer.parseInt(blockProperty.getValue());
                                    break;
                            }
                        }
                        GPUTaskInfo.Kernel kernelInfo = new GPUTaskInfo.Kernel();
                        kernelInfo.blockNum = blockNum;
                        kernelInfo.threadLength = flop;
                        kernelInfo.threadNum = threadNum;
                        gpuTaskInfo.kernels.add(kernelInfo);
                    }
                    break;

            }
        }
        return gpuTaskInfo;
    }

    public String parseJobXml(File f) {
        try {
            StringBuilder ret = new StringBuilder();
            SAXBuilder builder = new SAXBuilder();
            Document dom = builder.build(f);
            Element root = dom.getRootElement();
            List<Element> list = root.getChildren();
            for (Element job : list) {
                if(!job.getName().equals("job") && !job.getName().equals("duration")) {
                    Log.printLine("标签为" + job.getName() + "项，应为job / duration");
                    continue;
                }
                if(job.getName().equals("duration")) {
                    Parameters.duration = Double.parseDouble(job.getValue());
                    continue;
                }
                JobInfo jobInfo = new JobInfo();
                double memory = 0.0;
                if(job.getAttributeValue("period") != null)
                    jobInfo.period = Double.parseDouble(job.getAttributeValue("period"));
                else
                    jobInfo.period = 0;
                jobInfo.name = job.getAttributeValue("name");
                for(Element property: job.getChildren()) {
                    switch (property.getName()) {
                        case "cpu":
                            JobInfo.TaskInfo taskInfo = new JobInfo.TaskInfo();
                            taskInfo.cpuTaskInfo = parseCPUTaskInfo(property);
                            taskInfo.gpuTaskInfo = null;
                            jobInfo.cpuTask = taskInfo;
                            break;
                        case "gpu":
                            JobInfo.TaskInfo taskInfo1 = new JobInfo.TaskInfo();
                            taskInfo1.cpuTaskInfo = null;
                            taskInfo1.gpuTaskInfo = parseGPUTaskInfo(property);
                            jobInfo.gpuTask = taskInfo1;
                            break;
                        case "memory":
                            memory = Double.parseDouble(property.getChildren().get(0).getValue());
                            break;
                        case "faultInjection":
                            jobInfo.generator = parseFaultGenerator(property);
                            break;
                        default:
                            Log.printLine("包含无法解析的字段：" + property.getName());
                    }
                }
                jobInfo.cpuTask.cpuTaskInfo.ram = memory;
                ret.append('\n');
                ret.append(jobInfo.print());
                jobInfos.add(jobInfo);
            }
            return ret.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

}
