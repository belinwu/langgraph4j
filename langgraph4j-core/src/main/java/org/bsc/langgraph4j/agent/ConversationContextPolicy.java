package org.bsc.langgraph4j.agent;

import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;

/**
 * Policy used at model-call time to select which graph messages are sent to the LLM.
 * Implementations must not mutate the graph state and should return a filtered view/copy.
 */
@FunctionalInterface
public interface ConversationContextPolicy<Message> {

    /**
     * Filters graph messages before they are sent to the model.
     *
     * @param state the state containing messages currently stored in graph
     * @return filtered messages to be sent to the model
     */
    <State extends MessagesState<Message> > List<Message> filter(State state, RunnableConfig config);
}
