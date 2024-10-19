import seaborn as sns
from parser import *
import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

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
        cpu = host_dict['cpu']
        cpu_x = cpu.keys()
        cpu_y = cpu.values()
        cpu_y = [i * 100 ]
        print(cpu_x)
        print(cpu_y)
        memory = host_dict['memory']
        memory_x = memory.keys()
        memory_y = memory.values()
        print(memory_x)
        print(memory_y)
        fig, ax = plt.subplots()
        ax.plot(cpu_x, cpu_y, color='tab:red')
        # Plot Line2 (Right Y Axis)
        #ax2 = ax.twinx()  # instantiate a second axes that shares the same x-axis
        ax.plot(cpu_x, memory_y, color='tab:blue')
        plt.ylim(0, 100)
        plt.show()

        
           
painter('hostUtils.xml', 'jobRun.xml', 'fault.xml').draw_host('host1')
