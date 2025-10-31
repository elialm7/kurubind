package com.roelias.kurubind.ootb;

import com.roelias.kurubind.core.SQLGenerator;
import com.roelias.kurubind.metadata.EntityMetadata;
import com.roelias.kurubind.metadata.FieldMetadata;

import java.util.List;
import java.util.stream.Collectors;

public class GenericSQLGenerator implements SQLGenerator {
    @Override
    public String generateInsert(EntityMetadata meta, List<FieldMetadata> fields) {
        String tableName = meta.getFullTableName();

        String columns = fields.stream()
                .map(FieldMetadata::getColumnName)
                .collect(Collectors.joining(", "));

        String placeholders = fields.stream()
                .map(this::getPlaceholder)
                .collect(Collectors.joining(", "));

        return String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columns, placeholders);
    }

    @Override
    public String generateUpdate(EntityMetadata meta, List<FieldMetadata> fields) {
        String tableName = meta.getFullTableName();

        String setClause = fields.stream()
                .filter(f -> !f.isId())
                .map(f -> f.getColumnName() + " = " + getPlaceholder(f))
                .collect(Collectors.joining(", "));

        FieldMetadata idField = meta.getIdField();
        return String.format("UPDATE %s SET %s WHERE %s = :%s",
                tableName, setClause, idField.getColumnName(), idField.getColumnName());
    }

    @Override
    public String generateDelete(EntityMetadata meta) {
        FieldMetadata idField = meta.getIdField();
        return String.format("DELETE FROM %s WHERE %s = :%s",
                meta.getFullTableName(), idField.getColumnName(), idField.getColumnName());
    }

    @Override
    public String generateSelect(EntityMetadata meta) {
        return "SELECT * FROM " + meta.getFullTableName();
    }

    @Override
    public String getPlaceholder(FieldMetadata field) {
        return ":" + field.getColumnName();
    }

    @Override
    public String generateSelectById(EntityMetadata meta) {
        return "SELECT * FROM " + meta.getFullTableName() +
                " WHERE " + meta.getIdField().getColumnName() + " = :id";
    }

    @Override
    public String generateCount(EntityMetadata meta) {
        return "SELECT COUNT(*) FROM " + meta.getFullTableName();
    }
}
