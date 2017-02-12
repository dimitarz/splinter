package com.splinter.graphing;

/**
 * For a detailed description of how to use this utility class, see {@link com.splinter.graphing.SLog}.
 *
 * @see com.splinter.graphing.SLog
 */
public final class SLogBroadcastSend extends SLog {
    /**
     * Create a new Splinter Log with a task, request and operation.
     * See {@link SLog} for more information.
     *
     * @param task      the task name {@link #withTask(Object)}
     * @param broadcastId the broadcast id
     */
    public SLogBroadcastSend(String task, String broadcastId) {
        super(task, broadcastId, MessageType.S);
        withMulticast(true);
    }

    /**
     * Shorthand for creating a simple log with a task and operation.
     * @param task      the task name {@link #withTask(Object)}
     * @param broadcastId the broadcast id
     *
     * @return The newly built string that is ready to be logged by your preferred logger.
     */
    public static String log(String task, String broadcastId) {
        if(!SLog.isEnabled) return "";

        task = escape(task);
        broadcastId = escape(broadcastId);
        return SLog.build(task, broadcastId, null, null,
                null, true, MessageType.S);
    }

    /**
     * Shorthand for creating a simple log with a task, operation and user data..
     * @param task the task name {@link #withTask(Object)}
     * @param broadcastId the broadcast id
     * @param userKeyValuePairs key-value pairs of user data.
     *
     * @return The newly built string that is ready to be logged by your preferred logger.
     */
    public static String log(String task, String broadcastId, Object ...userKeyValuePairs) {
        if(!SLog.isEnabled) return "";

        task = escape(task);
        broadcastId = escape(broadcastId);
        Object[] kvPairs = SLog.escapeUserData(userKeyValuePairs);

        return SLog.build(task, broadcastId, null, null,
                null, true, MessageType.S, kvPairs);
    }
}
