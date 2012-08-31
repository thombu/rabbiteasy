package com.zanox.rabbiteasy.publisher;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.zanox.rabbiteasy.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

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
public class TransactionalPublisher extends ManagedPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalPublisher.class);

    public TransactionalPublisher(ConnectionFactory connectionFactory) {
        super(connectionFactory);
    }

    @Override
    public void send(Message message, DeliveryOptions deliveryOptions) throws IOException {
        send(Collections.<Message>singletonList(message), deliveryOptions);
    }

    @Override
    public void send(List<Message> messages, DeliveryOptions deliveryOptions) throws IOException {
        for (int attempt = 1; attempt <= DEFAULT_RETRY_ATTEMPTS; attempt++) {
            if (attempt > 1) {
                LOGGER.info("Attempt {} to send messages within transaction", attempt);
            }

            try {
                initChannel();
                try {
                    for (Message message : messages) {
                        publishMessage(message, deliveryOptions);
                    }
                    commitTransaction();
                } catch (IOException e) {
                    rollbackTransaction();
                    throw e;
                }
                return;
            } catch (IOException e) {
                handleIoException(attempt, e);
            }
        }
    }

    protected void initChannel()  throws IOException {
        Channel channel = getChannel();
        if (channel == null || !channel.isOpen()) {
            channel = createChannel();
            channel.txSelect();
        }
    }

    protected void commitTransaction() throws IOException {
        try {
            LOGGER.info("Committing transaction");
            getChannel().txCommit();
            LOGGER.info("Transaction committed");
        } catch (IOException e) {
            LOGGER.error("Failed to commit transaction", e);
            throw e;
        }
    }

    protected void rollbackTransaction() throws IOException {
        try {
            LOGGER.info("Rolling back transaction");
            getChannel().txRollback();
            LOGGER.info("Transaction rolled back");
        } catch (IOException e) {
            LOGGER.error("Failed to roll back transaction", e);
            throw e;
        }
    }
}
