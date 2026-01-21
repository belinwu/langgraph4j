package org.bsc.langgraph4j.langchain4j.serializer.std;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import org.bsc.langgraph4j.serializer.Serializer;
import org.bsc.langgraph4j.serializer.std.NullableObjectSerializer;


/**
 * The AiMessageSerializer class implements the Serializer interface for the AiMessage type.
 * It provides methods to serialize and deserialize AiMessage objects.
 */
public class AiMessageSerializer implements NullableObjectSerializer<AiMessage> {
    
    /**
     * Serializes the given AiMessage object to the specified output stream.
     *
     * @param object the AiMessage object to serialize
     * @param out the output stream to write the serialized object to
     * @throws IOException if an I/O error occurs during serialization
     */
    @Override
    public void write(AiMessage object, ObjectOutput out) throws IOException {
        boolean hasToolExecutionRequests = object.hasToolExecutionRequests();

        writeNullableUTF( object.thinking(), out );
        out.writeBoolean( hasToolExecutionRequests );

        if( hasToolExecutionRequests ) {
            out.writeObject( object.toolExecutionRequests() );

        }
        else {
            Serializer.writeUTF(object.text(), out);
        }

    }

    /**
     * Deserializes an AiMessage object from the specified input stream.
     *
     * @param in the input stream to read the serialized object from
     * @return the deserialized AiMessage object
     * @throws IOException if an I/O error occurs during deserialization
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     */
    @Override
    @SuppressWarnings("unchecked")
    public AiMessage read(ObjectInput in) throws IOException, ClassNotFoundException {
        final var builder = AiMessage.builder();

        readNullableUTF(in).ifPresent( builder::thinking );

        boolean hasToolExecutionRequests = in.readBoolean();
        if( hasToolExecutionRequests ) {
            List<ToolExecutionRequest> toolExecutionRequests = (List<ToolExecutionRequest>)in.readObject();
            builder.toolExecutionRequests( toolExecutionRequests );
        }
        else {
            builder.text( Serializer.readUTF(in) );
        }

        return builder.build();
    }
}
