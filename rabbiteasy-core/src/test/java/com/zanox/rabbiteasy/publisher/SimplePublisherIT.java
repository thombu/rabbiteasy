package com.zanox.rabbiteasy.publisher;

import com.rabbitmq.client.Connection;
import com.zanox.rabbiteasy.Message;
import com.zanox.rabbiteasy.SingleConnectionFactory;
import com.zanox.rabbiteasy.TestBrokerSetup;
import org.junit.Test;

import java.io.IOException;

public class SimplePublisherIT extends MessagePublisherIT {

    @Test
    public void shouldPublishMessage() throws Exception {
        SimplePublisher publisher = new SimplePublisher(singleConnectionFactory);
        Message message = new Message()
                .exchange(TestBrokerSetup.TEST_EXCHANGE)
                .routingKey(TestBrokerSetup.TEST_ROUTING_KEY)
                .body("abc");

        publisher.publish(message);
        Thread.sleep(100);
        brokerAssert.messageInQueue(TestBrokerSetup.TEST_QUEUE, message.getBodyAs(String.class));
    }

}
