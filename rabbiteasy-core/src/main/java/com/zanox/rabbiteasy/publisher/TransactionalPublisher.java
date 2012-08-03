package com.zanox.rabbiteasy.publisher;

import com.rabbitmq.client.ConnectionFactory;

/**
 * <p>A transactional publisher sends messages to a broker
 * within a transaction scope. A message is only put into
 * its destination queues when the transaction is committed</p>
 * 
 * @author christian.bick
 * @author uwe.janner
 * @author soner.dastan
 *
 */
public class TransactionalPublisher extends GenericPublisher {

    public TransactionalPublisher(ConnectionFactory connectionFactory) {
        super(connectionFactory, PublisherReliability.TRANSACTIONAL);
    }
    

}
