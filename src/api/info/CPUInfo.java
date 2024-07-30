package api.info;

import cloudsim.Pe;
import cloudsim.provisioners.PeProvisionerSimple;

import java.util.ArrayList;
import java.util.List;

public class CPUInfo {
    public String name;
    public double mips;
    public Integer cores;

    public List<Pe> tran2Pes() {
        List<Pe> pes = new ArrayList<>();
        for(int i = 0; i < cores; i++)
            pes.add(new Pe(0, new PeProvisionerSimple(mips)));
        return pes;
    }
}
