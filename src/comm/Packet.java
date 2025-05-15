package comm;

import lombok.Data;

@Data
public class Packet {
    String src;
    String src_task_name;
    String entity_id;
    String dst;
    String txt;



    @Override
    public String toString() {
        return src + "_" + src_task_name + "_" + entity_id + "_" + dst + "_" + txt;
    }

    public static Packet toPacket(String s) {
        String[] info = s.split("_");
        Packet ret = new Packet();
        ret.src = info[0];
        ret.src_task_name = info[1];
        ret.entity_id = info[2];
        ret.dst = info[3];
        ret.txt = info[4];
        return ret;
    }
}
