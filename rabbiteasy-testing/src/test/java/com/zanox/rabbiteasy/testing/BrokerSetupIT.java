package com.zanox.rabbiteasy.testing;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.GetResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class BrokerSetupIT {
    
    private BrokerSetup brokerSetup;
    
    @Before
    public void before() {
        brokerSetup = new BrokerSetup();
    }
    
    @Test
    public void shouldDeclareExchange() throws Exception {
        String exchange = "test.exchange";
        brokerSetup.declareExchange(exchange, "topic");
        brokerSetup.getChannel().exchangeDeclarePassive(exchange);
        brokerSetup.getChannel().exchangeDelete(exchange);
    }
    
    @Test
    public void shouldDeclareAndBindQueue() throws Exception {
        Channel channel = brokerSetup.getChannel();
        
        String exchange = "test.exchange";
        String queue = "test.queue";
        String routingKey = "test.key";
        channel.exchangeDeclare(exchange, "topic");
        brokerSetup.declareAndBindQueue(queue, exchange, routingKey);
        channel.queueDeclarePassive(queue);
        
        String body = "test.body";
        channel.basicPublish(exchange, routingKey, new BasicProperties(), body.getBytes()); 
        
        GetResponse response = channel.basicGet(queue, true);
        Assert.assertNotNull("no message in queue", response);
        Assert.assertEquals("wrong message in queue", new String(response.getBody(), "UTF-8"), body);
        
        channel.exchangeDelete(exchange);
        channel.queueDelete(queue);
    }
    
    @Test
    public void shouldTearDown() throws Exception {
        Channel channel = brokerSetup.getChannel();
        
        String exchange = "test.exchange";
        String queue = "test.queue";
        String routingKey = "test.key";
        brokerSetup.declareExchange(exchange, "topic");
        brokerSetup.declareAndBindQueue(queue, exchange, routingKey);
        channel.exchangeDeclarePassive(exchange);
        channel.queueDeclarePassive(queue);
        
        brokerSetup.tearDown();
        
        boolean exchangeExists = true;
        try {
            channel.exchangeDeclarePassive(exchange);
        } catch (Exception e) {
            exchangeExists = false;
        }
        boolean queueExists = true;
        try {
            queueExists = false;
        } catch (Exception e) {
            channel.queueDeclarePassive(queue);
        }
        Assert.assertFalse(exchangeExists);
        Assert.assertFalse(queueExists);
    }

}
