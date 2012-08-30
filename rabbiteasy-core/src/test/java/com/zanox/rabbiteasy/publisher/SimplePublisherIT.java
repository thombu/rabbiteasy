package com.zanox.rabbiteasy.publisher;

import com.rabbitmq.client.Connection;
import com.zanox.rabbiteasy.Message;
import com.zanox.rabbiteasy.TestBrokerSetup;
import org.junit.Test;

public class SimplePublisherIT extends MessagePublisherIT {

    @Test
    public void shouldSendMessage() throws Exception {
        SimplePublisher publisher = new SimplePublisher(singleConnectionFactory);
        Message message = new Message()
                .exchange(TestBrokerSetup.TEST_EXCHANGE)
                .routingKey(TestBrokerSetup.TEST_ROUTING_KEY)
                .body("abc");

        publisher.send(message);
        Thread.sleep(100);
        brokerAssert.messageInQueue(TestBrokerSetup.TEST_QUEUE, message.getBodyAs(String.class));
    }

    @Test
    public void shouldRecoverFromConnectionLoss() throws Exception {
        SimplePublisher publisher = new SimplePublisher(singleConnectionFactory);
        Message message = new Message()
                .exchange(TestBrokerSetup.TEST_EXCHANGE)
                .routingKey(TestBrokerSetup.TEST_ROUTING_KEY);

        publisher.send(message);
        Thread.sleep(100);
        brokerAssert.queueSize(TestBrokerSetup.TEST_QUEUE, 1);
        Connection connection = singleConnectionFactory.newConnection();
        connection.close();
        Thread.sleep(1000);
        publisher.send(message);
        Thread.sleep(100);
        brokerAssert.queueSize(TestBrokerSetup.TEST_QUEUE, 2);
    }

}
