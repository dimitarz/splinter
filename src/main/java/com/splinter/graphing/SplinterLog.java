package com.splinter.graphing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>Lightweight class to build Splinter logs which are used for drawing pretty graphs.</p>
 *
 * <p>There are 2 required parameters with each log </p>
 * <ol>
 *    <li> The task name </li>
 *    <li> The request id </li>
 * </ol>
 *
 * <p>A typical splinter graph is made of (you guessed it) labelled nodes and edges.
 * There is only one construct in splinter logs - requests, which are either outgoing or
 * incoming. Component X sends a message to Component Y. The requests include all the node
 * information and optionally some edge parameters such as the operation.
 * </p>
 * <pre><code>
 *                Task
 *
 *  +---------+ operation  +---------+
 *  |Component+-----------&gt;+Component|
 *  +---------+    0ms     +---------+
 * </code></pre>
 *
 * <p>(1) A SplinterLog is composed of a task name. All logs tagged with the same task name belong
 * to the same graph and is what the graph is identified by.</p>
 *
 * <p>(2) A SplinterLog is also composed of a request identifier. The request identifier is what establishes
 * the relationship (edge) between two components. Component X sends a message with id 5 to Component Y.
 * Both components print out a log with request id=5, allowing Splinter to connect the two.</p>
 *
 * <p>In addition, a SplinterLog can contain:</p>
 * <ul>
 *    <li>an operation name (see graph), in either the outgoing request or the incoming or both.</li>
 *    <li>user data which is displayed in the Splinter UI</li>
 *    <li>a component override - Splinter will determine it by introspecting the log but you can
 *      override that logic by providing a custom one. You would do this if you want your component
 *      to pretend to be another component.</li>
 *    <li>an instrumentation override (see graph) - The timestamps of the logs are used to determine the
 *      latency between requests. You can calculate your own latency and provide it with an override.</li>
 * </ul>
 *
 *
 * <p>This class will never throw any exceptions or log any errors.</p>
 *
 * <p>Not thread-safe.</p>
 *
 * @author dimitarz
 */
public final class SplinterLog {
    public enum TimeNotation {
        NANOS("ns"),
        MICROS("μs"),
        MILLIS("ms"),
        SECONDS("s"),
        MINUTES("min"),
        HOURS("h");

        private String mValue;

        TimeNotation(String value) {
            this.mValue = value;
        }

        public String notation() {
            return mValue;
        }

        @Override
        public String toString() {
            return mValue;
        }
    }

    private enum Key {
        TASK("$SPG$_T"),
        REQUEST("_R"),
        OPERATION("_O"),
        COMPONENT("_C^"),
        INSTRUMENTATION("_I^");

        private String mValue;
        Key(String value) {
            this.mValue = value;
        }

        public String key() {
            return mValue;
        }

        @Override
        public String toString() {
            return mValue;
        }
    }

    private String mTask;
    private String mRequest;
    private String mOperation;
    private String mComponentOverride;
    private String mInstrumentationOverride;
    private List<String> mUserData;

    /**
     * Create a new Splinter Log with a task name.
     *
     * @param task the task name {@link #withTask(String)}
     */
    public SplinterLog(String task) {
        this(task, null, null);
    }

    /**
     * Create a new Splinter Log with a task, and request.
     *
     * @param task the task name {@link #withTask(String)}
     * @param requestId the request id {@link #withRequestId(String)}
     */
    public SplinterLog(String task, String requestId) {
        this(task, requestId, null);
    }

    /**
     * Create a new Splinter Log with a task, request and operation.
     *
     * @param task the task name {@link #withTask(String)}
     * @param requestId the request id {@link #withRequestId(String)}
     * @param operation the operation {@link #withOperation(String)}
     */
    public SplinterLog(String task, String requestId, String operation) {
        this.mTask = escape(task);
        this.mRequest = escape(requestId);
        this.mOperation = escape(operation);
    }

    /**
     * New request id. A request is a loose term signifying a message being sent
     * or received by a component - thus there can be an outgoing request and an
     * incoming request. Both components (one that sends and one that receives)
     * must specify the request id so an association can be established.
     *
     * @param value the request id.
     * @return this object
     */
    public final SplinterLog withRequestId(String value) {
        this.mRequest = escape(value);
        return this;
    }

    /**
     * New operation name, replacing the existing one if there's one.
     * The operation is the action that a request is made for. It is
     * used to label the edges between talking components.
     *
     * @param value the operation.
     * @return this object
     */
    public final SplinterLog withOperation(String value) {
        this.mOperation = escape(value);
        return this;
    }

