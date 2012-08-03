package com.zanox.rabbiteasy.cdi;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.MessageProperties;
import com.zanox.rabbiteasy.consumer.ConsumerContainer;
import com.zanox.rabbiteasy.publisher.DeliveryOptions;
import com.zanox.rabbiteasy.publisher.PublisherReliability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Binds incoming CDI events to queues and outgoing CDI events to exchanges of a broker.</p>
 *
 * <p>Inherit from this class and override its {@link #bindEvents()} method to create
 * bindings.</p>
 *
 * <p><b>Queue example:</b></p>
 *
 * <pre>
 * protected void bindEvents() {
 *     bind(MyEventOne.class).toQueue("myQueueOne");
 *     bind(MyEventTwo.class).toQueue("myQueueTwo").autoAck();
 * }
 * </pre>
 *
 * <p><b>Exchange example:</b></p>
 *
 * <pre>
 * protected void bindEvents() {
 *     bind(MyEvent.class).toExchange("myExchange")
 *          .withRoutingKey("myRoutingKey")
 *          .withPublisherTransactions();
 * }
 * </pre>
 *
 * <p>To initialize the event bindings, inject the instance of this class
 * and call {@link #initialize}. In a web application, you would normally do
 * this in a context listener on application startup <b>after</b> your CDI
 * framework was initialized.</p>
 *
 * @author christian.bick
 */
@Singleton
public abstract class EventBinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBinder.class);

    @Inject
    Event<Object> remoteEventControl;
    @Inject
    Instance<Object> remoteEventPool;
    @Inject
    ConsumerContainer consumerContainer;
    @Inject
    EventPublisher eventPublisher;
    @Inject
    ConnectionConfigurator connectionConfigurator;

    BindingPipeline bindingPipeline = new BindingPipeline();

    protected abstract void bindEvents();

    public void initialize() throws IOException {
        bindEvents();
        connectionConfigurator.configureFactory(getClass());
        processQueueBindings();
        consumerContainer.startAllConsumers();
        processExchangeBindings();
    }

    void processExchangeBindings() {
        for (ExchangeBinding exchangeBinding : bindingPipeline.exchangeBindings) {
            bindExchange(exchangeBinding);
        }
        bindingPipeline.exchangeBindings.clear();
    }

    void processQueueBindings() {
        for (QueueBinding queueBinding : bindingPipeline.queueBindings) {
            bindQueue(queueBinding);
        }
        bindingPipeline.queueBindings.clear();
    }

    void bindQueue(final QueueBinding queueBinding) {
        @SuppressWarnings("unchecked")
        Event<Object> eventControl = (Event<Object>)remoteEventControl.select(queueBinding.eventType);
        @SuppressWarnings("unchecked")
        Instance<Object> eventPool = (Instance<Object>)remoteEventPool.select(queueBinding.eventType);
        EventConsumer consumer = new EventConsumer(eventControl, eventPool);
        consumerContainer.addConsumer(consumer, queueBinding.queue, queueBinding.autoAck);
        LOGGER.info("Binding between queue {} and event type {} activated",
                queueBinding.queue, queueBinding.eventType.getSimpleName());
    }

    void bindExchange(ExchangeBinding exchangeBinding)  {
        EventPublisher.PublisherConfiguration configuration = new EventPublisher.PublisherConfiguration(
                exchangeBinding.exchange,
                exchangeBinding.routingKey,
                exchangeBinding.persistent,
                exchangeBinding.reliability,
                exchangeBinding.deliveryOptions,
                exchangeBinding.basicProperties
        );
        eventPublisher.addEvent(exchangeBinding.eventType, configuration);
        LOGGER.info("Binding between exchange {} and event type {} activated",
                exchangeBinding.exchange, exchangeBinding.eventType.getSimpleName());
    }

    public EventBindingBuilder bind (Class<?> event) {
        return new EventBindingBuilder(event);
    }

    private static class BindingPipeline {
        private Set<QueueBinding> queueBindings =
                new HashSet<QueueBinding>();
        private Set<ExchangeBinding> exchangeBindings =
                new HashSet<ExchangeBinding>();
    }

    public class EventBindingBuilder {

        private Class<?> eventType;

        private EventBindingBuilder(Class<?> eventType) {
            this.eventType = eventType;
        }

        public QueueBinding toQueue(String queue) {
            return new QueueBinding(eventType, queue);
        }

        public ExchangeBinding toExchange(String exchange) {
            return new ExchangeBinding(eventType, exchange);
        }
    }

    public class QueueBinding {

        private Class<?> eventType;
        private String queue;
        private boolean autoAck = false;

        public QueueBinding(Class<?> eventType, String queue) {
            this.eventType = eventType;
            this.queue = queue;
            bindingPipeline.queueBindings.add(this);
            LOGGER.info("Binding created between queue {} and event type {}", queue, eventType.getSimpleName());
        }

        public QueueBinding autoAck() {
            this.autoAck = true;
            LOGGER.info("Auto acknowledges enabled for event type {}", eventType.getSimpleName());
            return this;
        }

    }

    public class ExchangeBinding {

        private Class<?> eventType;
        private String exchange;
        private String routingKey;
        private boolean persistent;
        private PublisherReliability reliability = PublisherReliability.NONE;
        private DeliveryOptions deliveryOptions = DeliveryOptions.NONE;
        private AMQP.BasicProperties basicProperties = MessageProperties.BASIC;

        public ExchangeBinding(Class<?> eventType, String exchange) {
            this.eventType = eventType;
            this.exchange = exchange;
            bindingPipeline.exchangeBindings.add(this);
            LOGGER.info("Binding created between exchange {} and event type {}", exchange, eventType.getSimpleName());
        }

        public ExchangeBinding withRoutingKey(String routingKey) {
            this.routingKey = routingKey;
            LOGGER.info("Routing key for event type {} set to {}", eventType.getSimpleName(), routingKey);
            return this;
        }

        public ExchangeBinding withPersistentMessages() {
            this.persistent = true;
            LOGGER.info("Persistent messages enabled for event type {}", eventType.getSimpleName());
            return this;
        }

        /**
         * @see PublisherReliability#TRANSACTIONAL
         */
        public ExchangeBinding withPublisherTransactions() {
            return setPublisherReliability(PublisherReliability.TRANSACTIONAL);
        }

        /**
         * @see PublisherReliability#CONFIRMED
         */
        public ExchangeBinding withPublisherConfirms() {
            return setPublisherReliability(PublisherReliability.CONFIRMED);
        }

        private ExchangeBinding setPublisherReliability(PublisherReliability reliability) {
            if (this.reliability != PublisherReliability.NONE) {
                LOGGER.warn("Publisher reliability for event type {} is overridden: {}", eventType.getSimpleName(), reliability);
            }
            this.reliability = reliability;
            LOGGER.info("Publisher reliability for event type {} set to {}", eventType.getSimpleName(), reliability);
            return this;
        }

        /**
         * @see DeliveryOptions#IMMEDIATE
         */
        public ExchangeBinding withImmediateDelivery() {
            return setDeliveryOptions(DeliveryOptions.IMMEDIATE);
        }

        /**
         * @see DeliveryOptions#MANDATORY
         */
        public ExchangeBinding withMandatoryDelivery() {
            return setDeliveryOptions(DeliveryOptions.MANDATORY);
        }

        private ExchangeBinding setDeliveryOptions(DeliveryOptions deliveryOptions) {
            if (this.deliveryOptions != DeliveryOptions.NONE) {
                LOGGER.warn("Delivery options for event type {} are overridden: {}", eventType.getSimpleName(), deliveryOptions);
            }
            this.deliveryOptions = deliveryOptions;
            LOGGER.info("Delivery options for event type {} set to {}", eventType.getSimpleName(), deliveryOptions);
            return this;
        }

        public ExchangeBinding withProperties(AMQP.BasicProperties basicProperties) {
            this.basicProperties = basicProperties;
            LOGGER.info("Publisher properties for event type {} set to {}", eventType.getSimpleName(), basicProperties.toString());
            return this;
        }

    }

}
