package org.bsc.langgraph4j.streaming;

import org.bsc.langgraph4j.state.AgentState;

public class StreamingOutputEnd<State extends AgentState> extends StreamingOutput<State> {

    public StreamingOutputEnd( String finalResponse, String node, State state) {
        super(finalResponse, node, state);
    }

    @Override
    public final boolean isEnd() {
        return true;
    }

}
