package faulttolerant;

import java.util.List;

public class Topo {
    public class TopoHost {
        public String hostName;

        public List<String> softwares;
    }

    public List<TopoHost> hosts;

    public String time;
}
