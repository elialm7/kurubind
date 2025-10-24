package com.roelias.kurubind.core;

import org.jdbi.v3.core.mapper.RowMapper;
/**
 * Row mapping interface for result set conversion.
 */
public interface EntityMapper<E> {

    E mapRow(java.sql.ResultSet rs, TableMetadata metadata) throws java.sql.SQLException;

}
