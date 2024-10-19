import xml.etree.ElementTree as ET


class parser:
    def __init__(self, path):
        self.tree = ET.parse(path)
        self.root = self.tree.getroot()

    def parse_host_xml(self):
        host_list = []
        for host in self.root.findall('Util'):
            host_dict = {}
            for child in host:
                if child.tag == 'host_name':
                    host_dict['host_name'] = child.text
                if child.tag == 'host_ip':
                    host_dict['host_ip'] = child.text
                if child.tag == 'host_os':
                    host_dict['host_os'] = child.text
            host_list.append(host_dict)
        return host_list