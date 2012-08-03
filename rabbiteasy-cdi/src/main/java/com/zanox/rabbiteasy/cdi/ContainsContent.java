package com.zanox.rabbiteasy.cdi;

/**
 * @author christian.bick
 */
public interface ContainsContent<T> {
    void setContent(T content);
    T getContent();
}
