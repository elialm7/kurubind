package com.roelias.kurubind.annotation;


import java.lang.annotation.*;

/**
 * Specifies the database column name for a field.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Documented
public @interface Column {
    /**
     * Column name. Defaults to snake_case of field name.
     */
    String value() default "";
}
