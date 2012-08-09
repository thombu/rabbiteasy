package com.zanox.rabbiteasy.cdi;

/**
 * @author christian.bick
 */
public class TestEventContainsIdExtension implements ContainsIdExtension {
    @Override
    public void setId(Integer id) {
        // Mock
    }

    @Override
    public Integer getId() {
        // Mock
        return null;
    }
}
