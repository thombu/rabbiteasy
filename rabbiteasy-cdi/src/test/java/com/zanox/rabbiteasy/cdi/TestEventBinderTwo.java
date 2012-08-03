package com.zanox.rabbiteasy.cdi;

/**
 * @author christian.bick
 */
@ConnectionConfigurations({
        @ConnectionConfiguration(host = "defaultHost", port=1234),
        @ConnectionConfiguration(profile="profileOne",  host = "hostOne", port=1111),
        @ConnectionConfiguration(profile="profileTwo",  host = "hostTwo", port=2222)
})
public class TestEventBinderTwo extends EventBinder {
    @Override
    protected void bindEvents() {
        // Just a Test
    }
}
