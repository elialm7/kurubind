package com.roelias.legacy.annotations;

import java.lang.annotation.*;

/**
 * Marks a class as a kurubind entity
 * Classes with this annotation will be handled byu kurubindRowMapper
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
public @interface KuruBind {

}
