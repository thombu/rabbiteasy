package com.zanox.rabbiteasy.publisher;

import com.rabbitmq.client.ConnectionFactory;
import com.zanox.rabbiteasy.Message;

import java.io.IOException;
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
    public void publish(Message message) throws IOException {
        publish(message, DeliveryOptions.NONE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(List<Message> messages) throws IOException {
        publish(messages, DeliveryOptions.NONE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(Message message, DeliveryOptions deliveryOptions)
            throws IOException {
        publisher.publish(message, deliveryOptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(List<Message> messages, DeliveryOptions deliveryOptions) throws IOException {
        publisher.publish(messages, deliveryOptions);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        publisher.close();
    }

}
