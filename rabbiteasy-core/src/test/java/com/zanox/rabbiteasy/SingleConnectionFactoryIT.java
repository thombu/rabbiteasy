package com.zanox.rabbiteasy;

import com.rabbitmq.client.Connection;
import com.zanox.rabbiteasy.testing.BrokerConnection;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SingleConnectionFactoryIT {

    private SingleConnectionFactory singleConnectionFactory;
    
    @Before
    public void before() {
        this.singleConnectionFactory = new SingleConnectionFactory();
        singleConnectionFactory.setHost(BrokerConnection.getDefaultHost());
        singleConnectionFactory.setPort(BrokerConnection.getDefaultPort());
    }
    
    @After
    public void after() {
        this.singleConnectionFactory.close();
    }
    
    @Test
    public void shouldNotifyAboutConnectionState() throws Exception {
        TestConnectionListener connectionListener = new TestConnectionListener();
        this.singleConnectionFactory.registerListener(connectionListener);
        Connection connection = this.singleConnectionFactory.newConnection();
        Assert.assertNotNull(connection);
        Assert.assertTrue(connection.isOpen());
        Assert.assertTrue(connectionListener.connectionEstablishedTriggered);
        connection.close();
        // Have a short sleep because connection closed notification happens asynchronously
        Thread.sleep(50);
        Assert.assertTrue(connectionListener.connectionLostTriggered);
        connection = this.singleConnectionFactory.newConnection();
        Assert.assertNotNull(connection);
        Assert.assertTrue(connectionListener.connectionEstablishedTriggered);
        Assert.assertTrue(connection.isOpen());
        this.singleConnectionFactory.close();
        Assert.assertTrue(connectionListener.connectionClosedTriggered);
    }

    @Test
    public void shouldReturnSameConnection() throws Exception {
        Connection connectionOne = singleConnectionFactory.newConnection();
        Connection connectionTwo = singleConnectionFactory.newConnection();
        Assert.assertTrue(connectionOne == connectionTwo);
    }
    
    @Test
    public void shouldReconnect() throws Exception {
        Connection connection = this.singleConnectionFactory.newConnection();
        Assert.assertNotNull(connection);
        Assert.assertTrue(connection.isOpen());
        connection.close();
        connection = this.singleConnectionFactory.newConnection();
        Assert.assertNotNull(connection);
        Assert.assertTrue(connection.isOpen());
    }
    
    private static class TestConnectionListener implements ConnectionListener {

        volatile boolean connectionEstablishedTriggered;
        volatile boolean connectionLostTriggered;
        volatile boolean connectionClosedTriggered;

        @Override
        public void onConnectionEstablished(Connection connection) {
            connectionEstablishedTriggered = true;
        }

        @Override
        public void onConnectionLost(Connection connection) {
            connectionLostTriggered = true;
        }

        @Override
        public void onConnectionClosed(Connection connection) {
            connectionClosedTriggered = true;
        }
    }
}
