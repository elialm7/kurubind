package com.roelias.kurubind.core;

import org.jdbi.v3.core.Jdbi;

/**
 * Builder for creating KuruBind instances.
 */
public interface KuruBindBuilder <E, ID> {

    KuruBindBuilder<E, ID> withJdbi(Jdbi jdbi);

    KuruBindBuilder<E, ID> withProvider(JdbiProvider provider);

    KuruBindBuilder<E, ID> withConfiguration(KuruBindConfiguration config);

    KuruBindBuilder<E, ID> withDialect(String dialectName);

    KuruBind<E, ID> build();
}
