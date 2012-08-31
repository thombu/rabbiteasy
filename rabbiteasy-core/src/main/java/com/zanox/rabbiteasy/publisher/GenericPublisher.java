package com.zanox.rabbiteasy.publisher;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.zanox.rabbiteasy.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Publishes messages with certain different levels of {@link PublisherReliability}.
 *
 * @author christian.bick
 */
public class GenericPublisher implements MessagePublisher {

    MessagePublisher publisher;

    /**
     * <p>Initializes the publisher with a certain level of reliability. All messages
     * sent by the producer are sent with this level of reliability. Uses the given
     * connection factory to establish connections.</p>
     *
     * @see SimplePublisher
     * @see ConfirmedPublisher
     * @see TransactionalPublisher
     *
     * @param connectionFactory The connection factory
     * @param reliability The reliability level
     */
    public GenericPublisher(ConnectionFactory connectionFactory, PublisherReliability reliability) {
        if (reliability == PublisherReliability.CONFIRMED) {
            publisher = new ConfirmedPublisher(connectionFactory);
        } else if (reliability == PublisherReliability.TRANSACTIONAL) {
            publisher = new TransactionalPublisher(connectionFactory);
        } else {
            publisher = new SimplePublisher(connectionFactory);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(Message message) throws IOException {
        send(message, DeliveryOptions.NONE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(List<Message> messages) throws IOException {
        send(messages, DeliveryOptions.NONE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(Message message, DeliveryOptions deliveryOptions)
            throws IOException {
        publisher.send(message, deliveryOptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(List<Message> messages, DeliveryOptions deliveryOptions) throws IOException {
        publisher.send(messages, deliveryOptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        publisher.close();
    }

}
