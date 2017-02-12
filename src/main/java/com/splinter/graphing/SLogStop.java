package com.splinter.graphing;

/**
 * For a detailed description of how to use this utility class, see {@link com.splinter.graphing.SLog}.
 *
 * @see com.splinter.graphing.SLog
 */
public final class SLogStop extends SLog {
    /**
     * Create a new Splinter Log with a task, request and operation.
     * See {@link SLog} for more information.
     *
     * @param task      the task name {@link #withTask(Object)}
     * @param operation the operation {@link #withOperation(Object)}
     */
    public SLogStop(String task, String operation) {
        super(task, operation, MessageType.F);
    }

    /**
     * Shorthand for creating a simple log with a task and operation.
     * @param task the task name {@link #withTask(Object)}
     * @param operation the operation {@link #withOperation(Object)}
     *
     * @return The newly built string that is ready to be logged by your preferred logger.
     */
    public static String log(String task, String operation) {
        if(!SLog.isEnabled) return "";

        task = escape(task);
        operation = escape(operation);
        return SLog.build(task, operation, null, null,
                null, false, MessageType.F);
    }

    /**
     * Shorthand for creating a simple log with a task, operation and user data..
     * @param task the task name {@link #withTask(Object)}
     * @param operation the operation {@link #withOperation(Object)}
     * @param userKeyValuePairs key-value pairs of user data.
     *
     * @return The newly built string that is ready to be logged by your preferred logger.
     */
    public static String log(String task, String operation, Object ...userKeyValuePairs) {
        if(!SLog.isEnabled) return "";

        task = escape(task);
        operation = escape(operation);
        Object[] kvPairs = SLog.escapeUserData(userKeyValuePairs);

        return SLog.build(task, operation, null, null,
                null, false, MessageType.F, kvPairs);
    }

}
