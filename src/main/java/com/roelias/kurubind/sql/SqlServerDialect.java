package com.roelias.kurubind.sql;

import com.roelias.kurubind.metadata.FieldMetadata;
import com.roelias.kurubind.metadata.MetaEntity;

import java.util.StringJoiner;

/**
 * SQL Server dialect.
 */
public class SqlServerDialect implements SqlDialect {
    @Override
    public String quoteIdentifier(String id) {
        return "[" + id + "]";
    }

    @Override
    public String buildInsert(MetaEntity meta) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(quoteIdentifier(meta.tableName()));
        sql.append(" (");

        StringJoiner columns = new StringJoiner(", ");
        StringJoiner placeholders = new StringJoiner(", ");

        for (FieldMetadata field : meta.insertableFields()) {
            columns.add(quoteIdentifier(field.columnName()));
            placeholders.add(":" + field.fieldName());
        }

        sql.append(columns).append(") ");

        if (meta.hasGeneratedId()) {
            sql.append("OUTPUT INSERTED.").append(quoteIdentifier(meta.idField().columnName())).append(" ");
        }

        sql.append("VALUES (").append(placeholders).append(")");
        return sql.toString();
    }

    @Override
    public String buildUpdate(MetaEntity meta) {
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
    public String buildDelete(MetaEntity meta) {
        return "DELETE FROM " + quoteIdentifier(meta.tableName()) +
                " WHERE " + quoteIdentifier(meta.idField().columnName()) +
                " = :" + meta.idField().fieldName();
    }

    @Override
    public String buildSelectById(MetaEntity meta) {
        return "SELECT * FROM " + quoteIdentifier(meta.tableName()) +
                " WHERE " + quoteIdentifier(meta.idField().columnName()) +
                " = :" + meta.idField().fieldName();
    }

    @Override
    public String buildSelectAll(MetaEntity meta) {
        return "SELECT * FROM " + quoteIdentifier(meta.tableName());
    }

    @Override
    public String buildCount(MetaEntity meta) {
        return "SELECT COUNT(*) FROM " + quoteIdentifier(meta.tableName());
    }

    @Override
    public String buildExistsById(MetaEntity meta) {
        return "SELECT CASE WHEN EXISTS(SELECT 1 FROM " + quoteIdentifier(meta.tableName()) +
                " WHERE " + quoteIdentifier(meta.idField().columnName()) +
                " = :" + meta.idField().fieldName() + ") THEN 1 ELSE 0 END";
    }

    @Override
    public String buildPagination(int limit, int offset) {
        return String.format("OFFSET %d ROWS FETCH NEXT %d ROWS ONLY", offset, limit);
    }
}
