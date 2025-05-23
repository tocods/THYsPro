from seaborn import *
from parser import *

class painter:
    def __init__(self, hostPath, jobPath, faultPath):
        self.hostPath = hostPath
        self.jobPath = jobPath
        self.faultPath = faultPath

    def draw_host(self):
        p = parser(self.hostPath)
        host_list = p.parse_host_xml()
        for host in host_list:
            cpu = host_list[host]['cpu']
            memory = host_list[host]['memory']
            gpu = host_list[host]['gpu']
            cpu = pd.Series(cpu)
            memory = pd.Series(memory)
            gpu = pd.DataFrame(gpu)
            cpu.plot()
            memory.plot()
            gpu.plot()
            plt.show()

