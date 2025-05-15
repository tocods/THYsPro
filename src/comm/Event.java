package comm;

import java.time.chrono.IsoEra;

public class Event {
    public String src_name;
    public String info;


    @Override
    public String toString() {
        return src_name + "_" + info;
    }

    public static Event toEvent(String s) {
        String[] infos = s.split("_");
        Event e = new Event();
        e.src_name = infos[0];
        e.info = infos[1];
        return e;
    }
}
