package com.zanox.rabbiteasy.cdi;

import com.zanox.rabbiteasy.Message;
import com.zanox.rabbiteasy.consumer.ConsumerContainer;
import com.zanox.rabbiteasy.consumer.MessageConsumer;
import com.zanox.rabbiteasy.publisher.DeliveryOptions;
import com.zanox.rabbiteasy.publisher.PublisherReliability;
import junit.framework.Assert;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Before;
import org.junit.Test;

import javax.enterprise.inject.Instance;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author christian.bick
 */
public class EventBinderTest {

    EventBinder eventBinder;

    @Before
    public void before() {
        TestConsumer.resetCount();
        // Initializes the weld container
        WeldContainer container = new Weld().initialize();
        Instance<Object> instancePool = container.instance();
        eventBinder = instancePool.select(TestEventBinder.class).get();
    }

    @Test
    public void testBindQueue() {
        eventBinder.bind(TestEventOne.class).toQueue(TestSetup.TEST_QUEUE_ONE);
        eventBinder.bind(TestEventTwo.class).toQueue(TestSetup.TEST_QUEUE_TWO).autoAck();
        eventBinder.processQueueBindings();
        // Retrieve consumers managed by the consumer container
        List<ConsumerContainer.ConsumerHolder> consumers = eventBinder.consumerContainer.getInactiveConsumers();
        // Sort consumers first because their order is arbitrary
        Collections.sort(consumers, new MessageConsumerComparator());
        // Check for exact amount of consumers
        Assert.assertEquals(2, consumers.size());
        ConsumerContainer.ConsumerHolder consumerOne = consumers.get(0);
        ConsumerContainer.ConsumerHolder consumerTwo = consumers.get(1);
        // Check for correct queue names
        Assert.assertEquals(TestSetup.TEST_QUEUE_ONE, consumerOne.getConfiguration().getQueueName());
        Assert.assertEquals(TestSetup.TEST_QUEUE_TWO, consumerTwo.getConfiguration().getQueueName());
        // Check for correct auto ack
        Assert.assertFalse(consumerOne.getConfiguration().isAutoAck());
        Assert.assertTrue(consumerTwo.getConfiguration().isAutoAck());
    }

    @Test
    public void testBindQueueWithConsumerContainer() {
        eventBinder.bind(TestEventOne.class).toQueue(TestSetup.TEST_QUEUE_ONE);
        eventBinder.bind(TestEventTwo.class).toQueue(TestSetup.TEST_QUEUE_TWO);
        eventBinder.processQueueBindings();
        // Retrieve consumers managed by the consumer container
        List<ConsumerContainer.ConsumerHolder> consumers = eventBinder.consumerContainer.getInactiveConsumers();
        // Sort consumers first because their order is arbitrary
        Collections.sort(consumers, new MessageConsumerComparator());

        MessageConsumer consumerOne = (MessageConsumer)consumers.get(0).getConsumer();
        MessageConsumer consumerTwo = (MessageConsumer)consumers.get(1).getConsumer();

        consumerOne.handleMessage(new Message());
        consumerOne.handleMessage(new Message());
        consumerTwo.handleMessage(new Message());
        consumerTwo.handleMessage(new Message());
        consumerTwo.handleMessage(new Message());

        Assert.assertEquals(2, TestConsumer.countOne);
        Assert.assertEquals(3, TestConsumer.countTwo);
        Assert.assertEquals(5, TestConsumer.countAll);
    }

    @Test
    public void testBindExchange() {
        eventBinder.bind(TestEventOne.class).toExchange(TestSetup.TEST_EXCHANGE_ONE);
        eventBinder.bind(TestEventTwo.class).toExchange(TestSetup.TEST_EXCHANGE_TWO)
                .withRoutingKey(TestSetup.TEST_ROUTING_KEY_TWO)
                .withPersistentMessages()
                .withPublisherTransactions()
                .withImmediateDelivery();
        eventBinder.bind(TestEventThree.class).toExchange(TestSetup.TEST_EXCHANGE_THREE)
                .withRoutingKey(TestSetup.TEST_ROUTING_KEY_THREE)
                .withPublisherConfirms()
                .withMandatoryDelivery();
        eventBinder.processExchangeBindings();

        Map<Class<?>, EventPublisher.PublisherConfiguration> configurations =
                eventBinder.eventPublisher.publisherConfigurations;

        EventPublisher.PublisherConfiguration config = configurations.get(TestEventOne.class);
        Assert.assertEquals(TestSetup.TEST_EXCHANGE_ONE, config.exchange);
        Assert.assertNull(config.routingKey);
        Assert.assertEquals(PublisherReliability.NONE, config.reliability);
        Assert.assertEquals(DeliveryOptions.NONE, config.deliveryOptions);

        config = configurations.get(TestEventTwo.class);
        Assert.assertEquals(TestSetup.TEST_EXCHANGE_TWO, config.exchange);
        Assert.assertEquals(TestSetup.TEST_ROUTING_KEY_TWO, config.routingKey);
        Assert.assertEquals(PublisherReliability.TRANSACTIONAL, config.reliability);
        Assert.assertEquals(DeliveryOptions.IMMEDIATE, config.deliveryOptions);

        config = configurations.get(TestEventThree.class);
        Assert.assertEquals(TestSetup.TEST_EXCHANGE_THREE, config.exchange);
        Assert.assertEquals(TestSetup.TEST_ROUTING_KEY_THREE, config.routingKey);
        Assert.assertEquals(PublisherReliability.CONFIRMED, config.reliability);
        Assert.assertEquals(DeliveryOptions.MANDATORY, config.deliveryOptions);
    }

    private static class MessageConsumerComparator implements Comparator<ConsumerContainer.ConsumerHolder> {
        @Override
        public int compare(ConsumerContainer.ConsumerHolder o1, ConsumerContainer.ConsumerHolder o2) {
            String queue1 = o1.getConfiguration().getQueueName();
            String queue2 = o2.getConfiguration().getQueueName();
            return queue1.compareTo(queue2);
        }
    }

}
