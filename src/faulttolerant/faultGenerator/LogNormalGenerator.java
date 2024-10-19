package faulttolerant.faultGenerator;

import faulttolerant.FaultConstants;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;

public class LogNormalGenerator extends NormalGenerator{
    @Override
    public void initSamples(double scale, double shape) {
        this.scale = scale;
        this.shape = shape;
        distribution = new LogNormalDistribution(scale, shape);
        samples = distribution.sample(FaultConstants.SAMPLE_SIZE);
        cursor = 0;
        updateCumulativeSamples();
    }

    @Override
    public void initRepair(double scale, double shape) {
        repair = new LogNormalDistribution(scale, shape);
    }

    @Override
    public String getType() {
        return "LogNormal";
    }
}
