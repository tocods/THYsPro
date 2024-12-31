package faulttolerant.faultGenerator;

import cloudsim.Log;
import faulttolerant.FaultConstants;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;

import java.util.Arrays;

public class NormalGenerator implements FaultGenerator{
    // 错误注入的数学分布
    protected RealDistribution distribution;

    protected RealDistribution repair;

    protected double[] samples;
    protected double[] cumulativeSamples;

    protected int cursor;

    protected double scale = 0.0;

    protected double shape = 0.0;

    protected String type;

    public double[] concat(double[] first, double[] second) {
        double[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public void updateCumulativeSamples() {
        cumulativeSamples = new double[samples.length];
        cumulativeSamples[0] = samples[0];
        for (int i = 1; i < samples.length; i++) {
            cumulativeSamples[i] = cumulativeSamples[i - 1] + Math.max(0, samples[i]);
        }
    }
    @Override
    public void initSamples(double scale, double shape) {
        this.scale = scale;
        this.shape = shape;
        distribution = new NormalDistribution(scale, shape);
        samples = distribution.sample(FaultConstants.SAMPLE_SIZE);
        cursor = 0;
        updateCumulativeSamples();
    }

    @Override
    public void initRepair(double scale, double shape) {
        repair = new NormalDistribution(scale, shape);

    }

    @Override
    public void extendSample() {
        double[] new_samples = distribution.sample(FaultConstants.SAMPLE_SIZE);
        samples = concat(samples, new_samples);
        updateCumulativeSamples();
    }

    @Override
    public double getNextSample() {
        if(cursor > samples.length-1) {
            double[] new_samples = distribution.sample(FaultConstants.SAMPLE_SIZE);
            samples = concat(samples, new_samples);
            updateCumulativeSamples();
        }
        double ret = samples[cursor];
        cursor++;
        return ret;
    }

    @Override
    public double[] getCumulativeSamples() {
        return cumulativeSamples;
    }


    @Override
    public double getScale() {
        return this.scale;
    }

    @Override
    public double getShape() {
        return this.shape;
    }

    @Override
    public String getType() {
        return "Normal";
    }

    @Override
    public double getRepairTime() {
        double ret = 0.0;
        double[] samples = repair.sample(10);
        for(double s: samples)
            ret += s;
        return ret / 10;
    }

    @Override
    public String getFaultType() {
        return type;
    }

    @Override
    public void setFaultType(String t) {
        type = t;
    }

}
