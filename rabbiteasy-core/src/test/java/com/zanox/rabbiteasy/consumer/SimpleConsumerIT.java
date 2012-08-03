package com.zanox.rabbiteasy.consumer;


import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.zanox.rabbiteasy.Message;
import com.zanox.rabbiteasy.SingleConnectionFactory;
import com.zanox.rabbiteasy.TestBrokerSetup;
import com.zanox.rabbiteasy.testing.BrokerAssert;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;


public class SimpleConsumerIT {
    
    private static final String TEST_QUEUE = "lib.test.dead.lettering";
    private static final String TEST_QUEUE_DEAD = TEST_QUEUE + ":dead";
    
    TestBrokerSetup testBrokerSetup;
    BrokerAssert brokerAssert;
    SingleConnectionFactory connectionFactory;
    
    @Before
    public void before() throws Exception {
        brokerAssert = new BrokerAssert();
        testBrokerSetup = new TestBrokerSetup();
        testBrokerSetup.declareQueueWithDeadLettering(TEST_QUEUE);
        connectionFactory = new SingleConnectionFactory();
        connectionFactory.setHost(testBrokerSetup.getHost());
        connectionFactory.setPort(testBrokerSetup.getPort());
    }
    
    @After
    public void after() throws Exception {
        testBrokerSetup.tearDown();
    }
    
    @Test
    public void shouldConsumeMessage() throws Exception {
        sendTestMessage();
        Channel consumerChannel = connectionFactory.newConnection().createChannel();
        AckingConsumer consumer = new AckingConsumer();
        consumer.setChannel(consumerChannel);
        consumer.setConfiguration(new ConsumerConfiguration(TEST_QUEUE, true));
        consumerChannel.basicConsume(TEST_QUEUE, consumer);
        Thread.sleep(100);
        Assert.assertTrue(consumer.called);
        brokerAssert.queueEmtpy(TEST_QUEUE);
        brokerAssert.queueEmtpy(TEST_QUEUE_DEAD);
    }
    
    @Test
    public void shouldPutMessageToDeadLetterQueue() throws Exception {
        sendTestMessage();
        Channel consumerChannel = connectionFactory.newConnection().createChannel();
        NackingConsumer consumer = new NackingConsumer();
        consumer.setChannel(consumerChannel);
        consumer.setConfiguration(new ConsumerConfiguration(TEST_QUEUE, false));
        consumerChannel.basicConsume(TEST_QUEUE, consumer);
        Thread.sleep(100);
        Assert.assertTrue(consumer.called);
        brokerAssert.queueEmtpy(TEST_QUEUE);
        brokerAssert.queueNotEmtpy(TEST_QUEUE_DEAD);
    }
    
    private void sendTestMessage() throws IOException, InterruptedException {
        Channel producerChannel = connectionFactory.newConnection().createChannel();
        producerChannel.confirmSelect();
        producerChannel.basicPublish("", TEST_QUEUE, new BasicProperties.Builder().build(), "test".getBytes("UTF-8"));
        producerChannel.waitForConfirmsOrDie();
        brokerAssert.queueNotEmtpy(TEST_QUEUE);
    }
    
    private class NackingConsumer extends MessageConsumer {
        boolean called = false;
        @Override
        public void handleMessage(Message message) {
            called = true;
            throw new RuntimeException("This should lead to a nack");
        }
    }
    
    private class AckingConsumer extends MessageConsumer {
        boolean called = false;
        @Override
        public void handleMessage(Message message) {
            called = true;
        }
        
    }
    
}
