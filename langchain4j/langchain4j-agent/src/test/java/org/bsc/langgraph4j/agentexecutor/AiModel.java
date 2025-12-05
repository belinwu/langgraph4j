package org.bsc.langgraph4j.agentexecutor;

import dev.langchain4j.model.chat.Capability;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;

public enum AiModel {

    OPENAI( (model, extra) ->
            OpenAiChatModel.builder()
                .apiKey( extraAttribute( extra, "OPENAI_API_KEY" ) )
                //.modelName( "gpt-4o-mini" )
                .modelName(model)
                .supportedCapabilities(Set.of(Capability.RESPONSE_FORMAT_JSON_SCHEMA))
                .logResponses(true)
                .maxRetries(2)
                .temperature(0.0)
                .build() ),
    GITHUB( (model, extra) ->
            OpenAiChatModel.builder()
                    .baseUrl("https://models.github.ai/inference")
                    .apiKey( extraAttribute( extra, "GITHUB_MODELS_TOKEN" ) )
                    //.modelName( "gpt-4o-mini" )
                    .modelName(model)
                    .supportedCapabilities(Set.of(Capability.RESPONSE_FORMAT_JSON_SCHEMA))
                    .logResponses(true)
                    .maxRetries(2)
                    .temperature(0.0)
                    .build() ),
    OLLAMA( (model, extra) ->
            OllamaChatModel.builder()
                //.modelName( "qwen2.5:7b" )
                //.modelName( "qwen3:14b" )
                .modelName( model )
                .baseUrl("http://localhost:11434")
                .supportedCapabilities(Capability.RESPONSE_FORMAT_JSON_SCHEMA)
                .logRequests(true)
                .logResponses(true)
                .maxRetries(2)
                .temperature(0.0)
                .build() ),
    GEMINI( (model, extra) -> {
            throw new UnsupportedOperationException("Gemini model is not supported yet");
        })
    ;

    private final BiFunction<String, Map<String,String>, ChatModel> model;

    private static  String extraAttribute(Map<String,String> extraAttributes, String key  ) {
        if( extraAttributes == null ) extraAttributes = Map.of();
        var result = extraAttributes.getOrDefault(
                requireNonNull(key,"key cannot be null"),
                System.getProperty(key, System.getenv(key)));
        return requireNonNull( result, "Value of attribute '%s' is null".formatted(key) );
    }

    public ChatModel chatModel(String model ) {
        return this.model.apply(model, Map.of());
    }
    public ChatModel chatModel(String model, Map<String,String> extraAttributes ) {
        return this.model.apply(model, extraAttributes);
    }

    AiModel( BiFunction<String, Map<String,String>, ChatModel> model ) {
        this.model = model;
    }
}
