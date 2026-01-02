package com.roelias.kurubind.annotation;


import java.lang.annotation.*;

/**
 * Specifies the database table for an entity.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface Table {
    /**
     * Table name. Defaults to snake_case of class name.
     */
    String value() default "";

    /**
     * Optional schema name.
     */
    String schema() default "";
}