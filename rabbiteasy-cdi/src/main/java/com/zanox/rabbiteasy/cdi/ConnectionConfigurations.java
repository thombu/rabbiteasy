package com.zanox.rabbiteasy.cdi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Embraces a list of several {@link ConnectionConfiguration} to
 * enable different configuration profiles.</p>
 *
 * @see com.zanox.rabbiteasy.cdi.ConnectionConfiguration#profile()
 * @author christian.bick
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConnectionConfigurations {
    ConnectionConfiguration[] value();
}
