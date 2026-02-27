package org.bsc.langgraph4j.hook;

import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.*;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.bsc.langgraph4j.streaming.StreamingOutput;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for Issue #336: AfterHook should be called after streaming completes
 *
 * Problem: When a Node returns an AsyncGenerator (streaming), the AfterHook.applyAfter()
 * was called immediately when the generator was created, before any streaming had occurred.
 *
 * Solution: Skip AfterHook in applyActionWithHooks when AsyncGenerator is detected,
 * and call AfterHook in embedGenerator callback after streaming completes.
 */
public class Issue336Test implements LG4JLoggable {

    static class State extends AgentState {

        public State(Map<String, Object> initData) {
            super(initData);
        }

        List<String> values() {
            return this.<List<String>>value("VALUE").orElseThrow();
        }
    }

    /**
     * A hook that records when it was called and what result it received.
     */
    static class AuditHook<State extends AgentState> implements NodeHook.AfterCall<State> {

        final AtomicReference<String> calledAtNode = new AtomicReference<>();
        final AtomicReference<Map<String, Object>> capturedResult = new AtomicReference<>();
        final AtomicInteger callCount = new AtomicInteger(0);

        @Override
        public CompletableFuture<Map<String, Object>> applyAfter(
                String nodeId,
                State state,
                RunnableConfig config,
                Map<String, Object> result) {

            calledAtNode.set(nodeId);
            capturedResult.set(result);
            callCount.incrementAndGet();

            return completedFuture(result);
        }

    }

    /**
     * Creates a streaming node using AsyncGeneratorQueue.Generator.
     * Simulates LLM streaming by pushing tokens to a queue.
     */
    private AsyncNodeActionWithConfig<State> createStreamingNode(String nodeId, List<String> tokens) {
        return (state, config) -> {

            final var iterator = tokens.iterator();
            final var generator = new AsyncGenerator.Base<StreamingOutput<State>>() {
                @Override
                public Data<StreamingOutput<State>> next() {
                    if (iterator.hasNext()) {
                        return AsyncGenerator.Data.of(new StreamingOutput<>(iterator.next(), nodeId, state));
                    }
                    return AsyncGenerator.Data.done(Map.of("VALUE", "streaming_completed"));
                }

            };
            return completedFuture(Map.of("content", generator));
        };
    }

    /**
     * Creates a blocking node that returns immediately.
     */
    private AsyncNodeActionWithConfig<State> createBlockingNode( String value ) {
        return (state, config) -> completedFuture(Map.of("VALUE", value));
    }

    @Test
    public void testBlockingNodeAfterHookCalledImmediately() throws Exception {
        // Given
        Map<String, Channel<?>> schema = Map.of("VALUE", Channels.appender(ArrayList::new));
        var auditHook = new AuditHook<State>();

        var workflow = new StateGraph<State>(schema, State::new)
                .addAfterCallNodeHook(auditHook)
                .addNode("blocking_node", createBlockingNode( "test_value"))
                .addEdge(START, "blocking_node")
                .addEdge("blocking_node", END)
                .compile();

        // When
        var result = workflow.invoke(Map.of());

        // Then
        assertTrue(result.isPresent());
        assertEquals(1, auditHook.callCount.get());
        assertEquals("blocking_node", auditHook.calledAtNode.get());
        assertNotNull(auditHook.capturedResult.get());
        // The VALUE in the hook result is the raw value before channel merging
        assertEquals("test_value", auditHook.capturedResult.get().get("VALUE"));
    }

    @Test
    public void testStreamingNodeAfterHookCalledAfterCompletion() throws Exception {
        // Given
        Map<String, Channel<?>> schema = Map.of("VALUE", Channels.appender(ArrayList::new));
        var auditHook = new AuditHook<State>();

        var workflow = new StateGraph<>(schema, State::new)
                .addAfterCallNodeHook(auditHook)
                .addNode("streaming_node", createStreamingNode("streaming_node", List.of("Hello", " ", "World")))
                .addEdge(START, "streaming_node")
                .addEdge("streaming_node", END)
                .compile();

        // When - consume the streaming generator
        var stream = workflow.stream(Map.of(), RunnableConfig.builder().build());

        // Consume all streaming output
        List<String> streamingChunks = new ArrayList<>();
        for (var output : stream) {
            if (output instanceof StreamingOutput<?> streamingOutput) {
                log.info("Streaming chunk output {}", streamingOutput.chunk());
                streamingChunks.add((String) streamingOutput.chunk());
            }
        }

        // Then - verify streaming completed and AfterHook was called with final result
        assertEquals(List.of("Hello", " ", "World"), streamingChunks);
        assertEquals(1, auditHook.callCount.get());
        assertEquals("streaming_node", auditHook.calledAtNode.get());
        assertNotNull(auditHook.capturedResult.get());

        // CRITICAL: Verify AfterHook received the final result, NOT the AsyncGenerator
        // In the old buggy code, AfterHook would receive Map.of("content", AsyncGenerator)
        // In the fixed code, AfterHook receives Map.of("VALUE", "streaming_completed")
        Object capturedValue = auditHook.capturedResult.get().get("VALUE");
        assertNotNull(capturedValue, "AfterHook should receive the final result with VALUE key");

        // Make sure we didn't receive the generator itself
        Object contentValue = auditHook.capturedResult.get().get("content");
        assertFalse(contentValue instanceof AsyncGenerator, "BUG: AfterHook received AsyncGenerator instead of final result! This means the fix is not working or not applied.");
        assertEquals("streaming_completed", capturedValue);
    }

    @Test
    public void testMixedBlockingAndStreamingNodes() throws Exception {
        // Given
        Map<String, Channel<?>> schema = Map.of("VALUE", Channels.appender(ArrayList::new));
        var auditHook = new AuditHook<State>();

        var workflow = new StateGraph<State>(schema, State::new)
                .addAfterCallNodeHook(auditHook)
                .addNode("blocking_node", createBlockingNode("blocking_result"))
                .addNode("streaming_node", createStreamingNode("streaming_node", List.of("A", "B", "C")))
                .addEdge(START, "blocking_node")
                .addEdge("blocking_node", "streaming_node")
                .addEdge("streaming_node", END)
                .compile();

        // When
        var stream = workflow.stream(Map.of(), RunnableConfig.builder().build());

        List<String> streamingChunks = new ArrayList<>();
        String lastNode = null;
        for (var output : stream) {
            if (output instanceof StreamingOutput<?> streamingOutput) {
                log.info("Streaming chunk output {}", streamingOutput.chunk());
                streamingChunks.add((String) streamingOutput.chunk());
            }
            log.info("Node ID {}", output.node());
            lastNode = output.node();
        }

        // Then
        assertEquals(END, lastNode);
        assertEquals(List.of("A", "B", "C"), streamingChunks);

        // Both hooks should have been called
        assertEquals(2, auditHook.callCount.get());

        // CRITICAL: The last call should be for streaming_node with the final result
        // (not the AsyncGenerator)
        assertEquals("streaming_node", auditHook.calledAtNode.get());
        Object capturedValue = auditHook.capturedResult.get().get("VALUE");
        assertNotNull(capturedValue, "Streaming node AfterHook should receive final result with VALUE key, not AsyncGenerator");
        assertEquals("streaming_completed", capturedValue);
    }


}
