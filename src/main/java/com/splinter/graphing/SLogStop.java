package com.splinter.graphing;

public class SLogStop extends SLog {
    /**
     * Create a new Splinter Log with a task, request and operation.
     * See {@link SLog} for more information.
     *
     * @param task      the task name {@link #withTask(String)}
     * @param operation the operation {@link #withOperation(String)}
     */
    public SLogStop(String task, String operation) {
        super(task, operation, MessageType.F);
    }
}
