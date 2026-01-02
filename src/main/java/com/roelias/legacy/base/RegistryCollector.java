package com.roelias.legacy.base;

import com.roelias.legacy.registry.HandlerRegistry;
import com.roelias.legacy.registry.SQLGeneratorRegistry;
import com.roelias.legacy.registry.ValidatorRegistry;
import com.roelias.legacy.registry.ValueGeneratorRegistry;

public class RegistryCollector {
    private final ValidatorRegistry validatorRegistry;
    private final HandlerRegistry handlerRegistry;
    private final ValueGeneratorRegistry valueGeneratorRegistry;
    private final SQLGeneratorRegistry sqlGeneratorRegistry;

    public RegistryCollector(
            ValidatorRegistry validatorRegistry,
            HandlerRegistry handlerRegistry,
            ValueGeneratorRegistry valueGeneratorRegistry,
            SQLGeneratorRegistry sqlGeneratorRegistry) {
        this.validatorRegistry = validatorRegistry;
        this.handlerRegistry = handlerRegistry;
        this.valueGeneratorRegistry = valueGeneratorRegistry;
        this.sqlGeneratorRegistry = sqlGeneratorRegistry;
    }

    public ValidatorRegistry validators() {
        return validatorRegistry;
    }

    public HandlerRegistry handlers() {
        return handlerRegistry;
    }

    public ValueGeneratorRegistry valueGenerators() {
        return valueGeneratorRegistry;
    }

    public SQLGeneratorRegistry sqlGenerators() {
        return sqlGeneratorRegistry;
    }
}
