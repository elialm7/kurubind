package com.roelias.kurubind.annotation;

import java.lang.annotation.*;

/**
 * Meta-annotation: Automatically set timestamp on INSERT.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Generated(value = "timestamp")
@Documented
public @interface CreatedAt {
}