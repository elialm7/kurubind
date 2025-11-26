package com.roelias.kurubind.base;

import org.jdbi.v3.core.Jdbi;

public interface JdbiProvider {
    Jdbi getJdbi();
}
