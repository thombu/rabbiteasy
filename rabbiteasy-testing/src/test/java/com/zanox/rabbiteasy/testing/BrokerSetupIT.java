package com.zanox.rabbiteasy.testing;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class BrokerSetupIT {

    private static final String TEST_EXCHANGE = "lib.test.exchange";
    private static final String TEST_QUEUE = "lib.test.queue";

    private BrokerSetup brokerSetup;
    
    @Before
    public void before() {
        brokerSetup = new BrokerSetup();
    }

    @After
    public void after() {
        try {
            brokerSetup.getChannel().exchangeDelete(TEST_EXCHANGE);
        } catch (Exception e) {}
        try {
            brokerSetup.getChannel().queueDelete(TEST_QUEUE);
        } catch (Exception e) { }
    }
    
    @Test
    public void shouldDeclareExchange() throws Exception {
        brokerSetup.declareExchange(TEST_EXCHANGE, "topic");
        brokerSetup.getChannel().exchangeDeclarePassive(TEST_EXCHANGE);
    }
    
    @Test
    public void shouldDeclareAndBindQueue() throws Exception {
        Channel channel = brokerSetup.getChannel();

        String routingKey = "test.key";
        channel.exchangeDeclare(TEST_EXCHANGE, "topic");
        brokerSetup.declareAndBindQueue(TEST_QUEUE, TEST_EXCHANGE, routingKey);
        channel.queueDeclarePassive(TEST_QUEUE);
        
        String body = "test.body";
        channel.basicPublish(TEST_EXCHANGE, routingKey, new BasicProperties(), body.getBytes());
        
        GetResponse response = channel.basicGet(TEST_QUEUE, true);
        Assert.assertNotNull("no message in queue", response);
        Assert.assertEquals("wrong message in queue", new String(response.getBody(), "UTF-8"), body);
        
        channel.exchangeDelete(TEST_EXCHANGE);
    }
    
    @Test
    public void shouldTearDown() throws Exception {
        Channel channel = brokerSetup.getChannel();

        String routingKey = "test.key";
        brokerSetup.declareExchange(TEST_EXCHANGE, "topic");
        brokerSetup.declareAndBindQueue(TEST_QUEUE, TEST_EXCHANGE, routingKey);
        channel.exchangeDeclarePassive(TEST_EXCHANGE);
        channel.queueDeclarePassive(TEST_QUEUE);
        
        brokerSetup.tearDown();
        
        boolean exchangeExists = true;
        try {
            channel.exchangeDeclarePassive(TEST_EXCHANGE);
        } catch (Exception e) {
            exchangeExists = false;
        }
        boolean queueExists = true;
        try {
            channel.queueDeclarePassive(TEST_QUEUE);
        } catch (Exception e) {
            queueExists = false;
        }
        Assert.assertFalse(exchangeExists);
        Assert.assertFalse(queueExists);
    }

}
