package org.bsc.langgraph4j.agentexecutor;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.agent.ConversationContextPolicy;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.LinkedList;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Sliding-window conversation context policy that keeps at most {@code maxMessages}
 * graph messages.
 *
 * Patterned after the MessageWindowChatMemory from langchain4j
 *
 * <p>A leading {@link SystemMessage} is preserved while evicting old messages.
 * If an evicted {@link AiMessage} contains tool execution requests, following
 * orphan {@link ToolExecutionResultMessage}s are evicted as well.</p>
 */
public class MessageWindowConversationContextPolicy implements ConversationContextPolicy<ChatMessage> {

    private final int maxMessages;

    public MessageWindowConversationContextPolicy(int maxMessages) {
        if (maxMessages < 1) {
            throw new IllegalArgumentException("maxMessages must be greater than 0");
        }
        this.maxMessages = maxMessages;
    }

    public int maxMessages() {
        return maxMessages;
    }

    @Override
    public <S extends MessagesState<ChatMessage>> List<ChatMessage> filter(S state, RunnableConfig config) {
        return ensureCapacity(  state.messages(), maxMessages);
    }

    private static List<ChatMessage> ensureCapacity(List<ChatMessage> sourceMessages, int maxMessages) {
        final var filtered = new LinkedList<>(sourceMessages);

        while (filtered.size() > maxMessages) {
            final int messageToEvictIndex = (filtered.get(0) instanceof SystemMessage) ? 1 : 0 ;

            final var evictedMessage = filtered.remove(messageToEvictIndex);
            if (evictedMessage instanceof AiMessage aiMessage && aiMessage.hasToolExecutionRequests()) {

                while (filtered.size() > messageToEvictIndex
                        && filtered.get(messageToEvictIndex) instanceof ToolExecutionResultMessage) {
                    filtered.remove(messageToEvictIndex);
                }
            }
        }
        return filtered;
    }

}
