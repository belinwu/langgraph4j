package org.bsc.langgraph4j.serializer.jackson;

import org.bsc.langgraph4j.serializer.plain_text.jackson.JacksonStateSerializer;
import org.bsc.langgraph4j.state.AgentState;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Issue181Test {

    String generateLongString( String pattern ) {
        return pattern.repeat(70_000 );
    }

    @Test
    public void testSerializeLongString() throws IOException, ClassNotFoundException {

        Map<String,Object> largeMap = Map.of(
                "largeString1", generateLongString( "A"),
                "largeString2", generateLongString( "B"),
                "largeString3", generateLongString( "C" )
        );

        var ser = new JacksonStateSerializer<>( AgentState::new ) {

        };

        var cloned = ser.cloneObject( largeMap );

        assertNotNull( cloned );
    }
}