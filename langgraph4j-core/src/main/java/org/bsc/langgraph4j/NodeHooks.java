package org.bsc.langgraph4j;

import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.hook.NodeHook;
import org.bsc.langgraph4j.state.AgentState;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

class NodeHooks<State extends AgentState> {

    private record WrapCallHolder<State extends AgentState>  (
            NodeHook.WrapCall<State> delegate,
            AsyncNodeActionWithConfig<State> action
    )  implements AsyncNodeActionWithConfig<State> {

        @Override
        public CompletableFuture<Map<String, Object>> apply(State state, RunnableConfig config) {
            return delegate.apply(state, config, action);
        }
    }

    private Map<String,List<NodeHook.WrapCall<State>>> wrapCallMap;
    private List<NodeHook.WrapCall<State>> wrapCallList;

    public void registerWrapCall( NodeHook.WrapCall<State> wrapCall ) {
        requireNonNull( wrapCall, "wrapCall cannot be null");

        if( wrapCallList == null ) { // Lazy Initialization
            wrapCallList = new LinkedList<>();
        }

        wrapCallList.add(wrapCall);
    }

    public void registerWrapCall( String nodeId, NodeHook.WrapCall<State> wrapCall ) {
        requireNonNull( nodeId, "nodeId cannot be null");
        requireNonNull( wrapCall, "wrapCall cannot be null");

        if( wrapCallMap == null ) { // Lazy Initialization
            wrapCallMap = new HashMap<>();
        }

        final var values = wrapCallMap.computeIfAbsent(nodeId, k -> new LinkedList<>());

        values.add(wrapCall);

    }

    private Stream<NodeHook.WrapCall<State>> wrapCallListAsStream( ) {
        return ofNullable(wrapCallList).stream().flatMap(Collection::stream);
    }

    private Stream<NodeHook.WrapCall<State>> wrapCallMapAsStream(String nodeId ) {
        return ofNullable(wrapCallMap).stream()
                .flatMap( map ->
                    ofNullable( map.get(nodeId) ).stream()
                            .flatMap( Collection::stream ));
    }

    public CompletableFuture<Map<String, Object>> applyWrapCall( AsyncNodeActionWithConfig<State> action, State state, RunnableConfig config ) {
        return Stream.concat( wrapCallListAsStream(), wrapCallMapAsStream(config.nodeId()))
                .reduce(action,
                        (acc, wrapper) -> new WrapCallHolder<>(wrapper, acc),
                        (v1, v2) -> v1)
                .apply(state, config);
    }

}
