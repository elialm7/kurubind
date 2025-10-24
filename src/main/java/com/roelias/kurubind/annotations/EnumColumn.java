package com.roelias.kurubind.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumColumn {

    EnumType value() default EnumType.STRING;

    enum EnumType {
        STRING,
        ORDINAL,
        CODE
    }
}
