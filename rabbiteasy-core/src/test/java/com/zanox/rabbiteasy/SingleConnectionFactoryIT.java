package com.zanox.rabbiteasy;

import com.rabbitmq.client.Connection;
import com.zanox.rabbiteasy.testing.BrokerConnection;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SingleConnectionFactoryIT {

    SingleConnectionFactory singleConnectionFactory;
    
    @Before
    public void before() {
        singleConnectionFactory = new SingleConnectionFactory();
        singleConnectionFactory.setHost(BrokerConnection.getDefaultHost());
        singleConnectionFactory.setPort(BrokerConnection.getDefaultPort());
    }
    
    @After
    public void after() {
        singleConnectionFactory.close();
    }
    
    @Test
    public void shouldNotifyAboutConnectionState() throws Exception {
        TestConnectionListener connectionListener = new TestConnectionListener();
        singleConnectionFactory.registerListener(connectionListener);
        Connection connection = singleConnectionFactory.newConnection();
        Assert.assertNotNull(connection);
        Assert.assertTrue(connection.isOpen());
        Assert.assertTrue(connectionListener.connectionEstablishedTriggered);
        connection.close();
        // Have a short sleep because connection closed notification happens asynchronously
        Thread.sleep(50);
        Assert.assertTrue(connectionListener.connectionLostTriggered);
        connection = singleConnectionFactory.newConnection();
        Assert.assertNotNull(connection);
        Assert.assertTrue(connectionListener.connectionEstablishedTriggered);
        Assert.assertTrue(connection.isOpen());
        singleConnectionFactory.close();
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
        Connection connection = singleConnectionFactory.newConnection();
        Assert.assertNotNull(connection);
        Assert.assertTrue(connection.isOpen());
        singleConnectionFactory.setPort(15345);
        connection.close();
        Thread.sleep(SingleConnectionFactory.CONNECTION_ESTABLISH_INTERVAL_IN_MS + SingleConnectionFactory.CONNECTION_TIMEOUT_IN_MS  * 2);
        singleConnectionFactory.setPort(BrokerConnection.getDefaultPort());
        Thread.sleep(SingleConnectionFactory.CONNECTION_ESTABLISH_INTERVAL_IN_MS + SingleConnectionFactory.CONNECTION_TIMEOUT_IN_MS  * 2);
        connection = singleConnectionFactory.newConnection();
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
