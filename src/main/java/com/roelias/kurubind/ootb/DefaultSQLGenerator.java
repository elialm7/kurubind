package com.roelias.kurubind.ootb;

import com.roelias.kurubind.base.SQLGenerator;
import com.roelias.kurubind.metadata.EntityMetadata;
import com.roelias.kurubind.metadata.FieldMetadata;

import java.util.List;
import java.util.stream.Collectors;

public class DefaultSQLGenerator implements SQLGenerator {

    @Override
    public String generateInsert(EntityMetadata meta, List<FieldMetadata> fields) {
        String tableName = getTableName(meta);
        String columns = getColumnsList(fields);
        String placeholders = getPlaceholdersList(fields);

        String baseSql =
                String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);
        return customizeInsert(baseSql, meta, fields);
    }

    @Override
    public String generateUpdate(EntityMetadata meta, List<FieldMetadata> fields) {
        String tableName = getTableName(meta);
        String setClause = getSetClause(fields);
        String whereClause = getWhereClauseById(meta);

        String baseSql =
                String.format("UPDATE %s SET %s WHERE %s", tableName, setClause, whereClause);
        return customizeUpdate(baseSql, meta, fields);
    }

    @Override
    public String generateDelete(EntityMetadata meta) {
        String tableName = getTableName(meta);
        String whereClause = getWhereClauseById(meta);

        String baseSql = String.format("DELETE FROM %s WHERE %s", tableName, whereClause);
        return customizeDelete(baseSql, meta);
    }

    @Override
    public String generateSelect(EntityMetadata meta) {
        String tableName = getTableName(meta);
        String columns = getSelectColumns(meta);

        String baseSql = String.format("SELECT %s FROM %s", columns, tableName);
        return customizeSelect(baseSql, meta);
    }

    @Override
    public String generateSelectById(EntityMetadata meta) {
        String tableName = getTableName(meta);
        String columns = getSelectColumns(meta);
        String whereClause = getWhereClauseById(meta);

        String baseSql =
                String.format("SELECT %s FROM %s WHERE %s", columns, tableName, whereClause);
        return customizeSelectById(baseSql, meta);
    }

    @Override
    public String generateCount(EntityMetadata meta) {
        String tableName = getTableName(meta);

        String baseSql = String.format("SELECT COUNT(*) FROM %s", tableName);
        return customizeCount(baseSql, meta);
    }

    protected String getTableName(EntityMetadata meta) {
        return meta.getFullTableName();
    }

    protected String getColumnsList(List<FieldMetadata> fields) {
        return fields.stream().map(FieldMetadata::getColumnName).collect(Collectors.joining(", "));
    }

    protected String getPlaceholdersList(List<FieldMetadata> fields) {
        return fields.stream().map(this::getPlaceholder).collect(Collectors.joining(", "));
    }

    protected String getSetClause(List<FieldMetadata> fields) {
        return fields.stream()
                .filter(f -> !f.isId())
                .map(f -> f.getColumnName() + " = " + getPlaceholder(f))
                .collect(Collectors.joining(", "));
    }

    protected String getWhereClauseById(EntityMetadata meta) {
        FieldMetadata idField = meta.getIdField();
        return idField.getColumnName() + " = :" + idField.getColumnName();
    }

    protected String getSelectColumns(EntityMetadata meta) {
        return "*";
    }

    @Override
    public String getPlaceholder(FieldMetadata field) {
        return ":" + field.getColumnName();
    }

    protected String customizeInsert(
            String baseSql, EntityMetadata meta, List<FieldMetadata> fields) {
        return baseSql;
    }

    protected String customizeUpdate(
            String baseSql, EntityMetadata meta, List<FieldMetadata> fields) {
        return baseSql;
    }

    protected String customizeDelete(String baseSql, EntityMetadata meta) {
        return baseSql;
    }

    protected String customizeSelect(String baseSql, EntityMetadata meta) {
        return baseSql;
    }

    protected String customizeSelectById(String baseSql, EntityMetadata meta) {
        return baseSql;
    }

    protected String customizeCount(String baseSql, EntityMetadata meta) {
        return baseSql;
    }
}
