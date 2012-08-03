package com.zanox.rabbiteasy.cdi;

/**
 * @author christian.bick
 */
public class TestEventContainsContent implements ContainsContent<String> {

    private String content;

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }
}
