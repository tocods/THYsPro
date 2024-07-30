package gpu;

import cloudsim.Pe;
import cloudsim.provisioners.PeProvisioner;

import java.net.Inet4Address;

/**
 * 流处理器。
 * 我们不需要实际地仿真流处理器内部所有核心。
 * 以线程块为单位，或者以warp为单位？
 */
public class SM{

    Integer id;

    /**
     * 核心数
     */
    Integer coreNum;

    /**
     * 已占用的核心
     */
    Integer usedCoreNum;


    /**
     * 最大线程块数
     */
    Integer maxBlockNum;

    /**
     * 已占用线程块数
     */
    Integer usedBlockNum;


    /**
     * 初始化流处理器
     *
     * @param id            流处理器ID
     * @pre id >= 0
     * @pre peProvisioner != null
     * @post $none
     */
    public SM(int id, Integer coreNum, Integer maxBlockNum) {
        this.id = id;
        this.coreNum = coreNum;
        this.maxBlockNum = maxBlockNum;
    }

    public Integer getCoreNum() {
        return coreNum;
    }

    public Integer getId() {
        return id;
    }

    public Integer getMaxBlockNum() {
        return maxBlockNum;
    }



    public Integer getUsedBlockNum() {
        return usedBlockNum;
    }

    public Integer getUsedCoreNum() {
        return usedCoreNum;
    }


    public void setId(Integer id) {
        this.id = id;
    }

    public void setCoreNum(Integer coreNum) {
        this.coreNum = coreNum;
    }

    public void setMaxBlockNum(Integer maxBlockNum) {
        this.maxBlockNum = maxBlockNum;
    }

    public void setUsedBlockNum(Integer usedBlockNum) {
        this.usedBlockNum = usedBlockNum;
    }

    public void setUsedCoreNum(Integer usedCoreNum) {
        this.usedCoreNum = usedCoreNum;
    }

}
