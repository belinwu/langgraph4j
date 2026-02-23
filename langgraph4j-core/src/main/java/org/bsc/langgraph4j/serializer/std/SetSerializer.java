package org.bsc.langgraph4j.serializer.std;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedHashSet;
import java.util.Set;

class SetSerializer implements NullableObjectSerializer<Set<Object>> {

    @Override
    public void write(Set<Object> object, ObjectOutput out) throws IOException {
        out.writeInt(object.size());

        for (Object value : object) {
            writeNullableObject(value, out);
        }

        out.flush();

    }

    @Override
    public Set<Object> read(ObjectInput in) throws IOException, ClassNotFoundException {
        Set<Object> result = new LinkedHashSet<>();

        int size = in.readInt();

        for (int i = 0; i < size; i++) {

            Object value = readNullableObject(in).orElse(null);

            result.add(value);

        }

        return result;
    }
}
