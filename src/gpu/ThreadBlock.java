package gpu;

import java.util.List;

/**
 * 线程块
 */
public class ThreadBlock {
    /**
     * warp是SM执行的单位
     */
    List<Warp> warps;

    /**
     * 线程块中包含的线程数
     */
    Integer threadNum;

    public void setThreadNum(Integer threadNum) {
        this.threadNum = threadNum;
    }

    public Integer getThreadNum() {
        return threadNum;
    }

    public void setWarps(List<Warp> warps) {
        this.warps = warps;
    }

    public List<Warp> getWarps() {
        return warps;
    }
}
