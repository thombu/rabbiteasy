package com.zanox.rabbiteasy.cdi;

/**
 * @author christian.bick
 */
public class TestEventContainsId implements ContainsId<Integer> {

    private Integer id;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }
}
