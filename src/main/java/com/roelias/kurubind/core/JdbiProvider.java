package com.roelias.kurubind.core;

import org.jdbi.v3.core.Jdbi;

public interface JdbiProvider {
    Jdbi getJdbi();
}
