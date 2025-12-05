package org.bsc.langgraph4j.agentexecutor;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.bsc.langgraph4j.StateGraph;


public class AgentExecutorITest extends AbstractAgentExecutorTest {


    @Override
    protected StateGraph<AgentExecutor.State> newGraph() throws Exception {

        var chatModel = OllamaChatModel.builder()
                .modelName( "qwen2.5:7b" )
                .baseUrl("http://localhost:11434")
                .logResponses(true)
                .maxRetries(2)
                .temperature(0.0)
                .build();

        return AgentExecutor.builder()
                .chatModel(chatModel)
                .toolsFromObject(new TestTools())
                .build();

    }
}
