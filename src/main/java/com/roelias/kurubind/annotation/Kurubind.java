package com.roelias.kurubind.annotation;

import java.lang.annotation.*;

/**
 * Marks a class as a Kurubind entity.
 * Classes with this annotation will be handled by the kurubind rowmapper.
 */

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Kurubind {
}
