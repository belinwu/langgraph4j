#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.spring.ai.agent;

import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.spring.ai.generators.StreamingChatGenerator;
import org.springframework.ai.chat.messages.Message;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static java.util.concurrent.CompletableFuture.failedFuture;

public class CallModelAction<State extends MessagesState<Message>> implements AsyncNodeActionWithConfig<State> {

    private final ReactAgent.ChatService chatService;
    private final boolean streaming;

    public CallModelAction(ReactAgent.ChatService chatService, boolean streaming) {
        this.chatService = chatService;
        this.streaming = streaming;
    }

    /**
     * Calls a model with the given workflow state.
     *
     * @param state The current state containing input and intermediate steps.
     * @return A map containing the outcome of the agent call, either an action or a finish.
     */
    @Override
    public CompletableFuture<Map<String, Object>> apply(State state, RunnableConfig config) {

        var messages = state.messages();

        if (messages.isEmpty()) {
            return failedFuture( new IllegalArgumentException("no input provided!") );
        }

        if (streaming && !config.isRunningInStudio() ) {
            var flux = chatService.streamingExecute(messages);

            var generator = StreamingChatGenerator.builder()
                    .startingNode("agent")
                    .startingState(state)
                    .mapResult(response -> Map.of("messages", response.getResult().getOutput()))
                    .build(flux);

            return completedFuture(Map.of("messages", generator));
        } else {
            var response = chatService.execute(messages);

            var output = response.getResult().getOutput();

            return completedFuture(Map.of("messages", output));
        }

    }

}
