package com.roelias.kurubind.core;

import java.util.List;
import java.util.Optional;

/**
 * Query builder for custom queries.
 */
public interface QueryBuilder<E> {

    QueryBuilder<?> sql(String sql);

    QueryBuilder<E> bind(String name, Object value);

    List<E> list();

    Optional<E> findOne();

    E one();
}
