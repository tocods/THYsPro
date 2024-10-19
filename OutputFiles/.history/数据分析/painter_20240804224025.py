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
        memory = host_dict['memory']
        memory_x = memory.keys()
        memory_y = memory.values()
        fig, ax1 = plt.subplots(1,1,figsize=(16,9), dpi= 80)
        ax1.plot(cpu_x, cpu_y, color='tab:red')

        # Plot Line2 (Right Y Axis)
        ax2 = ax1.twinx()  # instantiate a second axes that shares the same x-axis
        ax2.plot(memory_x, memory_y, color='tab:blue')

        # Decorations
        # ax1 (left Y axis)
        ax1.set_xlabel('Year', fontsize=20)
        ax1.tick_params(axis='x', rotation=0, labelsize=12)
        ax1.set_ylabel('Personal Savings Rate', color='tab:red', fontsize=20)
        ax1.tick_params(axis='y', rotation=0, labelcolor='tab:red' )
        ax1.grid(alpha=.4)

        # ax2 (right Y axis)
        ax2.set_ylabel("# Unemployed (1000's)", color='tab:blue', fontsize=20)
        ax2.tick_params(axis='y', labelcolor='tab:blue')
        ax2.set_xticks(np.arange(0, len(memory_x), 60))

        ax2.set_title("Personal Savings Rate vs Unemployed: Plotting in Secondary Y Axis", fontsize=22)
        fig.tight_layout()
        plt.show()

        
           
painter('hostUtils.xml', 'jobRun.xml', 'fault.xml').draw_host('host1')
