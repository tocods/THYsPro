package api.info;

import cloudsim.Pe;
import cloudsim.provisioners.PeProvisionerSimple;

import java.util.ArrayList;
import java.util.List;

public class CPUInfo {
    public String name;
    public double mips;
    public Integer cores;
    public Integer intMips;
    public Integer matrixMips;

    public List<Pe> tran2Pes() {
        List<Pe> pes = new ArrayList<>();
        for(int i = 0; i < cores; i++)
            pes.add(new Pe(0, new PeProvisionerSimple(mips), new PeProvisionerSimple(intMips), new PeProvisionerSimple(matrixMips)));
        return pes;
    }
}
