package com.roelias.kurubind.mapper;

import com.roelias.kurubind.core.Dialect;
import com.roelias.kurubind.core.Handler;
import com.roelias.kurubind.metadata.EntityMetadata;
import com.roelias.kurubind.metadata.FieldMetadata;
import com.roelias.kurubind.registry.HandlerRegistry;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KurubindRowMapper<T> implements RowMapper<T> {

    /** private final EntityMetadata metadata;
    private final HandlerRegistry handlerRegistry;
    private final Dialect dialect;

    public KurubindRowMapper(Class<T> entityClass, HandlerRegistry handlerRegistry, Dialect dialect) {
        this.metadata = new EntityMetadata(entityClass);
        this.handlerRegistry = handlerRegistry;
        this.dialect = dialect;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T map(ResultSet rs, StatementContext ctx) throws SQLException {
        T entity = (T) metadata.createInstance();

        for (FieldMetadata field : metadata.getFields()) {
            String columnName = field.getColumnName();

            try {
                Object value = rs.getObject(columnName);

                if (value != null) {
                    // Aplicar handlers de lectura
                    List<Handler> handlers = handlerRegistry.getHandlersForField(field, dialect);
                    for (Handler handler : handlers) {
                        value = handler.handleRead(value, field);
                    }

                    // Convertir tipos si es necesario
                    value = convertType(value, field.getFieldType());
                }

                field.setValue(entity, value);
            } catch (SQLException e) {
                // Columna no existe en ResultSet, ignorar
            }
        }

        return entity;
    }

    private Object convertType(Object value, Class<?> targetType) {
        if (value == null || targetType.isInstance(value)) {
            return value;
        }

        // Conversiones comunes
        if (targetType == String.class) {
            return value.toString();
        }

        if (targetType == Integer.class || targetType == int.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            return Integer.parseInt(value.toString());
        }

        if (targetType == Long.class || targetType == long.class) {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            return Long.parseLong(value.toString());
        }

        if (targetType == Double.class || targetType == double.class) {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
            return Double.parseDouble(value.toString());
        }

        if (targetType == Float.class || targetType == float.class) {
            if (value instanceof Number) {
                return ((Number) value).floatValue();
            }
            return Float.parseFloat(value.toString());
        }

        if (targetType == Boolean.class || targetType == boolean.class) {
            if (value instanceof Number) {
                return ((Number) value).intValue() != 0;
            }
            return Boolean.parseBoolean(value.toString());
        }

        // Para tipos más complejos, devolver el valor tal cual
        return value;
    }*/



    private final EntityMetadata metadata;
    private final HandlerRegistry handlerRegistry;
    private final Dialect dialect;
    private  Set<String> columnsInResultSet;


    public KurubindRowMapper(EntityMetadata metadata, HandlerRegistry handlerRegistry, Dialect dialect) {
        this.metadata = metadata;
        this.handlerRegistry = handlerRegistry;
        this.dialect = dialect;
    }



    @Override
    @SuppressWarnings("unchecked")
    public T map(ResultSet rs, StatementContext ctx) throws SQLException {
        T entity = (T) metadata.createInstance();


        if(columnsInResultSet == null){
            columnsInResultSet = new HashSet<>();
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                columnsInResultSet.add(rsmd.getColumnLabel(i).toLowerCase());
            }
        }

        for(FieldMetadata field : metadata.getFields()){

            String columnName = field.getColumnName();
            if(columnsInResultSet.contains(columnName.toLowerCase())){

                Object value = rs.getObject(columnName);

                if(value != null){

                    List<Handler> handler = handlerRegistry.getHandlersForField(field, dialect);
                    for(Handler h : handler){
                        value = h.handleRead(value, field);
                    }

                }
                try {

                    field.setValue(entity, value);

                }catch (IllegalArgumentException e){
                    throw new SQLException(
                      "Mapping Error for field '"+    field.getFieldName()+
                              "' of type '"+ field.getFieldType().getName() +
                              "' with value '"+ value +
                              "' of type '"+ (value != null ? value.getClass().getName() : "null") + "'", e
                    );
                }
            }
        }

        return entity;
    }
}
