package fncs;


import cloudsim.Log;

public class JNInet {
    public JNInet() {
    }

    public static void main(String[] var0) {
        //int[] var2 = get_version();
        //System.out.printf("jcoverage test running FNCS version %d.%d.%d\n", var2[0], var2[1], var2[2]);
        initialize();
        Log.printLine("Aaa");
        assert is_initialized();
        System.out.printf("I am federate %d out of %d other federates\n", get_id(), get_simulator_count());

        int time_stop = 10;
        long time = 0;
        long time_desired = 0;
        long time_granted = 0;
        while(time < time_stop) {
            publish("object.attribute", "value");
            String[] var8 = get_events();

            for(String s: var8)
                Log.printLine("get " + s);
            time_desired += 2;
            time_granted = time_request(time_desired);
            time = time_granted;
            Log.printLine("time: " + time_desired + " " + time_granted);
        }

        end();

        assert !is_initialized();

    }

    public static native void initialize();

    public static native void initialize(String var0);

    public static native void agentRegister();

    public static native void agentRegister(String var0);

    public static native boolean is_initialized();

    public static native long time_request(long var0);

    public static native void publish(String var0, String var1);

    public static native void publish_anon(String var0, String var1);

    public static native void agentPublish(String var0);

    public static native void route(String var0, String var1, String var2, String var3);

    public static native void die();

    public static native void end();

    public static native void update_time_delta(long var0);

    public static native String[] get_events();

    public static native String agentGetEvents();

    public static native String get_value(String var0);

    public static native String[] get_values(String var0);

    public static native String[] get_keys();

    public static native String get_name();

    public static native long get_time_delta();

    public static native long get_id();

    public static native long get_simulator_count();

    public static native int[] get_version();

    static {
        System.loadLibrary("JNIfncs");
    }
}
