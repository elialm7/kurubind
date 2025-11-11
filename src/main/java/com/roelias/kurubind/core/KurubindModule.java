package com.roelias.kurubind.core;

@FunctionalInterface
public interface KurubindModule {
    void configure(RegistryCollector registries);
}
