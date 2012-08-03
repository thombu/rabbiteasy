package com.zanox.rabbiteasy.cdi;

/**
 * @author christian.bick
 */
@ConnectionConfiguration(host = "hostOne", virtualHost = "vHostOne", port=1111, frameMax = 111,
        heartbeat = 11, timeout = 1, username = "userOne", password = "passOne")
public class TestEventBinderOne extends EventBinder {
    @Override
    protected void bindEvents() {
        // Just a Test
    }
}
