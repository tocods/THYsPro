package faulttolerant.faultGenerator;

/**
 * 错误生成器负责根据指定的概率分布生成一系列错误时间点
 */
public interface FaultGenerator {

    /**
     * 对错误点数组进行初始化
     * @param scale 概率分布参数
     * @param shape 概率分布参数
     */
    void initSamples(double scale, double shape);

    /**
     * 对错误点数组进行扩展
     */
    void extendSample();

    /**
     * 错误注入器动态地判断是否需要对错误点数组进行扩展
     */
    double getNextSample();

    /**
     * 得到错误点数组，每一个错误点的值代表这个时刻发生了错误
     * @return 错误点数组
     */
    double[] getCumulativeSamples();

    double getScale();

    double getShape();

    String getType();
}
