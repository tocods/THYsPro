package workflow.taskCluster;

import cloudsim.Cloudlet;
import workflow.GpuJob;

import java.util.List;

/**
 * The ClusteringInterface for all clustering methods
 *
 * @author Weiwei Chen
 * @since WorkflowSim Toolkit 1.0
 * @date Apr 9, 2013
 */
public interface ClusteringInterface {

    /**
     * set the task list.
     * @param list
     */
    void setTaskList(List<? extends Cloudlet> list);

    /**
     * get job list.
     * @return 
     */
    List<GpuJob> getJobList();

    /**
     * get task list.
     * @return 
     */
    List<? extends Cloudlet> getTaskList();

    /**
     * the main function.
     */
    void run();

}
