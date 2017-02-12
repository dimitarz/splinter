package com.splinter.graphing;

/**
 * For a detailed description of how to use this utility class, see {@link com.splinter.graphing.SLog}.
 *
 * @see com.splinter.graphing.SLog
 */
public final class SLogBroadcastStart extends SLog {
    /**
     * Create a new Splinter Log with a task, request and operation.
     * See {@link SLog} for more information.
     *
     * @param task      the task name {@link #withTask(Object)}
     * @param broadcastId the broadcast id.
     * @param operation the name of the operation.
     */
    public SLogBroadcastStart(String task, String broadcastId, String operation) {
        super(task, broadcastId, MessageType.A);
        withOperationAlias(operation);
    }

    /**
     * Shorthand for creating a simple log with a task and operation.
     * @param task      the task name {@link #withTask(Object)}
     * @param broadcastId the broadcast id
     * @param operation the operation
     * @return The newly built string that is ready to be logged by your preferred logger.
     */
    public static String log(String task, String broadcastId, String operation) {
        if(!SLog.isEnabled) return "";

        task = escape(task);
        broadcastId = escape(broadcastId);
        operation = escape(operation);
        return SLog.build(task, broadcastId, operation, null,
                null, false, MessageType.A);
    }

    /**
     * Shorthand for creating a simple log with a task, operation and user data..
     * @param task the task name {@link #withTask(Object)}
     * @param broadcastId the broadcast id
     * @param operation the operation
     * @param userKeyValuePairs key-value pairs of user data.
     *
     * @return The newly built string that is ready to be logged by your preferred logger.
     */
    public static String log(String task, String broadcastId, String operation, Object ...userKeyValuePairs) {
        if(!SLog.isEnabled) return "";

        task = escape(task);
        broadcastId = escape(broadcastId);
        operation = escape(operation);
        Object[] kvPairs = SLog.escapeUserData(userKeyValuePairs);

        return SLog.build(task, broadcastId, operation, null,
                null, false, MessageType.A, kvPairs);
    }
}
