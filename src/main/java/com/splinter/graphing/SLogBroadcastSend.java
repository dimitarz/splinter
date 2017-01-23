package com.splinter.graphing;

public class SLogBroadcastSend extends SLog {
    /**
     * Create a new Splinter Log with a task, request and operation.
     * See {@link SLog} for more information.
     *
     * @param task      the task name {@link #withTask(String)}
     * @param broadcastId the broadcast id
     */
    public SLogBroadcastSend(String task, String broadcastId) {
        super(task, broadcastId, MessageType.S);
        withMulticast(true);
    }
}
