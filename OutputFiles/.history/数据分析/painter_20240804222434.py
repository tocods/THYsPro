from seaborn import *
from parser import *
import pandas as pd

class painter:
    def __init__(self, hostPath, jobPath, faultPath):
        self.hostPath = hostPath
        self.jobPath = jobPath
        self.faultPath = faultPath

    def draw_host(self, ):
        p = parser(self.hostPath)
        host_list = p.parse_host_xml()
        for host in host_list:
            cpu = host_list[host]['cpu']
           

