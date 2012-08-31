package com.zanox.rabbiteasy.publisher;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.zanox.rabbiteasy.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * @author christian.bick
 */
public abstract class ManagedPublisher implements MessagePublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManagedPublisher.class);

    public static final int DEFAULT_RETRY_ATTEMPTS = 3;
    public static final int DEFAULT_RETRY_INTERVAL = 1000;

    private Channel channel;
    private ConnectionFactory connectionFactory;

    public ManagedPublisher(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void send(Message message) throws IOException {
        send(message, DeliveryOptions.NONE);
    }

    @Override
    public void send(List<Message> messages) throws IOException {
        send(messages, DeliveryOptions.NONE);
    }

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

    protected Channel createChannel() throws IOException {
        Connection connection = connectionFactory.newConnection();
        channel = connection.createChannel();
        return channel;
    }

    protected Channel getChannel() throws IOException {
        return channel;
    }

    protected void publishMessage(Message message, DeliveryOptions deliveryOptions)
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

    protected void handleIoException(int attempt, IOException ioException) throws IOException {
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
    }
}
