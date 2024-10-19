import xml.etree.ElementTree as ET


class parser:
    def __init__(self, path):
        self.tree = ET.parse(path)
        self.root = self.tree.getroot()

    def parse_host_xml(self):
        host_list = {}
        for host in self.root.findall('Util'):
            for child in host:
                if child.tag == 'name':
                    name = child.text
                elif child.tag == 'ip':
                    ip = child.text
                elif child.tag == 'port':
                    port = child.text
                elif child.tag == 'username':
                    username = child.text
                elif child.tag == 'password':
                    password = child.text
                elif child.tag == 'type':
                    type = child.text
        return host_list