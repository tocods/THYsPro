import seaborn as sns
from parser import *
import pandas as pd

class painter:
    def __init__(self, hostPath, jobPath, faultPath):
        self.hostPath = hostPath
        self.jobPath = jobPath
        self.faultPath = faultPath

    def draw_host(self, host):
        p = parser(self.hostPath)
        host_list = p.parse_host_xml()
        if host not in host_list:
            return
        host_dict = host_list[host]
        cpu = host_dict['cpu'])
        memory = pd.DataFrame(host_dict['memory'])
        print(cpu)
        print(memory)
           
painter('hostUtils.xml', 'jobRun.xml', 'fault.xml').draw_host('host1')
