# Introduction 

## Purpose

RabbitEasy is a library for easily integrating [RabbitMQ](http://rabbitmq.com) into your Java infrastructure. It is built on top
of the official RabbitMQ Java client and improves realization of many common scenarios for Java SE and EE
applications.

## Overview

### Core

- connection factory for long living single connections
- managed simple, confirmed and transactional publishers that recover from connection loss
- managed consumers that recover from connection loss and re-attach to the broker

Get it from [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22rabbiteasy-core%22)
and read the [documentation](https://github.com/zanox/rabbiteasy#core-1)

### CDI

- convenient integration for JEE6/CDI applications
- publishing of AMQP messages to exchanges for CDI events
- consuming of AMQP messages from queues as CDI events  

Get it from [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22rabbiteasy-cdi%22)
and read the [documentation](https://github.com/zanox/rabbiteasy#cdi-1)

### Testing

- convenient broker definition setup and tear down
- convenient asserts on current broker state

Get it from [Maven Central](http://search.maven.org/#search%7Cga%7C1%7Ca%3A%22rabbiteasy-testing%22)

# Core

## Connection Factory

A single connection factory always provides the same connection on calling newConnection() as long as the connection
persists. A new connection is established as soon as the current connection is lost.

SingleConnectionFactory extends ConnectionFactory from the RabbitMQ standard library and is used just
the same way as the factory from the standard library. The only difference: From now on you don't have
to care about too many connections being established to a broker any more.

Creating a single connection factory:

```Java
ConnectionFactory connectionFactory = new SingleConnectionFactory();
connectionFactory.setHost("example.com");
connectionFactory.setPort(4224);
```

## Messages

A message object was introduced to provide convenient and save configuration of a message and to improve the way
how message content is written and read. A message is created using a builder pattern.

Creating a message without properties:

```Java
Message message = new Message()
        .exchange("my.exchange")
        .routingKey("my.routing.key")
        .body("My message content");
```

Creating a message with properties:

```Java
AMQP.BasicProperties basicProperties = new AMQP.BasicProperties.Builder()
        .messageId("1")
        .build();

Message message = new Message(basicProperties)
        .exchange("my.exchange")
        .routingKey("my.routing.key")
        .body("My message content");
```

Note: Setting the body will also automatically adjust the content type and encoding in the message properties.

Also, a convenient method is provided to read a message's content and to transform it into the desired Java
type directly.

Reading content from a message:

```Java
Message message = new Message()
        .body("123");

String contentAsString = message.getBodyAs(String.class);
Integer contentAsInteger = message.getBodyAs(Integer.class);
Long contentAsLong = message.getBodyAs(Long.class);
```

Publishing a message:

```Java
ConnectionFactory connectionFactory = new SingleConnectionFactory();
Channel channel = connectionFactory.newConnection().createChannel();

Message message = new Message()
        .exchange("my.exchange")
        .routingKey("my.routing.key")
        .body("My message content")
        .publish(channel);
```

Managing connections and channels oneself easily becomes complex and repatitive.
Message publishers manage connections and channels themselves and provide a convenient way of 
publishing messages without having to take care of scenarios like connection aborts.

## Message Publishers

### Simple Publisher

A simple publisher publishes messages in a fire-and-forget manner. Publishing messages this way, there is no guarantee
that messages reach there destination queues. Choose this publisher for sending messages that are of low importance.

Publishing a message with a simple publisher:

```Java
ConnectionFactory connectionFactory = new SingleConnectionFactory();

Message message = new Message()
        .exchange("my.exchange")
        .body("My message content");

Publisher publisher = new SimplePublisher(connectionFactory);
publisher.publish(message);
publisher.close();
```

Publishing multiple messages with a simple publisher:

```Java
ConnectionFactory connectionFactory = new SingleConnectionFactory();

Message messageOne = new Message()
        .exchange("my.exchange")
        .body("My message one");
Message messageTwo = new Message()
        .exchange("my.exchange")
        .body("My message two");

List<Message> messageList = new ArrayList<Message>();
messageList.add(messageOne);
messageList.add(messageTwo);

MessagePublisher publisher = new SimplePublisher(connectionFactory);
publisher.publish(messageList);
publisher.close();
```

### Confirmed Publisher

Confirmed publishers are used to publish messages so that every single message is confirmed by the broker to have
reached all its destination queues. Choose this publisher for publishing messages that are of importance but
where delivery can fail independently of other messages.

Initializing a confirmed publisher:

```Java
MessagePublisher publisher = new ConfirmedPublisher(connectionFactory);
```

### Transactional Publisher

Transactional publishers are used to publish a set of messages for which a delivery shall succeed for all messages or
none. Choose this publisher for publishing multiple messages for which it is important that delivery succeeds for
all or none.

Initializing a transactional publisher:

```Java
MessagePublisher publisher = new TransactionalPublisher(connectionFactory);
```

### Generic Publisher

Generic publishers can be used to publish messages with different reliability constraints, depending on the
initialization parameter. This is useful in situations where your implementation is very generic and where 
reliability depends on the actual use case.

Initializing a generic publisher using publisher confirms:

```Java
PublisherReliability reliability = PublisherReliability.CONFIRMED;
GenericPublisher publisher = new GenericPublisher(connectionFactory, reliability);
```

## Consumers

### Message consumer

The counter part of a message publisher is the message consumer. Extending the MessageConsumer class and
implementing the handleMessage() method is the purposed way of consuming messages.

Extending the message consumer:

```Java
class MyConsumer extends MessageConsumer {
    @Override
    public void handleMessage(Message message) {
        String messageContent = message.getBodyAsString();
        System.out.println(messageContent);
    }
}
```

### Consumer container

A consumer container is provided to manage the consumers in a central component.
The container ensures that its contained consumers stay alive, meaning that it reestablishes lost connections and
recreates its channels. Also, the container provides the possibility to monitor the actual consumer's status and to
enabled and disable certain consumers.

Initializing a consumer container and adding a consumer bound to "my.queue":

```Java
ConnectionFactory connectionFactory = new SingleConnectionFactory();
ConsumerContainer consumerContainer = new ConsumerContainer(connectionFactory);
consumerContainer.addConsumer(new MyConsumer(), "my.queue");
consumerContainer.startAllConsumers();
```

Adding an auto-acknowledging consumer:

```Java
ConnectionFactory connectionFactory = new SingleConnectionFactory();
ConsumerContainer consumerContainer = new ConsumerContainer(connectionFactory);
consumerContainer.addConsumer(new MyConsumer(), "my.queue", true); // <- last parameter indicates auto-ack
consumerContainer.startAllConsumers();
```

# CDI

## Using event binders

Trying to integrate AMQP and RabbitMQ into JEE with JMS is a rocky road with many compromises. This is,
why we suggest to integrate RabbitMQ into JEE via bindings between CDI events and message brokers:

- to fire CDI events remotely, bind them to be published as messages to broker exchanges 
- to observe CDI events remotely, bind them to be consumed as messages from broker queues

You could also look at it the other way round:

- to publish messages to broker exchanges, bind them to fired CDI events
- to consume messages from broker queues, bind them to observed CDI events

To bind events, first create a subclass of EventBinder and override its bindEvents() method:

```Java
public class MyEventBinder extends EventBinder {
    @Override
    protected void bindEvents() {
        // Your event bindings
    }
}
```

## Binding events to exchanges

This is how you bind an event to an exchange to publish it:

```Java
public class MyEventBinder extends EventBinder {
    @Override
    protected void bindEvents() {
        bind(MyEvent.class).toExchange("my.exchange").withRoutingKey("my.routing.Key");
    }
}
```

Firing a CDI event is going to publish a message to the given exchange and routing key:

```Java
public class MyEventSource {
    @Inject MyEvent event;
    @Inject Event<MyEvent> eventControl;

    public void testEventFiring() {
        eventControl.fire(event);
    }
}
```

This is going to publish the fired event to local observers of MyEvent and is also going to publish a message to
the exchange "my.exchange" with routing key "my.routing.Key" as we have defined it in the binding.

## Binding events to queues

Binding an event to a queue for consuming events works the same:

```Java
public class MyEventBinder extends EventBinder {
    @Override
    protected void bindEvents() {
        bind(MyEvent.class).toQueue("my.queue");
    }
}
```

Now, CDI observers of the bound event are going to consume messages from "my.queue" in form of the bound event:

```Java
public class MyEventObserver {
    public void testEventObserving(@Observes MyEvent event) {
        // Processing of an event
    }
}
```

## Connection configuration

Per default, localhost and the standard AMQP port 5672 are used to establish connections. You can
configure the used connection for your binder via annotations:

```Java
@ConnectionConfiguration(host = "my.host", port=1337)
public class MyEventBinder extends EventBinder {
    @Override
    protected void bindEvents() {
        // Your event bindings
    }
}
```

You can also define multiple connection configurations which can be enabled and disabled with profiles.
The system property "rabbiteasy.profile" can be used to define the profile name.

In the example below, three profiles are defined: One for a staging and one for a quality environment. If none
of those profiles is given in the system property then the first configuration is taken because it has no
profile property and is such is treated as default configuration:

```Java
@ConnectionConfigurations({
        @ConnectionConfiguration(host = "live.host"),
        @ConnectionConfiguration(profile="staging", host = "staging.host"),
        @ConnectionConfiguration(profile="quality", host = "quality.host")
})
public class MyEventBinder extends EventBinder {
    @Override
    protected void bindEvents() {
        // Your event bindings
    }
}
```

## Binder initialization

To enable your bindings, inject an instance of your event binder and call its initialize() method. Here is
an example of how to enable an event binder in a servlet context listener:

```Java
public class MyServletContextListener implements ServletContextListener  {
    @Inject MyEventBinder eventBinder;

    public void contextInitialized(ServletContextEvent e) {
        eventBinder.initialize();
    }

}
```

Important: Ensure that your CDI provider is already initialized at this point.

## Events with content

Interfaces on events are used to make the framework aware of content that shall be published with
or consumed from a message. The framework recognizes the existence of those interfaces on your event
classes and takes care of message body serialization and deserialization.

### Adding casual content

To transport content within an event, implement the ContainsContent interface and specify of which type the
content is in the Generic Parameter. This way, the framework knows how to serialize and deserialize the
event content:

```Java
public class MyEvent implements ContainsContent<String> {
    private String content;
    
    @Override
    public void setContent(String content) {
        this.content = content;
    }
    @Override
    public String getContent() {
        return content;
    }
}
```

Strings and primitives are (de)serialized from/to their textual representation. All other types are (de)serialized
from/to their XML representation if not specified differently in the bindings.

### Adding identifiers as content

Because transporting IDs in events is so common, we also added a ContainsId interface that behaves exactly
the same as ContainsContent but with different naming, so your code stays more readable:

```Java
public class MyEvent implements ContainsId<Integer> {
    private Integer id;
    
    @Override
    public Integer getId() {
        return id;
    }
    @Override
    public void setId(Integer id) {
        this.id = id;
    }
}
```

### Adding rare data as content

To transport rare data like binary files within an event, implement the ContainsData interface:

```Java
public class MyEvent implements ContainsData {
    private byte[] data;
    
    @Override
    public byte[] getData() {
        return data;
    }
    @Override
    public void setData(byte[] data) {
        this.data = data;
    }
}
```

## Example application

For a full example, have a look at our [example application](https://github.com/zanox/rabbitordering).