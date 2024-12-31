package test;

import backend.SimEngine;
import org.junit.jupiter.api.Test;
import workflow.Parameters;

public class Tester {
    public enum TestEnum {
        TEST_ROOT("D:\\gpuworkflowsim\\src\\test\\"),
        TEST_OUT("\\output"),
        TEST_HOST("\\input\\hosts.json"),
        TEST_JOB("\\input\\jobs.json"),
        TEST_FAULT("\\input\\faults.json"),
        TEST_1A_CPU("Test1a\\CPUTest"),
        TEST_1A_GPU("Test1a\\GPUTest"),
        TEST_1A_RAM("Test1a\\RamTest"),
        TEST_1C("Test1c"),
        TEST_1D("Test1df"),
        TEST_1E("test1eg"),
        TEST_1F("Test1df"),
        Test_1G("Test1eg"),
        TEST_3D("Test3d"),
        TEST_3E1("Test3e\\Test3e1"),

        TEST_LARGE("TestLarge");

        private String path;
        private TestEnum(String path) {
            this.path = path;
        }

        public String getPath() {
            return path;
        }

        public String getOutputTruPath() {
            return TEST_ROOT.path + getPath() +  TEST_OUT.getPath();
        }

        public String getHostTruePath() {
            return TEST_ROOT.getPath() + getPath() + TEST_HOST.getPath();
        }

        public String getJobTruePath() {
            return TEST_ROOT.path + getPath() + TEST_JOB.getPath();
        }

        public String getFaultTruePath() {
            return TEST_ROOT.path + getPath() + TEST_FAULT.getPath();
        }
    }
    private void testSim(String outPath, String jobPath, String hostPath, String faultPath,  Double duration) {
        SimEngine engine = new SimEngine();
        if(duration > 0)
            Parameters.duration = duration;
        engine.parseJsonOfHost(hostPath);
        engine.parseJsonOfJob(jobPath);
        engine.parseJsonOfFault(faultPath);
        engine.setAlgorithm(engine.getAlgorithm(0));
        engine.start(outPath);
    }

    public void testTruePath(TestEnum dir, Double duration) {
        testSim(dir.getOutputTruPath(), dir.getJobTruePath(), dir.getHostTruePath(), dir.getFaultTruePath(), duration);
    }

    @Test
    public void test_1a_CPU() {
        testTruePath(TestEnum.TEST_1A_CPU, -1.0);
    }

    @Test
    public void test_1a_GPU() {
        testTruePath(TestEnum.TEST_1A_GPU, -1.0);
    }

    @Test
    public void test_1a_Ram() {
        testTruePath(TestEnum.TEST_1A_RAM, -1.0);
    }

    @Test
    public void test_1c() {
        testTruePath(TestEnum.TEST_1C, 100.0);
    }

    @Test
    public void test_1d() {
        testTruePath(TestEnum.TEST_1D, 100.0);
    }

    @Test
    public void test_1e() {
        testTruePath(TestEnum.TEST_1E, 100.0);
    }

    @Test
    public void test_1f() {
        testTruePath(TestEnum.TEST_1F, 100.0);
    }

    @Test
    public void test_1g() {
        testTruePath(TestEnum.Test_1G, 100.0);
    }

    @Test
    public void test_3d() {
        testTruePath(TestEnum.TEST_3D, 100.0);
    }

    @Test
    public void test_3e_1() {
        testTruePath(TestEnum.TEST_3E1, 100.0);
    }

    @Test
    public void test_large() {
        testTruePath(TestEnum.TEST_LARGE, 1000.0);
    }

}
