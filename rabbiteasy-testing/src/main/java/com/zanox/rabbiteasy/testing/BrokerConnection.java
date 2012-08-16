package com.zanox.rabbiteasy.testing;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A broker connection provides one single connection for each broker
 * involved in a test.
 * 
 * @author christian.bick
 *
 */
public class BrokerConnection {

    public static final String PROPERTY_HOST = "rabbiteasy.test.host";
    public static final String PROPERTY_PORT = "rabbiteasy.test.port";
    public static final String DEFAULT_HOST = "localhost";
    public static final int    DEFAULT_PORT = 5672;
    
    private static Map<String, Connection> connections = new HashMap<String, Connection>();
    
    public static Connection getConnection() {
        String host = System.getProperty(PROPERTY_HOST) == null ? DEFAULT_HOST : System.getProperty(PROPERTY_HOST);
        int port = System.getProperty(PROPERTY_PORT) == null ? DEFAULT_PORT : Integer.valueOf(System.getProperty(PROPERTY_PORT));
        return getConnection(host, port);
    }
    
    public static synchronized Connection getConnection(String host, int port) {
        String factoryKey = host + ":" + port;
        try {
            if (!connections.containsKey(factoryKey)) {
                ConnectionFactory connectionFactory = new ConnectionFactory();
                connectionFactory.setHost(host);
                connectionFactory.setPort(port);
                connections.put(factoryKey, connectionFactory.newConnection());
            }
            return connections.get(factoryKey);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create a new connection for " + factoryKey, e);
        }
    }

}
