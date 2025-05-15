package faulttolerant;

import workflow.WorkflowSimTags;

public class FaultTolerantTags {
    public final static int FAULT_DATACENTER_EVENT = WorkflowSimTags.WORKFLOW_TAG_LAST + 1;

    public final static int FAULT_HOST_REPAIR = WorkflowSimTags.WORKFLOW_TAG_LAST + 2;

    public final static int RECORD_FAULT_RECORD = WorkflowSimTags.WORKFLOW_TAG_LAST + 3;

    public final static int FAULT_TAG_LAST = RECORD_FAULT_RECORD + 1;
    public static boolean IF_TEST = false;
}
