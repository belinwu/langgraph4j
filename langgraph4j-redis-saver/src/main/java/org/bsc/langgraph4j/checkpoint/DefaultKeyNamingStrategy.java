package org.bsc.langgraph4j.checkpoint;

/**
 * Default implementation of {@link KeyNamingStrategy} using standard LangGraph4j key patterns.
 * <p>
 * This implementation uses the following key patterns:
 * <ul>
 *   <li>Thread: {@code langgraph4j:thread:{thread_id}}</li>
 *   <li>Thread name: {@code langgraph4j:thread:name:{thread_name}:active}</li>
 *   <li>Checkpoint: {@code langgraph4j:checkpoint:{checkpoint_id}}</li>
 *   <li>Checkpoints: {@code langgraph4j:thread:{thread_id}:checkpoints}</li>
 *   <li>Key prefix: {@code langgraph4j:}</li>
 * </ul>
 */
public class DefaultKeyNamingStrategy implements KeyNamingStrategy {

    private static final String PREFIX = "langgraph4j:";
    private static final String THREAD_SUFFIX = "thread:";
    private static final String THREAD_NAME_SUFFIX = "thread:name:";
    private static final String THREAD_ACTIVE_SUFFIX = ":active";
    private static final String CHECKPOINT_SUFFIX = "checkpoint:";
    private static final String CHECKPOINTS_SUFFIX = ":checkpoints";

    @Override
    public String threadKey(String threadId) {
        return PREFIX + THREAD_SUFFIX + threadId;
    }

    @Override
    public String threadNameKey(String threadName) {
        return PREFIX + THREAD_NAME_SUFFIX + threadName + THREAD_ACTIVE_SUFFIX;
    }

    @Override
    public String checkpointKey(String checkpointId) {
        return PREFIX + CHECKPOINT_SUFFIX + checkpointId;
    }

    @Override
    public String checkpointsKey(String threadId) {
        return threadKey(threadId) + CHECKPOINTS_SUFFIX;
    }

    @Override
    public String keyPrefix() {
        return PREFIX;
    }
}
