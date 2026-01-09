package org.bsc.langgraph4j;

import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.hook.NodeHook;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.AgentStateFactory;
import org.bsc.langgraph4j.state.Channel;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.bsc.langgraph4j.utils.CollectionsUtils.mergeMap;

class NodeHooks<State extends AgentState> {

    static class Calls<T> {
        private Map<String,Deque<T>> callMap;
        private Deque<T> callList;

        public void add(T call ) {
            requireNonNull( call, "call cannot be null");

            if( callList == null ) { // Lazy Initialization
                callList = new ArrayDeque<>();
            }

            callList.push(call);
        }

        public void add(String nodeId, T call ) {
            requireNonNull( nodeId, "nodeId cannot be null");
            requireNonNull( call, "call cannot be null");

            if( callMap == null ) { // Lazy Initialization
                callMap = new HashMap<>();
            }

            callMap.computeIfAbsent(nodeId, k -> new ArrayDeque<>())
                    .push(call);

        }

        protected Stream<T> callListAsStream( ) {
            return ofNullable(callList).stream().flatMap(Collection::stream);
        }

        protected Stream<T> callMapAsStream( String nodeId ) {
            requireNonNull( nodeId, "nodeId cannot be null");
            return ofNullable(callMap).stream()
                    .flatMap( map ->
                            ofNullable( map.get(nodeId) ).stream()
                                    .flatMap( Collection::stream ));
        }

    }

    // BEFORE CALL HOOK
    class BeforeCalls extends Calls<NodeHook.BeforeCall<State>> {

        public CompletableFuture<Map<String, Object>> apply(State state, RunnableConfig config, AgentStateFactory<State> stateFactory, Map<String, Channel<?>> schema ) {
            return Stream.concat( callListAsStream(), callMapAsStream(config.nodeId()))
                    .reduce( completedFuture(state.data()),
                            (futureResult, call) ->
                                    futureResult.thenCompose( result -> call.accept(stateFactory.apply(result), config)
                                            .thenApply( partial -> AgentState.updateState( result, partial, schema ) )),
                            (f1, f2) -> f1.thenCompose(v -> f2) // Combiner for parallel streams
                    );
        }

    }
    final BeforeCalls beforeCalls = new BeforeCalls();

    // AFTER CALL HOOK
    class AfterCalls extends Calls<NodeHook.AfterCall<State>> {

        public CompletableFuture<Map<String, Object>> apply(State state, RunnableConfig config, Map<String,Object> partialResult ) {
            return Stream.concat( callListAsStream(), callMapAsStream(config.nodeId()))
                    .reduce( completedFuture(partialResult),
                            (futureResult, call) ->
                                    futureResult.thenCompose( result -> call.accept( state, config, result)
                                            .thenApply( partial -> mergeMap(result, partial, ( oldValue, newValue) -> newValue ) )),
                            (f1, f2) -> f1.thenCompose(v -> f2) // Combiner for parallel streams
                    );
        }

    }

    final AfterCalls afterCalls = new AfterCalls();

    // WRAP CALL HOOK

    private record WrapCallChainLink<State extends AgentState>  (
            NodeHook.WrapCall<State> delegate,
            AsyncNodeActionWithConfig<State> action
    )  implements AsyncNodeActionWithConfig<State> {

        @Override
        public CompletableFuture<Map<String, Object>> apply(State state, RunnableConfig config) {
            return delegate.apply(state, config, action);
        }
    }


    class WrapCalls extends Calls<NodeHook.WrapCall<State>> {
        public CompletableFuture<Map<String, Object>> apply(State state, RunnableConfig config, AsyncNodeActionWithConfig<State> action ) {
            return Stream.concat( callListAsStream(), callMapAsStream(config.nodeId()))
                    .reduce(action,
                            (acc, wrapper) -> new WrapCallChainLink<>(wrapper, acc),
                            (v1, v2) -> v1)
                    .apply(state, config);
        }

    }
    final WrapCalls wrapCalls = new WrapCalls();

    // ALL IN ONE METHOD

    public CompletableFuture<Map<String, Object>> applyActionWithHooks( AsyncNodeActionWithConfig<State> action,
                                                                        State state,
                                                                        RunnableConfig config,
                                                                        AgentStateFactory<State> stateFactory,
                                                                        Map<String, Channel<?>> schema ) {
        return beforeCalls.apply( state, config, stateFactory, schema )
                .thenApply( processedResult -> {
                    final var newStateData = AgentState.updateState(state, processedResult, schema);
                    return stateFactory.apply(newStateData);
                })
                .thenCompose( newState -> wrapCalls.apply(newState, config, action)
                    .thenCompose( partial -> afterCalls.apply(newState, config, partial) ));

    }
}
