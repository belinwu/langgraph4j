package org.bsc.langgraph4j.agentexecutor;

import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import org.bsc.langgraph4j.StateGraph;

import java.time.Duration;
import java.util.Set;


public class AgentExecutorGeminiITest extends AbstractAgentExecutorTest {


    @Override
    protected StateGraph<AgentExecutor.State> newGraph( AgentExecutor.Serializers serializer ) throws Exception {

        final var chatModel = GoogleAiGeminiChatModel
                .builder()
                .timeout(Duration.ofSeconds(10))
                .supportedCapabilities(Set.of(Capability.RESPONSE_FORMAT_JSON_SCHEMA))
                .temperature(0.0)
                .apiKey(System.getenv( "GEMINI_API_KEY")) //Put your gemini api key here
                .modelName("gemini-3.1-flash-lite-preview")
                .returnThinking(true)
                .sendThinking(true)
                .build();

        return AgentExecutor.builder()
                .stateSerializer( serializer.object() )
                .chatModel(chatModel)
                .toolsFromObject(new TestTools())
                .build();

    }

    @Override
    protected StateGraph<AgentExecutor.State> newGraphWithStreaming( AgentExecutor.Serializers serializer, boolean emitStreamingOutputEnd ) throws Exception {

        final var chatModel = GoogleAiGeminiStreamingChatModel
                .builder()
                .timeout(Duration.ofSeconds(10))
                .temperature(0.0)
                .apiKey(System.getenv( "GEMINI_API_KEY")) //Put your gemini api key here
                .modelName("gemini-3.1-flash-lite-preview")
                .returnThinking(true)
                .sendThinking(true)
                .build();

        return AgentExecutor.builder()
                .stateSerializer( serializer.object() )
                .chatModel(chatModel, emitStreamingOutputEnd)
                .toolsFromObject(new TestTools())
                .build();

    }
}
