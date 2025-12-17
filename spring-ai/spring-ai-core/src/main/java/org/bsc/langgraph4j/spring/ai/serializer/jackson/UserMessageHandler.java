package org.bsc.langgraph4j.spring.ai.serializer.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;

import java.io.IOException;
import java.util.ArrayList;

import static org.bsc.langgraph4j.spring.ai.serializer.jackson.SerializationHelper.deserializeMetadata;
import static org.bsc.langgraph4j.spring.ai.serializer.jackson.SerializationHelper.serializeMetadata;

public interface UserMessageHandler {
    enum Field {
        TEXT("text"),
        MEDIA("media");

        final String name;

        Field(String name ) {
            this.name = name;
        }
    }

    class Serializer extends StdSerializer<UserMessage> {

        public Serializer() {
            super(UserMessage.class);
        }

        @Override
        public void serialize(UserMessage msg, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("@type", msg.getMessageType().name());
            gen.writeStringField( Field.TEXT.name, msg.getText() );
            var media = msg.getMedia();
            if( !media.isEmpty()) {
                gen.writeArrayFieldStart( Field.MEDIA.name );
                for( var m : media ) {
                    gen.writeObject( m );
                }
                gen.writeEndArray();
            }
            serializeMetadata( gen, msg.getMetadata() );
            gen.writeEndObject();
        }
    }

    class Deserializer extends StdDeserializer<UserMessage> {

        public Deserializer() {
            super(UserMessage.class);
        }

        @Override
        public UserMessage deserialize(JsonParser jsonParser, DeserializationContext ctx) throws IOException {
            var mapper = (ObjectMapper) jsonParser.getCodec();
            ObjectNode node = mapper.readTree(jsonParser);

            var textNode = node.get( Field.TEXT.name) ;
            var resultBuilder = UserMessage.builder()
                    .text( textNode.asText() )
                    .metadata( deserializeMetadata( mapper, node ) );
                    ;

            var mediaNode = node.findValue( Field.MEDIA.name );
            if( mediaNode != null && mediaNode.isArray() && !mediaNode.isEmpty()  ) {
                var mediaList = new ArrayList<Media>(mediaNode.size());

                for( var mediaNodeItem : mediaNode ) {
                    mediaList.add( mapper.treeToValue( mediaNodeItem, Media.class ) );
                }
                resultBuilder.media( mediaList );

            }

            return resultBuilder.build();

        }

    }

}
