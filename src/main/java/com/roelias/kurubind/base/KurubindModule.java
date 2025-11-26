package com.roelias.kurubind.base;

@FunctionalInterface
public interface KurubindModule {
    void configure(RegistryCollector registries);
}
