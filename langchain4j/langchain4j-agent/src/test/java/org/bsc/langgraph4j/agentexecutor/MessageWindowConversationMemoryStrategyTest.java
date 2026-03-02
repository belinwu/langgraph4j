package org.bsc.langgraph4j.agentexecutor;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageWindowConversationMemoryStrategyTest {

    @Test
    void shouldKeepMostRecentMessagesWithinWindow() {
        var strategy = new MessageWindowConversationMemoryStrategy(3);
        List<ChatMessage> messages = List.of(
                UserMessage.from("m1"),
                UserMessage.from("m2"),
                UserMessage.from("m3"),
                UserMessage.from("m4"),
                UserMessage.from("m5")
        );

        var filtered = strategy.filter(messages);

        assertEquals(3, filtered.size());
        assertEquals("m3", ((UserMessage) filtered.get(0)).singleText());
        assertEquals("m4", ((UserMessage) filtered.get(1)).singleText());
        assertEquals("m5", ((UserMessage) filtered.get(2)).singleText());
    }

    @Test
    void shouldPreserveLeadingSystemMessageWhenEvicting() {
        var strategy = new MessageWindowConversationMemoryStrategy(2);
        var messages = List.of(
                SystemMessage.from("system"),
                UserMessage.from("m1"),
                UserMessage.from("m2"),
                UserMessage.from("m3")
        );

        var filtered = strategy.filter(messages);

        assertEquals(2, filtered.size());
        assertEquals(SystemMessage.class, filtered.get(0).getClass());
        assertEquals("m3", ((UserMessage) filtered.get(1)).singleText());
    }

    @Test
    void shouldRemoveOrphanToolResultsWhenEvictingAiToolRequest() {
        var strategy = new MessageWindowConversationMemoryStrategy(1);
        var toolRequest = ToolExecutionRequest.builder()
                .id("id-1")
                .name("test_tool")
                .arguments("{}")
                .build();

        var messages = List.of(
                AiMessage.from(toolRequest),
                ToolExecutionResultMessage.from(toolRequest, "done"),
                UserMessage.from("latest")
        );

        var filtered = strategy.filter(messages);

        assertEquals(1, filtered.size());
        assertEquals(UserMessage.class, filtered.get(0).getClass());
        assertEquals("latest", ((UserMessage) filtered.get(0)).singleText());
    }

    @Test
    void shouldNotMutateInputMessages() {
        var strategy = new MessageWindowConversationMemoryStrategy(2);
        List<ChatMessage> original = new ArrayList<>(List.of(
                UserMessage.from("m1"),
                UserMessage.from("m2"),
                UserMessage.from("m3")
        ));

        strategy.filter(original);

        assertEquals(3, original.size());
        assertEquals("m1", ((UserMessage) original.get(0)).singleText());
        assertEquals("m2", ((UserMessage) original.get(1)).singleText());
        assertEquals("m3", ((UserMessage) original.get(2)).singleText());
    }

    @Test
    void shouldFailForNonPositiveWindowSize() {
        assertThrows(IllegalArgumentException.class, () -> new MessageWindowConversationMemoryStrategy(0));
    }
}
