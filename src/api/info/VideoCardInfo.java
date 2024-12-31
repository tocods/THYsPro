package api.info;

import cloudsim.Log;
import cloudsim.Pe;
import cloudsim.provisioners.PeProvisionerSimple;
import gpu.*;
import gpu.hardware_assisted.grid.GridPerformanceVgpuSchedulerFairShare;
import gpu.hardware_assisted.grid.GridVideoCardPowerModelK1;
import gpu.hardware_assisted.grid.GridVideoCardTags;
import gpu.performance.models.PerformanceModel;
import gpu.performance.models.PerformanceModelGpuConstant;
import gpu.power.PowerVideoCard;
import gpu.power.models.VideoCardPowerModel;
import gpu.provisioners.GpuBwProvisionerShared;
import gpu.provisioners.GpuGddramProvisionerSimple;
import gpu.provisioners.VideoCardBwProvisioner;
import gpu.provisioners.VideoCardBwProvisionerShared;
import gpu.selection.PgpuSelectionPolicy;
import gpu.selection.PgpuSelectionPolicyNull;

import java.util.ArrayList;
import java.util.List;

public class VideoCardInfo {
    public String name;

    public List<GPUInfo> gpuInfos;

    public long PCIeBW;

    public List<GPUInfo> getGpuInfos() {
        return gpuInfos;
    }

    public VideoCard tran2Entity(int id) {
        int gpuId = 0;
        List<Pgpu> gpus = new ArrayList<>();
        for(GPUInfo info: getGpuInfos()) {
            List<Pe> blocks = new ArrayList<>();
            int SMNum = info.cores / info.corePerSM;
            int blockNum = SMNum * info.maxBlockPerSM;
            double flopsPerSM = (info.flopsPerCore / info.cores) * info.corePerSM * 1000; //基准单位是GFLOPS，但输入文件里GPU的计算指标单位是TFLOPS
            double flopsPerBlock = flopsPerSM / info.maxBlockPerSM;
            double iflopsPerSM = (info.intFlopsPerCore / info.cores) * info.corePerSM * 1000; //基准单位是GFLOPS，但输入文件里GPU的计算指标单位是TFLOPS
            double iflopsPerBlock = iflopsPerSM / info.maxBlockPerSM;
            double mflopsPerSM = (info.matrixFlopsPerCore / info.cores) * info.corePerSM * 1000; //基准单位是GFLOPS，但输入文件里GPU的计算指标单位是TFLOPS
            double mflopsPerBlock = mflopsPerSM / info.maxBlockPerSM;
            for(int peId = 0; peId < blockNum; peId ++) {
                Pe block = new Pe(peId, new PeProvisionerSimple(flopsPerBlock), new PeProvisionerSimple(iflopsPerBlock), new PeProvisionerSimple(mflopsPerBlock));
                blocks.add(block);
            }
            Pgpu pgpu = new Pgpu(gpuId, GridVideoCardTags.NVIDIA_K1_GPU_TYPE, blocks,
                    new GpuGddramProvisionerSimple(info.gddram * 1024), new GpuBwProvisionerShared(info.bw));
            GpuTaskSchedulerLeftover scheduler = new GpuTaskSchedulerLeftover();
            scheduler.initSMs(info.cores, info.corePerSM, info.maxBlockPerSM);
            scheduler.setGddram(info.gddram * 1024);
            pgpu.setGpuTaskScheduler(scheduler);
            gpuId ++;
            gpus.add(pgpu);
        }
        PgpuSelectionPolicy pgpuSelectionPolicy = new PgpuSelectionPolicyNull();
        // Performance Model
        double performanceLoss = 0.1;
        PerformanceModel<VgpuScheduler, Vgpu> performanceModel = new PerformanceModelGpuConstant(performanceLoss);
        // Scheduler
        GridPerformanceVgpuSchedulerFairShare vgpuScheduler = new GridPerformanceVgpuSchedulerFairShare(
                GridVideoCardTags.NVIDIA_K1_CARD, gpus, pgpuSelectionPolicy, performanceModel);
        // PCI Express Bus Bw Provisioner
        VideoCardBwProvisioner videoCardBwProvisioner = new VideoCardBwProvisionerShared(PCIeBW * 1024);
        // Video Card Power Model
        VideoCardPowerModel videoCardPowerModel = new GridVideoCardPowerModelK1(false);
        // Create a video card
        PowerVideoCard videoCard = new PowerVideoCard(id, GridVideoCardTags.NVIDIA_K1_CARD, vgpuScheduler,
                videoCardBwProvisioner, videoCardPowerModel);
        return videoCard;

    }

    public static class GPUInfo {
        public double flopsPerCore;
        public double intFlopsPerCore;
        public double matrixFlopsPerCore;
        public int gddram;
        public long bw;

        public int cores;

        public int corePerSM;

        public int maxBlockPerSM;


    }
}
