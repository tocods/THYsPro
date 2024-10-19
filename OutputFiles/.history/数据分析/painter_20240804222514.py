from seaborn import *
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
        dict = 
           

