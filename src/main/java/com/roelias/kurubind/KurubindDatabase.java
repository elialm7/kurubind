package com.roelias.kurubind;

import com.roelias.kurubind.annotations.DefaultValue;
import com.roelias.kurubind.annotations.Generated;
import com.roelias.kurubind.base.*;
import com.roelias.kurubind.exceptions.ValidationError;
import com.roelias.kurubind.exceptions.ValidationException;
import com.roelias.kurubind.mapper.KurubindRowMapper;
import com.roelias.kurubind.metadata.EntityMetadata;
import com.roelias.kurubind.metadata.FieldMetadata;
import com.roelias.kurubind.registry.HandlerRegistry;
import com.roelias.kurubind.registry.SQLGeneratorRegistry;
import com.roelias.kurubind.registry.ValidatorRegistry;
import com.roelias.kurubind.registry.ValueGeneratorRegistry;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.Query;
import org.jdbi.v3.core.statement.Update;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class KurubindDatabase {
    private final JdbiProvider jdbiProvider;
    private final HandlerRegistry handlerRegistry;
    private final SQLGeneratorRegistry sqlGeneratorRegistry;
    private final ValidatorRegistry validatorRegistry;
    private final ValueGeneratorRegistry valueGeneratorRegistry;
    private final Dialect dialect;

    private final Map<Class<?>, EntityMetadata> metadataCache = new ConcurrentHashMap<>();

    public KurubindDatabase(
            JdbiProvider jdbiProvider,
            HandlerRegistry handlerRegistry,
            SQLGeneratorRegistry sqlGeneratorRegistry,
            ValidatorRegistry validatorRegistry,
            ValueGeneratorRegistry valueGeneratorRegistry,
            Dialect dialect) {
        this.handlerRegistry = handlerRegistry;
        this.sqlGeneratorRegistry = sqlGeneratorRegistry;
        this.validatorRegistry = validatorRegistry;
        this.valueGeneratorRegistry = valueGeneratorRegistry;
        this.dialect = dialect;
        this.jdbiProvider = jdbiProvider;
    }

    public static Builder builder() {
        return new Builder();
    }

    private EntityMetadata getMetadata(Class<?> entityClass) {
        return metadataCache.computeIfAbsent(entityClass, EntityMetadata::new);
    }

    public <T> void insert(T entity) {
        EntityMetadata metadata = getMetadata(entity.getClass());
        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException("Can't insert @QueryResponse entities, reason: " + metadata.getEntityClass().getName());
        }
        generateValues(entity, metadata, true, false);
        validateEntity(entity, metadata);
        executeInTransaction(
                handle -> {
                    internalInsert(handle, entity, metadata);
                });
    }

    public <T> void insertAll(List<T> entities) {
        if (entities.isEmpty()) return;

        EntityMetadata metadata = getMetadata(entities.get(0).getClass());
        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException("Can't insert @QueryResponse entities, reason: " + metadata.getEntityClass().getName());
        }
        for (T entity : entities) {
            generateValues(entity, metadata, true, false);
            validateEntity(entity, metadata);
        }
        executeInTransaction(
                (Consumer<Handle>)
                        handle -> entities.forEach(entity -> internalInsert(handle, entity, metadata)));
    }

    public <T> void update(T entity) {
        EntityMetadata metadata = getMetadata(entity.getClass());

        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException("Can't update @QueryResponse entities, reason: " + metadata.getEntityClass().getName());
        }
        if (!metadata.hasIdField()) {
            throw new IllegalArgumentException("Entity must have an @Id field to perform update, problema in class :  " + metadata.getEntityClass().getName());
        }

        generateValues(entity, metadata, false, true);
        validateEntity(entity, metadata);

        executeInTransaction(
                handle -> {
                    internalUpdate(handle, entity, metadata);
                });
    }

    public <T> void updateAll(List<T> entities) {
        if (entities.isEmpty()) return;

        EntityMetadata metadata = getMetadata(entities.get(0).getClass());

        for (T entity : entities) {
            if (metadata.isQueryResponse()) {
                throw new IllegalArgumentException("Can't update @QueryResponse entities, reason: " + metadata.getEntityClass().getName());
            }
            if (!metadata.hasIdField()) {
                throw new IllegalArgumentException("Entity must have an @Id field to perform update");
            }
            generateValues(entity, metadata, false, true);
            validateEntity(entity, metadata);
        }
        executeInTransaction(
                (Consumer<Handle>)
                        handle -> entities.forEach(entity -> internalUpdate(handle, entity, metadata)));
    }

    public <T> void delete(T entity) {
        EntityMetadata metadata = getMetadata(entity.getClass());

        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException("No se pueden eliminar entidades @QueryResponse");
        }
        if (!metadata.hasIdField()) {
            throw new IllegalArgumentException("La entidad debe tener un campo @Id para eliminar");
        }

        executeInTransaction(
                handle -> {
                    internalDelete(handle, entity, metadata);
                });
    }

    public <T> void deleteAll(List<T> entities) {
        if (entities.isEmpty()) return;

        EntityMetadata metadata = getMetadata(entities.get(0).getClass());
        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException("No se pueden eliminar entidades @QueryResponse");
        }
        if (!metadata.hasIdField()) {
            throw new IllegalArgumentException("La entidad debe tener un campo @Id para eliminar");
        }

        executeInTransaction(
                (Consumer<Handle>)
                        handle -> entities.forEach(entity -> internalDelete(handle, entity, metadata)));
    }

    public <T> void deleteByIds(Class<T> entityClass, List<Object> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        EntityMetadata metadata = getMetadata(entityClass); //
        if (metadata.isQueryResponse()) { //
            throw new IllegalArgumentException("No se pueden eliminar entidades @QueryResponse");
        }
        if (!metadata.hasIdField()) { //
            throw new IllegalArgumentException("La entidad debe tener un campo @Id para eliminar");
        }

        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateDelete(metadata);
        String idColumnName = metadata.getIdField().getColumnName();

        executeInTransaction(
                handle -> {
                    ids.forEach(
                            id -> {
                                handle.createUpdate(sql).bind(idColumnName, id).execute();
                            });
                });
    }

    public <T> void deleteById(Class<T> entityClass, Object id) {
        EntityMetadata metadata = getMetadata(entityClass);

        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException("No se pueden eliminar entidades @QueryResponse");
        }
        if (!metadata.hasIdField()) {
            throw new IllegalArgumentException("La entidad debe tener un campo @Id para eliminar");
        }

        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateDelete(metadata);

        executeInTransaction(
                handle -> {
                    handle.createUpdate(sql).bind(metadata.getIdField().getColumnName(), id).execute();
                });
    }

    public <T> List<T> findAll(Class<T> entityClass) {
        EntityMetadata metadata = getMetadata(entityClass);
        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException(
                    "No se pueden listar entidades @QueryResponse. Use query() en su lugar.");
        }
        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateSelect(metadata);
        return query(sql, entityClass);
    }

    public <T> List<T> findAll(Class<T> entityClass, int limit) {
        return findAll(entityClass, limit, 0);
    }

    public <T> List<T> findAll(Class<T> entityClass, int limit, int offset) {
        EntityMetadata metadata = getMetadata(entityClass);
        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException(
                    "No se pueden listar entidades @QueryResponse. Use query() en su lugar.");
        }

        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateSelect(metadata);
        String pagedSql = buildPaginatedQuery(sql, 1, 1); // Helper para añadir LIMIT/OFFSET

        Map<String, Object> params = new HashMap<>();
        params.put("limit", limit);
        params.put("offset", offset);

        return query(pagedSql, entityClass, params);
    }

    public <T> Optional<T> findById(Class<T> entityClass, Object id) {
        EntityMetadata metadata = getMetadata(entityClass);
        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException(
                    "No se pueden consultar @QueryResponse por ID. Use query() en su lugar.");
        }
        if (!metadata.hasIdField()) {
            throw new IllegalArgumentException(
                    "La entidad debe tener un campo @Id para consultar por ID");
        }

        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateSelectById(metadata);
        KurubindRowMapper<T> mapper = new KurubindRowMapper<>(metadata, handlerRegistry, dialect);

        return jdbiProvider
                .getJdbi()
                .withHandle(
                        handle ->
                                handle
                                        .createQuery(sql)
                                        .bind(metadata.getIdField().getColumnName(), id)
                                        .map(mapper)
                                        .findOne());
    }

    public <T> List<T> findAllByIds(Class<T> entityClass, List<Object> ids) {
        if (ids.isEmpty()) return Collections.emptyList();
        List<T> results = new ArrayList<>();
        for (Object id : ids) {
            findById(entityClass, id).ifPresent(results::add);
        }
        return results;
    }

    public <T> Optional<T> findFirst(Class<T> entityClass) {
        List<T> results = findAll(entityClass, 1);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public <T> boolean exists(Class<T> entityClass, Object id) {
        return findById(entityClass, id).isPresent();
    }

    public Long countAll(Class<?> entityClass) {
        EntityMetadata metadata = getMetadata(entityClass);
        if (metadata.isQueryResponse()) {
            throw new IllegalArgumentException("No se pueden contar entidades @QueryResponse.");
        }
        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateCount(metadata);
        return jdbiProvider
                .getJdbi()
                .withHandle(handle -> handle.createQuery(sql).mapTo(Long.class).one());
    }

    public <T> List<T> query(String sql, Class<T> resultClass) {
        EntityMetadata metadata = getMetadata(resultClass);
        KurubindRowMapper<T> mapper = new KurubindRowMapper<>(metadata, handlerRegistry, dialect);
        return jdbiProvider.getJdbi().withHandle(handle -> handle.createQuery(sql).map(mapper).list());
    }

    public <T> List<T> query(String sql, Class<T> resultClass, Map<String, Object> params) {
        EntityMetadata metadata = getMetadata(resultClass);
        KurubindRowMapper<T> mapper = new KurubindRowMapper<>(metadata, handlerRegistry, dialect);
        return jdbiProvider
                .getJdbi()
                .withHandle(
                        handle -> {
                            Query query = handle.createQuery(sql);
                            params.forEach(query::bind);
                            return query.map(mapper).list();
                        });
    }

    public <T> Optional<T> queryOne(String sql, Class<T> resultClass) {
        EntityMetadata metadata = getMetadata(resultClass);
        KurubindRowMapper<T> mapper = new KurubindRowMapper<>(metadata, handlerRegistry, dialect);
        return jdbiProvider
                .getJdbi()
                .withHandle(handle -> handle.createQuery(sql).map(mapper).findOne());
    }

    public <T> Optional<T> queryOne(String sql, Class<T> resultClass, Map<String, Object> params) {
        EntityMetadata metadata = getMetadata(resultClass);
        KurubindRowMapper<T> mapper = new KurubindRowMapper<>(metadata, handlerRegistry, dialect);
        return jdbiProvider
                .getJdbi()
                .withHandle(
                        handle -> {
                            Query query = handle.createQuery(sql);
                            params.forEach(query::bind);
                            return query.map(mapper).findOne();
                        });
    }

    public <T> Stream<T> queryStream(String sql, Class<T> resultClass, Map<String, Object> params) {
        EntityMetadata metadata = getMetadata(resultClass); //
        KurubindRowMapper<T> mapper =
                new KurubindRowMapper<>(
                        metadata, handlerRegistry, dialect //
                );

        Handle handle = jdbiProvider.getJdbi().open(); //
        try {
            Query query = handle.createQuery(sql);
            params.forEach(query::bind); //
            return query.map(mapper).stream().onClose(handle::close);

        } catch (Exception e) {
            handle.close();
            throw new RuntimeException("Error al crear el stream de la consulta", e);
        }
    }

    public <T> Stream<T> queryStream(String sql, Class<T> resultClass) {
        return queryStream(sql, resultClass, Collections.emptyMap());
    }

    public List<Map<String, Object>> queryForMaps(String sql) {
        return jdbiProvider.getJdbi().withHandle(handle -> handle.createQuery(sql).mapToMap().list());
    }

    public List<Map<String, Object>> queryForMaps(String sql, Map<String, Object> params) {
        return jdbiProvider
                .getJdbi()
                .withHandle(
                        handle -> {
                            Query query = handle.createQuery(sql);
                            params.forEach(query::bind);
                            return query.mapToMap().list();
                        });
    }

    public Optional<Map<String, Object>> queryForMap(String sql) {
        return jdbiProvider
                .getJdbi()
                .withHandle(handle -> handle.createQuery(sql).mapToMap().findOne());
    }

    public Optional<Map<String, Object>> queryForMap(String sql, Map<String, Object> params) {
        return jdbiProvider
                .getJdbi()
                .withHandle(
                        handle -> {
                            Query query = handle.createQuery(sql);
                            params.forEach(query::bind);
                            return query.mapToMap().findOne();
                        });
    }

    public <T> Optional<T> queryForObject(String sql, Class<T> type) {
        return jdbiProvider
                .getJdbi()
                .withHandle(handle -> handle.createQuery(sql).mapTo(type).findOne());
    }

    public <T> Optional<T> queryForObject(String sql, Class<T> type, Map<String, Object> params) {
        return jdbiProvider
                .getJdbi()
                .withHandle(
                        handle -> {
                            Query query = handle.createQuery(sql);
                            params.forEach(query::bind);
                            return query.mapTo(type).findOne();
                        });
    }

    public <T> List<T> queryForList(String sql, Class<T> type) {
        return jdbiProvider.getJdbi().withHandle(handle -> handle.createQuery(sql).mapTo(type).list());
    }

    public <T> List<T> queryForList(String sql, Class<T> type, Map<String, Object> params) {
        return jdbiProvider
                .getJdbi()
                .withHandle(
                        handle -> {
                            Query query = handle.createQuery(sql);
                            params.forEach(query::bind);
                            return query.mapTo(type).list();
                        });
    }

    public Long queryForLong(String sql) {
        return queryForObject(sql, Long.class).orElse(0L);
    }

    public Long queryForLong(String sql, Map<String, Object> params) {
        return queryForObject(sql, Long.class, params).orElse(0L);
    }

    public Integer queryForInt(String sql) {
        return queryForObject(sql, Integer.class).orElse(0);
    }

    public Integer queryForInt(String sql, Map<String, Object> params) {
        return queryForObject(sql, Integer.class, params).orElse(0);
    }

    public String queryForString(String sql) {
        return queryForObject(sql, String.class).orElse(null);
    }

    public String queryForString(String sql, Map<String, Object> params) {
        return queryForObject(sql, String.class, params).orElse(null);
    }

    public <T> PageResult<T> queryPage(Class<T> entityClass, int page, int pageSize) {
        EntityMetadata metadata = getMetadata(entityClass);
        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateSelect(metadata);
        return queryPage(sql, entityClass, Collections.emptyMap(), page, pageSize);
    }

    public <T> PageResult<T> queryPage(String sql, Class<T> resultClass, int page, int pageSize) {
        return queryPage(sql, resultClass, Collections.emptyMap(), page, pageSize);
    }

    public <T> PageResult<T> queryPage(
            String sql, Class<T> resultClass, Map<String, Object> params, int page, int pageSize) {
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") AS count_query";
        long total = queryForLong(countSql, params);
        String pagedSql = buildPaginatedQuery(sql, page, pageSize);
        Map<String, Object> allParams = new HashMap<>(params);
        allParams.put("limit", pageSize);
        allParams.put("offset", (page - 1) * pageSize);

        List<T> results = query(pagedSql, resultClass, allParams);

        return new PageResult<>(results, page, pageSize, total);
    }

    public PageResult<Map<String, Object>> queryPageForMaps(String sql, int page, int pageSize) {
        return queryPageForMaps(sql, Collections.emptyMap(), page, pageSize);
    }

    public PageResult<Map<String, Object>> queryPageForMaps(
            String sql, Map<String, Object> params, int page, int pageSize) {
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") AS count_query";
        long total = queryForLong(countSql, params);

        String pagedSql = buildPaginatedQuery(sql, page, pageSize);
        Map<String, Object> allParams = new HashMap<>(params);
        allParams.put("limit", pageSize);
        allParams.put("offset", (page - 1) * pageSize);

        List<Map<String, Object>> results = queryForMaps(pagedSql, allParams);
        return new PageResult<>(results, page, pageSize, total);
    }

    public int executeUpdate(String sql) {
        return jdbiProvider.getJdbi().inTransaction(handle -> handle.createUpdate(sql).execute());
    }

    public int executeUpdate(String sql, Map<String, Object> params) {
        return jdbiProvider
                .getJdbi()
                .inTransaction(
                        handle -> {
                            Update update = handle.createUpdate(sql);
                            params.forEach(update::bind);
                            return update.execute();
                        });
    }

    public List<Integer> executeBatch(String sql, List<Map<String, Object>> batchParams) {
        return jdbiProvider
                .getJdbi()
                .inTransaction(
                        handle -> {
                            List<Integer> results = new ArrayList<>();
                            for (Map<String, Object> params : batchParams) {
                                Update update = handle.createUpdate(sql);
                                params.forEach(update::bind);
                                results.add(update.execute());
                            }
                            return results;
                        });
    }

    public void executeInTransaction(Consumer<Handle> handleConsumer) {
        jdbiProvider.getJdbi().useTransaction(handleConsumer::accept);
    }

    public <T> T executeInTransaction(Function<Handle, T> handleFunction) {
        return jdbiProvider.getJdbi().inTransaction(handleFunction::apply);
    }

    public void executeWithHandle(Consumer<Handle> handleConsumer) {
        jdbiProvider.getJdbi().useHandle(handleConsumer::accept);
    }

    public <T> T executeWithHandle(Function<Handle, T> handleFunction) {
        return jdbiProvider.getJdbi().withHandle(handleFunction::apply);
    }

    public Jdbi getJdbi() {
        return jdbiProvider.getJdbi();
    }

    public <T> RowMapper<T> getRowMapper(Class<T> resultClass) {
        EntityMetadata metadata = getMetadata(resultClass);
        return (resultSet, ctx) -> {
            KurubindRowMapper<T> mapper = new KurubindRowMapper<>(metadata, handlerRegistry, dialect);
            return mapper.map(resultSet, ctx);
        };
    }

    private <T> void internalInsert(Handle hande, T entity, EntityMetadata metadata) {
        List<FieldMetadata> fieldsToInsert = getFieldsForInsert(metadata);
        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateInsert(metadata, fieldsToInsert);

        Update update = hande.createUpdate(sql);
        bindFields(update, entity, fieldsToInsert);

        if (metadata.hasAutoGeneratedId()) {
            Object generatedId =
                    update
                            .executeAndReturnGeneratedKeys(metadata.getIdField().getColumnName())
                            .mapTo(metadata.getIdField().getFieldType())
                            .one();
            metadata.getIdField().setValue(entity, generatedId);
        } else {
            update.execute();
        }
    }

    private <T> void internalUpdate(Handle handle, T entity, EntityMetadata metadata) {
        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateUpdate(metadata, metadata.getFields());

        Update update = handle.createUpdate(sql);
        bindFields(update, entity, metadata.getNonIdFields());
        update.bind(metadata.getIdField().getColumnName(), metadata.getIdField().getValue(entity));
        update.execute();
    }

    private <T> void internalDelete(Handle handle, T entity, EntityMetadata metadata) {
        SQLGenerator generator = sqlGeneratorRegistry.getGenerator(dialect);
        String sql = generator.generateDelete(metadata);

        handle
                .createUpdate(sql)
                .bind(metadata.getIdField().getColumnName(), metadata.getIdField().getValue(entity))
                .execute();
    }

    private String buildPaginatedQuery(String sql, int page, int pageSize) {
        return sql + " LIMIT :limit OFFSET :offset";
    }

    private <T> void generateValues(
            T entity, EntityMetadata metadata, boolean isInsert, boolean isUpdate) {
        for (FieldMetadata field : metadata.getFields()) {
            if (field.hasAnnotation(DefaultValue.class)) {
                DefaultValue defaultValue = field.getAnnotation(DefaultValue.class);
                Object currentValue = field.getValue(entity);

                if (currentValue == null) {
                    if (!defaultValue.value().isEmpty() && !defaultValue.generator().isEmpty()) {
                        throw new IllegalStateException(
                                "El campo "
                                        + field.getFieldName()
                                        + " tiene 'value' y 'generator' en @DefaultValue");
                    }

                    if (!defaultValue.value().isEmpty()) {
                        Object parsedValue = parseLiteral(defaultValue.value(), field.getFieldType());
                        field.setValue(entity, parsedValue);
                    } else if (!defaultValue.generator().isEmpty()) {
                        ValueGenerator generator =
                                valueGeneratorRegistry.getGenerator(defaultValue.generator());
                        Object generatedValue = generator.generate(entity, field);
                        field.setValue(entity, generatedValue);
                    }
                }
            }

            if (field.hasAnnotation(Generated.class)) {
                Generated generated = field.getAnnotation(Generated.class);
                boolean shouldGenerate =
                        (isInsert && generated.onInsert()) || (isUpdate && generated.onUpdate());

                if (shouldGenerate) {
                    ValueGenerator generator = valueGeneratorRegistry.getGenerator(generated.generator());
                    Object generatedValue = generator.generate(entity, field);
                    field.setValue(entity, generatedValue);
                }
            }
        }
    }

    private Object parseLiteral(String literal, Class<?> targetType) {
        if (targetType == String.class) {
            return literal;
        }
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(literal);
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(literal);
        }
        if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(literal);
        }
        if (targetType == Float.class || targetType == float.class) {
            return Float.parseFloat(literal);
        }
        if (targetType == Boolean.class || targetType == boolean.class) {
            return Boolean.parseBoolean(literal);
        }
        return literal;
    }

    private <T> void validateEntity(T entity, EntityMetadata metadata) {
        List<ValidationError> errors = new ArrayList<>();

        for (FieldMetadata field : metadata.getFields()) {
            Object value = field.getValue(entity);
            List<Validator> validators = validatorRegistry.getValidators(field);

            for (Validator validator : validators) {
                try {
                    validator.validate(value, field);
                } catch (ValidationException e) {
                    errors.addAll(e.getErrors());
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    private List<FieldMetadata> getFieldsForInsert(EntityMetadata metadata) {
        return metadata.getFields().stream()
                .filter(f -> !f.isId() || !metadata.hasAutoGeneratedId())
                .collect(Collectors.toList());
    }

    private void bindFields(Update update, Object entity, List<FieldMetadata> fields) {
        for (FieldMetadata field : fields) {
            Object value = field.getValue(entity);

            List<Handler> handlers = handlerRegistry.getHandlersForField(field, dialect);
            for (Handler handler : handlers) {
                value = handler.handleWrite(value, field);
            }

            update.bind(field.getColumnName(), value);
        }
    }

    public static class PageResult<T> {
        private final List<T> results;
        private final int page;
        private final int pageSize;
        private final long totalElements;
        private final long totalPages;

        public PageResult(List<T> results, int page, int pageSize, long totalElements) {
            this.results = results;
            this.page = page;
            this.pageSize = pageSize;
            this.totalElements = totalElements;
            this.totalPages = (totalElements + pageSize - 1) / pageSize;
        }

        public List<T> getResults() {
            return results;
        }

        public int getPage() {
            return page;
        }

        public int getPageSize() {
            return pageSize;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public long getTotalPages() {
            return totalPages;
        }

        public boolean hasNext() {
            return page < totalPages;
        }

        public boolean hasPrevious() {
            return page > 1;
        }

        @Override
        public String toString() {
            return "PageResult{"
                    + "page="
                    + page
                    + ", pageSize="
                    + pageSize
                    + ", totalElements="
                    + totalElements
                    + ", totalPages="
                    + totalPages
                    + ", results="
                    + results.size()
                    + '}';
        }
    }

    public static class Builder {
        private boolean withProviderState = false;
        private Jdbi jdbi;
        private Dialect dialect;
        private JdbiProvider jdbiProvider;
        private List<KurubindModule> modules = new ArrayList<>();
        private ValidatorRegistry validatorRegistry = new ValidatorRegistry();
        private HandlerRegistry handlerRegistry = new HandlerRegistry();
        private ValueGeneratorRegistry valueGeneratorRegistry = new ValueGeneratorRegistry();
        private SQLGeneratorRegistry sqlGeneratorRegistry = new SQLGeneratorRegistry();

        public Builder withJdbi(Jdbi jdbi) {
            this.jdbi = jdbi;
            this.withProviderState = false;
            return this;
        }

        public Builder withDialect(Dialect dialect) {
            this.dialect = dialect;
            return this;
        }

        public Builder withJdbiProvider(JdbiProvider jdbiProvider) {
            this.jdbiProvider = jdbiProvider;
            this.withProviderState = true;
            return this;
        }

        public Builder installModule(KurubindModule module) {
            if (module != null) {
                this.modules.add(module);
            }
            return this;
        }

        public KurubindDatabase build() {
            Objects.requireNonNull(jdbi, "Jdbi can't be null, You must provide a Jdbi instance.");
            if (this.dialect == null) {
                this.dialect = new Dialect("ANSI");
            }

            JdbiProvider jdbiProvider;

            if (withProviderState) {
                jdbiProvider = this.jdbiProvider;
            } else {
                jdbiProvider = () -> this.jdbi;
            }

            RegistryCollector collector =
                    new RegistryCollector(
                            this.validatorRegistry,
                            this.handlerRegistry,
                            this.valueGeneratorRegistry,
                            this.sqlGeneratorRegistry);
            for (KurubindModule module : this.modules) {
                module.configure(collector);
            }
            return new KurubindDatabase(
                    jdbiProvider,
                    this.handlerRegistry,
                    this.sqlGeneratorRegistry,
                    this.validatorRegistry,
                    this.valueGeneratorRegistry,
                    this.dialect);
        }
    }
}
