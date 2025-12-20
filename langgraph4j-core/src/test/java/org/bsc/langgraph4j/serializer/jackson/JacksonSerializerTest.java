package org.bsc.langgraph4j.serializer.jackson;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.serializer.plain_text.jackson.JacksonStateSerializer;
import org.bsc.langgraph4j.serializer.plain_text.jackson.TypeMapper;
import org.bsc.langgraph4j.state.AgentState;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JacksonSerializerTest {

    static class State extends AgentState {

        /**
         * needed for Jackson deserialization unless use a custom deserializer
         */
        protected State() {
            super( Map.of() );
        }

        /**
         * Constructs an AgentState with the given initial data.
         *
         * @param initData the initial data for the agent state
         */
        public State(Map<String, Object> initData) {
            super(initData);
        }
    }

    static class MyStateSerializer extends JacksonStateSerializer<State> {
        public MyStateSerializer() {
            super(State::new);
        }

    }
    @Test
    public void serializeWithTypeInferenceTest() throws IOException, ClassNotFoundException {

        State state = new State( Map.of( "prop1", "value1") );

        var serializer = new MyStateSerializer();

        var type = serializer.getStateType();

        assertTrue(type.isPresent());
        assertEquals(State.class, type.get());

        byte[] bytes = serializer.objectToBytes(state);

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        AgentState deserializedState = serializer.bytesToObject(bytes);

        assertNotNull(deserializedState);
        assertEquals( 1, deserializedState.data().size() );
        assertEquals( "value1", deserializedState.data().get("prop1") );
    }

    static class MyJacksonStateSerializer extends JacksonStateSerializer<AgentState> {

        public MyJacksonStateSerializer() {
            super(AgentState::new);
        }
    }

    @Test
    public void NodOutputJacksonSerializationTest() throws Exception {

        var serializer = new MyJacksonStateSerializer();

        NodeOutput<AgentState> output = new NodeOutput<>("node", null);
        var mapper = serializer.objectMapper()
                            .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        var json = mapper.writeValueAsString(output);
        assertEquals("""
                {"end":false,"node":"node","start":false,"state":null}""", json );

        output = new NodeOutput<>("node", null);
        json = serializer.objectMapper().writeValueAsString(output);

        assertEquals( """
                {"end":false,"node":"node","start":false,"state":null}""", json );
    }

    @Test
    public void TypeMapperTest() throws Exception {

        var mapper = new TypeMapper();

        var tr = new TypeReference<State>() {};
        System.out.println(tr.getType());
        mapper.register( new TypeMapper.Reference<State>("MyState") { } );

        var ref = mapper.getReference("MyState");

        assertTrue( ref.isPresent() );
        assertEquals( "MyState", ref.get().getTypeName() );
        System.out.println( ref.get().getType() );
        assertEquals( State.class, ref.get().getType() );
    }

    record Person ( String name, int age ){}

    @Test
    public void valueFromNodeTest() throws Exception {

        var serializer = new MyJacksonStateSerializer();

        var data = Map.of(
                "integer", 10,
                "string", "value",
                "boolean", true,
                "long", 10_000_000_000_000L,
                "double", 10_000.34567,
                "big_decimal", new BigDecimal(123412345678901L),
                "person", new Person("John", 30));

        var state = serializer.stateFactory().apply( data );

        var bytes = serializer.objectToBytes( state );

        var clonedState = serializer.bytesToObject( bytes );

        var clonedData = clonedState.data();

        assertEquals( data.size(), clonedData.size() );
        assertEquals( data.get("integer"), clonedData.get("integer") );
        assertEquals( data.get("string"), clonedData.get("string") );
        assertEquals( data.get("boolean"), clonedData.get("boolean") );
        assertEquals( data.get("long"), clonedData.get("long") );
        assertEquals( data.get("double"), clonedData.get("double") );
        assertInstanceOf( Number.class, data.get("big_decimal"));
        assertInstanceOf( Number.class, clonedData.get("big_decimal"));
        assertEquals( Objects.toString(data.get("big_decimal")), Objects.toString(clonedData.get("big_decimal")) );
        assertInstanceOf( Map.class, clonedData.get("person") );
        @SuppressWarnings("unchecked")
        final Map<String,Object> personMap = (Map<String, Object>) clonedData.get("person");
        assertInstanceOf( Person.class, data.get("person") );
        final Person person = (Person) data.get("person");
        assertEquals( person.name(), personMap.get("name"));
        assertEquals( person.age(), personMap.get("age"));




    }
}
