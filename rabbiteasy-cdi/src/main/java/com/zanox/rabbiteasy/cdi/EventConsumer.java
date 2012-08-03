package com.zanox.rabbiteasy.cdi;

import com.zanox.rabbiteasy.Message;
import com.zanox.rabbiteasy.consumer.MessageConsumer;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import java.lang.reflect.ParameterizedType;

/**
 * @author christian.bick
 */
public class EventConsumer extends MessageConsumer {

    private Event<Object> eventControl;
    private Instance<Object> eventPool;

    public EventConsumer(Event<Object> eventControl, Instance<Object> eventPool) {
        this.eventControl = eventControl;
        this.eventPool = eventPool;
    }

    @Override
    public void handleMessage(Message message) {
        Object event = buildEvent(message);
        eventControl.fire(event);
    }

    @SuppressWarnings("unchecked")
    Object buildEvent(Message message) {
        Object event = eventPool.get();
        if (event instanceof ContainsData) {
            ((ContainsData) event).setData(message.getBodyContent());
        } else if (event instanceof ContainsContent) {
            Class<?> parameterType = getParameterType(event);
            ((ContainsContent) event).setContent(message.getBodyAs(parameterType));
        } else if (event instanceof ContainsId) {
            Class<?> parameterType = getParameterType(event);
            ((ContainsId) event).setId(message.getBodyAs(parameterType));
        }
        return event;
    }

    static Class<?> getParameterType(Object object) {
        ParameterizedType parameterizedType = (ParameterizedType) object.getClass().getGenericInterfaces()[0];
        return (Class<?>) parameterizedType.getActualTypeArguments()[0];
    }
}
