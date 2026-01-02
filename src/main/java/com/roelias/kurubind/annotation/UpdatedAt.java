package com.roelias.kurubind.annotation;

import java.lang.annotation.*;

/**
 * Meta-annotation: Automatically set timestamp on INSERT and UPDATE.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Generated(value = "timestamp", onUpdate = true)
@Documented
public @interface UpdatedAt {
}