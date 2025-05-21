package comm;

import cloudsim.Log;
import faulttolerant.FaultTolerantTags;
import fncs.JNIfncs;

import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.List;

public class Api {
    public static List<String> values = new ArrayList<>();
    public static enum CommType {
        SEND_WITHOUT_REPLY,
        SEND_REPLY
    }

    public final static int RECEIVE_EVENT = FaultTolerantTags.FAULT_TAG_LAST + 1;

    private static String[] topics = {"net", "net"};
    public static void publish(CommType type, String value) {
        values.add(value);
        //JNIfncs.publish(topics[type.ordinal()], value);
    }

    public static void sendEnd() {
        Packet p = new Packet();
        p.txt = "end";
        JNIfncs.publish("net",p.toString());
    }



    public static void truePublish() {
        if(values.isEmpty())
            return;
        StringBuilder out = new StringBuilder();
        out.append(values.get(0));
        values.remove(0);
        for(String s: values)
            out.append("/").append(s);
        JNIfncs.publish(topics[0], out.toString());
        values.clear();
    }


    public static String[] getEvents() {
        return JNIfncs.get_events();
    }

    public static String getValue(String s) {
        return JNIfncs.get_value(s);
    }

    public static long timeRequest(long next_time) {
        //Log.printLine("时间请求： " + next_time);
        return JNIfncs.time_request(next_time);
    }

    public static void end() {
        JNIfncs.end();
    }

}
