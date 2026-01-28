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

public class OTELWrapCallTraceSetParentHook<State extends AgentState> implements NodeHook.WrapCall<State>, EdgeHook.WrapCall<State>, Instrumentable {
    final TracerHolder tracer;
    final Span span;

    public OTELWrapCallTraceSetParentHook(String scope, String name, Attributes attributes ) {
        tracer = new TracerHolder(otel(), requireNonNull(scope, "scope cannot be null") );

        span = tracer.spanBuilder(requireNonNull(name, "name cannot be null"))
                .setAllAttributes(attributes)
                .startSpan();
    }
    public OTELWrapCallTraceSetParentHook( String name, Attributes attributes ) {
        this( "LG4J", name, attributes);
    }

    public OTELWrapCallTraceSetParentHook( String name ) {
        this( "LG4J", name, Attributes.empty());
    }

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
