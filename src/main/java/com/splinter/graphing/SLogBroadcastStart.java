package com.splinter.graphing;

public class SLogBroadcastStart extends SLog {
    /**
     * Create a new Splinter Log with a task, request and operation.
     * See {@link SLog} for more information.
     *
     * @param task      the task name {@link #withTask(String)}
     * @param broadcastId the broadcast id.
     * @param operation the name of the operation.
     */
    public SLogBroadcastStart(String task, String broadcastId, String operation) {
        super(task, broadcastId, MessageType.A);
        withOperationAlias(operation);
    }
}
