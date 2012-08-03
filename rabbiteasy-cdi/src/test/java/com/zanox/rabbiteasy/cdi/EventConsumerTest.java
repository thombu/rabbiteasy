package com.zanox.rabbiteasy.cdi;

import com.zanox.rabbiteasy.Message;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import java.nio.charset.Charset;

/**
 * @author christian.bick
 */
public class EventConsumerTest {

    WeldContainer container;

    @Before
    public void before() {
        TestConsumer.resetCount();
        container = new Weld().initialize();
    }

    @After
    public void after() {
        TestConsumer.resetCount();
    }

    @Test
    public void testHandleMessageShouldFireEvent() {
        EventConsumer eventConsumer = buildEventConsumer(TestEventOne.class);
        eventConsumer.handleMessage(new Message());
        Assert.assertEquals(1, TestConsumer.countOne);
        Assert.assertEquals(0, TestConsumer.countTwo);
        Assert.assertEquals(1, TestConsumer.countAll);
    }

    @Test
    public void testBuildEventShouldReturnEvent() {
        Message message = new Message();
        EventConsumer eventConsumer = buildEventConsumer(TestEventOne.class);
        TestEventOne event = (TestEventOne)eventConsumer.buildEvent(message);
        Assert.assertNotNull(event);
    }

    @Test
    public void testBuildEventShouldReturnSameData() {
        byte[] input = "abc".getBytes(Charset.forName("UTF-8"));
        Message message = new Message().body(input);
        EventConsumer eventConsumer = buildEventConsumer(TestEventContainsData.class);
        TestEventContainsData event = (TestEventContainsData)eventConsumer.buildEvent(message);
        byte[] output = event.getData();
        Assert.assertNotNull(output);
        Assert.assertArrayEquals(input, output);
    }

    @Test
    public void testBuildEventShouldReturnSameId() {
        Integer input = 12345;
        Message message = new Message().body(input);
        EventConsumer eventConsumer = buildEventConsumer(TestEventContainsId.class);
        TestEventContainsId event = (TestEventContainsId)eventConsumer.buildEvent(message);
        Integer output = event.getId();
        Assert.assertNotNull(output);
        Assert.assertEquals(input, output);
    }

    @Test
    public void testBuildEventShouldReturnSameContent() {
        String input = "test";
        Message message = new Message().body(input);
        EventConsumer eventConsumer = buildEventConsumer(TestEventContainsContent.class);
        TestEventContainsContent event = (TestEventContainsContent)eventConsumer.buildEvent(message);
        String output = event.getContent();
        Assert.assertNotNull(output);
        Assert.assertEquals(input, output);
    }

    @SuppressWarnings("unchecked")
    EventConsumer buildEventConsumer(Class<?> eventClass) {
        Event<Object> eventControl = (Event<Object>)container.event().select(eventClass);
        Instance<Object> eventPool = (Instance<Object>)container.instance().select(eventClass);
        return new EventConsumer(eventControl, eventPool);
    }

}
