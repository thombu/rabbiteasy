package com.zanox.rabbiteasy.publisher;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.zanox.rabbiteasy.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

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
public class ConfirmedPublisher extends ManagedPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmedPublisher.class);

    public ConfirmedPublisher(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public void send(Message message, DeliveryOptions deliveryOptions) throws IOException {
        for (int attempt = 1; attempt <= DEFAULT_RETRY_ATTEMPTS; attempt++) {
            if (attempt > 1) {
                LOGGER.info("Attempt {} to send message", attempt);
            }

            try {
                initChannel();
                publishMessage(message, deliveryOptions);
                LOGGER.info("Waiting for publisher ack");
                getChannel().waitForConfirmsOrDie();
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

    @Override
    public void send(List<Message> messages, DeliveryOptions deliveryOptions) throws IOException {
        for (Message message : messages) {
            send(message, deliveryOptions);
        }
    }

    protected void initChannel() throws IOException {
        Channel channel = getChannel();
        if (channel == null || !channel.isOpen()) {
            channel = createChannel();
            channel.confirmSelect();
        }
    }
}
