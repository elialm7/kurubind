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
