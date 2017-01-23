package com.splinter.graphing;

public class SLogStart extends SLog {

    /**
     * Create a new Splinter Log with a task, request and operation.
     * See {@link SLog} for more information.
     *
     * @param task      the task name {@link #withTask(String)}
     * @param operation the operation {@link #withOperation(String)}
     */
    public SLogStart(String task, String operation) {
        super(task, operation, MessageType.A);
    }
}
