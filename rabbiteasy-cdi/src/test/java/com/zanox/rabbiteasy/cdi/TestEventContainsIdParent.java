package com.zanox.rabbiteasy.cdi;

/**
 * @author christian.bick
 */
public class TestEventContainsIdParent implements ContainsId<Integer> {
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
