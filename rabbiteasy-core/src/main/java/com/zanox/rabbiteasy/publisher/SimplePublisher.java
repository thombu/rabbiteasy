package com.zanox.rabbiteasy.publisher;

import com.rabbitmq.client.ConnectionFactory;

/**
 * <p>
 * A simple publisher sends messages to a broker in a fire-and-forget manner.
 * </p>
 * 
 * @author christian.bick
 * @author uwe.janner
 * @author soner.dastan
 * 
 */
public class SimplePublisher extends GenericPublisher {

    public SimplePublisher(ConnectionFactory connectionFactory) {
        super(connectionFactory, PublisherReliability.NONE);
    }
}
