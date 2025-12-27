package org.bsc.langgraph4j.internal.node;

import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.action.InterruptableAction;
import org.bsc.langgraph4j.action.InterruptionMetadata;
import org.bsc.langgraph4j.hook.NodeHooks;
import org.bsc.langgraph4j.hook.TrackGraphNodeHook;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public class ManagedAsyncNodeActionWithConfig<State extends AgentState> implements AsyncNodeActionWithConfig<State> {

    public final NodeHooks<State> hooks = new NodeHooks<>();

    protected final AsyncNodeActionWithConfig<State> delegate;

    public ManagedAsyncNodeActionWithConfig(String nodeId, AsyncNodeActionWithConfig<State> delegate) {
        this.delegate = requireNonNull(delegate, "delegate cannot be null");
        this.hooks.registerWrapCall( new TrackGraphNodeHook<>( requireNonNull(nodeId, "nodeId cannot be null")));

    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(State state, RunnableConfig config) {
        return hooks.applyWrapCall(state, config, delegate);
    }


    private static class Interruptable<State extends AgentState> extends ManagedAsyncNodeActionWithConfig<State> implements InterruptableAction<State> {

        public Interruptable(String nodeId, AsyncNodeActionWithConfig<State> delegate) {
            super(nodeId, delegate);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Optional<InterruptionMetadata<State>> interrupt(String nodeId, State state, RunnableConfig config) {
            return ((InterruptableAction<State>) delegate).interrupt(nodeId, state, config);
        }
    }

    public static <State extends AgentState> Node.ActionFactory<State> factory( String nodeId, AsyncNodeActionWithConfig<State> delegate) {
        return ( config ) -> {
            if( delegate instanceof InterruptableAction<?>  ) {
                return new Interruptable<>( nodeId, delegate );
            }
            return new ManagedAsyncNodeActionWithConfig<>( nodeId, delegate);
        };
    }

}
