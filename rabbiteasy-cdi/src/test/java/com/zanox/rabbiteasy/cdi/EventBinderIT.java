package com.zanox.rabbiteasy.cdi;

import com.rabbitmq.client.ConnectionFactory;
import com.zanox.rabbiteasy.Message;
import com.zanox.rabbiteasy.publisher.ConfirmedPublisher;
import com.zanox.rabbiteasy.testing.BrokerAssert;
import com.zanox.rabbiteasy.testing.BrokerSetup;
import junit.framework.Assert;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import java.io.IOException;

/**
 * @author christian.bick
 */
public class EventBinderIT {

    BrokerSetup brokerSetup;
    BrokerAssert brokerAssert;

    EventBinder eventBinder;
    ConfirmedPublisher publisher;
    Event<Object> eventControl;

    @Before
    public void before() throws IOException {
        TestConsumer.resetCount();
        // Initialize broker setup
        brokerSetup = new BrokerSetup();
        brokerSetup.declareExchange(TestSetup.TEST_EXCHANGE_ONE, "topic");
        brokerSetup.declareExchange(TestSetup.TEST_EXCHANGE_TWO, "topic");
        brokerSetup.declareAndBindQueue(
                TestSetup.TEST_QUEUE_ONE, TestSetup.TEST_EXCHANGE_ONE, TestSetup.TEST_ROUTING_KEY_ONE);
        brokerSetup.declareAndBindQueue(
                TestSetup.TEST_QUEUE_TWO, TestSetup.TEST_EXCHANGE_TWO, TestSetup.TEST_ROUTING_KEY_TWO);
        brokerAssert = new BrokerAssert();
        // Initializes the weld container
        WeldContainer container = new Weld().initialize();
        Instance<Object> instancePool = container.instance();
        eventControl = container.event();
        eventBinder = instancePool.select(TestEventBinder.class).get();
        ConnectionFactory connectionFactory = instancePool.select(ConnectionFactory.class).get();
        connectionFactory.setHost(brokerSetup.getHost());
        connectionFactory.setPort(brokerSetup.getPort());
        publisher = new ConfirmedPublisher(connectionFactory);
    }

    @After
    public void after() throws IOException {
        brokerSetup.tearDown();
    }

    @Test
    public void testBindQueue() throws Exception {
        eventBinder.bind(TestEventOne.class).toQueue(TestSetup.TEST_QUEUE_ONE);
        eventBinder.bind(TestEventTwo.class).toQueue(TestSetup.TEST_QUEUE_TWO);
        eventBinder.initialize();

        publisher.publish(new Message().queue(TestSetup.TEST_QUEUE_ONE));
        publisher.publish(new Message().queue(TestSetup.TEST_QUEUE_ONE));
        publisher.publish(new Message().queue(TestSetup.TEST_QUEUE_TWO));
        publisher.publish(new Message().queue(TestSetup.TEST_QUEUE_TWO));
        publisher.publish(new Message().queue(TestSetup.TEST_QUEUE_TWO));

        // Wait until messages are completely consumed
        Thread.sleep(100);

        Assert.assertEquals(2, TestConsumer.countOne);
        Assert.assertEquals(3, TestConsumer.countTwo);
        Assert.assertEquals(5, TestConsumer.countAll);
    }

    @Test
    public void testBindExchange() throws Exception {

        eventBinder.bind(TestEventOne.class)
                .toExchange(TestSetup.TEST_EXCHANGE_ONE)
                .withRoutingKey(TestSetup.TEST_ROUTING_KEY_ONE)
                .withPublisherConfirms();
        eventBinder.bind(TestEventTwo.class)
                .toExchange(TestSetup.TEST_EXCHANGE_TWO)
                .withRoutingKey(TestSetup.TEST_ROUTING_KEY_TWO)
                .withPublisherConfirms();
        eventBinder.initialize();

        eventControl.select(TestEventOne.class).fire(new TestEventOne());
        eventControl.select(TestEventTwo.class).fire(new TestEventTwo());

        Thread.sleep(1000);

        brokerAssert.queueSize(TestSetup.TEST_QUEUE_ONE, 1);
        brokerAssert.queueSize(TestSetup.TEST_QUEUE_TWO, 1);
    }

}
