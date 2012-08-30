package com.zanox.rabbiteasy.consumer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.rabbitmq.client.Connection;
import com.zanox.rabbiteasy.Message;
import com.zanox.rabbiteasy.SingleConnectionFactory;
import com.zanox.rabbiteasy.TestBrokerSetup;
import com.zanox.rabbiteasy.publisher.MessagePublisher;
import com.zanox.rabbiteasy.publisher.SimplePublisher;


public class ConsumerContainerIT {
    
    private static final int MESSAGE_AMOUNT = 10;
    
    private TestBrokerSetup brokerSetup;
    
    private MessagePublisher publisher;
    private SingleConnectionFactory connectionFactory;
    
    @Before
    public void before() throws Exception {
        brokerSetup = new TestBrokerSetup();
        
        connectionFactory = new SingleConnectionFactory();
        connectionFactory.setHost(brokerSetup.getHost());
        connectionFactory.setPort(brokerSetup.getPort());
        publisher = new SimplePublisher(connectionFactory);
    }
    
    @After
    public void after() throws Exception {
        brokerSetup.tearDown();
        connectionFactory.close();
    }
    
    @Test
    public void shouldActivateAllConsumers() throws Exception {
        brokerSetup.prepareSimpleTest();
        ConsumerContainer consumerContainer = prepareConsumerContainer(
            new TestConsumer(), TestBrokerSetup.TEST_QUEUE);
        consumerContainer.startAllConsumers();
        int activeConsumerCount = consumerContainer.getActiveConsumers().size();
        Assert.assertEquals(1, activeConsumerCount);
    }
    
    @Test
    public void shouldReActivateAllConsumers() throws Exception {
        brokerSetup.prepareSimpleTest();
        ConsumerContainer consumerContainer = prepareConsumerContainer(
            new TestConsumer(), TestBrokerSetup.TEST_QUEUE);
        consumerContainer.startAllConsumers();
        int activeConsumerCount = consumerContainer.getActiveConsumers().size();
        Assert.assertEquals(1, activeConsumerCount);
        Connection connection = connectionFactory.newConnection();
        connectionFactory.setHost("unreachablehostnamethatdoesnotexist");
        connection.close();
        Thread.sleep(SingleConnectionFactory.CONNECTION_ESTABLISH_INTERVAL_IN_MS * 3);
        connectionFactory.setHost(brokerSetup.getHost());
        Thread.sleep(SingleConnectionFactory.CONNECTION_ESTABLISH_INTERVAL_IN_MS * 2);
        activeConsumerCount = consumerContainer.getActiveConsumers().size();
        Assert.assertEquals(1, activeConsumerCount);
    }
    
    @Test
    public void shouldReceiveAllMessages() throws Exception {
        brokerSetup.prepareSimpleTest();
        TestConsumer testConsumer = new TestConsumer();
        ConsumerContainer consumerContainer = prepareConsumerContainer(testConsumer, TestBrokerSetup.TEST_QUEUE);
        consumerContainer.startAllConsumers();
        for (int i=1; i<=MESSAGE_AMOUNT; i++) {
            Message message = new Message()
                    .exchange(TestBrokerSetup.TEST_EXCHANGE)
                    .routingKey(TestBrokerSetup.TEST_ROUTING_KEY)
                    .body("" + i);
            publisher.send(message);
        }
        // Sleep depending on the amount of messages sent but at least 100 ms, and at most 1 sec
        Thread.sleep(Math.max(100, Math.min(1000, MESSAGE_AMOUNT * 10)));
        List<Message> receivedMessages = testConsumer.getReceivedMessages();
        Assert.assertEquals(MESSAGE_AMOUNT, receivedMessages.size());
        for (int i=1; i<=MESSAGE_AMOUNT; i++) {
            Message receivedMessage = receivedMessages.get(i-1);
            Assert.assertNotNull(receivedMessage);
            Assert.assertEquals(i, (int)receivedMessage.getBodyAs(Integer.class));
        }
    }
    
    @Test(expected = IOException.class)
    public void shouldFailToStartConsumers() throws Exception {
        brokerSetup.prepareSimpleTest();
        TestConsumer failingConsumer = new TestConsumer();
        ConsumerContainer consumerContainer = prepareConsumerContainer(failingConsumer, "test.missing.queue");
        consumerContainer.startAllConsumers();
    }
    
    @Test
    public void shouldActivateConsumersUsingHighAvailability() throws Exception {
        brokerSetup.prepareHighAvailabilityTest();
        TestConsumer testConsumer = new TestConsumer();
        ConsumerContainer consumerContainer = prepareConsumerContainer(testConsumer, TestBrokerSetup.TEST_HA_QUEUE);
        consumerContainer.startAllConsumers();
        int activeConsumerCount = consumerContainer.getActiveConsumers().size();
        Assert.assertEquals(1, activeConsumerCount);
    }
    
    private ConsumerContainer prepareConsumerContainer(MessageConsumer consumer, String queue) {
        ConsumerContainer consumerContainer = new ConsumerContainer(connectionFactory);
        consumerContainer.addConsumer(consumer, queue);
        return consumerContainer;
    }
    
    private class TestConsumer extends MessageConsumer {
        
        private List<Message> receivedMessages = new ArrayList<Message>(MESSAGE_AMOUNT);

        @Override
        public void handleMessage(Message message) {
            this.receivedMessages.add(message);
        }
        
        public List<Message> getReceivedMessages() {
            return receivedMessages;
        }
        
    }

}
