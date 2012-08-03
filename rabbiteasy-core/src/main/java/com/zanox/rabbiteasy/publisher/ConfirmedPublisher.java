package com.zanox.rabbiteasy.publisher;

import com.rabbitmq.client.ConnectionFactory;

/**
 * <p>A confirmed publisher sends messages to a broker
 * and waits for a confirmation that the message was
 * received by the broker.</p>
 * 
 * @author christian.bick
 * @author uwe.janner
 * @author soner.dastan
 *
 */
public class ConfirmedPublisher extends GenericPublisher {

    public ConfirmedPublisher(ConnectionFactory connectionFactory) {
        super(connectionFactory, PublisherReliability.CONFIRMED);
    }

}
