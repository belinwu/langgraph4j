package org.bsc.langgraph4j.action;

import org.bsc.langgraph4j.utils.CollectionsUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * Represents the outcome of a {@link CommandAction} within a LangGraph4j graph.
 * A {@code Command} encapsulates instructions for the graph's next step, including
 * a target node to transition to and a map of updates to be applied
 * to the {@link org.bsc.langgraph4j.state.AgentState}.
 *
 * @param gotoNode containing the name of the next node to execute.
 * @param update   A {@link Map} containing key-value pairs representing updates
 *                 to be merged into the current agent state. An empty map indicates
 *                 no state updates.
 */
public record Command(String gotoNode, Map<String,Object> update) {
    private static final Command EMPTY_COMMAND = new Command( Map.of() );

    public static Command emptyCommand() {
        return EMPTY_COMMAND;
    }

    public String gotoNode() {
        return Objects.requireNonNull(gotoNode, "gotoNode cannot be null");
    }

    public Map<String,Object> update() {
        return ofNullable(update).orElseGet(Map::of);
    }

    public Optional<String> gotoNodeSafe() {
        return ofNullable(gotoNode);
    }

    /**
     * check for null values
     */
    public Command {
        if( gotoNode == null && update == null ) {
            throw new IllegalArgumentException("gotoNode and update cannot both be null");
        }
    }

    /**
     * Constructs a {@code Command} that specifies only the next node to transition to,
     * with no state updates.
     * If {@code gotoNode} is null, it will be treated as an empty {@link Optional}.
     *
     * @param gotoNode The name of the next node to transition to. Can be null.
     */
    public Command( String gotoNode ) {
        this( gotoNode, null );
    }

    public Command( Map<String,Object> update ) {
        this( null, update );
    }

    @Override
    public String toString() {
        if( update == null && gotoNode == null ) {
            return "empty command";
        }
        if( update == null ) {
            return "goto node '%s'%n".formatted( gotoNode );
        }

        return "goto node '%s' with update %s%n".formatted( gotoNode, CollectionsUtils.toString(update));
    }
}