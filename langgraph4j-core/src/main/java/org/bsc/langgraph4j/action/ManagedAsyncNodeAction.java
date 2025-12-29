package org.bsc.langgraph4j.action;

import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.hook.NodeHooks;
import org.bsc.langgraph4j.internal.node.Node;
import org.bsc.langgraph4j.state.AgentState;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;

public class ManagedAsyncNodeAction<State extends AgentState> implements AsyncNodeActionWithConfig<State> {

    public final NodeHooks<State> hooks = new NodeHooks<>();

    protected final AsyncNodeActionWithConfig<State> delegate;

    public ManagedAsyncNodeAction(String nodeId, AsyncNodeActionWithConfig<State> delegate) {
        this.delegate = requireNonNull(delegate, "delegate cannot be null");
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(State state, RunnableConfig config) {
        return hooks.applyWrapCall(state, config, delegate);
    }

    private static class Interruptable<State extends AgentState> extends ManagedAsyncNodeAction<State> implements InterruptableAction<State> {

        public Interruptable(String nodeId, AsyncNodeActionWithConfig<State> delegate) {
            super(nodeId, delegate);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Optional<InterruptionMetadata<State>> interrupt(String nodeId, State state, RunnableConfig config) {
            return ((InterruptableAction<State>) delegate).interrupt(nodeId, state, config);
        }
    }

    @SuppressWarnings("unchecked")
    public static <State extends AgentState> Node.ActionFactory<State> factory(String nodeId, AsyncNodeActionWithConfig<State> delegate) {

        return ( config ) -> {

            final var action = new ManagedAsyncNodeAction<>( nodeId, delegate);

            if( delegate instanceof InterruptableAction<?> ) {
                final var proxyInstance = Proxy.newProxyInstance(action.getClass().getClassLoader(),
                        new Class[]{AsyncNodeActionWithConfig.class, InterruptableAction.class},
                        (proxy, method, methodArgs) -> {
                            if (method.getName().equals("interrupt")) {
                                return method.invoke(delegate, methodArgs);
                            }
                            return method.invoke(action, methodArgs);
                        }
                );
                return (AsyncNodeActionWithConfig<State>) proxyInstance;
            }
            return action;
        };
    }

}
