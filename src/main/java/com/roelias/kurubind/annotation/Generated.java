package com.roelias.kurubind.annotation;

import java.lang.annotation.*;

/**
 * Core lifecycle annotation for value generation.
 * Can be used directly or composed into meta-annotations.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Repeatable(Generated.List.class)
@Documented
public @interface Generated {
    /**
     * Generator name (registered in GeneratorRegistry).
     */
    String value();

    /**
     * Execute generator on INSERT.
     */
    boolean onInsert() default true;

    /**
     * Execute generator on UPDATE.
     */
    boolean onUpdate() default false;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @Documented
    @interface List {
        Generated[] value();
    }
}
