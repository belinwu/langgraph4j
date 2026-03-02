package org.bsc.langgraph4j.streaming;

import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.state.AgentState;

public class StreamingOutput<State extends AgentState> extends NodeOutput<State> {

    private final String chunk; // null

    public StreamingOutput(String chunk, String node, State state) {
        super(node, state);

        this.chunk = chunk;
    }

    public String chunk() {
        return chunk;
    }

    public boolean isEnd() {
        return false;
    }

    @Override
    public String toString() {
        if( node() == null ) {
            return "%s{chunk=%s}".formatted( getClass().getSimpleName(), chunk());
        }
        return "%s{node=%s, chunk=%s, state=%s }".formatted( getClass().getSimpleName(),node(), chunk(), state() );
    }

}
