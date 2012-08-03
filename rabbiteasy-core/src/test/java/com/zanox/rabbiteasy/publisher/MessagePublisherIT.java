package com.zanox.rabbiteasy.publisher;

import com.zanox.rabbiteasy.SingleConnectionFactory;
import com.zanox.rabbiteasy.TestBrokerSetup;
import com.zanox.rabbiteasy.testing.BrokerAssert;
import org.junit.After;
import org.junit.Before;


public abstract class MessagePublisherIT {

    protected SingleConnectionFactory singleConnectionFactory;
    protected TestBrokerSetup brokerSetup;
    protected BrokerAssert brokerAssert;

    @Before
    public void beforeAll() throws Exception {
        brokerAssert = new BrokerAssert();
        brokerSetup = new TestBrokerSetup();
        brokerSetup.prepareSimpleTest();
        singleConnectionFactory = new SingleConnectionFactory();
        singleConnectionFactory.setHost(brokerSetup.getHost());
        singleConnectionFactory.setPort(brokerSetup.getPort());
    }

    @After
    public void after() throws Exception {
        brokerSetup.tearDown();
        singleConnectionFactory.close();
    }

}
