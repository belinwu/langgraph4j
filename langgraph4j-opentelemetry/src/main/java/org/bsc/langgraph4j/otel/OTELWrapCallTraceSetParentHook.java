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
public class OTELWrapCallTraceSetParentHook<State extends AgentState> implements NodeHook.WrapCall<State>, EdgeHook.WrapCall<State>, Instrumentable {
    final TracerHolder tracer;
    final Span span;

    /**
     * Creates a parent span with explicit tracer scope, name, and attributes.
     *
     * @param scope tracer scope name
     * @param name span name
     * @param attributes span attributes
     */
    public OTELWrapCallTraceSetParentHook(String scope, String name, Attributes attributes ) {
        tracer = new TracerHolder(otel(), requireNonNull(scope, "scope cannot be null") );

        span = tracer.spanBuilder(requireNonNull(name, "name cannot be null"))
                .setAllAttributes(attributes)
                .startSpan();
    }
    /**
     * Creates a parent span with default scope {@code LG4J}.
     *
     * @param name span name
     * @param attributes span attributes
     */
    public OTELWrapCallTraceSetParentHook( String name, Attributes attributes ) {
        this( "LG4J", name, attributes);
    }

    /**
     * Creates a parent span with default scope {@code LG4J} and no attributes.
     *
     * @param name span name
     */
    public OTELWrapCallTraceSetParentHook( String name ) {
        this( "LG4J", name, Attributes.empty());
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
        return tracer.applySpan( span, $ -> {
            try ( var scope = span.makeCurrent() ) {
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
        return tracer.applySpan(span, $ -> {
            try (var scope = span.makeCurrent()) {
                return action.apply(state, config);
            }
        });
    }
}
