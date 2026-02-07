package org.bsc.langgraph4j.checkpoint;

/**
 * Strategy interface for customizing Redis key naming patterns.
 * <p>
 * Implementations can provide custom key naming conventions to integrate
 * with existing Redis key structures or to follow organizational naming standards.
 * <p>
 * Example custom implementation:
 * <pre>
 * KeyNamingStrategy customNaming = new KeyNamingStrategy() {
 *     {@literal @}Override
 *     public String threadKey(String threadId) {
 *         return "myapp:threads:" + threadId;
 *     }
 *     // ... implement other methods
 * };
 * </pre>
 *
 * @see DefaultKeyNamingStrategy
 */
public interface KeyNamingStrategy {

    /**
     * Returns the Redis key for storing thread metadata.
     *
     * @param threadId the thread identifier
     * @return the Redis key for thread hash
     */
    String threadKey(String threadId);

    /**
     * Returns the Redis key for looking up active thread by name.
     *
     * @param threadName the thread name
     * @return the Redis key for active thread lookup
     */
    String threadNameKey(String threadName);

    /**
     * Returns the Redis key for storing checkpoint data.
     *
     * @param checkpointId the checkpoint identifier
     * @return the Redis key for checkpoint hash
     */
    String checkpointKey(String checkpointId);

    /**
     * Returns the Redis key for the sorted set containing ordered checkpoints.
     *
     * @param threadId the thread identifier
     * @return the Redis key for checkpoints sorted set
     */
    String checkpointsKey(String threadId);

    /**
     * Returns the key prefix used for cleanup operations.
     * <p>
     * This prefix is used to identify all keys managed by this naming strategy
     * for bulk operations like clearing all data.
     *
     * @return the key prefix
     */
    String keyPrefix();
}
