package org.bsc.langgraph4j.checkpoint;

import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class CustomPostgreSQLWaitStrategy implements WaitStrategy {

    private WaitStrategy logWaitStrategy;
    private WaitStrategy portWaitStrategy;

    public CustomPostgreSQLWaitStrategy() {

        this.logWaitStrategy = (new LogMessageWaitStrategy()).withRegEx(
                        ".*database system is ready to accept connections.*\\s").withTimes(2)
                .withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS));

        this.portWaitStrategy = Wait.defaultWaitStrategy();
    }

    @Override
    public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {
        logWaitStrategy.waitUntilReady(waitStrategyTarget);
        portWaitStrategy.waitUntilReady(waitStrategyTarget);
    }

    @Override
    public WaitStrategy withStartupTimeout(Duration duration) {

        logWaitStrategy = logWaitStrategy.withStartupTimeout(duration);
        portWaitStrategy = portWaitStrategy.withStartupTimeout(duration);
        return this;
    }
}
