package com.roelias.kurubind.generator;

import com.roelias.kurubind.exception.GeneratorException;
import com.roelias.kurubind.metadata.FieldMetadata;
import com.roelias.kurubind.ootb.TimestampGenerator;
import org.jdbi.v3.core.Handle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for value generators.
 * Thread-safe and supports custom generators registration.
 */
public class GeneratorRegistry {


    private static final Map<String, ValueGenerator> generators = new ConcurrentHashMap<>();

    static {
        register("timestamp", new TimestampGenerator());
    }

    /**
     * Register a custom generator.
     *
     * @param name      Generator name (used in @Generated annotation)
     * @param generator Generator implementation
     */
    public static void register(String name, ValueGenerator generator) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Generator name cannot be null or empty");
        }
        if (generator == null) {
            throw new IllegalArgumentException("Generator cannot be null");
        }
        generators.put(name, generator);
    }

    /**
     * Get a registered generator by name.
     *
     * @param name Generator name
     * @return Generator implementation
     * @throws GeneratorException if generator not found
     */
    public static ValueGenerator get(String name) {
        ValueGenerator generator = generators.get(name);
        if (generator == null) {
            throw new GeneratorException("Generator not found: " + name);
        }
        return generator;
    }

    /**
     * Check if a generator is registered.
     */
    public static boolean exists(String name) {
        return generators.containsKey(name);
    }

    /**
     * Unregister a generator (useful for testing).
     */
    public static void unregister(String name) {
        generators.remove(name);
    }

    /**
     * Apply generators to an entity for INSERT operation.
     *
     * @param entity Entity to apply generators to
     * @param field  Field metadata with generator configs
     * @param handle Jdbi Handle for database access
     */
    public static void applyInsertGenerators(
            Object entity,
            FieldMetadata field,
            Handle handle
    ) {
        for (GeneratorConfig config : field.getGeneratorsForInsert()) {
            try {
                ValueGenerator generator = get(config.generatorName());
                Object value = generator.generate(entity, field, handle);
                field.setValue(entity, value);
            } catch (Exception e) {
                throw new GeneratorException(
                        String.format(
                                "Failed to apply generator '%s' to field '%s'",
                                config.generatorName(),
                                field.fieldName()
                        ),
                        e
                );
            }
        }
    }

    /**
     * Apply generators to an entity for UPDATE operation.
     *
     * @param entity Entity to apply generators to
     * @param field  Field metadata with generator configs
     * @param handle Jdbi Handle for database access
     */
    public static void applyUpdateGenerators(
            Object entity,
            FieldMetadata field,
            Handle handle
    ) {
        for (GeneratorConfig config : field.getGeneratorsForUpdate()) {
            try {
                ValueGenerator generator = get(config.generatorName());
                Object value = generator.generate(entity, field, handle);
                field.setValue(entity, value);
            } catch (Exception e) {
                throw new GeneratorException(
                        String.format(
                                "Failed to apply generator '%s' to field '%s'",
                                config.generatorName(),
                                field.fieldName()
                        ),
                        e
                );
            }
        }
    }
}
