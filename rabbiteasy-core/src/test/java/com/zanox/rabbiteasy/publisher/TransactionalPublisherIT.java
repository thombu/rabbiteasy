package com.zanox.rabbiteasy.publisher;

import com.zanox.rabbiteasy.Message;
import com.zanox.rabbiteasy.TestBrokerSetup;
import org.junit.Test;


public class TransactionalPublisherIT extends MessagePublisherIT {

    @Test
    public void shouldSendMessage() throws Exception {
        TransactionalPublisher publisher = new TransactionalPublisher(singleConnectionFactory);
        Message message = new Message()
                .exchange(TestBrokerSetup.TEST_EXCHANGE)
                .routingKey(TestBrokerSetup.TEST_ROUTING_KEY)
                .body("abc");
        publisher.send(message);
        publisher.commitTransaction();
        Thread.sleep(100);
        brokerAssert.messageInQueue(TestBrokerSetup.TEST_QUEUE, message.getBodyAs(String.class));
    }

}
