package org.bsc.langgraph4j.otel;

import io.opentelemetry.api.common.Attributes;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.AsyncCommandAction;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.action.Command;
import org.bsc.langgraph4j.hook.EdgeHook;
import org.bsc.langgraph4j.hook.NodeHook;
import org.bsc.langgraph4j.serializer.StateSerializer;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public class OTELWrapCallTraceHook<State extends AgentState> implements NodeHook.WrapCall<State>, EdgeHook.WrapCall<State>, Instrumentable {

    final StateSerializer<?> stateSerializer;
    final TracerHolder tracer;

    public OTELWrapCallTraceHook(StateSerializer<?> stateSerializer) {
        this.stateSerializer = requireNonNull(stateSerializer, "stateSerializer cannot be null");
        tracer = new TracerHolder(otel(), getClass().getName() );
    }

    @Override
    public CompletableFuture<Map<String, Object>> applyWrap(String nodeId,
                                                            State state,
                                                            RunnableConfig config,
                                                            AsyncNodeActionWithConfig<State> action) {
        var span = tracer.spanBuilder("evaluateNode")
                .setAttribute("nodeId", nodeId)
                .setAllAttributes( Instrumentable.attrsOf( config ) )
                .startSpan();

        return tracer.applySpan( span, $ -> {
            otelLog.info("\nnode start: '{}' with state: {}",
                    nodeId,
                    state);

            try ( var scope = span.makeCurrent() ) {

                span.addEvent( "start", Instrumentable.attrsOf( state.data(), stateSerializer) );

                return action.apply( state, config )
                    .whenComplete( (result, ex ) -> {
                        if( ex != null ) {
                            return;
                        }

                        span.addEvent( "end", Instrumentable.attrsOf( result, stateSerializer) );

                        otelLog.info("\nnode end: '{}' with result: {}",
                                nodeId,
                                result);
                    });
                }
            });

    }

    @Override
    public CompletableFuture<Command> applyWrap(String sourceId,
                                                State state,
                                                RunnableConfig config,
                                                AsyncCommandAction<State> action) {

        var span = tracer.spanBuilder("evaluateEdge")
                .setAttribute("sourceId", sourceId)
                .setAllAttributes( Instrumentable.attrsOf( config ) )
                .startSpan();

        return tracer.applySpan( span, $ -> {

            otelLog.info("\nedge start from: '{}' with state: {}", sourceId, state);

            try ( var scope = span.makeCurrent() ) {

                span.addEvent("start", Instrumentable.attrsOf(state.data(), stateSerializer));

                return action.apply(state, config).whenComplete((result, ex) -> {

                    if (ex != null) {
                        return;
                    }

                    span.addEvent("end", Instrumentable.attrsOf(result, stateSerializer));

                    otelLog.info("\nedge end: {}", result);

                });
            }
        });
    }
}
