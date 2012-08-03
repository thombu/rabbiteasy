package com.zanox.rabbiteasy.cdi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;

/**
 * @author christian.bick
 */
public class TestConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestConsumer.class);

    public static int countOne = 0;
    public static int countTwo = 0;
    public static int countAll = 0;

    public static void resetCount() {
        countOne = 0;
        countTwo = 0;
        countAll = 0;
    }

    public void consumeOne(@Observes TestEventOne event) {
        LOGGER.debug("Received remote event one");
        countOne++;
    }

    public void consumeTwo(@Observes TestEventTwo event) {
        LOGGER.debug("Received remote event two");
        countTwo++;
    }

    public void consumeAll(@Observes TestEvent event) {
        LOGGER.debug("Received remote event");
        countAll++;
    }

}
