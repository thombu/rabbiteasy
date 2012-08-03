package com.zanox.rabbiteasy.cdi;

/**
 * @author christian.bick
 */
public interface ContainsId<T> {
    void setId(T id);
    T getId();
}
