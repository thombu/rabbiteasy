package com.zanox.rabbiteasy.cdi;

import com.zanox.rabbiteasy.Message;
import com.zanox.rabbiteasy.consumer.MessageConsumer;

import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

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
            Class<ContainsContent> parameterType = getParameterType(event, ContainsContent.class);
            ((ContainsContent) event).setContent(message.getBodyAs(parameterType));
        } else if (event instanceof ContainsId) {
            Class<ContainsId> parameterType = getParameterType(event, ContainsId.class);
            ((ContainsId) event).setId(message.getBodyAs(parameterType));
        }
        return event;
    }

    @SuppressWarnings("unchecked")
    static <T> Class<T> getParameterType(Object object, Class<T> expectedType) {
        Collection<Class<?>> extendedAndImplementedTypes =
                getExtendedAndImplementedTypes(object.getClass(), new LinkedList<Class<?>>());

        for  (Class<?> type : extendedAndImplementedTypes) {
            Type[] implementedInterfaces  = type.getGenericInterfaces();
            for (Type implementedInterface : implementedInterfaces) {
                if (implementedInterface instanceof ParameterizedType) {
                    ParameterizedType parameterizedCandidateType = (ParameterizedType) implementedInterface;
                    if (parameterizedCandidateType.getRawType().equals(expectedType)) {
                        Type[] typeArguments = parameterizedCandidateType.getActualTypeArguments();
                        Type typeArgument;
                        if (typeArguments.length == 0) {
                            typeArgument = Object.class;
                        } else {
                            typeArgument = parameterizedCandidateType.getActualTypeArguments()[0];
                        }
                        return (Class<T>) typeArgument;
                    }
                }

            }
        }
        // This may never happen if the caller checked if object instanceof expectedType
        throw new RuntimeException("Expected type " + expectedType +
                " is not in class hierarchy of " + object.getClass());
    }

    static List<Class<?>> getExtendedAndImplementedTypes(Class<?> clazz, List<Class<?>> hierarchy) {
        if (clazz.equals(Object.class)) {
            return hierarchy;
        }
        hierarchy.add(clazz);
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            hierarchy = getExtendedAndImplementedTypes(superClass, hierarchy);
        }
        for (Class<?> implementedInterface : clazz.getInterfaces()) {
            hierarchy = getExtendedAndImplementedTypes(implementedInterface, hierarchy);
        }
        return hierarchy;
    }
}
