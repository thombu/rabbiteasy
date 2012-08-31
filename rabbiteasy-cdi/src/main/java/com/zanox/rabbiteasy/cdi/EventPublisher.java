package com.zanox.rabbiteasy.cdi;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import com.zanox.rabbiteasy.Message;
import com.zanox.rabbiteasy.publisher.DeliveryOptions;
import com.zanox.rabbiteasy.publisher.GenericPublisher;
import com.zanox.rabbiteasy.publisher.MessagePublisher;
import com.zanox.rabbiteasy.publisher.PublisherReliability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Publishes events to exchanges of a broker.
 *
 * @author christian.bick
 */
@Singleton
public class EventPublisher {

    private static Logger LOGGER = LoggerFactory.getLogger(EventPublisher.class);

    ConnectionFactory connectionFactory;

    Map<Class<?>, PublisherConfiguration> publisherConfigurations =
            new HashMap<Class<?>, PublisherConfiguration>();

    @Inject
    public EventPublisher(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Adds events of the given type to the CDI events to which the event publisher listens in order to
     * publish them. The publisher configuration is used to decide where to and how to publish messages.
     *
     * @param eventType The event type
     * @param configuration The configuration used when publishing and event
     * @param <T> The event type
     */
    public <T> void addEvent(Class<T> eventType, PublisherConfiguration configuration) {
        publisherConfigurations.put(eventType, configuration);
    }

    /**
     * Observes CDI events for remote events and publishes those events if their event type
     * was added before.
     *
     * @param event The event to publish
     * @throws IOException if the event failed to be published
     */
    public void publishEvent(@Observes Object event) throws IOException {
        Class<?> eventType = event.getClass();
        LOGGER.debug("Receiving event of type {}", eventType.getSimpleName());
        if (! publisherConfigurations.containsKey(eventType)) {
            LOGGER.debug("No publisher configured for event of type {}", eventType.getSimpleName());
            return;
        }
        PublisherConfiguration publisherConfiguration = publisherConfigurations.get(eventType);
        Message message = buildMessage(publisherConfiguration, event);
        MessagePublisher messagePublisher = new GenericPublisher(connectionFactory, publisherConfiguration.reliability);
        try {
            LOGGER.info("Publishing event of type {}", eventType.getSimpleName());
            messagePublisher.publish(message, publisherConfiguration.deliveryOptions);
            LOGGER.info("Successfully published event of type {}", eventType.getSimpleName());
        } catch (IOException e) {
            LOGGER.error("Failed to publish event {}", eventType.getSimpleName(), e);
            throw e;
        } finally {
            messagePublisher.close();
        }
    }

    /**
     * A publisher configuration stores all important settings and options used for publishing and event.
     *
     * @author christian.bick
     */
    public static class PublisherConfiguration {
        public PublisherConfiguration(String exchange, String routingKey, Boolean persistent,
                                      PublisherReliability reliability, DeliveryOptions deliveryOptions,
                                      AMQP.BasicProperties basicProperties) {
            this.exchange = exchange;
            this.routingKey = routingKey;
            this.persistent = persistent;
            this.reliability = reliability;
            this.deliveryOptions = deliveryOptions;
            this.basicProperties = basicProperties;
        }

        String exchange;
        String routingKey;
        Boolean persistent;
        PublisherReliability reliability;
        DeliveryOptions deliveryOptions;
        AMQP.BasicProperties basicProperties;
    }

    /**
     * Builds a message based on a CDI event and its publisher configuration.
     *
     * @param publisherConfiguration The publisher configuration
     * @param event The CDI event
     * @return The message
     */
    static Message buildMessage(PublisherConfiguration publisherConfiguration, Object event) {
        Message message = new Message(publisherConfiguration.basicProperties)
                .exchange(publisherConfiguration.exchange)
                .routingKey(publisherConfiguration.routingKey);
        if (publisherConfiguration.persistent) {
            message.persistent();
        }
        if (event instanceof ContainsData) {
            message.body(((ContainsData) event).getData());
        } else if (event instanceof ContainsContent) {
            message.body(((ContainsContent) event).getContent());
        } else if (event instanceof ContainsId) {
            message.body(((ContainsId) event).getId());
        }
        return message;
    }
}
