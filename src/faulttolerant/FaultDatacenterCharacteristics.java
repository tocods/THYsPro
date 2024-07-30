package faulttolerant;

import cloudsim.DatacenterCharacteristics;
import cloudsim.Host;
import faulttolerant.faultGenerator.FaultGenerator;
import faulttolerant.faultInject.FaultInjector;

import java.util.List;
import java.util.Map;

public class FaultDatacenterCharacteristics extends DatacenterCharacteristics {
    private Map<Host, FaultGenerator> hostId2FaultInject = null;
    /**
     * Creates a new DatacenterCharacteristics object. If the time zone is invalid, then by
     * default, it will be GMT+0.
     *
     * @param architecture   the architecture of the datacenter
     * @param os             the operating system used on the datacenter's PMs
     * @param vmm            the virtual machine monitor used
     * @param hostList       list of machines in the datacenter
     * @param timeZone       local time zone of a user that owns this reservation. Time zone should be of
     *                       range [GMT-12 ... GMT+13]
     * @param costPerSec     the cost per sec of CPU use in the datacenter
     * @param costPerMem     the cost to use memory in the datacenter
     * @param costPerStorage the cost to use storage in the datacenter
     * @param costPerBw      the cost of each byte of bandwidth (bw) consumed
     * @pre architecture != null
     * @pre OS != null
     * @pre VMM != null
     * @pre machineList != null
     * @pre timeZone >= -12 && timeZone <= 13
     * @pre costPerSec >= 0.0
     * @pre costPerMem >= 0
     * @pre costPerStorage >= 0
     * @post $none
     */
    public FaultDatacenterCharacteristics(String architecture, String os, String vmm, List<? extends Host> hostList, double timeZone, double costPerSec, double costPerMem, double costPerStorage, double costPerBw) {
        super(architecture, os, vmm, hostList, timeZone, costPerSec, costPerMem, costPerStorage, costPerBw);
    }

    public void setHostId2FaultInject(Map<Host, FaultGenerator> hostId2FaultInject) {
        this.hostId2FaultInject = hostId2FaultInject;
    }

    public FaultGenerator getHostId2FaultInject(Host h) {
        if(hostId2FaultInject == null)
            return null;
        return hostId2FaultInject.getOrDefault(h, null);
    }
}
