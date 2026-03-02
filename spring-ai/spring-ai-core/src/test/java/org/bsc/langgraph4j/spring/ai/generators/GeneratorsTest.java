package org.bsc.langgraph4j.spring.ai.generators;

import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.streaming.StreamingOutput;
import org.bsc.langgraph4j.streaming.StreamingOutputEnd;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.fail;


public class GeneratorsTest {

    record Message( List<String> elements ) {

        public Message( String arg ) {
            this( List.of(arg) );
        }
    }

    @Test
    public void issue284Test() {

        final var elements = List.of( new Message("a"), new Message("b"), new Message("c"));

        //////////////////////
        // ORIGINAL SOLUTION
        //////////////////////
        Flux.fromIterable( elements )
                .scan( (lastMessage, newMessage) -> {

                    var mergeStream = Stream.concat(
                            lastMessage.elements().stream(),
                            newMessage.elements().stream());

                    return new Message(mergeStream.toList());
                })
                .map(next ->
                        String.join(" - ", next.elements())
                )
                .doOnNext( e -> System.out.printf( "step: %s%n", e) )
                .last()
                .doOnSuccess( e -> System.out.printf( "result: %s%n", e)  )
                .doOnError(Assertions::fail)
                .subscribe( ) ;

        //////////////////////
        // PR#285 SOLUTION
        //////////////////////
        Flux.fromIterable( elements )
                .handle(new BiConsumer<Message, SynchronousSink<Message>>() {
                    Message last = null;

                    @Override
                    public void accept(Message current, SynchronousSink<Message> sink) {
                        last = ( last == null ) ?
                                current :
                                new Message(Stream.concat(
                                    last.elements().stream(),
                                    current.elements().stream()).toList());

                        sink.next(last);

                    }
                })
                .map(next -> String.join(" - ", next.elements()))
                .doOnNext( e -> System.out.printf( "step: %s%n", e) )
                .last()
                .doOnSuccess( e -> System.out.printf( "result: %s%n", e)  )
                .doOnError(Assertions::fail)
                .subscribe() ;

    }
}
