package cloudsim.container.hostSelectionPolicies;

import cloudsim.container.core.ContainerHost;
import cloudsim.container.utils.RandomGen;
import cloudsim.core.CloudSim;

import java.util.List;
import java.util.Set;

/**
 * Created by sareh on 12/08/15.
 */
public class HostSelectionPolicyRandomSelection extends  HostSelectionPolicy {

    @Override
    public ContainerHost getHost(List<ContainerHost> hostList, Object obj, Set<? extends ContainerHost> excludedHostList) {
        ContainerHost host = null;

        if (CloudSim.clock() > 1.0) {
            while (true) {
                if (hostList.size() > 0) {
                    int randomNum = new RandomGen().getNum(hostList.size());
//                System.out.format("The Selection Algorithm has chosen: %d from %d%n",  randomNum, hostList.size());

                    host = hostList.get(randomNum);
                    if (excludedHostList.contains(host)) {
                        continue;
                    }
                } else {

                    System.out.println("Error");
                }

                return host;
            }
        } else {

//            At the simulation start all the VMs by leastFull algorithms.

            host = new HostSelectionPolicyFirstFit().getHost(hostList,obj ,excludedHostList);

            return host;
        }
    }
}