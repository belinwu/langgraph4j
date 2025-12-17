package org.bsc.langgraph4j.spring.ai.serializer.jackson;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeType;

import java.io.IOException;

public interface MediaHandler {

    class Serializer extends StdSerializer<Media> {

        protected Serializer() {
            super(Media.class);
        }

        @Override
        public void serialize(Media media, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {

            gen.writeStartObject();
            gen.writeStringField("@type", Media.class.getName()); // nullable
            gen.writeStringField("id", media.getId()); // nullable
            gen.writeStringField("name", media.getName()); // nullable
            gen.writeStringField("mimetype", "%s/%s".formatted( media.getMimeType().getType(),
                                                                            media.getMimeType().getSubtype()));
            gen.writeBinaryField( "data", media.getDataAsByteArray());
            gen.writeEndObject();
        }
    }

    class Deserializer extends StdDeserializer<Media> {

        protected Deserializer() {
            super(Media.class);
        }

        @Override
        public Media deserialize(JsonParser p, DeserializationContext deserializationContext) throws IOException, JacksonException {
            final JsonNode node = p.getCodec().readTree(p);

            var idNode = node.get("id");
            var nameNode = node.get("name");
            var mimeType = node.get("mimetype").asText();
            var data = node.get("data").binaryValue();

            return Media.builder()
                    .id(idNode.isNull() ? null : idNode.asText())
                    .name( nameNode.isNull() ? null : nameNode.asText() )
                    .mimeType(MimeType.valueOf(mimeType))
                    .data( data )
                    .build();
        }
    }

}
