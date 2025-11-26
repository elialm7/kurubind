package com.roelias.kurubind.registry;

import com.roelias.kurubind.base.Dialect;
import com.roelias.kurubind.base.SQLGenerator;
import com.roelias.kurubind.ootb.DefaultSQLGenerator;

import java.util.HashMap;
import java.util.Map;

public class SQLGeneratorRegistry {

    private final Map<Dialect, SQLGenerator> generators;
    private final SQLGenerator defaultGenerator;

    public SQLGeneratorRegistry() {
        this.generators = new HashMap<>();
        this.defaultGenerator = new DefaultSQLGenerator();
    }

    public void register(Dialect dialect, SQLGenerator generator) {
        generators.put(dialect, generator);
    }

    public SQLGenerator getGenerator(Dialect dialect) {
        if (dialect != null && generators.containsKey(dialect)) {
            return generators.get(dialect);
        }
        return defaultGenerator;
    }
}
