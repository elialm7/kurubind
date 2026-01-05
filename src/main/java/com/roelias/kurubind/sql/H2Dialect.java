package com.roelias.kurubind.sql;


import com.roelias.kurubind.metadata.EntityMetaData;
import com.roelias.kurubind.metadata.FieldMetadata;

import java.util.StringJoiner;

/**
 * H2 dialect.
 * H2 supports standard ANSI SQL with some PostgreSQL compatibility.
 */
public class H2Dialect implements SqlDialect {


    @Override
    public String quoteIdentifier(String id) {
        return "\"" + id + "\"";
    }

    @Override
    public String buildInsert(EntityMetaData meta) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(quoteIdentifier(meta.tableName()));
        sql.append(" (");

        StringJoiner columns = new StringJoiner(", ");
        StringJoiner placeholders = new StringJoiner(", ");

        for (FieldMetadata field : meta.insertableFields()) {
            columns.add(quoteIdentifier(field.columnName()));
            placeholders.add(":" + field.fieldName());
        }

        sql.append(columns).append(") VALUES (").append(placeholders).append(")");
        return sql.toString();
    }

    @Override
    public String buildUpdate(EntityMetaData meta) {
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(quoteIdentifier(meta.tableName()));
        sql.append(" SET ");

        StringJoiner sets = new StringJoiner(", ");
        for (FieldMetadata field : meta.updatableFields()) {
            sets.add(quoteIdentifier(field.columnName()) + " = :" + field.fieldName());
        }

        sql.append(sets);
        sql.append(" WHERE ").append(quoteIdentifier(meta.idField().columnName()));
        sql.append(" = :").append(meta.idField().fieldName());

        return sql.toString();
    }

    @Override
    public String buildDelete(EntityMetaData meta) {
        return "DELETE FROM " + quoteIdentifier(meta.tableName()) +
                " WHERE " + quoteIdentifier(meta.idField().columnName()) +
                " = :" + meta.idField().fieldName();
    }

    @Override
    public String buildSelectById(EntityMetaData meta) {
        return "SELECT * FROM " + quoteIdentifier(meta.tableName()) +
                " WHERE " + quoteIdentifier(meta.idField().columnName()) +
                " = :" + meta.idField().fieldName();
    }

    @Override
    public String buildSelectAll(EntityMetaData meta) {
        return "SELECT * FROM " + quoteIdentifier(meta.tableName());
    }

    @Override
    public String buildCount(EntityMetaData meta) {
        return "SELECT COUNT(*) FROM " + quoteIdentifier(meta.tableName());
    }

    @Override
    public String buildExistsById(EntityMetaData meta) {
        // H2 supports EXISTS but returns boolean, wrap in CASE for consistency
        return "SELECT CASE WHEN EXISTS(SELECT 1 FROM " + quoteIdentifier(meta.tableName()) +
                " WHERE " + quoteIdentifier(meta.idField().columnName()) +
                " = :" + meta.idField().fieldName() + ") THEN TRUE ELSE FALSE END";
    }

    @Override
    public String buildPagination(int limit, int offset) {
        return String.format("LIMIT %d OFFSET %d", limit, offset);
    }
}
