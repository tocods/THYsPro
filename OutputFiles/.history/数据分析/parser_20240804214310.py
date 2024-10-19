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
                        if grandchild.tag == 'GP':
                            if grandchild.attrib['id'] not in host_list[child.attrib['name']]['gpu']:
                                host_list[child.attrib['name']]['gpu'][grandchild.attrib['id']] = {}
                        host_list[child.attrib['name']]['gpu'][grandchild.attrib['id']][host.attrib['time']] = grandchild.attrib['gpu']
        return host_list
    

if __name__ == '__main__':
    p = parser('hostUtils.xml')
    print(p.parse_host_xml())