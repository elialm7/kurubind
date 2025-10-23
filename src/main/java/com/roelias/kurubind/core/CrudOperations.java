package com.roelias.kurubind.core;

import java.util.List;
import java.util.Optional;

/**
 * CRUD operations interface.
 */
public interface CrudOperations<E, ID> {

    long count();

    List<E> findAll();

    List<E> findAll(int limit, int offset);

    Optional<E> findById(ID id);

    boolean existsById(ID id);

    ID save(E entity);

    void saveAll(List<E> entities);

    boolean update(E entity);

    boolean deleteById(ID id);

    int deleteAll();
}
