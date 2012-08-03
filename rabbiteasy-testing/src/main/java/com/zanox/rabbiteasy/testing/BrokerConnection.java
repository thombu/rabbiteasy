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
    
    public static final String DEFAULT_HOST = "localhost";
    public static final int    DEFAULT_PORT = 5672;
    
    private static Map<String, Connection> connections = new HashMap<String, Connection>();
    
    public static Connection getConnection() {
        return getConnection(DEFAULT_HOST, DEFAULT_PORT);
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
