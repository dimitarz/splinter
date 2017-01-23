package com.splinter.graphing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <p>Lightweight class to build Splinter logs which are used for drawing pretty graphs.</p>
 *
 * <p>There are 3 required parameters with each log </p>
 * <ol>
 *    <li> The task name - a globally unique name which groups all logs under the same graph and is used as the graph title. </li>
 *    <li> The operation name - the name or id identifying a function or request, used to label edges.  The operation identifier is what establishes
 * the relationship (edge) between two components. </li>
 *    <li> The message type - a flag denoting the place this log is emitted from.
          <ul>
            <li>SEND - use right before making a call or sending a request</li>
            <li>ACK - use at the start of functions to track latency (Optional) </li>
            <li>FIN - use at the end of functions or process to complete a call.</li>
         </ul>
 *    </li>
 * </ol>
 *
 * <p> Splinter logs can measure two types of behaviors: 1) one function calling another or 2) a broadcast
 * sent to one or more recipients.</p>
 *
 * <p>(1) When calling another function, submit one log before the call is made and one call after the call completes.
 *  For example:</p>
 * <pre><code>
 * class CoffeeMaker {
 *  void brewCoffee() {
 *         LOG.info(new SLogCall("Coffee Time", "pumpWater"));
 *         waterPump.pumpWater();
 *  }
 * }
 * class WaterPump {
 *  void pumpWater() {
 *       //..pump it good..//
 *        LOG.info(new SLogStop("Coffee Time", "pumpWater"));
 *   }
 * }
 * </code></pre>
 *
 * <p>This will create the following graph:</p>
 * <pre><code>
 *              Coffee Time
 *
 *  +-----------+ pumpWater  +----------+
 *  |CoffeeMaker+-----------&gt;+WaterPump|
 *  +-----------+            +----------+
 * </code></pre>
 *
 * <p> (2) When sending a broadcast, submit a log before the broadcast is sent and a Start (ACK) and Stop (FIN) messages in each recipient.
 * For example: </p>
 * <pre><code>
 * class CoffeeMaker {
 *  void brewCoffee() {
 *         Broadcast broadcast = new Broadcast("coffeeComplete", ..other params..);
 *         LOG.info(new SLogBroadcastSend("Coffee Time", broadcast.getId())); //coffeeComplete is the id
 *         broadcastFramework.submit(broadcast);
 *  }
 * }
 * class ControlPanel {
 *  void chime(Broadcast broadcast) {
 *        LOG.info(new SLogBroadcastStart("Coffee Time", broadcast.getId(), "chime"));
 *       //make a noise when the coffee is ready//
 *        LOG.info(new SLogBroadcastStop("Coffee Time", broadcast.getId(), "chime"));
 *   }
 * }
 * </code></pre>
 * <p>
 * This will create the following graph:
 * </p>
 * <pre><code>
 *              Coffee Time
 *
 *  +-----------+   chime    +-------------+
 *  |CoffeeMaker+-----------&gt;+ControlPanel|
 *  +-----------+    0ms     +-------------+
 * </code></pre>
 * <p>The SLogBroadcastStart is optional, but would be used to measure the transport latency of the
 * broadcast framework (especially if there's network involved).</p>
 *
 * <p>In addition, an SLog can contain:</p>
 * <ul>
 *    <li>user data which is displayed in the Splinter UI</li>
 *    <li>a component override - Splinter will determine it by introspecting the log but you can
 *      override that logic by providing a custom one. You would do this if you want your component
 *      to pretend to be another component. It's possible that you can't add a log to a particular function,
 *      so you would add it after the function is called and use the override to pretend to be the component
 *      that executed it.</li>
 *    <li>an instrumentation override (see graph) - The timestamps of the logs are used to determine the
 *      latency between requests. You can calculate your own latency and provide it with an override. If
 *      the override is used on a Start (ACK) log, it is treated to be the transport latency. If it is
 *      used on a Stop (FIN) log, it is treated to be the processing latency. </li>
 * </ul>
 *
 * <p>This class will never throw any exceptions or log any errors.</p>
 *
 * <p>Not thread-safe.</p>
 *
 * @author dimitarz
 */
public class SLog {
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

    enum Key {
        TASK("$SPG$+T"),
        OPERATION("+O"),
        MESSAGE_TYPE("+M"),
        OPERATION_ALIAS("+OA"),
        COMPONENT("+C^"),
        INSTRUMENTATION("+I^"),
        MULTICAST("+MC");

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

    /**
     * <p>The message type:</p>
     * <ul>
     *     <li>SEND - use right before making a call or sending a request</li>
     *     <li>ACK - use at the start of functions to track latency (Optional) </li>
     *     <li>FIN - use at the end of a function or process to complete a call.</li>
     * </ul>
     *
     */
    public enum MessageType {
        S, /* SEND        */
        A, /* ACKNOWLEDGE */
        F  /* FINISH      */
    }

    private static boolean isEnabled = true;

    private String mTask;
    private String mOperation;
    private String mOperationAlias;
    private String mComponentOverride;
    private String mInstrumentationOverride;
    private List<String> mUserData;
    private MessageType mMessageType;
    private boolean mMulticast;

    /**
     * Globally enable or disable the log creation.
     * @param isEnabled whether to enable or disable log creation.
     */
    public static void setEnabled(boolean isEnabled) {
        SLog.isEnabled = isEnabled;
    }

    /**
     * Create a new Splinter Log with a task, operation and message type.
     *
     * @param task the task name {@link #withTask(String)}
     * @param operation the operation {@link #withOperation(String)}
     * @param msgType the message type {@link MessageType}
     */
    public SLog(String task, String operation, MessageType msgType) {
        this.mTask = escape(task);
        this.mOperation = escape(operation);
        this.mMessageType = msgType == null ? MessageType.S : msgType;
    }

    /**
     * Mark the request with multicast flag. This means there
     * may be more than one recipient of the message.
     * @param value the multicast flag
     * @return this object
     */
    public final SLog withMulticast(boolean value) {
        if(!isEnabled) return this;

        mMulticast = value;
        return this;
    }

    /**
     * Set the alias of this operation - use to disambiguate multiple
     * Starts and Stops who have the same operation id. For e.g. a broadcast
     * with one operation id may be received by multiple recipients -
     * the recipients can specify the function they serve by setting the
     * operation alias.
     *
     * @param value the operation alias
     * @return this object
     */
    public final SLog withOperationAlias(String value) {
        if(!isEnabled) return this;

        mOperationAlias = escape(value);
        return this;
    }

    /**
     * New operation name, replacing the existing one if there's one.
     * The operation is the action that is being executed. It is
     * used to label the edges between talking components.
     *
     * @param value the operation.
     * @return this object
     */
    public final SLog withOperation(String value) {
        if(!isEnabled) return this;

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
    public SLog withTask(String value) {
        if(!isEnabled) return this;

        this.mTask = escape(value);
        return this;
    }

    /**
     * Add a component override. See the description of this class for more on this.
     * @param value the component override.
     * @return this object
     */
    public final SLog withComponentOverride(String value) {
        if(!isEnabled) return this;

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
    public final SLog withInstrumentationOverride(int value, TimeNotation timeNotation) {
        if(!isEnabled) return this;

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
    public final SLog withUserData(String key, String value) {
        if(!isEnabled) return this;

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
    public final SLog withUserData(Map<String, String> userData) {
        if(!isEnabled || userData == null || userData.size() == 0) {
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
        if(!isEnabled) return "";

        //sanitize
        if(mTask == null || mTask.length() == 0)
            mTask = "_MISSING_TASK_";
        if(mOperation == null || mOperation.length() == 0)
            mOperation = "_MISSING_OPERATION_";

        //compute capacity
        int len = 18 + mTask.length() + mOperation.length()   /* 20 => '+M=S;' = 5, '$SPG$+T=;' = 9, '+O=;' = 4  */
                + (mOperationAlias != null ? mOperationAlias.length() + 5 : 0)
                + (mComponentOverride != null ? mComponentOverride.length() + 5 : 0)
                + (mInstrumentationOverride != null ? mInstrumentationOverride.length() + 5 : 0);
        for(int i = 0; mUserData != null && i < mUserData.size(); ++i) {
            len += mUserData.get(i) != null ? mUserData.get(i).length() + 1 : 0;
        }

        //build string
        StringBuilder builder = new StringBuilder(len);
        builder.append(Key.TASK).append('=').append(mTask).append(';')
                .append(Key.OPERATION).append('=').append(mOperation).append(';')
                .append(Key.MESSAGE_TYPE).append('=').append(mMessageType).append(';');
        if(mOperationAlias != null)
            builder.append(Key.OPERATION_ALIAS).append('=').append(mOperationAlias).append(';');
        if(mComponentOverride != null)
            builder.append(Key.COMPONENT).append('=').append(mComponentOverride).append(';');
        if(mInstrumentationOverride != null)
            builder.append(Key.INSTRUMENTATION).append('=').append(mInstrumentationOverride).append(';');
        if(mMulticast)
            builder.append(Key.MULTICAST).append("=1;");


        for(int i = 0; mUserData != null && i < mUserData.size(); i = i + 2) {
            builder.append(mUserData.get(i)).append('=').append(mUserData.get(i+1)).append(';');
        }

        return builder.toString();
    }

}
