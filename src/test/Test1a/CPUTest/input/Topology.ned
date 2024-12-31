import inet.linklayer.configurator.gatescheduling.contract.IGateScheduleConfigurator;
import inet.networklayer.configurator.contract.INetworkConfigurator;
import inet.node.tsn.TsnDevice;
import inet.node.tsn.TsnSwitch;
import inet.networks.base.WiredNetworkBase;
import inet.node.contract.IEthernetNetworkNode;
import inet.node.ethernet.EthernetLink;
import inet.node.ethernet.Eth100M;
import inet.node.inet.StandardHost;
import inet.node.ethernet.EthernetSwitch;
import inet.node.inet.DDSStandardHost;
import inet.node.rocev2.RoceHost;
import inet.node.rocev2.RoceHostNew;
import inet.node.rocev2.RoceSwitch;
import inet.transportLayer.contract.IRoce;
network TargetNetwork extends WiredNetworkBase
{
    submodules:
    connections:
}
