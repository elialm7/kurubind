package com.roelias.kurubind.core;

/**
 * SQL generation interface - dialect-specific implementations.
 */
public interface SqlGenerator {

    String generateInsertSql(TableMetadata metadata);

    String generateUpdateSql(TableMetadata metadata);

    String generateSelectSql(TableMetadata metadata);

    String generateDeleteSql(TableMetadata metadata);

    String generateCountSql(TableMetadata metadata);

    String generateExistsSql(TableMetadata metadata);


}
