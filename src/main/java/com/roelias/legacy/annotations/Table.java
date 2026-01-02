package com.roelias.legacy.annotations;

import java.lang.annotation.*;

/**
 * Specifies the database table name and schema for the annotated class.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
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
