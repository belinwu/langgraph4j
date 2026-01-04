package org.bsc.langgraph4j.action;

import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.hook.NodeHooks;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public class ManagedAsyncNodeAction<State extends AgentState> implements AsyncNodeActionWithConfig<State>, InterruptableAction<State> {

    public final NodeHooks<State> hooks = new NodeHooks<>();

    protected final AsyncNodeActionWithConfig<State> delegate;

    public ManagedAsyncNodeAction(String nodeId, AsyncNodeActionWithConfig<State> delegate) {
        this.delegate = requireNonNull(delegate, "delegate cannot be null");
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(State state, RunnableConfig config) {
        return hooks.applyWrapCall(state, config, delegate);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<InterruptionMetadata<State>> interrupt(String nodeId, State state, RunnableConfig config) {
        if( delegate instanceof InterruptableAction<?> interruptableAction) {
            return ((InterruptableAction<State>) interruptableAction).interrupt(nodeId, state, config);
        }
        return Optional.empty();
    }

}
