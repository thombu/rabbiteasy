package com.zanox.rabbiteasy.cdi;

import com.rabbitmq.client.ConnectionFactory;
import com.zanox.rabbiteasy.consumer.ConsumerContainer;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author christian.bick
 */
public class ConsumerContainerProvider {

    @Inject
    ConnectionFactory connectionFactory;

    @Produces @Singleton
    public ConsumerContainer provideConsumerContainer() {
        return new ConsumerContainer(connectionFactory);
    }

}
