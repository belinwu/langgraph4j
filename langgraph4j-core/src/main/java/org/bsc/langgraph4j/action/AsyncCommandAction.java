package org.bsc.langgraph4j.action;

import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.state.AgentState;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * Represents an asynchronous action that, given a state and a configuration,
 * returns a {@link CompletableFuture} of a {@link Command}.
 * <p>
 * This is typically used for conditional edges in a state graph, where the
 * command determines the next node to execute and any state updates.
 *
 * @param <S> the type of the agent state, which must extend {@link AgentState}
 */
@FunctionalInterface
public interface AsyncCommandAction<S extends AgentState> extends BiFunction<S, RunnableConfig, CompletableFuture<Command>> {

    /**
     * Creates an {@link AsyncCommandAction} from a synchronous {@link CommandAction}.
     * <p>
     * This method wraps the synchronous action in a {@link CompletableFuture},
     * handling any exceptions that may occur during execution.
     *
     * @param syncAction the synchronous command action to convert
     * @param <S>        the type of the agent state
     * @return an asynchronous command action
     */
    static <S extends AgentState> AsyncCommandAction<S> command_async(CommandAction<S> syncAction) {
        return (state, config ) -> {
            var result = new CompletableFuture<Command>();
            try {
                result.complete(syncAction.apply(state, config));
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
            return result;
        };
    }


    /**
     * Creates an {@link AsyncCommandAction} from a synchronous {@link CommandAction}.
     *
     * @param syncAction the synchronous command action to convert
     * @param <S>        the type of the agent state
     * @return an asynchronous command action
     * @deprecated use {@link #command_async(CommandAction)} instead. This method will be removed in a future version.
     */
    @Deprecated( forRemoval = true )
    static <S extends AgentState> AsyncCommandAction<S> node_async(CommandAction<S> syncAction) {
        return command_async(syncAction);
    }

    /**
     * Creates an {@link AsyncCommandAction} from an {@link AsyncEdgeAction}.
     * <p>
     * The resulting string from the {@link AsyncEdgeAction} is used to create a new {@link Command}.
     *
     * @param action the asynchronous edge action
     * @param <S>    the type of the agent state
     * @return an asynchronous command action that wraps the result of the edge action in a command
     */
    static <S extends AgentState> AsyncCommandAction<S> of(AsyncEdgeAction<S> action) {
        return (state, config) ->
                action.apply(state).thenApply(Command::new);
    }

}