import xml.etree.ElementTree as ET


class runningRecord:
    def __init__(self, startTime, endTime, host, duration):
        self.startTime = startTime
        self.endTime = endTime
        self.host = host
        self.duration = duration

    def __str__(self):
        return 'JobID: %s, StartTime: %s, EndTime: %s, Host: %s, CPU: %s, Memory: %s, GPU: %s' % (self.jobID, self.startTime, self.endTime, self.host, self.cpu, self.memory, self.gpu)

    def __repr__(self):
        return 'JobID: %s, StartTime: %s, EndTime: %s, Host: %s, CPU: %s, Memory: %s, GPU: %s' % (self.jobID, self.startTime, self.endTime, self.host, self.cpu, self.memory, self.gpu)

class parser:
    def __init__(self, path):
        self.tree = ET.parse(path)
        self.root = self.tree.getroot()

    def parse_host_xml(self):
        host_list = {}
        for host in self.root.findall('Util'):
            for child in host:
                if child.tag == 'Host':
                    if child.attrib['name'] not in host_list:
                        host_dict = {'cpu': {}, 'memory': {}, 'gpu':{}}
                        host_list[child.attrib['name']] = host_dict
                    host_list[child.attrib['name']]['cpu'][host.attrib['time']] = child.attrib['cpuUtilization']
                    host_list[child.attrib['name']]['memory'][host.attrib['time']] = child.attrib['ramUtilization']
                    for grandchild in child:
                        if grandchild.tag == 'gpuUtilization':
                            if grandchild.attrib['id'] not in host_list[child.attrib['name']]['gpu']:
                                host_list[child.attrib['name']]['gpu'][grandchild.attrib['id']] = {}
                        host_list[child.attrib['name']]['gpu'][grandchild.attrib['id']][host.attrib['time']] = grandchild.attrib['gpu']
        return host_list
    
    def parse_job_xml(self):
        job_list = {}
        for job in self.root.findall('Job'):
            job_dict = []
            for child in job:
                if child.tag == 'RunningRecord':
                    job_dict.append(runningRecord(child.attrib['start'], child.attrib['end'], child.attrib['host']))
            job_list[job.attrib['name']] = job_dict
        return job_list
    

if __name__ == '__main__':
    p = parser('hostUtils.xml')
    print(p.parse_host_xml())