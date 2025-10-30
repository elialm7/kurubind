package com.roelias.kurubind.registry;

import com.roelias.kurubind.core.ValueGenerator;

import java.util.HashMap;
import java.util.Map;

public class ValueGeneratorRegistry {
    private final Map<String, ValueGenerator> generators;

    public ValueGeneratorRegistry() {
        this.generators = new HashMap<>();
    }

    public void register(String name, ValueGenerator generator) {
        generators.put(name, generator);
    }

    public ValueGenerator getGenerator(String name) {
        ValueGenerator generator = generators.get(name);
        if (generator == null) {
            throw new IllegalArgumentException("No ValueGenerator registered with name: " + name);
        }
        return generator;
    }

    public boolean hasGenerator(String name) {
        return generators.containsKey(name);
    }
}
