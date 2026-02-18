#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.spring.ai.agentexecutor;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class ChatModelConfiguration {

    @Bean
    @Profile("ollama")
    public ChatModel ollamaModel() {
        return AiModel.OLLAMA.chatModel("qwen3");
    }

    @Bean
    @Profile("openai")
    public ChatModel openaiModel() {
        return AiModel.OPENAI.chatModel("gpt-5-mini");
    }

    @Bean
    @Profile("gemini")
    public ChatModel geminiModel() {
        return AiModel.GEMINI.chatModel("gemini-2.5-pro");
    }

    @Bean
    @Profile("github-models")
    public ChatModel githubModel() {
        return AiModel.GITHUB_MODEL.chatModel("gpt-4o-mini");
    }

}
