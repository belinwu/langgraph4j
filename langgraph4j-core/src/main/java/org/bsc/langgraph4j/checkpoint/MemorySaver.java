package org.bsc.langgraph4j.checkpoint;

import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.utils.TryFunction;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class MemorySaver extends AbstractCheckpointSaver {
    private final Map<String, LinkedList<Checkpoint>> _checkpointsByThread = new HashMap<>();
    private final ReentrantLock _lock = new ReentrantLock();

    protected final Map<String, LinkedList<Checkpoint>> cache() {
        return Map.copyOf(_checkpointsByThread);
    }

    @Override
    protected final void insertedCheckpoint( RunnableConfig config, LinkedList<Checkpoint> checkpoints, Checkpoint checkpoint) throws Exception {
    }

    @Override
    protected final void updatedCheckpoint( RunnableConfig config, LinkedList<Checkpoint> checkpoints, Checkpoint checkpoint) throws Exception {
    }

    @Override
    protected final Tag releaseCheckpoints(RunnableConfig config, LinkedList<Checkpoint> checkpoints) throws Exception {
        final var threadId = threadId(config);
        return new Tag( threadId(config), _checkpointsByThread.remove( threadId ) );
    }

    @Override
    protected LinkedList<Checkpoint> loadCheckpoints(RunnableConfig config) throws Exception {
        final var threadId = threadId(config);

        return _checkpointsByThread.computeIfAbsent(threadId, k -> new LinkedList<>());

    }

    @Override
    protected final <T> T loadOrInitCheckpoints(RunnableConfig config,
                                                TryFunction<LinkedList<Checkpoint>, T, Exception> transformer) throws Exception {
        _lock.lock();
        try {
            return super.loadOrInitCheckpoints(config, transformer);
        } finally {
            _lock.unlock();
        }
    }

}
