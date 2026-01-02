package com.roelias.legacy.base;

import org.jdbi.v3.core.Jdbi;

public interface JdbiProvider {
    Jdbi getJdbi();
}
