package cloudsim.container.resourceAllocatorMigrationEnabled;

import cloudsim.container.containerSelectionPolicies.PowerContainerSelectionPolicy;
import cloudsim.container.core.ContainerDatacenter;
import cloudsim.container.core.ContainerHost;
import cloudsim.container.core.ContainerVm;
import cloudsim.container.core.PowerContainerHost;
import cloudsim.container.hostSelectionPolicies.HostSelectionPolicy;
import cloudsim.container.vmSelectionPolicies.PowerContainerVmSelectionPolicy;

import java.util.List;

/**
 * Created by sareh on 18/08/15.
 */
public class PowerContainerVmAllocationPolicyMigrationStaticThresholdMCUnderUtilized extends PowerContainerVmAllocationPolicyMigrationAbstractContainerHostSelectionUnderUtilizedAdded{


    /**
     * The utilization threshold.
     */
    private double utilizationThreshold = 0.9;

    /**
     * Instantiates a new power vm allocation policy migration mad.
     *
     * @param hostList             the host list
     * @param vmSelectionPolicy    the vm selection policy
     * @param utilizationThreshold the utilization threshold
     */
    public PowerContainerVmAllocationPolicyMigrationStaticThresholdMCUnderUtilized(
            List<? extends ContainerHost> hostList,
            PowerContainerVmSelectionPolicy vmSelectionPolicy, PowerContainerSelectionPolicy containerSelectionPolicy,
            HostSelectionPolicy hostSelectionPolicy, double utilizationThreshold, double underUtilizationThresh,
            int numberOfVmTypes, int[] vmPes, float[] vmRam, long vmBw, long vmSize, double[] vmMips) {
        super(hostList, vmSelectionPolicy, containerSelectionPolicy, hostSelectionPolicy,underUtilizationThresh,
        		 numberOfVmTypes, vmPes, vmRam, vmBw, vmSize, vmMips);
        setUtilizationThreshold(utilizationThreshold);
    }

    /**
     * Checks if is host over utilized.
     *
     * @param host the _host
     * @return true, if is host over utilized
     */
    @Override
    protected boolean isHostOverUtilized(PowerContainerHost host) {
        addHistoryEntry(host, getUtilizationThreshold());
        double totalRequestedMips = 0;
        for (ContainerVm vm : host.getVmList()) {
            totalRequestedMips += vm.getCurrentRequestedTotalMips();
        }
        double utilization = totalRequestedMips / host.getTotalMips();
        return utilization > getUtilizationThreshold();
    }

    @Override
    protected boolean isHostUnderUtilized(PowerContainerHost host) {
        return false;
    }

    /**
     * Sets the utilization threshold.
     *
     * @param utilizationThreshold the new utilization threshold
     */
    protected void setUtilizationThreshold(double utilizationThreshold) {
        this.utilizationThreshold = utilizationThreshold;
    }

    /**
     * Gets the utilization threshold.
     *
     * @return the utilization threshold
     */
    protected double getUtilizationThreshold() {
        return utilizationThreshold;
    }
    @Override
    public void setDatacenter(ContainerDatacenter datacenter) {
        super.setDatacenter(datacenter);
    }



}
