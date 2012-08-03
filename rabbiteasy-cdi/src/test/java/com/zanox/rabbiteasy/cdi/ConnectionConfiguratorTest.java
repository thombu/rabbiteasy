package com.zanox.rabbiteasy.cdi;

import com.rabbitmq.client.ConnectionFactory;
import junit.framework.Assert;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.Before;
import org.junit.Test;

import javax.enterprise.inject.Instance;

/**
 * @author christian.bick
 */
public class ConnectionConfiguratorTest {

    Instance<Object> instancePool;

    @Before
    public void before() {
        TestConsumer.resetCount();
        // Initializes the weld container
        WeldContainer container = new Weld().initialize();
        instancePool = container.instance();
    }

    @Test
    public void testConfigureFactoryForSingleConfig() {
        ConnectionConfigurator configurator = instancePool.select(ConnectionConfigurator.class).get();
        configurator.configureFactory(TestEventBinderOne.class);
        ConnectionFactory connectionFactory = configurator.connectionFactory;
        Assert.assertEquals("hostOne", connectionFactory.getHost());
        Assert.assertEquals("vHostOne", connectionFactory.getVirtualHost());
        Assert.assertEquals(1111, connectionFactory.getPort());
        Assert.assertEquals(111, connectionFactory.getRequestedFrameMax());
        Assert.assertEquals(11, connectionFactory.getRequestedHeartbeat());
        Assert.assertEquals(1, connectionFactory.getConnectionTimeout());
        Assert.assertEquals("userOne", connectionFactory.getUsername());
        Assert.assertEquals("passOne", connectionFactory.getPassword());
    }

    @Test
    public void testConfigureFactoryForMultiConfigsWithNoProfile() {
        ConnectionConfigurator configurator = instancePool.select(ConnectionConfigurator.class).get();
        configurator.configureFactory(TestEventBinderTwo.class);
        ConnectionFactory connectionFactory = configurator.connectionFactory;
        Assert.assertEquals("defaultHost", connectionFactory.getHost());
        Assert.assertEquals(1234, connectionFactory.getPort());
    }

    @Test
    public void testConfigureFactoryForMultiConfigsWithProfileOne() {
        System.setProperty(ConnectionConfiguration.PROFILE_PROPERTY, "profileOne");
        ConnectionConfigurator configurator = instancePool.select(ConnectionConfigurator.class).get();
        configurator.configureFactory(TestEventBinderTwo.class);
        ConnectionFactory connectionFactory = configurator.connectionFactory;
        Assert.assertEquals("hostOne", connectionFactory.getHost());
        Assert.assertEquals(1111, connectionFactory.getPort());
    }

    @Test
    public void testConfigureFactoryForMultiConfigsWithProfileTwo() {
        System.setProperty(ConnectionConfiguration.PROFILE_PROPERTY, "profileTwo");
        ConnectionConfigurator configurator = instancePool.select(ConnectionConfigurator.class).get();
        configurator.configureFactory(TestEventBinderTwo.class);
        ConnectionFactory connectionFactory = configurator.connectionFactory;
        Assert.assertEquals("hostTwo", connectionFactory.getHost());
        Assert.assertEquals(2222, connectionFactory.getPort());
    }
}
