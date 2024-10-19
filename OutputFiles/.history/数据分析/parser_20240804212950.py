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
                        host_dict = {'cpu': 0, 'memory': 0, 'gpu':{}}
                        host_list[child.attrib['name']] = host_dict
                        host_list[child.attrib['name']]['cpu'] = child.attrib['cpuUtilization']
                        host_list[child.attrib['name']]['memory'] = child.attrib['ramUtilization']
                    for grandchild in child:
                        host_list[child.attrib['name']]['gpu'][grandchild.attrib['id']] = grandchild.attrib['gpu']
        return host_list
    

def