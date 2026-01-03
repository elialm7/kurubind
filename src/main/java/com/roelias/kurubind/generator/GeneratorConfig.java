package com.roelias.kurubind.generator;

/**
 * Configuration for a generator extracted from @Generated annotation.
 */
public record GeneratorConfig(
        String generatorName,
        boolean onInsert,
        boolean onUpdate
) {
}
