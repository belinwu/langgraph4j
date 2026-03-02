package org.bsc.langgraph4j.agentexecutor;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;

/**
 * Strategy used at model-call time to select which graph messages are sent to the LLM.
 * Implementations must not mutate the graph state and should return a filtered view/copy.
 */
@FunctionalInterface
public interface ConversationMemoryStrategy {

    /**
     * Filters graph messages before they are sent to the model.
     *
     * @param graphMessages messages currently stored in graph state
     * @return filtered messages to be sent to the model
     */
    List<ChatMessage> filter(List<ChatMessage> graphMessages);
}
