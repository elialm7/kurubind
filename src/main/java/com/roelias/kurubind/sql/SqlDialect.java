package com.roelias.kurubind.sql;


import com.roelias.kurubind.metadata.MetaEntity;
import org.jdbi.v3.core.Handle;

import java.sql.SQLException;

/**
 * Interface for database-specific SQL generation.
 */
public interface SqlDialect {


    /**
     * Quote an identifier (table/column name) according to database rules.
     */
    String quoteIdentifier(String identifier);

    /**
     * Build INSERT statement.
     */
    String buildInsert(MetaEntity meta);

    /**
     * Build UPDATE statement.
     */
    String buildUpdate(MetaEntity meta);

    /**
     * Build DELETE statement.
     */
    String buildDelete(MetaEntity meta);

    /**
     * Build SELECT by ID statement.
     */
    String buildSelectById(MetaEntity meta);

    /**
     * Build SELECT all statement.
     */
    String buildSelectAll(MetaEntity meta);

    /**
     * Build COUNT statement.
     */
    String buildCount(MetaEntity meta);

    /**
     * Build EXISTS by ID statement.
     */
    String buildExistsById(MetaEntity meta);

    /**
     * Build pagination clause (LIMIT/OFFSET).
     */
    String buildPagination(int limit, int offset);

    /**
     * Detect dialect from database connection.
     */
    static SqlDialect detect(Handle handle) {
        try {
            String productName = handle.getConnection()
                    .getMetaData()
                    .getDatabaseProductName()
                    .toLowerCase();

            if (productName.contains("postgresql")) return new PostgresDialect();
            if (productName.contains("mysql") || productName.contains("mariadb")) return new MySqlDialect();
            if (productName.contains("h2")) return new H2Dialect();
            if (productName.contains("sqlite")) return new SqliteDialect();
            if (productName.contains("microsoft sql server") || productName.contains("sql server"))
                return new SqlServerDialect();

            return new GenericDialect();
        } catch (SQLException e) {
            return new GenericDialect();
        }
    }
}
