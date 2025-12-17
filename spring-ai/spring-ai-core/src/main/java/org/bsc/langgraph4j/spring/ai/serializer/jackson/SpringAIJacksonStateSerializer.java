package org.bsc.langgraph4j.spring.ai.serializer.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bsc.langgraph4j.serializer.plain_text.jackson.JacksonStateSerializer;
import org.bsc.langgraph4j.serializer.plain_text.jackson.TypeMapper;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.AgentStateFactory;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.content.Media;

public class SpringAIJacksonStateSerializer<State extends AgentState>  extends JacksonStateSerializer<State> {

    interface ChatMessageDeserializer {
        SystemMessageHandler.Deserializer system = new SystemMessageHandler.Deserializer();
        UserMessageHandler.Deserializer user = new UserMessageHandler.Deserializer();
        AssistantMessageHandler.Deserializer ai = new AssistantMessageHandler.Deserializer();
        ToolResponseMessageHandler.Deserializer tool = new ToolResponseMessageHandler.Deserializer();

        static void registerTo( SimpleModule module ) {
            module
                    .addDeserializer(ToolResponseMessage.class, tool)
                    .addDeserializer(SystemMessage.class, system )
                    .addDeserializer(UserMessage.class, user )
                    .addDeserializer(AssistantMessage.class, ai )
            ;
        }

    }

    interface ChatMessageSerializer  {
        SystemMessageHandler.Serializer system = new SystemMessageHandler.Serializer();
        UserMessageHandler.Serializer user = new UserMessageHandler.Serializer();
        AssistantMessageHandler.Serializer ai = new AssistantMessageHandler.Serializer();
        ToolResponseMessageHandler.Serializer tool = new ToolResponseMessageHandler.Serializer();

        static void registerTo( SimpleModule module ) {
            module
                    .addSerializer(ToolResponseMessage.class, tool)
                    .addSerializer(SystemMessage.class, system)
                    .addSerializer(UserMessage.class, user)
                    .addSerializer(AssistantMessage.class, ai)
            ;

        }

    }

    public SpringAIJacksonStateSerializer(AgentStateFactory<State> stateFactory) {
        super(stateFactory);

        var module = new SimpleModule();

        ChatMessageSerializer.registerTo(module);
        ChatMessageDeserializer.registerTo(module);

        typeMapper
                .register(new TypeMapper.Reference<ToolResponseMessage>(MessageType.TOOL.name()) {} )
                .register(new TypeMapper.Reference<SystemMessage>(MessageType.SYSTEM.name()) {} )
                .register(new TypeMapper.Reference<UserMessage>(MessageType.USER.name()) {} )
                .register(new TypeMapper.Reference<AssistantMessage>(MessageType.ASSISTANT.name()) {} )
                .register(new TypeMapper.Reference<Media>( Media.class.getName() ) {})
        ;

        module.addSerializer( Media.class, new MediaHandler.Serializer() );
        module.addDeserializer( Media.class, new MediaHandler.Deserializer() );

        objectMapper.registerModule( module );
    }
}
