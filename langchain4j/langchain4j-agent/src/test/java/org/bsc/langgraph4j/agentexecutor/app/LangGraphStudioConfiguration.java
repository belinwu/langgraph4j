package org.bsc.langgraph4j.agentexecutor.app;

import dev.langchain4j.data.message.UserMessage;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.GraphRepresentation;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.agentexecutor.AgentExecutorEx;
import org.bsc.langgraph4j.agentexecutor.AiModel;
import org.bsc.langgraph4j.agentexecutor.TestTools;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.studio.LangGraphStudioServer;
import org.bsc.langgraph4j.studio.springboot.LangGraphStudioConfig;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Objects;

@Configuration
public class LangGraphStudioConfiguration extends LangGraphStudioConfig {

    final Map<String, LangGraphStudioServer.Instance> instanceMap;

    @Override
    public Map<String, LangGraphStudioServer.Instance> instanceMap() {
        return instanceMap;
    }

    public LangGraphStudioConfiguration() throws GraphStateException {

        var workflow =  AgentExecutorEx.builder()
                .chatModel(AiModel.OLLAMA.chatModel( "qwen2.5:7b" ))
                .toolsFromObject(new TestTools())
                .build();
        System.out.println( workflow.getGraph(GraphRepresentation.Type.PLANTUML, "ReACT Agent", false ).content() );

        this.instanceMap = agentWorkflow( workflow );
    }

    private Map<String, LangGraphStudioServer.Instance> agentWorkflow( StateGraph<? extends AgentState> workflow ) throws GraphStateException {

        return  Map.of( "sample", LangGraphStudioServer.Instance.builder()
                            .title("LangGraph Studio (LangChain4j)")
                            .addInputStringArg( "messages", true, v -> UserMessage.from( Objects.toString(v) ) )
                            .graph( workflow )
                            .compileConfig( CompileConfig.builder()
                                    .checkpointSaver( new MemorySaver() )
                                    .build())
                            .build());

    }

}
