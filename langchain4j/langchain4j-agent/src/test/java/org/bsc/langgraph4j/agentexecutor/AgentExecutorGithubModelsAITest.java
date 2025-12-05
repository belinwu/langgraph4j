package org.bsc.langgraph4j.agentexecutor;

import dev.langchain4j.model.github.GitHubModelsChatModel;
import dev.langchain4j.model.github.GitHubModelsChatModelName;
import org.bsc.langgraph4j.StateGraph;

public class AgentExecutorGithubModelsAITest extends AbstractAgentExecutorTest  {

    @Override
    protected StateGraph<AgentExecutor.State> newGraph() throws Exception {

        var chatLanguageModel = GitHubModelsChatModel.builder()
                .gitHubToken( System.getenv( "GITHUB_MODELS_TOKEN") )
                .modelName( GitHubModelsChatModelName.GPT_4_O_MINI)
                .logRequestsAndResponses(true)
                .temperature(0.0)
                .build();

        return AgentExecutor.builder()
                .chatModel(chatLanguageModel)
                .toolsFromObject(new TestTools())
                .build();
    }
}
