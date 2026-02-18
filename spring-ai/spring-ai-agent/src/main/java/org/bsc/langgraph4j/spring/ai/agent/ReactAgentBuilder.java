package org.bsc.langgraph4j.spring.ai.agent;

import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.serializer.StateSerializer;
import org.bsc.langgraph4j.state.Channel;
import org.springaicommunity.agent.tools.FileSystemTools;
import org.springaicommunity.agent.tools.ShellTools;
import org.springaicommunity.agent.tools.SkillsTool;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.core.io.Resource;

import java.util.*;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

public abstract class ReactAgentBuilder<B extends ReactAgentBuilder<B,State>, State extends MessagesState<Message>> {

    protected StateSerializer<State> stateSerializer;
    protected ChatModel chatModel;
    protected String systemMessage;
    protected boolean streaming = false;
    protected final Set<ToolCallback> tools = new HashSet<>();
    private SkillsTool.Builder skillsBuilder;
    protected Map<String, Channel<?>> schema = MessagesState.SCHEMA;

    public Optional<String> systemMessage() {
        return ofNullable(systemMessage);
    }

    public List<ToolCallback> tools() {
        return tools.stream().toList();
    }

    @SuppressWarnings("unchecked")
    protected B result() {
        return (B)this;
    }



    public B schema(Map<String, Channel<?>> schema) {
        this.schema = schema;
        return result();
    }

    /**
     * Sets the state serializer for the graph builder.
     *
     * @param stateSerializer the state serializer to set
     * @return the current instance of GraphBuilder for method chaining
     */
    public B stateSerializer(StateSerializer<State> stateSerializer) {
        this.stateSerializer = stateSerializer;
        return result();
    }

    public B chatModel(ChatModel chatModel, boolean streaming ) {
        this.chatModel = chatModel;
        this.streaming = streaming;
        return result();
    }

    public B chatModel(ChatModel chatModel ) {
        return chatModel( chatModel, false );
    }

    public B defaultSystem(String systemMessage) {
        this.systemMessage = systemMessage;
        return result();
    }

    public B tool(ToolCallback tool) {
        this.tools.add(requireNonNull(tool, "tool cannot be null!"));
        return result();
    }

    public B tools(List<ToolCallback> tools) {
        this.tools.addAll(requireNonNull(tools, "tools cannot be null!"));
        return result();
    }

    public B tools(ToolCallbackProvider toolCallbackProvider) {
        requireNonNull(toolCallbackProvider, "toolCallbackProvider cannot be null!");
        var toolCallbacks = toolCallbackProvider.getToolCallbacks();
        if (toolCallbacks.length == 0) {
            throw new IllegalArgumentException("toolCallbackProvider.getToolCallbacks() cannot be empty!");
        }
        this.tools.addAll(List.of(toolCallbacks));
        return result();
    }

    public B toolsFromObject(Object objectWithTools) {
        var tools = ToolCallbacks.from(requireNonNull(objectWithTools, "objectWithTools cannot be null"));
        this.tools.addAll(List.of(tools));
        return result();
    }

    public B skills( String skillDirectory ) {
        if( skillsBuilder == null ) {
            skillsBuilder = SkillsTool.builder();
        }
        skillsBuilder.addSkillsDirectory( requireNonNull(skillDirectory, "skillDirectory cannot be null!"));
        return result();
    }

    public B skills( Resource skillsRootPath ) {
        if( skillsBuilder == null ) {
            skillsBuilder = SkillsTool.builder();
        }
        skillsBuilder.addSkillsResource( requireNonNull(skillsRootPath, "skillDirectory cannot be null!"));
        return result();
    }

    public abstract StateGraph<State> build(Function<ReactAgentBuilder<?,?>, ReactAgent.ChatService> chatServiceFactory ) throws GraphStateException;

    public final StateGraph<State> build() throws GraphStateException {
        // Apply skills
        if( skillsBuilder != null ) {
            this.tools.add( skillsBuilder.build() );
            this.tools.addAll(List.of(ToolCallbacks.from(FileSystemTools.builder().build())));
            this.tools.addAll(List.of(ToolCallbacks.from(ShellTools.builder().build())));
        }

        return build(DefaultChatService::new);
    }

}

