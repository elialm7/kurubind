package com.roelias.kurubind.mapper;

import com.roelias.kurubind.base.Dialect;
import com.roelias.kurubind.base.Handler;
import com.roelias.kurubind.metadata.EntityMetadata;
import com.roelias.kurubind.metadata.FieldMetadata;
import com.roelias.kurubind.registry.HandlerRegistry;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.lang.reflect.Type;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class KurubindRowMapper<T> implements RowMapper<T> {
    private final EntityMetadata metadata;
    private final HandlerRegistry handlerRegistry;
    private final Dialect dialect;
    private Set<String> columnsInResultSet;

    public KurubindRowMapper(EntityMetadata metadata, HandlerRegistry handlerRegistry, Dialect dialect) {
        this.metadata = metadata;
        this.handlerRegistry = handlerRegistry;
        this.dialect = dialect;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T map(ResultSet rs, StatementContext ctx) throws SQLException {
        T entity = (T) metadata.createInstance();

        if (columnsInResultSet == null) {
            columnsInResultSet = new HashSet<>();
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                columnsInResultSet.add(rsmd.getColumnLabel(i).toLowerCase());
            }
        }

        for (FieldMetadata field : metadata.getFields()) {
            String columnName = field.getColumnName();
            if (columnsInResultSet.contains(columnName.toLowerCase())) {

                // 1. Check if the user defined custom Handlers for this field
                // Custom handlers take priority over Jdbi default mappers
                List<Handler> handlers = handlerRegistry.getHandlersForField(field, dialect);

                if (!handlers.isEmpty()) {
                    // Existing Logic: Manual extraction + Handler processing
                    Object value = rs.getObject(columnName);
                    if (value != null) {
                        for (Handler h : handlers) {
                            value = h.handleRead(value, field);
                        }
                    }
                    setEntityValue(entity, field, value);
                } else {
                    // 2. JDBI Native Path
                    // Delegate to Jdbi to find a mapper for this specific type (e.g., UUID, Instant, JSON)
                    try {
                        Type targetType = field.getGenericType();
                        Optional<ColumnMapper<?>> mapper = ctx.findColumnMapperFor(targetType);

                        if (mapper.isPresent()) {
                            // Jdbi knows how to map this!
                            Object value = mapper.get().map(rs, columnName, ctx);
                            setEntityValue(entity, field, value);
                        } else {
                            // Fallback to raw object if Jdbi has no clue (rare for standard types)
                            Object value = rs.getObject(columnName);
                            setEntityValue(entity, field, value);
                        }
                    } catch (Exception e) {
                        throw new SQLException("Error mapping field '" + field.getFieldName() + "'", e);
                    }
                }
            }
        }
        return entity;
    }

    private void setEntityValue(T entity, FieldMetadata field, Object value) throws SQLException {
        try {
            field.setValue(entity, value);
        } catch (IllegalArgumentException e) {
            throw new SQLException(
                    "Mapping Error for field '" + field.getFieldName() +
                            "' of type '" + field.getFieldType().getName() +
                            "' with value '" + value +
                            "' of type '" + (value != null ? value.getClass().getName() : "null") + "'", e
            );
        }
    }


}
