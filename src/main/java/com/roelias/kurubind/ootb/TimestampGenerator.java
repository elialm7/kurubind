package com.roelias.kurubind.ootb;

import com.roelias.kurubind.generator.ValueGenerator;
import com.roelias.kurubind.metadata.FieldMetadata;
import org.jdbi.v3.core.Handle;

import java.time.*;
import java.util.Date;
import java.util.Map;
import java.util.function.Supplier;

public class TimestampGenerator implements ValueGenerator {
    private static final Map<Class<?>, Supplier<Object>> GENERATORS = Map.ofEntries(
            Map.entry(Instant.class, Instant::now),
            Map.entry(LocalDateTime.class, LocalDateTime::now),
            Map.entry(OffsetDateTime.class, OffsetDateTime::now),
            Map.entry(ZonedDateTime.class, ZonedDateTime::now),
            Map.entry(LocalDate.class, LocalDate::now),
            Map.entry(LocalTime.class, LocalTime::now),
            Map.entry(Date.class, Date::new),
            Map.entry(java.sql.Timestamp.class, () -> new java.sql.Timestamp(System.currentTimeMillis())),
            Map.entry(java.sql.Date.class, () -> new java.sql.Date(System.currentTimeMillis())),
            Map.entry(Long.class, System::currentTimeMillis),
            Map.entry(long.class, System::currentTimeMillis)
    );

    @Override
    public Object generate(Object entity, FieldMetadata field, Handle handle) throws Exception {
        Class<?> fieldType = field.type();
        Supplier<Object> supplier = GENERATORS.get(fieldType);

        if (supplier == null) {
            throw new UnsupportedOperationException(
                    "TimestampGenerator does not support field type: " + fieldType.getName()
            );
        }

        return supplier.get();
    }
}
