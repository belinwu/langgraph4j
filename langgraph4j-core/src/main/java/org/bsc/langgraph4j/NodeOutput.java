package org.bsc.langgraph4j;

import org.bsc.langgraph4j.state.AgentState;

import java.util.Objects;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

/**
 * Represents the output of a node in a graph.
 *
 * @param <State> the type of the state associated with the node output
 */
public class NodeOutput<State extends AgentState> {

    public static <State extends AgentState> NodeOutput<State> of( String node, State state ) {
        return new NodeOutput<>(node, state);
    }

    /**
     * The identifier of the node.
     */
    private final String node;

    /**
     * The state associated with the node.
     */
    private final State state;

    /**
     * Returns the node name.
     *
     * @return the node name
     */
    public String node() {
        return node;
    }

    public State state() {
        return state;
    }

    /**
     * Checks if the current node refers to the start of the graph processing.
     *
     * @return {@code true} if the current node refers to the start of the graph processing
     */
    public boolean isSTART() {
        return Objects.equals(node(),START);
    }
    /**
     * Checks if the current node refers to the end of the graph processing.
     * useful to understand if the workflow has been interrupted.
     *
     * @return {@code true} if the current node refers to the end of the graph processing
     */
    public boolean isEND() {
        return Objects.equals(node(),END);
    }

    public NodeOutput( String node, State state ) {
        this.node = node;
        this.state = state;
    }

    @Override
    public String toString() {
        return "NodeOutput{ node=%s, state=%s}".formatted(
                node(),
                state());
    }

}
