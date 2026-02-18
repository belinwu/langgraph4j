#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.spring.ai.agentexecutor;

import org.bsc.langgraph4j.prebuilt.MessagesState;
import org.bsc.langgraph4j.spring.ai.agent.ReactAgent;
import org.bsc.langgraph4j.spring.ai.serializer.std.SpringAIStateSerializer;
import org.bsc.langgraph4j.state.AgentState;
import org.springframework.ai.chat.messages.Message;

import java.util.Map;

/**
 * Represents the core component responsible for executing agent logic.
 * It includes methods for building and managing the execution graph,
 * as well as handling agent actions and state transitions.
 *
 * @author lambochen
 */
public interface AgentExecutor {

    /**
     * Class responsible for building a state graph.
     */
    class Builder extends ReactAgent.Builder<State> {

    }

    /**
     * Returns a new instance of {@link Builder}.
     *
     * @return a new {@link Builder} object
     */
    static Builder builder() {
        var result = new Builder();
        result.stateSerializer( new SpringAIStateSerializer<>(State::new) );
        return result;
    }

    /**
     * Represents the state of an agent in a system.
     * This class extends {@link AgentState} and defines constants for keys related to input, agent outcome,
     * and intermediate steps. It includes a static map schema that specifies how these keys should be handled.
     */
    class State extends MessagesState<Message> {

        /**
         * Constructs a new State object using the initial data provided in the initData map.
         *
         * @param initData the map containing the initial settings for this state
         */
        public State(Map<String, Object> initData) {
            super(initData);
        }

    }

}