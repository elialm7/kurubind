package com.roelias.kurubind.ootb;

import com.roelias.kurubind.core.SQLGenerator;
import com.roelias.kurubind.metadata.EntityMetadata;
import com.roelias.kurubind.metadata.FieldMetadata;

import java.util.List;
import java.util.stream.Collectors;

public class GenericSQLGenerator implements SQLGenerator {
   /* @Override
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
    }*/



    // =========================================================================
    // MÉTODOS PRINCIPALES (Template Methods)
    // =========================================================================

    @Override
    public String generateInsert(EntityMetadata meta, List<FieldMetadata> fields) {
        String tableName = getTableName(meta);
        String columns = getColumnsList(fields);
        String placeholders = getPlaceholdersList(fields);

        String baseSql = String.format(
                "INSERT INTO %s (%s) VALUES (%s)",
                tableName, columns, placeholders
        );

        // Hook: Permitir customización (ej: RETURNING, ON CONFLICT)
        return customizeInsert(baseSql, meta, fields);
    }

    @Override
    public String generateUpdate(EntityMetadata meta, List<FieldMetadata> fields) {
        String tableName = getTableName(meta);
        String setClause = getSetClause(fields);
        String whereClause = getWhereClauseById(meta);

        String baseSql = String.format(
                "UPDATE %s SET %s WHERE %s",
                tableName, setClause, whereClause
        );

        // Hook: Permitir customización
        return customizeUpdate(baseSql, meta, fields);
    }

    @Override
    public String generateDelete(EntityMetadata meta) {
        String tableName = getTableName(meta);
        String whereClause = getWhereClauseById(meta);

        String baseSql = String.format(
                "DELETE FROM %s WHERE %s",
                tableName, whereClause
        );

        // Hook: Permitir customización
        return customizeDelete(baseSql, meta);
    }

    @Override
    public String generateSelect(EntityMetadata meta) {
        String tableName = getTableName(meta);
        String columns = getSelectColumns(meta);

        String baseSql = String.format(
                "SELECT %s FROM %s",
                columns, tableName
        );

        // Hook: Permitir customización
        return customizeSelect(baseSql, meta);
    }

    @Override
    public String generateSelectById(EntityMetadata meta) {
        String tableName = getTableName(meta);
        String columns = getSelectColumns(meta);
        String whereClause = getWhereClauseById(meta);

        String baseSql = String.format(
                "SELECT %s FROM %s WHERE %s",
                columns, tableName, whereClause
        );

        // Hook: Permitir customización
        return customizeSelectById(baseSql, meta);
    }

    @Override
    public String generateCount(EntityMetadata meta) {
        String tableName = getTableName(meta);

        String baseSql = String.format(
                "SELECT COUNT(*) FROM %s",
                tableName
        );

        // Hook: Permitir customización
        return customizeCount(baseSql, meta);
    }

    // =========================================================================
    // MÉTODOS AUXILIARES REUTILIZABLES
    // =========================================================================

    /**
     * Obtiene el nombre completo de la tabla (con schema si aplica).
     * Sobrescribir para customizar formato de tabla.
     */
    protected String getTableName(EntityMetadata meta) {
        return meta.getFullTableName();
    }

    /**
     * Obtiene la lista de columnas separadas por coma.
     */
    protected String getColumnsList(List<FieldMetadata> fields) {
        return fields.stream()
                .map(FieldMetadata::getColumnName)
                .collect(Collectors.joining(", "));
    }

    /**
     * Obtiene la lista de placeholders separados por coma.
     * Sobrescribir para agregar casts específicos.
     */
    protected String getPlaceholdersList(List<FieldMetadata> fields) {
        return fields.stream()
                .map(this::getPlaceholder)
                .collect(Collectors.joining(", "));
    }

    /**
     * Genera el SET clause para UPDATE.
     * Sobrescribir para customizar formato.
     */
    protected String getSetClause(List<FieldMetadata> fields) {
        return fields.stream()
                .filter(f -> !f.isId())
                .map(f -> f.getColumnName() + " = " + getPlaceholder(f))
                .collect(Collectors.joining(", "));
    }

    /**
     * Genera el WHERE clause usando el ID.
     */
    protected String getWhereClauseById(EntityMetadata meta) {
        FieldMetadata idField = meta.getIdField();
        return idField.getColumnName() + " = :" + idField.getColumnName();
    }

    /**
     * Obtiene las columnas para SELECT.
     * Por defecto "*", pero puede sobrescribirse para columnas específicas.
     */
    protected String getSelectColumns(EntityMetadata meta) {
        return "*";
    }

    // =========================================================================
    // PLACEHOLDER (el más importante para customizar)
    // =========================================================================

    /**
     * Genera el placeholder para un campo.
     *
     * Este es el método MÁS IMPORTANTE para sobrescribir en dialectos específicos.
     *
     * Ejemplos de customización:
     * - PostgreSQL: agregar ::jsonb para campos JSON
     * - MySQL: agregar funciones específicas
     * - Oracle: usar formato de bind diferente
     *
     * @param field El campo para el cual generar el placeholder
     * @return El placeholder (ej: ":columnName" o ":columnName::jsonb")
     */
    @Override
    public String getPlaceholder(FieldMetadata field) {
        return ":" + field.getColumnName();
    }

    // =========================================================================
    // HOOKS DE CUSTOMIZACIÓN (Template Method Pattern)
    // =========================================================================

    /**
     * Hook para customizar el INSERT SQL.
     *
     * Casos de uso:
     * - PostgreSQL: agregar RETURNING
     * - PostgreSQL: agregar ON CONFLICT (UPSERT)
     * - MySQL: agregar ON DUPLICATE KEY UPDATE
     *
     * @param baseSql SQL base generado
     * @param meta Metadata de la entidad
     * @param fields Lista de campos
     * @return SQL customizado
     */
    protected String customizeInsert(String baseSql, EntityMetadata meta, List<FieldMetadata> fields) {
        return baseSql;
    }

    /**
     * Hook para customizar el UPDATE SQL.
     */
    protected String customizeUpdate(String baseSql, EntityMetadata meta, List<FieldMetadata> fields) {
        return baseSql;
    }

    /**
     * Hook para customizar el DELETE SQL.
     */
    protected String customizeDelete(String baseSql, EntityMetadata meta) {
        return baseSql;
    }

    /**
     * Hook para customizar el SELECT SQL.
     */
    protected String customizeSelect(String baseSql, EntityMetadata meta) {
        return baseSql;
    }

    /**
     * Hook para customizar el SELECT BY ID SQL.
     */
    protected String customizeSelectById(String baseSql, EntityMetadata meta) {
        return baseSql;
    }

    /**
     * Hook para customizar el COUNT SQL.
     */
    protected String customizeCount(String baseSql, EntityMetadata meta) {
        return baseSql;
    }
}
