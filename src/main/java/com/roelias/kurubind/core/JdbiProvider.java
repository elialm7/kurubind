package com.roelias.kurubind.core;

import org.jdbi.v3.core.Jdbi;

/**
 * Provider for JDBI instances.
 */
public interface JdbiProvider {
    Jdbi getJdbi();
}
