package com.zanox.rabbiteasy.publisher;

import com.rabbitmq.client.Connection;
import com.zanox.rabbiteasy.Message;
import com.zanox.rabbiteasy.SingleConnectionFactory;
import com.zanox.rabbiteasy.TestBrokerSetup;
import org.junit.Test;

import java.io.IOException;


public class TransactionalPublisherIT extends MessagePublisherIT {

    @Test
    public void shouldPublishMessage() throws Exception {
        TransactionalPublisher publisher = new TransactionalPublisher(singleConnectionFactory);
        Message message = new Message()
                .exchange(TestBrokerSetup.TEST_EXCHANGE)
                .routingKey(TestBrokerSetup.TEST_ROUTING_KEY)
                .body("abc");
        publisher.publish(message);
        Thread.sleep(100);
        brokerAssert.messageInQueue(TestBrokerSetup.TEST_QUEUE, message.getBodyAs(String.class));
    }

    @Test
    public void shouldRecoverFromConnectionLoss() throws Exception {
        TransactionalPublisher publisher = new TransactionalPublisher(singleConnectionFactory);
        Message message = new Message()
                .exchange(TestBrokerSetup.TEST_EXCHANGE)
                .routingKey(TestBrokerSetup.TEST_ROUTING_KEY);

        publisher.publish(message);
        Thread.sleep(100);
        brokerAssert.queueSize(TestBrokerSetup.TEST_QUEUE, 1);
        Connection connection = singleConnectionFactory.newConnection();
        singleConnectionFactory.setPort(15345);
        connection.close();
        int waitForReconnects = SingleConnectionFactory.CONNECTION_ESTABLISH_INTERVAL_IN_MS + SingleConnectionFactory.CONNECTION_TIMEOUT_IN_MS * 2;
        Thread.sleep(waitForReconnects);
        try {
            publisher.publish(message);
        } catch (IOException e) { }
        singleConnectionFactory.setPort(brokerSetup.getPort());
        Thread.sleep(waitForReconnects);
        publisher.publish(message);
        Thread.sleep(waitForReconnects);
        brokerAssert.queueSize(TestBrokerSetup.TEST_QUEUE, 2);
    }

}