    /**
     * New task name, replacing the existing one if there's one.
     * The task name is what associates all logs that belong to the same graph.
     *
     * @param value the new task name.
     * @return this object
     */
    public SplinterLog withTask(String value) {
        this.mTask = escape(value);
        return this;
    }

    /**
     * Add a component override. See the description of this class for more on this.
     * @param value the component override.
     * @return this object
     */
    public final SplinterLog withComponentOverride(String value) {
        this.mComponentOverride = escape(value);
        return this;
    }

    /**
     * Add an instrumentation override. See the description of this class for more on this.
     *
     * @param value the instrumentation value
     * @param timeNotation the notation
     * @return this object
     */
    public final SplinterLog withInstrumentationOverride(int value, TimeNotation timeNotation) {
        if(timeNotation == null) {
            timeNotation = TimeNotation.MILLIS;
        }
        this.mInstrumentationOverride = String.format("%d%s", value, timeNotation.notation());
        return this;
    }

    /**
     * Add custom user data to the log. The key must be non-null and non-empty.
     * There are no restrictions on the value.
     *
     * @param key non-null and non-empty key
     * @param value the mapping to the key.
     * @return this object
     */
    public final SplinterLog withUserData(String key, String value) {
        if(mUserData == null) {
            mUserData = new ArrayList<String>();
        }
        if(key == null || key.length() == 0) {
            key = "_MISSING_KEY_" + mUserData.size() / 2;
        }
        this.mUserData.add(escape(key));
        this.mUserData.add(escape(value));

        return this;
    }

    /**
     * Add a map of user key-value pairs to the log.
     * @param userData map of key-value pairs/
     * @return this object
     */
    public final SplinterLog withUserData(Map<String, String> userData) {
        if(userData == null || userData.size() == 0) {
            return this;
        }

        for(Map.Entry<String, String> entry : userData.entrySet()) {
            withUserData(entry.getKey(), entry.getValue());
        }
        return this;
    }

    /**
     * Escapes a string's backward slashes (\), semicolons (;), newlines (\n) and equals(=)
     * @param string
     * @return
     */
    static String escape(String string) {
        if(string == null || string.length() == 0) {
            return string;
        }

        int len = string.length();
        for(int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if(c == '\\' || c == ';' || c == '\n' || c == '=') {
                len++;
            }
        }

        if(len == string.length()) {
            return string;
        }

        StringBuilder builder = new StringBuilder(len);
        for(int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if(c == '\\' || c == ';' || c == '\n' || c == '=') {
                builder.append('\\');
                if(c == '\n') {
                    builder.append('n');
                    continue;
                }
            }
            builder.append(c);
        }
        return builder.toString();
    }

    /**
     * Build the string representation of this log.
     * @return The newly built string that is ready to be logged by your preferred logger.
     */
    @Override
    public String toString() {
        return build();
    }

    /**
     * Build the log.
     * @return The newly built string that is ready to be logged by your preferred logger.
     */
    public final String build() {
        //sanitize
        if(mTask == null || mTask.length() == 0)
            mTask = "_MISSING_TASK_";
        if(mRequest == null || mRequest.length() == 0)
            mRequest = "_MISSING_REQUEST_";

        //compute capacity
        int len = 13 + mTask.length() + mRequest.length() + (mOperation != null ? mOperation.length() + 4 : 0)
                + (mComponentOverride != null ? mComponentOverride.length() + 5 : 0)
                + (mInstrumentationOverride != null ? mInstrumentationOverride.length() + 5 : 0);
        for(int i = 0; mUserData != null && i < mUserData.size(); ++i) {
            len += mUserData.get(i) != null ? mUserData.get(i).length() + 1 : 0;
        }

        //build string
        StringBuilder builder = new StringBuilder(len);
        builder.append(Key.TASK).append('=').append(mTask).append(';')
                .append(Key.REQUEST).append('=').append(mRequest).append(';');
        if(mOperation != null)
            builder.append(Key.OPERATION).append('=').append(mOperation).append(';');
        if(mComponentOverride != null)
            builder.append(Key.COMPONENT).append('=').append(mComponentOverride).append(';');
        if(mInstrumentationOverride != null)
            builder.append(Key.INSTRUMENTATION).append('=').append(mInstrumentationOverride).append(';');

        for(int i = 0; mUserData != null && i < mUserData.size(); i = i + 2) {
            builder.append(mUserData.get(i)).append('=').append(mUserData.get(i+1)).append(';');
        }

        return builder.toString();
    }

}
