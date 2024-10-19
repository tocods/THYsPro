import xml.etree.ElementTree as ET


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
            job_dict = {}
            for child in job:
                if child.tag == 'JobID':
                    job_dict['JobID'] = child.text
                if child.tag == 'JobName':
                    job_dict['JobName'] = child.text
                if child.tag == 'JobType':
                    job_dict['JobType'] = child.text
                if child.tag == 'SubmitTime':
                    job_dict['SubmitTime'] = child.text
                if child.tag == 'StartTime':
                    job_dict['StartTime'] = child.text
                if child.tag == 'EndTime':
                    job_dict['EndTime'] = child.text
                if child.tag == 'Host':
                    job_dict['Host'] = child.text
                if child.tag == 'CPU':
                    job_dict['CPU'] = child.text
                if child.tag == 'Memory':
                    job_dict['Memory'] = child.text
                if child.tag == 'GPU':
                    job_dict['GPU'] = child.text
            job_list[job_dict['JobID']] = job_dict
        return job_list
    

if __name__ == '__main__':
    p = parser('hostUtils.xml')
    print(p.parse_host_xml())