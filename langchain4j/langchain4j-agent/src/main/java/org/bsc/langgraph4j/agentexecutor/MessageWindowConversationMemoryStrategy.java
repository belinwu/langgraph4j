package org.bsc.langgraph4j.agentexecutor;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Sliding-window conversation memory strategy that keeps at most {@code maxMessages}
 * graph messages.
 *
 * Patterned after the MessageWindowChatMemory from langchain4j
 *
 * <p>A leading {@link SystemMessage} is preserved while evicting old messages.
 * If an evicted {@link AiMessage} contains tool execution requests, following
 * orphan {@link ToolExecutionResultMessage}s are evicted as well.</p>
 */
public class MessageWindowConversationMemoryStrategy implements ConversationMemoryStrategy {

    private final int maxMessages;

    public MessageWindowConversationMemoryStrategy(int maxMessages) {
        if (maxMessages < 1) {
            throw new IllegalArgumentException("maxMessages must be greater than 0");
        }
        this.maxMessages = maxMessages;
    }

    public int maxMessages() {
        return maxMessages;
    }

    @Override
    public List<ChatMessage> filter(List<ChatMessage> graphMessages) {
        Objects.requireNonNull(graphMessages, "graphMessages cannot be null");
        var filtered = new LinkedList<>(graphMessages);
        ensureCapacity(filtered, maxMessages);
        return filtered;
    }

    private static void ensureCapacity(List<ChatMessage> messages, int maxMessages) {
        while (messages.size() > maxMessages) {
            int messageToEvictIndex = 0;
            if (messages.get(0) instanceof SystemMessage) {
                messageToEvictIndex = 1;
            }

            ChatMessage evictedMessage = messages.remove(messageToEvictIndex);
            if (evictedMessage instanceof AiMessage aiMessage && aiMessage.hasToolExecutionRequests()) {
                while (messages.size() > messageToEvictIndex
                        && messages.get(messageToEvictIndex) instanceof ToolExecutionResultMessage) {
                    messages.remove(messageToEvictIndex);
                }
            }
        }
    }
}
