package com.roelias.kurubind.base;

public class Dialect {
    private final String name;

    public Dialect(String name) {
        this.name = name.toUpperCase();
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dialect dialect = (Dialect) o;
        return name.equals(dialect.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Dialect{" + name + "}";
    }
}
