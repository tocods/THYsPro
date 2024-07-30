package api.info;

import cloudsim.Log;
import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import faulttolerant.faultGenerator.FaultGenerator;

import java.util.List;

public class HostInfo {

    public String name;
    public List<VideoCardInfo> videoCardInfos;
    public List<CPUInfo> cpuInfos;

    public FaultGenerator generator = null;
    public int ram;

    public String print() {
        AsciiTable at = new AsciiTable();
        at.addRule();
        at.addRow("Name", "FaultGenerator", "Scale", "Shape", "", "");
        at.addRule();
        if(generator == null)
            at.addRow(name,"", "", "", "", "");
        else
            at.addRow(name, generator.getType(), generator.getScale(), generator.getShape(), "", "");
        at.addRule();
        int i = 0;
        for(CPUInfo cpuInfo: cpuInfos) {
            at.addRow("CPU", "cores", "mips", "", "", "");
            at.addRule();
            at.addRow(i, cpuInfo.cores, cpuInfo.mips, "", "", "");
            at.addRule();
            i++;
        }
        if(videoCardInfos != null && !videoCardInfos.isEmpty()) {
            i = 0;
            at.addRow("GPU", "cores", "coresPerSM", "maxBlockPerSM", "Gddram", "FLOPS");
            at.addRule();
            for(VideoCardInfo videoCardInfo: videoCardInfos) {
                for(VideoCardInfo.GPUInfo gpuInfo: videoCardInfo.getGpuInfos()) {
                    at.addRow(i, gpuInfo.cores, gpuInfo.corePerSM, gpuInfo.maxBlockPerSM, gpuInfo.gddram, gpuInfo.flopsPerCore);
                    at.addRule();
                    i++;
                }
            }
        }
        at.setTextAlignment(TextAlignment.CENTER);
        Log.printLine(at.render());
        return at.render();
    }
}
