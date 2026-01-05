package com.roelias.kurubind.mapper;

import com.roelias.kurubind.annotation.Kurubind;
import com.roelias.kurubind.exception.MappingException;
import com.roelias.kurubind.metadata.EntityMetaData;
import com.roelias.kurubind.metadata.EntityMetadataCache;
import com.roelias.kurubind.metadata.FieldMetadata;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.mapper.RowMapperFactory;
import org.jdbi.v3.core.statement.StatementContext;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * High-performance RowMapper using MethodHandles.
 * Integrates with Jdbi's ColumnMapper system for plugin support.
 */
public class KurubindRowMapper<T> implements RowMapper<T> {
    private final EntityMetaData metadata;


    public KurubindRowMapper(Class<T> entity) {
        this.metadata = EntityMetadataCache.get(entity);
    }

    @Override
    public T map(ResultSet rs, StatementContext ctx) throws SQLException {
        try {
            if (metadata.isRecord()) {
                return mapRecord(rs, ctx);
            } else {
                return mapPojo(rs, ctx);
            }
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new MappingException(
                    "Failed to map result to " + metadata.entityClass().getName(),
                    e
            );
        }
    }


    /**
     * Map ResultSet to a Record (immutable class with canonical constructor).
     */
    @SuppressWarnings("unchecked")
    private T mapRecord(ResultSet rs, StatementContext ctx) throws Exception {
        Object[] args = new Object[metadata.fields().size()];

        for (int i = 0; i < metadata.fields().size(); i++) {
            FieldMetadata field = metadata.fields().get(i);
            args[i] = mapColumn(rs, field, ctx);
        }

        return (T) metadata.constructor().newInstance(args);
    }

    /**
     * Map ResultSet to a POJO (mutable class with setters).
     */
    @SuppressWarnings("unchecked")
    private T mapPojo(ResultSet rs, StatementContext ctx) throws Exception {
        T instance = (T) metadata.constructor().newInstance();

        for (FieldMetadata field : metadata.fields()) {
            Object value = mapColumn(rs, field, ctx);
            if (value != null || hasColumn(rs, field.columnName())) {
                field.setValue(instance, value);
            }
        }

        return instance;
    }

    /**
     * Map a single column to a value.
     * First tries Jdbi's ColumnMapper (for plugin integration),
     * then falls back to direct JDBC access.
     */
    private Object mapColumn(ResultSet rs, FieldMetadata field, StatementContext ctx)
            throws SQLException {

      /*
        if (!hasColumn(rs, field.columnName())) {
            return null;
        }


        var jdbiMapper = ctx.getConfig(ColumnMappers.class)
                .findFor(field.type());

        if (jdbiMapper.isPresent()) {
            return jdbiMapper.get().map(rs, field.columnName(), ctx);
        }


        Object value = rs.getObject(field.columnName());
        if (value == null) {
            return null;
        }


        if (field.type().isInstance(value)) {
            return value;
        }

        // Try to convert
        return convertValue(value, field.type());*/


        // Column not present → behave like Jdbi (return null)
        if (!hasColumn(rs, field.columnName())) {
            return null;
        }

        ColumnMapper<?> mapper = ctx
                .findColumnMapperFor(field.type())
                .orElseThrow(() -> new MappingException(
                        "No ColumnMapper registered for type " + field.type().getName()
                                + " (column '" + field.columnName() + "')"
                ));

        return mapper.map(rs, field.columnName(), ctx);
    }

    /**
     * Check if a column exists in the result set.
     */
    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            rs.findColumn(columnName);
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Basic type conversion (can be extended).
     */
    /*
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // Primitive type conversions
        if (targetType == int.class || targetType == Integer.class) {
            return ((Number) value).intValue();
        } else if (targetType == long.class || targetType == Long.class) {
            return ((Number) value).longValue();
        } else if (targetType == double.class || targetType == Double.class) {
            return ((Number) value).doubleValue();
        } else if (targetType == float.class || targetType == Float.class) {
            return ((Number) value).floatValue();
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            }
            return Boolean.parseBoolean(value.toString());
        } else if (targetType == String.class) {
            return value.toString();
        }

        return value;
    }*/

    public static class Factory implements RowMapperFactory {
        @Override
        public Optional<RowMapper<?>> build(Type type, ConfigRegistry config) {

            if (!(type instanceof Class<?> cls)) {
                return Optional.empty();
            }
            if (!cls.isAnnotationPresent(Kurubind.class)) {
                return Optional.empty();
            }
            return Optional.of(new KurubindRowMapper<>(cls));
        }
    }

}
