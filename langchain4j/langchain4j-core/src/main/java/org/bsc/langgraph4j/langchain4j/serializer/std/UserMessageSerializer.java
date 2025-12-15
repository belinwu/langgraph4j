package org.bsc.langgraph4j.langchain4j.serializer.std;

import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.UserMessage;
import org.bsc.langgraph4j.serializer.Serializer;
import org.bsc.langgraph4j.serializer.std.NullableObjectSerializer;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

/**
 * The UserMessageSerializer class implements the NullableObjectSerializer interface for the UserMessage type.
 * It provides methods to serialize and deserialize UserMessage objects.
 */
public class UserMessageSerializer implements NullableObjectSerializer<UserMessage> {

    /**
     * Serializes the given UserMessage object to the specified ObjectOutput.
     *
     * @param object the UserMessage object to serialize
     * @param out the ObjectOutput to write the serialized data to
     * @throws IOException if an I/O error occurs during serialization
     * @throws IllegalArgumentException if the content type of the UserMessage is unsupported
     */
    @Override
    public void write(UserMessage object, ObjectOutput out) throws IOException {

        if( object.hasSingleText() ) {
            Serializer.writeUTF( object.singleText(), out );
        }
        else {
            out.writeObject( object.contents() );
        }
        writeNullableUTF( object.name(), out);
    }

    /**
     * Deserializes a UserMessage object from the specified ObjectInput.
     *
     * @param in the ObjectInput to read the serialized data from
     * @return the deserialized UserMessage object
     * @throws IOException if an I/O error occurs during deserialization
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     */
    @Override
    public UserMessage read(ObjectInput in) throws IOException, ClassNotFoundException {

        try {
            var text = Serializer.readUTF(in);
            return readNullableUTF(in)
                    .map(name -> UserMessage.from(name, text))
                    .orElseGet(() -> UserMessage.from(text));
        }
        catch( EOFException ex ) {
            // This exception is managed to keep backward compatibility

            @SuppressWarnings("unchecked")
            var contents = (List<Content>)in.readObject();
            return readNullableUTF(in)
                    .map( name -> UserMessage.from(name, contents)
                    ).orElseGet(() -> UserMessage.from(contents));
        }

    }
}
