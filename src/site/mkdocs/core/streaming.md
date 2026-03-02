# Streaming

LangGraph4j is built with first class support for streaming. it uses [java-async-generator] library to help with this. Below there are the different ways to stream back outputs from a graph run

## Streaming graph outputs (_.stream()_)

`.stream()` is an method for streaming back outputs from a graph run. It returns an [AsyncGenerator] on which you must iterate to fetch  the sequence of performed steps as instance of a [NodeOutput] class that bascally report the executed **node name** and the resulted **state**.

### Streaming of Streaming (embed and composition)

[AsyncGenerator] supports embed (i.e. is composable), it can pause main iteration to perform a nested [AsyncGenerator] after that it resume main iteration.
Relies on this feature we can return from Node action an [AsyncGenerator] that will be embed in main one  of the graph, which result will be fetched from the same iterator given from `.stream()` making sub-streaming a seamlessy experience.

## LangChain4j - How to stream LLM chunks 

To achieve streaming LLM tokens from an AI call using [Langchain4j] we use [StreamingChatLanguageModel], below an example:

```java
StreamingChatLanguageModel model = OpenAiStreamingChatModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName(GPT_4_O_MINI)
    .build();

model.generate("Tell me a joke";, new StreamingResponseHandler<AiMessage>() {
        public void onNext(String token) { ... }

        public void onComplete(Response<T> response) { ... }

        public void onError(Throwable error) { ... }
}  );

```

### StreamingChatGenerator

**LangGraph4j** provides an utility class  [StreamingChatGenerator] that convert the [StreamingResponseHandler] in an [AsyncGenerator]. Below a code snippet, a working example is in the notebook [llm-streaming])

```java
var generator = StreamingChatGenerator.builder()
                        .mapResult( r -> Map.of( "content", r.content() ) )
                        .build();

StreamingChatLanguageModel model = OpenAiStreamingChatModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName(GPT_4_O_MINI)
    .build();

model.generate("Tell me a joke", generator.handler() );

for( var step : generator ) {
    log.info( "{}", step);
}
  
log.info( "RESULT: {}", GraphResult.from(generator) );
```

When we build [StreamingChatGenerator] we must provide a mapping function `Function<CompletionResult, Map<String,Object>>`  that will be invoked on stream completion to convert completion result in a `Map` that represent a **Partial state result** that is what **Langgrap4j** expects  as result.

### Put all together in Node Action

Now we are ready to implement a **Langgraph4j Node Action**, below a represenattiove code snippet, for a complete implementation take a look to [AgentExecutor] sample. 

```java
Map<String,Object> callAgent( State state )  {

    // Mapping function
    final Function<Response<AiMessage>, Map<String,Object>> mapResult = response -> {

        if (response.finishReason() == FinishReason.TOOL_EXECUTION) {

            var toolExecutionRequests = response.content().toolExecutionRequests();
            var action = new AgentAction(toolExecutionRequests.get(0), "");

            return Map.of("agent_outcome", new AgentOutcome(action, null));

        } else {
            var result = response.content().text();
            var finish = new AgentFinish(Map.of("returnValues", result), result);

            return Map.of("agent_outcome", new AgentOutcome(null, finish));
        }
    };

    var generator = StreamingChatGenerator.<AiMessage, State>builder()
            .mapResult(mapResult)
            .startingNode("agent") // optional: the node that require streaming 
            .startingState( state ) // optional: the state of node before streaming 
            .build();

    // call LLM in streaming mode
    streamingChatLanguageModel.generate( messages, tools, generator.handler() );        

    // return the "embed" generator
    return Map.of( "agent_outcome", generator);

}
```

## Spring AI - How to stream LLM chunks 

To achieve streaming LLM tokens from an AI call using [SpringAI] we can use [ChatClient] in streaming mode, below an example:

```java
var chatClient = ChatClient.builder(chatModel)
        .defaultSystem("You are a helpful AI Assistant answering questions.")
        .build();

Flux<ChatResponse> flux = chatClient.prompt()
        .messages(new UserMessage("tell me a joke"))
        .stream()
        .chatResponse();
```

### StreamingChatGenerator

**LangGraph4j** provides an utility class [SpringStreamingChatGenerator] that converts a `Flux<ChatResponse>` into an [AsyncGenerator]. Below a code snippet, a working example is in the notebook [llm-streaming-springai].

```java
var generator = StreamingChatGenerator.builder()
        .startingNode("agent")
        .mapResult(response -> Map.of("messages", response.getResult().getOutput()))
        .build(flux);

for (var step : generator) {
    log.info("{}", step);
}
```

When we build [SpringStreamingChatGenerator] we must provide a mapping function `Function<ChatResponse, Map<String,Object>>` that will be invoked on stream completion to convert completion result in a `Map` that represent a **Partial state result** that is what **LangGraph4j** expects as result.

### Put all together in Node Action

Now we are ready to implement a **LangGraph4j Node Action**. Below a representative code snippet based on the same approach used in the notebook [llm-streaming-springai]:

```java
Map<String, Object> callAgent(State state) {
    Flux<ChatResponse> flux = chatClient.prompt()
            .messages(state.messages())
            .stream()
            .chatResponse();

    var generator = StreamingChatGenerator.<State>builder()
            .startingNode("agent")
            .startingState(state)
            .mapResult(response -> Map.of("messages", response.getResult().getOutput()))
            .build(flux);

    return Map.of("messages", generator);
}
```


[java-async-generator]: https://github.com/bsorrentino/java-async-generator
[AsyncGenerator]: https://bsorrentino.github.io/java-async-generator/apidocs/org/bsc/async/AsyncGenerator.html
[Langchain4j]: https://github.com/langchain4j/langchain4j
[SpringAI]: https://github.com/spring-projects/spring-ai
[StreamingChatLanguageModel]: https://docs.langchain4j.dev/apidocs/dev/langchain4j/model/chat/StreamingChatLanguageModel.html
[StreamingResponseHandler]: https://docs.langchain4j.dev/apidocs/dev/langchain4j/model/StreamingResponseHandler.html
[ChatClient]: https://docs.spring.io/spring-ai/docs/current/api/org/springframework/ai/chat/client/ChatClient.html
[SpringStreamingChatGenerator]: /langgraph4j/apidocs/org/bsc/langgraph4j/spring/ai/generators/StreamingChatGenerator.html
[StreamingChatGenerator]: /langgraph4j/apidocs/org/bsc/langgraph4j/langchain4j/generators/StreamingChatGenerator.html
[llm-streaming]: ../how-tos/llm-streaming.ipynb
[llm-streaming-springai]: ../how-tos/llm-streaming-springai.ipynb
[AgentExecutor]: https://github.com/bsorrentino/langgraph4j/tree/main/langchain4j/langchain4j-agent