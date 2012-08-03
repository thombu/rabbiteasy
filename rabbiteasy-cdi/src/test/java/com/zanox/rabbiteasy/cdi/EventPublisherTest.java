package com.zanox.rabbiteasy.cdi;

import com.rabbitmq.client.*;
import com.zanox.rabbiteasy.Message;
import com.zanox.rabbiteasy.publisher.DeliveryOptions;
import com.zanox.rabbiteasy.publisher.PublisherReliability;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.api.easymock.annotation.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import java.nio.charset.Charset;
import java.util.UUID;

import static org.easymock.EasyMock.*;

/**
 * @author christian.bick
 */
@RunWith(PowerMockRunner.class)
public class EventPublisherTest {

    public static final String TEST_EXCHANGE = "lib.test.exchange";
    public static final String TEST_ROUTING_KEY = "lib.test.routingKey";

    public static final EventPublisher.PublisherConfiguration TEST_CONFIGURATION = new EventPublisher.PublisherConfiguration(
            TEST_EXCHANGE,
            TEST_ROUTING_KEY,
            false,
            PublisherReliability.NONE,
            DeliveryOptions.NONE,
            MessageProperties.BASIC
    );

    @Mock
    ConnectionFactory connectionFactory;
    @Mock
    Connection connection;
    @Mock
    Channel channel;

    EventPublisher eventPublisher;

    @Before
    public void before()  throws Exception {
        expect(connectionFactory.newConnection()).andReturn(connection).anyTimes();
        expect(connection.createChannel()).andReturn(channel).anyTimes();
        expect(channel.isOpen()).andReturn(false).anyTimes();
        channel.close();
        expectLastCall().anyTimes();
        eventPublisher = new EventPublisher(connectionFactory);
    }

    @Test
    public void testPublishEventShouldPublishEmptyMessage() throws Exception {

        TestEventOne remoteEventOne = new TestEventOne();
        eventPublisher.addEvent(remoteEventOne.getClass(), TEST_CONFIGURATION);

        channel.basicPublish(eq(TEST_EXCHANGE), eq(TEST_ROUTING_KEY), eq(false), eq(false),
                anyObject(AMQP.BasicProperties.class), aryEq(new byte[0]));
        expectLastCall().once();
        PowerMock.replayAll();
        eventPublisher.publishEvent(new TestEventOne());

        PowerMock.verifyAll();
    }

    @Test
    public void buildMessageShouldReturnSameInteger() {
        final int input = 300;
        Message message = EventPublisher.buildMessage(TEST_CONFIGURATION,
                new ContainsId<Integer>() {
                    @Override
                    public void setId(Integer id) { }
                    @Override
                    public Integer getId() {
                        return input;
                    }
                }
        );
        int output = message.getBodyAs(Integer.class);
        Assert.assertEquals(input, output);
    }

    @Test
    public void buildMessageShouldReturnSameLong() {
        final long input = 3000115550607l;
        Message message = EventPublisher.buildMessage(TEST_CONFIGURATION,
                new ContainsId<Long>() {
                    @Override
                    public void setId(Long id) { }
                    @Override
                    public Long getId() {
                        return input;
                    }
                }
        );
        long output = message.getBodyAs(Long.class);
        Assert.assertEquals(input, output);
    }

    @Test
    public void buildMessageShouldReturnSameUUID() {
        final String input = UUID.randomUUID().toString();
        Message message = EventPublisher.buildMessage(TEST_CONFIGURATION,
                new ContainsId<String>() {
                    @Override
                    public void setId(String id) {}
                    @Override
                    public String getId() {
                        return input;
                    }
                }
        );
        String output = message.getBodyAs(String.class);
        Assert.assertEquals(input, output);
    }

    @Test
    public void buildMessageShouldReturnSameText() {
        final String input = "testText";
        Message message = EventPublisher.buildMessage(TEST_CONFIGURATION,
                new ContainsContent<String>() {
                    @Override
                    public void setContent(String content) { }
                    @Override
                    public String getContent() {
                        return input;
                    }
                }
        );
        String output = message.getBodyAs(String.class);
        Assert.assertEquals(input, output);
    }

    @Test
    public void buildMessageShouldReturnSameData() {
        final byte[] input = "testText".getBytes(Charset.forName("UTF-8"));
        Message message = EventPublisher.buildMessage(TEST_CONFIGURATION,
                new ContainsData() {
                    @Override
                    public void setData(byte[] data) {}
                    @Override
                    public byte[] getData() {
                        return input;
                    }
                }
        );
        byte[] output = message.getBodyContent();
        Assert.assertArrayEquals(input, output);
    }
}
