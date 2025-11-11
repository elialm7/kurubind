package com.roelias.kurubind.core;

import com.roelias.kurubind.registry.HandlerRegistry;
import com.roelias.kurubind.registry.SQLGeneratorRegistry;
import com.roelias.kurubind.registry.ValidatorRegistry;
import com.roelias.kurubind.registry.ValueGeneratorRegistry;

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
