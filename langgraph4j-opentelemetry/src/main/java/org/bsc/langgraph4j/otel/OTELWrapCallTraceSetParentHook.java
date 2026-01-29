package org.bsc.langgraph4j.otel;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.AsyncCommandAction;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.action.Command;
import org.bsc.langgraph4j.hook.EdgeHook;
import org.bsc.langgraph4j.hook.NodeHook;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

/**
 * Creates a parent span and executes node/edge calls within its scope.
 * <p>
 * Use this hook when you want all node/edge spans created by other hooks to be
 * parented under a specific span (for example, one per workflow invocation).
 * </p>
 *
 * @param <State> workflow state type
 */
public class OTELWrapCallTraceSetParentHook<State extends AgentState> implements NodeHook.WrapCall<State>, EdgeHook.WrapCall<State>, OTELObservable {
    final TracerHolder tracer;
    final Span groupSpan;

    public static class Builder<State extends AgentState> {
        String scope;
        String groupName;
        Attributes groupAttributes;

        public Builder<State> scope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder<State> groupName(String groupName) {
            this.groupName = groupName;
            return this;
        }

        public Builder<State> groupAttributes(Attributes groupAttributes) {
            this.groupAttributes = groupAttributes;
            return this;
        }

        public OTELWrapCallTraceSetParentHook<State> build() {
            requireNonNull(groupName, "groupName cannot be null");
            return new OTELWrapCallTraceSetParentHook<>(this);
        }
    }

    public static <State extends AgentState> Builder<State> builder() {
        return new Builder<>();
    }

    private OTELWrapCallTraceSetParentHook( Builder<State> builder ) {
        tracer = new TracerHolder(otel(), ofNullable(builder.scope).orElse( "LG4J") );

        groupSpan = tracer.spanBuilder(builder.groupName)
                .setAllAttributes( ofNullable(builder.groupAttributes).orElseGet(Attributes::empty) )
                .startSpan();
    }

    /**
     * Executes a node action with the parent span as current.
     *
     * @param nodeId node identifier
     * @param state current state
     * @param config runnable configuration
     * @param action node action
     * @return future result of the action
     */
    @Override
    public CompletableFuture<Map<String, Object>> applyWrap(String nodeId,
                                                            State state,
                                                            RunnableConfig config,
                                                            AsyncNodeActionWithConfig<State> action) {
        return tracer.applySpan(groupSpan, $ -> {
            try ( var scope = groupSpan.makeCurrent() ) {
                return action.apply( state, config );
            }
        });

    }

    /**
     * Executes an edge action with the parent span as current.
     *
     * @param sourceId source node identifier
     * @param state current state
     * @param config runnable configuration
     * @param action edge action
     * @return future command result
     */
    @Override
    public CompletableFuture<Command> applyWrap(String sourceId,
                                                State state,
                                                RunnableConfig config,
                                                AsyncCommandAction<State> action) {
        return tracer.applySpan(groupSpan, $ -> {
            try (var scope = groupSpan.makeCurrent()) {
                return action.apply(state, config);
            }
        });
    }
}
