package com.zanox.rabbiteasy.cdi;

/**
 * @author christian.bick
 */
public interface ContainsData {
    void setData(byte[] data);
    byte[] getData();
}
