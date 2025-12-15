package org.bsc.langgraph4j.langchain4j.serializer.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;

import java.io.IOException;

public class ContentSerializer extends StdSerializer<Content> {
    protected ContentSerializer() {
        super(Content.class);
    }

    @Override
    public void serialize(Content content, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {

        gen.writeStartObject();
        gen.writeStringField("@type", content.type().name());

        if( content instanceof TextContent textContent ) {
            gen.writeStringField("text", textContent.text());
        }
        else  if( content instanceof ImageContent imageContent ) {
            if( imageContent.image().url()==null ) {
                gen.writeNullField("url");
            }
            else {
                gen.writeStringField("url", imageContent.image().url().toString());
            }
            gen.writeStringField("mimeType", imageContent.image().mimeType());
            gen.writeStringField("base64data", imageContent.image().base64Data());
            gen.writeStringField("detailLevel", imageContent.detailLevel().name());
        }
        else {
            throw new UnsupportedOperationException("unsupported content type: %s".formatted(content.type().name()));
        }

        gen.writeEndObject();
    }
}
