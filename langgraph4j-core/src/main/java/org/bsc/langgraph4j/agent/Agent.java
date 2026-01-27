package org.bsc.langgraph4j.agent;

import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncCommandAction;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.hook.EdgeHook;
import org.bsc.langgraph4j.hook.NodeHook;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.serializer.StateSerializer;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.utils.EdgeMappings;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static org.bsc.langgraph4j.StateGraph.START;

/**
 * Interface representing an Agent Executor (AKA ReACT agent).
 * This implementation make in evidence the tools execution using and action dispatcher node
 * <pre>
 *              ┌─────┐
 *              │start│
 *              └─────┘
 *                 |
 *              ┌─────┐
 *              │model│
 *              └─────┘
 *                 |
 *          ┌─────────────────┐
 *          │ action_executor │
 *          └─────────────────┘
 *                 |
 *               ┌────┐
 *               │stop│
 *               └────┘
 * </pre>
 */
public interface Agent {

    String AGENT_LABEL = "agent";
    String ACTION_LABEL = "action";

    String END_LABEL = "end";

    static <M, S extends MessagesState<M>> Builder<M,S> builder() {
        return new Builder<>();
    }

    class Builder<M, S extends MessagesState<M>> {

        private StateSerializer<S> stateSerializer;
        private AsyncNodeActionWithConfig<S> callModelAction;
        private AsyncCommandAction<S> executeToolsAction;
        private Map<String, Channel<?>> schema;
        private List<NodeHook.WrapCall<S>> callModelHooks;
        private List<EdgeHook.WrapCall<S>> executeToolsHooks;

        private static <H> List<H> addHook( List<H> list, H hook) {
            if( list == null ) {
                list = new LinkedList<>();
            }
            list.add( hook );
            return list;
        }

        public Builder<M,S> stateSerializer(StateSerializer<S> stateSerializer) {
            this.stateSerializer = stateSerializer;
            return this;
        }

        public Builder<M,S> schema(Map<String, Channel<?>> schema) {
            this.schema = schema;
            return this;
        }

        public Builder<M,S> callModelAction(AsyncNodeActionWithConfig<S> callModelAction) {
            this.callModelAction = callModelAction;
            return this;
        }

        public Builder<M,S> addCallModelHook(NodeHook.WrapCall<S> wrapCall ) {
            callModelHooks = addHook(callModelHooks, wrapCall);
            return this;
        }

        public Builder<M,S> executeToolsAction(AsyncCommandAction<S> executeToolsAction) {
            this.executeToolsAction = executeToolsAction;
            return this;
        }

        public Builder<M,S> addExecuteToolsHook(EdgeHook.WrapCall<S> wrapCall ) {
            executeToolsHooks = addHook(executeToolsHooks, wrapCall);
            return this;
        }

        public StateGraph<S> build() throws GraphStateException {

            var graph =  new StateGraph<>(
                    requireNonNull(schema, "schema is required!"),
                    requireNonNull(stateSerializer, "stateSerializer is required!"));
            // add hooks
            ofNullable(callModelHooks)
                    .orElseGet( List::of )
                    .forEach( hook -> graph.addWrapCallNodeHook( AGENT_LABEL, hook));
            ofNullable(executeToolsHooks)
                    .orElseGet( List::of )
                    .forEach( hook -> graph.addWrapCallEdgeHook( ACTION_LABEL, hook));

            return graph
                .addNode(AGENT_LABEL,  requireNonNull(callModelAction, "callModelAction is required!") )
                .addNode(ACTION_LABEL,
                        requireNonNull(executeToolsAction, "executeToolsAction is required!"),
                        EdgeMappings.builder()
                        .to(AGENT_LABEL)
                        .toEND(END_LABEL)
                        .build())
                .addEdge(START, AGENT_LABEL)
                .addEdge(AGENT_LABEL, ACTION_LABEL)
                ;

        }

    }

}
