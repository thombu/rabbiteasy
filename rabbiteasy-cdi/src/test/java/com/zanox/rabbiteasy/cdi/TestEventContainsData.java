package com.zanox.rabbiteasy.cdi;

/**
 * @author christian.bick
 */
public class TestEventContainsData implements ContainsData {

    private byte[] data;

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
