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

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericPublisher.class);
    private static final int DEFAULT_RETRY_ATTEMPTS = 3;
    private static final int DEFAULT_RETRY_INTERVAL = 1000;

    private ConnectionFactory connectionFactory;
    private Channel channel;

    private PublisherReliability reliability;

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
        this.connectionFactory = connectionFactory;
        this.reliability = reliability;
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
        if (reliability == PublisherReliability.CONFIRMED) {
            publishMessageConfirmed(message, deliveryOptions);
        } else if (reliability == PublisherReliability.TRANSACTIONAL) {
            publishMessageTransactional(message, deliveryOptions);
        } else {
            publishMessage(message, deliveryOptions);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(List<Message> messages, DeliveryOptions deliveryOptions) throws IOException {
        if (reliability == PublisherReliability.CONFIRMED) {
            publishMessagesConfirmed(messages, deliveryOptions);
        } else if (reliability == PublisherReliability.TRANSACTIONAL) {
            publishMessagesTransactional(messages, deliveryOptions);
        } else {
            publishMessages(messages, deliveryOptions);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        if (channel == null) {
            LOGGER.warn("Attempt to close a publisher channel that has not been initialized");
            return;
        } else if (! channel.isOpen()) {
            LOGGER.warn("Attempt to close a publisher channel that has already been closed or is already closing");
            return;
        }
        LOGGER.debug("Closing publisher channel");
        channel.close();
        channel = null;
        LOGGER.debug("Successfully closed publisher channel");
    }

    protected void publishMessage(Message message, DeliveryOptions deliveryOptions) throws IOException {
        for (int attempt = 1; attempt <= DEFAULT_RETRY_ATTEMPTS; attempt++) {
            if (attempt > 1) {
                LOGGER.info("Attempt {} to send message", attempt);
            }

            try {
                initChannel();
                publishMessage(message, deliveryOptions, channel);
                return;
            } catch (IOException e) {
                handleIoException(attempt, e);
            }
        }
    }

    protected void publishMessages(List<Message> messages, DeliveryOptions deliveryOptions) throws IOException {
        for (Message message : messages) {
            publishMessage(message, deliveryOptions);
        }
    }

    protected void publishMessageConfirmed(Message message, DeliveryOptions deliveryOptions) throws IOException {
        for (int attempt = 1; attempt <= DEFAULT_RETRY_ATTEMPTS; attempt++) {
            if (attempt > 1) {
                LOGGER.info("Attempt {} to send message", attempt);
            }

            try {
                initConfirmedChannel();
                publishMessage(message, deliveryOptions, channel);
                LOGGER.info("Waiting for publisher ack");
                channel.waitForConfirmsOrDie();
                LOGGER.info("Received publisher ack");
                return;
            } catch (IOException e) {
                handleIoException(attempt, e);
            } catch (InterruptedException e) {
                LOGGER.warn("Publishing message interrupted while waiting for producer ack", e);
                return;
            }
        }
    }

    protected void publishMessagesConfirmed(List<Message> messages, DeliveryOptions deliveryOptions) throws IOException {
        for (Message message : messages) {
            publishMessageConfirmed(message, deliveryOptions);
        }
    }

    protected void publishMessageTransactional(Message message, DeliveryOptions deliveryOptions) throws IOException {
        publishMessagesTransactional(Collections.<Message>singletonList(message), deliveryOptions);
    }

    protected void publishMessagesTransactional(List<Message> messages, DeliveryOptions deliveryOptions) throws IOException {
        for (int attempt = 1; attempt <= DEFAULT_RETRY_ATTEMPTS; attempt++) {
            if (attempt > 1) {
                LOGGER.info("Attempt {} to send messages within transaction", attempt);
            }

            try {
                initTransactionalChannel();
                for (Message message : messages) {
                    publishMessage(message, deliveryOptions, channel);
                }
                LOGGER.info("Committing transaction");
                commitTransaction();
                LOGGER.info("Transaction committed");
                return;
            } catch (IOException e) {
                rollbackTransaction();
                handleIoException(attempt, e);
            }
        }
    }

    static void publishMessage(Message message, DeliveryOptions deliveryOptions, Channel channel)
            throws IOException {
        // assure we have a timestamp
        if (message.getBasicProperties().getTimestamp() == null) {
            message.getBasicProperties().builder().timestamp(new Date());
        }

        AMQP.BasicProperties basicProperties = message.getBasicProperties();
        String exchange = message.getExchange();
        String routingKey = message.getRoutingKey();
        boolean mandatory = deliveryOptions == DeliveryOptions.MANDATORY;
        boolean immediate = deliveryOptions == DeliveryOptions.IMMEDIATE;
        byte[] bodyContent = message.getBodyContent();

        LOGGER.info("Publishing message to exchange '{}' with routing key '{}' (deliveryOptions: {}, persistent: {})",
                new Object[] { exchange, routingKey, deliveryOptions, basicProperties.getDeliveryMode() == 2 });

        channel.basicPublish(exchange, routingKey, mandatory, immediate, basicProperties, bodyContent);
        LOGGER.info("Successfully published message to exchange '{}' with routing key '{}'", exchange, routingKey);

    }

    protected void createChannel() throws IOException {
        Connection connection = connectionFactory.newConnection();
        channel = connection.createChannel();
    }

    protected void initChannel() throws IOException {
        if (channel == null || !channel.isOpen()) {
            createChannel();
        }
    }

    protected void initConfirmedChannel() throws IOException {
        if (channel == null || !channel.isOpen()) {
            createChannel();
            channel.confirmSelect();createChannel();
        }
    }

    protected void initTransactionalChannel()  throws IOException {
        if (channel == null || !channel.isOpen()) {
            createChannel();
            channel.txSelect();
        }
    }

    protected void commitTransaction() throws IOException {
        try {
            LOGGER.info("Committing transaction");
            channel.txCommit();
            LOGGER.info("Transaction committed");
        } catch (IOException e) {
            LOGGER.error("Failed to commit transaction", e);
            throw e;
        }
    }

    protected void rollbackTransaction() throws IOException {
        try {
            LOGGER.info("Rolling back transaction");
            channel.txRollback();
            LOGGER.info("Transaction rolled back");
        } catch (IOException e) {
            LOGGER.error("Failed to roll back transaction", e);
            throw e;
        }
    }

    boolean handleIoException(int attempt, IOException ioException) throws IOException {
        if (channel != null && channel.isOpen()) {
            try {
                channel.close();
            } catch (IOException e) {
                LOGGER.warn("Failed to close channel after failed publish", e);
            }
        }
        channel = null;
        if (attempt == DEFAULT_RETRY_ATTEMPTS) {
            throw ioException;
        }
        try {
            Thread.sleep(DEFAULT_RETRY_INTERVAL);
        } catch (InterruptedException e) {
            LOGGER.warn("Sending message interrupted while waiting for retry attempt", e);
        }
        return true;
    }

}
