package com.splinter.graphing;

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

    protected static boolean isEnabled = true;

    private String mTask;
    private String mOperation;
    private String mOperationAlias;
    private String mComponentOverride;
    private String mInstrumentationOverride;
    private ArrayBackedList<String> mUserData;
    private MessageType mMessageType;
    private boolean mMulticast;

    private static class ArrayBackedList<T> {
        private Object[] array;
        private int size;

        public ArrayBackedList(int capacity) {
            array = new Object[capacity];
        }

        public ArrayBackedList(Object[] backingArray, int size) {
            this.array = backingArray;
            this.size = size;
        }

        public void add(T t) {
            if(size == array.length) {
                Object[] newarray = new Object[array.length * 2];
                System.arraycopy(array, 0, newarray, 0, array.length);
                array = newarray;
            }

            array[size++] = t;
        }

        public T get(int i) {
            if(i >= 0 && i < size)
                return (T) array[i];
            throw new ArrayIndexOutOfBoundsException("Index out of bounds: " + i);
        }

        public Object[] backingArray() {
            return  array;
        }

        public int size() {
            return size;
        }
    }

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
     * @param task the task name {@link #withTask(Object)}
     * @param operation the operation {@link #withOperation(Object)}
     * @param msgType the message type {@link MessageType}
     */
    public SLog(Object task, Object operation, MessageType msgType) {
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
    public final SLog withOperationAlias(Object value) {
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
    public final SLog withOperation(Object value) {
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
    public SLog withTask(Object value) {
        if(!isEnabled) return this;

        this.mTask = escape(value);
        return this;
    }

    /**
     * Add a component override. See the description of this class for more on this.
     * @param value the component override.
     * @return this object
     */
    public final SLog withComponentOverride(Object value) {
        if(!isEnabled) return this;

        this.mComponentOverride = escape(value);
        return this;
    }

    /**
     * Add a component override. See the description of this class for more on this.
     * @param clazz the component override.
     * @return this object
     */
    public final SLog withComponentOverride(Class<?> clazz) {
        if(!isEnabled) return this;

        this.mComponentOverride = escape(clazz.getSimpleName());
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
    public final SLog withUserData(Object key, Object value) {
        if(!isEnabled) return this;

        if(mUserData == null) {
            mUserData = new ArrayBackedList<String>(4);
        }
        String strKey = key == null ? "" : key.toString();

        if(strKey.length() == 0) {
            strKey = "_MISSING_KEY_" + mUserData.size() / 2;
        }
        this.mUserData.add(escape(strKey));
        this.mUserData.add(escape(value));

        return this;
    }

    /**
     * Add a map of user key-value pairs to the log.
     * @param userData map of key-value pairs/
     * @return this object
     */
    public final SLog withUserData(Map<?, ?> userData) {
        if(!isEnabled || userData == null || userData.size() == 0) {
            return this;
        }

        for(Map.Entry<?, ?> entry : userData.entrySet()) {
            withUserData(entry.getKey().toString(), entry.getValue().toString());
        }
        return this;
    }

    static final Object[] EMPTY_ARRAY = {};
    /**
     * Escape objects in an array and convert them to String. The operation happens on
     * the input array.
     * @param objects
     */
    static Object[] escapeUserData(Object[] objects) {
        if(objects == null || objects.length < 2) {
            return EMPTY_ARRAY;
        }
        int i = 0;
        for(; i + 1 < objects.length; i = i + 2) {
            objects[i] = objects[i] == null ? "_MISSING_KEY_" +  i/2: escape(objects[i]);
            objects[i+1] = escape(objects[i+1]);
        }
        for(; i < objects.length; ++i) {
            objects[i] = null;
        }
        return objects;
    }

    /**
     * Escapes a string's backward slashes (\), semicolons (;), newlines (\n) and equals(=)
     * @param object
     * @return
     */
    static String escape(Object object) {
        if(object == null) return null;

        String string = object.toString();
        if(string.length() == 0) {
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
        return build(mTask, mOperation, mOperationAlias, mInstrumentationOverride,
                mComponentOverride, mMulticast, mMessageType,
                mUserData != null ? mUserData.backingArray() : null);
    }

    /**
     * Build the log.
     *
     * @return The newly built string that is ready to be logged by your preferred logger.
     */
    protected static String build(String task, String operation, String operationAlias,
                              String instrOverride, String compOverride,
                              boolean isMulticast, MessageType msgType, Object ...userData) {
        if(!isEnabled) return "";

        //sanitize
        if(task == null || task.length() == 0)
            task = "_MISSING_TASK_";
        if(operation == null || operation.length() == 0)
            operation = "_MISSING_OPERATION_";

        //compute capacity
        int len = 18 + task.length() + operation.length()   /* 20 => '+M=S;' = 5, '$SPG$+T=;' = 9, '+O=;' = 4  */
                + (operationAlias != null ? operationAlias.length() + 5 : 0)
                + (compOverride != null ? compOverride.length() + 5 : 0)
                + (instrOverride != null ? instrOverride.length() + 5 : 0);
        for(int i = 0; userData != null && i < userData.length; ++i) {
            len += userData[i] != null ? ((String)userData[i]).length() + 1 : 0;
        }

        //build string
        StringBuilder builder = new StringBuilder(len);
        builder.append(Key.TASK).append('=').append(task).append(';')
                .append(Key.OPERATION).append('=').append(operation).append(';')
                .append(Key.MESSAGE_TYPE).append('=').append(msgType).append(';');
        if(operationAlias != null)
            builder.append(Key.OPERATION_ALIAS).append('=').append(operationAlias).append(';');
        if(compOverride != null)
            builder.append(Key.COMPONENT).append('=').append(compOverride).append(';');
        if(instrOverride != null)
            builder.append(Key.INSTRUMENTATION).append('=').append(instrOverride).append(';');
        if(isMulticast)
            builder.append(Key.MULTICAST).append("=1;");


        for(int i = 0; userData != null && i < userData.length; i = i + 2) {
            if(userData[i] != null) {
                builder.append(userData[i]).append('=').append(userData[i + 1]).append(';');
            }
        }

        return builder.toString();
    }

}
