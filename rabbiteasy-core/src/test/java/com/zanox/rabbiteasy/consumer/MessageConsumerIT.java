package com.zanox.rabbiteasy.consumer;


import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.zanox.rabbiteasy.Message;
import com.zanox.rabbiteasy.SingleConnectionFactory;
import com.zanox.rabbiteasy.TestBrokerSetup;
import com.zanox.rabbiteasy.testing.BrokerAssert;
import com.zanox.rabbiteasy.testing.BrokerSetup;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;


public class MessageConsumerIT {

    BrokerSetup brokerSetup;
    BrokerAssert brokerAssert;
    SingleConnectionFactory connectionFactory;
    
    @Before
    public void before() throws Exception {
        brokerAssert = new BrokerAssert();
        brokerSetup = new BrokerSetup();
        brokerSetup.declareQueueWithDeadLettering(TestBrokerSetup.TEST_QUEUE);
        connectionFactory = new SingleConnectionFactory();
        connectionFactory.setHost(brokerSetup.getHost());
        connectionFactory.setPort(brokerSetup.getPort());
    }
    
    @After
    public void after() throws Exception {
        brokerSetup.tearDown();
    }
    
    @Test
    public void shouldConsumeMessage() throws Exception {
        sendTestMessage();
        Channel consumerChannel = connectionFactory.newConnection().createChannel();
        AckingConsumer consumer = new AckingConsumer();
        consumer.setChannel(consumerChannel);
        consumer.setConfiguration(new ConsumerConfiguration(TestBrokerSetup.TEST_QUEUE, true));
        consumerChannel.basicConsume(TestBrokerSetup.TEST_QUEUE, consumer);
        Thread.sleep(100);
        Assert.assertTrue(consumer.called);
        brokerAssert.queueEmtpy(TestBrokerSetup.TEST_QUEUE);
        brokerAssert.queueEmtpy(TestBrokerSetup.TEST_QUEUE_DEAD);
    }
    
    @Test
    public void shouldPutMessageToDeadLetterQueue() throws Exception {
        sendTestMessage();
        Channel consumerChannel = connectionFactory.newConnection().createChannel();
        NackingConsumer consumer = new NackingConsumer();
        consumer.setChannel(consumerChannel);
        consumer.setConfiguration(new ConsumerConfiguration(TestBrokerSetup.TEST_QUEUE, false));
        consumerChannel.basicConsume(TestBrokerSetup.TEST_QUEUE, consumer);
        Thread.sleep(100);
        Assert.assertTrue(consumer.called);
        brokerAssert.queueEmtpy(TestBrokerSetup.TEST_QUEUE);
        brokerAssert.queueNotEmtpy(TestBrokerSetup.TEST_QUEUE_DEAD);
    }
    
    private void sendTestMessage() throws IOException, InterruptedException {
        Channel producerChannel = connectionFactory.newConnection().createChannel();
        producerChannel.confirmSelect();
        producerChannel.basicPublish("", TestBrokerSetup.TEST_QUEUE, new BasicProperties.Builder().build(), "test".getBytes("UTF-8"));
        producerChannel.waitForConfirmsOrDie();
        brokerAssert.queueNotEmtpy(TestBrokerSetup.TEST_QUEUE);
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
