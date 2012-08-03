package com.zanox.rabbiteasy.publisher;

import com.zanox.rabbiteasy.Message;

import java.io.IOException;
import java.util.List;

/**
 * A message publisher publishes messages to a broker.
 * 
 * @author christian.bick
 * @author uwe.janner
 * @author soner.dastan
 *
 */
public interface MessagePublisher {

    /**
     * Sends a message to the broker using no delivery options.
     * 
     * @param message The message to be sent
     * @throws IOException if the message could not be sent
     */
    void send(Message message) throws IOException;

    /**
     * Sends a message to the broker using the given delivery options.
     * 
     * @param message The message to be sent
     * @param deliveryOptions The delivery options
     * @throws IOException if the message could not be sent
     */
    void send(Message message, DeliveryOptions deliveryOptions) throws IOException;
    
    /**
     * Closes the publisher by closing its underlying channel.
     * 
     * @throws IOException if the channel cannot be closed correctly. Usually occurs when the channel 
     * is already closing or is already closed.
     */
    void close() throws IOException;

    /**
     * Sends messages to the broker using no delivery options.
     *
     * @param messages The list of messages to send
     * @throws IOException if the message could not be sent
     */
    void send(List<Message> messages) throws IOException;

    /**
     * Sends messages to the broker using the given delivery options.
     *
     * @param messages The list of messages to send
     * @param deliveryOptions The delivery options
     * @throws IOException if the message could not be sent
     */
    void send(List<Message> messages, DeliveryOptions deliveryOptions) throws IOException;
}
