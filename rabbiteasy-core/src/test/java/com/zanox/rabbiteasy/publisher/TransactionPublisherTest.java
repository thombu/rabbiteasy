package com.zanox.rabbiteasy.publisher;

import com.zanox.rabbiteasy.Message;
import org.junit.Test;
import org.powermock.api.easymock.PowerMock;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

public class TransactionPublisherTest extends MessagePublisherTest {

	@Test
	public void shouldSendMessageAndWaitForAck() throws Exception {
		Message message = new Message().exchange(TEST_EXCHANGE).routingKey(TEST_ROUTING_KEY);
		TransactionalPublisher publisher = new TransactionalPublisher(connectionFactory);

		mockConnectionOperations();
		expect(channel.txSelect()).andReturn(null);
		expectLastCall().once();
		channel.basicPublish(TEST_EXCHANGE, TEST_ROUTING_KEY, false, false, message.getBasicProperties(), message.getBodyContent());
		expectLastCall().once();
		expect(channel.txCommit()).andReturn(null);
		expectLastCall().once();
		PowerMock.replayAll();

		publisher.publish(message);

		PowerMock.verifyAll();
	}

}
