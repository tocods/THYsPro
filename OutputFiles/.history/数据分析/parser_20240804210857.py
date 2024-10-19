import xml.etree.ElementTree as ET


class parser:
    def __init__(self, path):
        self.tree = ET.parse(path)
        self.root = self.tree.getroot()

    def parse_host_xml(self):
        host_list = {}
        for host in self.root.findall('Util'):
            for 
        return host_list