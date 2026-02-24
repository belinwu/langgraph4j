#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.spring.ai.agentexecutor;

import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.*;
import org.bsc.langgraph4j.action.AsyncCommandAction;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.action.Command;
import org.bsc.langgraph4j.action.InterruptionMetadata;
import org.bsc.langgraph4j.agent.AgentEx;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.hook.EdgeHook;
import org.bsc.langgraph4j.hook.NodeHook;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.spring.ai.agentexecutor.gemini.TestTools4Gemini;
import org.bsc.langgraph4j.streaming.StreamingOutput;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.springframework.core.io.ResourceLoader;
/**
 * Demonstrates the use of Spring Boot CLI to execute a task using an agent executor.
 */
@Controller
public class DemoConsoleController implements CommandLineRunner {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DemoConsoleController.class);

    static class WrapCallLogHook<S extends MessagesState<Message>> implements NodeHook.WrapCall<S>, EdgeHook.WrapCall<S> {

        @Override
        public CompletableFuture<Map<String, Object>> applyWrap(String nodeId,
                                                                S state,
                                                                RunnableConfig config,
                                                                AsyncNodeActionWithConfig<S> action) {

            log.info("${symbol_escape}nnode start: '{}' with state: {}", nodeId, state);

            return action.apply( state, config ).whenComplete( (result, ex ) -> {

                if( ex != null ) {
                    return;
                }

                log.info("${symbol_escape}nnode end: '{}' with result: {}", nodeId, result);

            });
        }

        @Override
        public CompletableFuture<Command> applyWrap(String sourceId,
                                                    S state,
                                                    RunnableConfig config,
                                                    AsyncCommandAction<S> action) {
            log.info("${symbol_escape}nedge start from: '{}' with state: {}", sourceId, state);

            return action.apply( state, config ).whenComplete( (result, ex ) -> {

                if( ex != null ) {
                    return;
                }

                log.info("${symbol_escape}nedge end: {}", result);

            });
        }
    }

    private final ChatModel chatModel;
    private final ResourceLoader resourceLoader;

    public DemoConsoleController(ChatModel chatModel, ResourceLoader resourceLoader) {
        this.chatModel = chatModel;
        this.resourceLoader = resourceLoader;
    }

    enum Call {
        runAgentWithInterruption,
        runAgentWithApproval
    }
    /**
     * Executes the command-line interface to demonstrate a Spring Boot application.
     * This method logs a welcome message, constructs a graph using an agent executor,
     * compiles it into a workflow, invokes the workflow with a specific input,
     * and then logs the final result.
     *
     * @param args Command line arguments (Unused in this context)
     * @throws Exception If any error occurs during execution
     */
    @Override
    public void run(String... args) throws Exception {

        log.info("Welcome to the Spring Boot CLI application!");

        switch( Call.runAgentWithApproval ) {
            case runAgentWithInterruption ->{
                var userMessage = """
                    perform test twice with message 'this is a test' and reports their results and also number of current active threads
                    """;
                runAgentWithInterruption(userMessage, false, System.console());
            }
            case runAgentWithApproval -> {
                var userMessageWithApproval = """
                        get number of current active threads and perform test with message 'this is a test'
                        """;
                runAgentWithApproval(userMessageWithApproval, false, System.console());
            }
        }

        /*

        var userMessageWitCancellation = """
                perform test twice with message 'this is a test' and reports their results and also number of current active threads
                """;
        runAgentWithCancellation(userMessageWitCancellation, streaming, console);


        runAgentExWithSkill(  console );

        */

    }

    public void runAgentWithApproval(String userMessage, boolean streaming, java.io.Console console) throws Exception {

        var saver = new MemorySaver();

        var compileConfig = CompileConfig.builder()
                .checkpointSaver(saver)
                .build();

        var agent = AgentExecutorEx.builder()
                .chatModel(chatModel, streaming)
                .toolsFromObject(new TestTools()) // Support without providing tools
                .approvalOn("threadCount", (nodeId, state) ->
                        InterruptionMetadata.builder(nodeId, state)
                                .addMetadata("label", "confirm thread count execution?")
                                .build())
                .build()
                .compile(compileConfig);

        log.info("{}", agent.getGraph(GraphRepresentation.Type.MERMAID, "ReAct Agent", false));

        Map<String, Object> input = Map.of("messages", new UserMessage(userMessage));

        var runnableConfig = RunnableConfig.builder().build();

        while (true) {
            var result = agent.stream(input, runnableConfig);

            var output = result.stream()
                    .peek(s -> {
                        if (s instanceof StreamingOutput<?> out) {
                            System.out.printf("%s: (%s)${symbol_escape}n", out.node(), out.chunk());
                        } else {
                            System.out.println(s.node());
                        }
                    })
                    .reduce((a, b) -> b)
                    .orElseThrow();

            if (output.isEND()) {
                console.format("result: %s${symbol_escape}n", output.state());
                break;

            } else {

                var returnValue = AsyncGenerator.resultValue(result);

                if (returnValue.isPresent()) {

                    log.info("interrupted: {}", returnValue.orElse("NO RESULT FOUND!"));

                    if (returnValue.get() instanceof InterruptionMetadata<?> interruption) {

                        var answer = console.readLine(format("%s : (N${symbol_escape}${symbol_escape}y) ${symbol_escape}t${symbol_escape}n", interruption.metadata("label").orElse("Approve action ?")));

                        if (Objects.equals(answer, "Y") || Objects.equals(answer, "y")) {
                            runnableConfig = agent.updateState(runnableConfig, Map.of(AgentEx.APPROVAL_RESULT_PROPERTY, AgentEx.ApprovalState.APPROVED.name()));
                        } else {
                            runnableConfig = agent.updateState(runnableConfig, Map.of(AgentEx.APPROVAL_RESULT_PROPERTY, AgentEx.ApprovalState.REJECTED.name()));
                        }
                    }
                    input = null;
                }

            }

        }
    }

    public void runAgent(String userMessage, boolean streaming, java.io.Console console) throws Exception {

        var saver = new MemorySaver();

        var compileConfig = CompileConfig.builder()
                .checkpointSaver(saver)
                .build();

        var agentBuilder = AgentExecutor.builder()
                .chatModel(chatModel, streaming);

        // FIX for GEMINI MODEL
        if (chatModel instanceof VertexAiGeminiChatModel) {
            agentBuilder.toolsFromObject(new TestTools4Gemini());
        } else {
            agentBuilder.toolsFromObject(new TestTools());
        }

        var agent = agentBuilder.build().compile(compileConfig);

        log.info("{}", agent.getGraph(GraphRepresentation.Type.MERMAID, "ReAct Agent", false));

        Map<String, Object> input = Map.of("messages", new UserMessage(userMessage));
        var runnableConfig = RunnableConfig.builder().build();

        var result = agent.stream(input, runnableConfig);

        var output = result.stream()
                .peek(System.out::println)
                .reduce((a, b) -> b)
                .orElseThrow();

        console.format("result: %s${symbol_escape}n",
                output.state().lastMessage()
                        .map(AssistantMessage.class::cast)
                        .map(AssistantMessage::getText)
                        .orElseThrow());

    }

    public void runAgentWithInterruption(String userMessage, boolean streaming, java.io.Console console) throws Exception {

        var saver = new MemorySaver();

        var compileConfig = CompileConfig.builder()
                .checkpointSaver(saver)
                .interruptAfter("threadCount")
                .interruptBeforeEdge( true )
                .build();

        var agentBuilder = AgentExecutorEx.builder()
                .chatModel(chatModel, streaming);

        // FIX for GEMINI MODEL
        if (chatModel instanceof VertexAiGeminiChatModel) {
            agentBuilder.toolsFromObject(new TestTools4Gemini());
        } else {
            agentBuilder.toolsFromObject(new TestTools());
        }

        var agent = agentBuilder.build().compile(compileConfig);

        log.info("{}", agent.getGraph(GraphRepresentation.Type.MERMAID, "ReAct Agent", false));

        Map<String, Object> input = Map.of("messages", new UserMessage(userMessage));
        var runnableConfig = RunnableConfig.builder().build();

        var iterator = agent.stream(input, runnableConfig);

        var output = iterator.stream()
                .peek(System.out::println)
                .reduce((a, b) -> b)
                .orElseThrow();

        final var result = GraphResult.from(iterator);

        assertTrue( result.isInterruptionMetadata() );

        final var interruptionMetadata = result.<AgentExecutorEx.State>asInterruptionMetadata();

        final var lastMessage = interruptionMetadata.state().lastMessage();
        assertTrue( lastMessage.isPresent() );
        assertInstanceOf( ToolResponseMessage.class, lastMessage.get() );

        console.format("result: %s${symbol_escape}n", result);

    }

    public void runAgentWithCancellation(String userMessage, boolean streaming, java.io.Console console) throws Exception {

        var saver = new MemorySaver();

        var compileConfig = CompileConfig.builder()
                .checkpointSaver(saver)
                .build();

        var agentBuilder = AgentExecutor.builder()
                .chatModel(chatModel, streaming);

        // FIX for GEMINI MODEL
        if (chatModel instanceof VertexAiGeminiChatModel) {
            agentBuilder.toolsFromObject(new TestTools4Gemini());
        } else {
            agentBuilder.toolsFromObject(new TestTools());
        }

        var agent = agentBuilder.build().compile(compileConfig);

        log.info("{}", agent.getGraph(GraphRepresentation.Type.MERMAID, "ReAct Agent", false));

        Map<String, Object> input = Map.of("messages", new UserMessage(userMessage));

        var runnableConfig = RunnableConfig.builder().build();

        var generator = agent.stream(input, runnableConfig);


        var future = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(100);
                generator.cancel(true);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        var output = generator.stream()
                .peek(s -> {
                    if (s instanceof StreamingOutput<?> out) {
                        System.out.printf("%s: (%s)${symbol_escape}n", out.node(), out.chunk());
                    } else {
                        System.out.println(s.node());
                    }
                })
                .reduce((a, b) -> b)
                .orElseThrow();


        future.get();

        if (!generator.isCancelled()) {
            console.format("generator lastState: %s${symbol_escape}n",
                    output.state().lastMessage()
                            .map(AssistantMessage.class::cast)
                            .map(AssistantMessage::getText)
                            .orElseThrow());
        } else {
            var result = AsyncGenerator.resultValue(generator).orElse("<None>");
            console.format("generator execution has been cancelled on node: '%s' with result: %s${symbol_escape}n", output.node(), result);
        }
    }

    public void runAgentWithSkill( java.io.Console console) throws Exception {

        final var hook = new WrapCallLogHook<AgentExecutor.State>();

        var saver = new MemorySaver();

        var compileConfig = CompileConfig.builder()
                .recursionLimit(10)
                .checkpointSaver(saver)
                .build();

        var agent = AgentExecutor.builder()
                .addCallModelHook( hook )
                .addExecuteToolsHook( hook )
                .chatModel(chatModel, false)
                .defaultSystem("Always use the available skills to assist the user in their requests.")
                .skills(resourceLoader.getResource("classpath:skills"))
                .build()
                .compile(compileConfig);

        log.info("{}", agent.getGraph(GraphRepresentation.Type.MERMAID, "ReAct Agent", false));

        final var userMessage = """
					update changelog in the current folder.
					Use required skills.
					Use absolute paths for the skills and scripts. Do not ask me for more details.
					""";
        var input = GraphInput.args(Map.of("messages", new UserMessage(userMessage)));

        var runnableConfig = RunnableConfig.builder().build();

        var generator = agent.stream(input, runnableConfig);

        var output = generator.stream()
                .peek(s -> {
                    if (s instanceof StreamingOutput<?> out) {
                        System.out.printf("%s: (%s)%n", out.node(), out.chunk());
                    } else {
                        System.out.println(s.node());
                    }
                })
                .reduce((a, b) -> b)
                .orElseThrow();
    }

    public void runAgentExWithSkill( java.io.Console console) throws Exception {

        final var hook = new WrapCallLogHook<AgentExecutorEx.State>();

        var saver = new MemorySaver();

        var compileConfig = CompileConfig.builder()
                .checkpointSaver(saver)
                .build();

        var agent = AgentExecutorEx.builder()
                .addCallModelHook( hook )
                .addApprovalActionHook( hook )
                .addDispatchActionHook( hook )
                .addShouldContinueHook( hook )
                .addDispatchToolsHook( hook )
                .chatModel(chatModel, false)
                .defaultSystem("Always use the available skills to assist the user in their requests.")
                .skills(resourceLoader.getResource("classpath:skills"))
                .build()
                .compile(compileConfig);

        log.info("{}", agent.getGraph(GraphRepresentation.Type.MERMAID, "ReAct Agent", false));

        final var userMessage = """
					update changelog in the current folder.
					Use required skills.
					Use absolute paths for the skills and scripts. Do not ask me for more details.
					""";
        var input = GraphInput.args(Map.of("messages", new UserMessage(userMessage)));

        var runnableConfig = RunnableConfig.builder().build();

        var generator = agent.stream(input, runnableConfig);

        var output = generator.stream()
                .peek(s -> {
                    if (s instanceof StreamingOutput<?> out) {
                        System.out.printf("%s: (%s)%n", out.node(), out.chunk());
                    } else {
                        System.out.println(s.node());
                    }
                })
                .reduce((a, b) -> b)
                .orElseThrow();
    }

}