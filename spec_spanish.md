# **Kurubind – Especificación Técnica Completa**

---

## 1️⃣ Objetivo

Kurubind es una librería ligera que actúa como **puente entre clases anotadas y JDBI**, proporcionando:

* **Operaciones CRUD automáticas** (`insert`, `update`, `delete`, `list`)
* **Ejecución de queries SQL** personalizadas con mapeo a objetos
* **Sistema extensible** mediante handlers y generadores SQL
* **Validación de datos** mediante validators integrados
* **Generación de valores** mediante value generators
* **Soporte multitenancy** mediante JdbiProvider

### Filosofía

Kurubind **no es un ORM completo**. Es una capa delgada sobre JDBI que:
- Elimina SQL repetitivo para operaciones comunes
- Permite SQL manual cuando se necesita control total
- Se extiende mediante interfaces claras y desacopladas
- Proporciona validación y generación de valores integradas
- No impone restricciones ni frameworks adicionales

---

## 2️⃣ Principios de Diseño

1. **Ligera:** Solo depende de JDBI
2. **No intrusiva:** Las entidades son POJOs simples con anotaciones
3. **Extensible:** Todo se puede customizar mediante interfaces públicas
4. **SQL-first:** El desarrollador mantiene control total del SQL cuando lo necesita
5. **Separación clara:** Cada componente tiene una única responsabilidad
6. **Validación integrada:** Validators para garantizar integridad de datos
7. **Generación flexible:** Value generators para valores automáticos

---

## 3️⃣ Arquitectura General

```
                    ┌─────────────────┐
                    │  Desarrollador  │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              │              │              │
              ▼              ▼              ▼
     ┌──────────────┐ ┌────────────┐ ┌──────────────┐
     │   Handlers   │ │ SQL        │ │  Anotaciones │
     │   Validators │ │ Generators │ │   Generators │
     │   (Custom)   │ │            │ │   (Custom)   │
     └──────┬───────┘ └─────┬──────┘ └──────┬───────┘
            │               │               │
            └───────────────┼───────────────┘
                            ▼
                 ┌─────────────────────┐
                 │ KurubindDatabase    │
                 │  (Core Framework)   │
                 │  - Validation       │
                 │  - Value Generation │
                 └──────────┬──────────┘
                            │
                 ┌──────────┴──────────┐
                 ▼                     ▼
          ┌─────────────┐       ┌──────────────┐
          │  RowMapper  │       │ Entity       │
          │             │       │ Metadata     │
          └──────┬──────┘       └──────┬───────┘
                 │                     │
                 └──────────┬──────────┘
                            ▼
                      ┌──────────┐
                      │   JDBI   │
                      └──────────┘
```

---

## 4️⃣ Componentes Principales

### 4.1 KurubindDatabase

**Responsabilidad:** Coordinar todas las operaciones de persistencia.

**Lo que hace:**
- Recibe objetos y comandos del desarrollador
- **Valida datos usando validators registrados antes de operaciones**
- **Genera valores usando value generators registrados**
- Delega generación de SQL al `SQLGenerator` correspondiente
- Ejecuta SQL usando JDBI
- Aplica `Handlers` para transformar datos antes de escribir
- Usa `RowMapper` para convertir `ResultSet` en objetos
- Aplica `Handlers` para transformar datos después de leer

**Lo que NO hace:**
- No genera SQL directamente
- No conoce dialectos específicos
- No conoce anotaciones custom (más allá de las base)

**API Pública:**

```java
// CRUD
<T> void insert(T entity)
<T> void insertAll(List<T> entities)
<T> void update(T entity)
<T> void delete(T entity)
<T> void deleteAll(List<T> entities)

// Queries
<T> List<T> list(Class<T> entityClass)
<T> List<T> query(String sql, Class<T> resultClass)

// Acceso directo a JDBI
void execute(Consumer<Handle> handleConsumer)
```

---

### 4.2 SQLGenerator (Interface)

**Responsabilidad:** Generar SQL específico para cada dialecto.

**Por qué existe:**
Cada base de datos tiene diferencias:
- PostgreSQL usa `RETURNING` para obtener IDs generados
- MySQL usa `AUTO_INCREMENT`
- Oracle usa `SEQUENCES`
- Algunos dialectos soportan `::jsonb`, otros no

**Métodos:**

```java
interface SQLGenerator {
    // Genera: INSERT INTO table (cols) VALUES (placeholders)
    String generateInsert(EntityMetadata meta, List<FieldMetadata> fields);
    
    // Genera: UPDATE table SET col1 = ?, col2 = ? WHERE id = ?
    String generateUpdate(EntityMetadata meta, List<FieldMetadata> fields);
    
    // Genera: DELETE FROM table WHERE id = ?
    String generateDelete(EntityMetadata meta);
    
    // Genera: SELECT * FROM table
    String generateSelect(EntityMetadata meta);
    
    // Genera placeholder con modificadores: ":columnName" o ":columnName::jsonb"
    String getPlaceholder(FieldMetadata field);
}
```

**Implementación incluida:**
- `GenericSQLGenerator`: SQL estándar ANSI (usado por defecto)

**Implementaciones del desarrollador:**
- `PostgreSQLGenerator`: Con `RETURNING`, `::jsonb`, etc.
- `MySQLGenerator`: Con sintaxis específica de MySQL
- Cualquier otra base de datos que necesite

---

### 4.3 Handler (Interface)

**Responsabilidad:** Transformar valores de campos entre Java y la base de datos.

**Por qué existe:**
Algunos tipos de datos necesitan conversión:
- `Map<String, Object>` ↔ JSON string
- `LocalDateTime` ↔ `Timestamp`
- Objetos custom ↔ representación en DB
- Valores encriptados

**Métodos:**

```java
interface Handler {
    // Transforma valor Java antes de escribir a DB
    Object handleWrite(Object javaValue);
    
    // Transforma valor DB después de leer
    Object handleRead(Object dbValue);
}
```

**Características:**
- Un handler se asocia a una anotación específica
- Opcionalmente puede ser específico de un dialecto
- NO genera SQL, solo transforma valores

---

### 4.4 Validator (Interface)

**Responsabilidad:** Validar valores de campos antes de operaciones de escritura.

**Por qué existe:**
Garantizar integridad de datos antes de ejecutar SQL:
- Campos obligatorios no nulos
- Rangos numéricos válidos
- Formatos correctos
- Reglas de negocio

**Métodos:**

```java
interface Validator {
    // Valida un valor y lanza ValidationException si falla
    void validate(Object value, FieldMetadata field) throws ValidationException;
    
    // Mensaje de error personalizado
    String getErrorMessage(Object value, FieldMetadata field);
}
```

**Implementaciones incluidas:**

```java
// Validator para @NotNull
class NotNullValidator implements Validator {
    void validate(Object value, FieldMetadata field) {
        if (value == null) {
            throw new ValidationException(
                getErrorMessage(value, field)
            );
        }
    }
    
    String getErrorMessage(Object value, FieldMetadata field) {
        return "Field " + field.getFieldName() + " cannot be null";
    }
}

// Validator para @Min
class MinValidator implements Validator {
    void validate(Object value, FieldMetadata field) {
        if (value == null) return; // @NotNull se encarga de nulls
        
        Min minAnnotation = field.getAnnotation(Min.class);
        long minValue = minAnnotation.value();
        
        if (value instanceof Number) {
            long numValue = ((Number) value).longValue();
            if (numValue < minValue) {
                throw new ValidationException(
                    getErrorMessage(value, field)
                );
            }
        }
    }
    
    String getErrorMessage(Object value, FieldMetadata field) {
        Min minAnnotation = field.getAnnotation(Min.class);
        return "Field " + field.getFieldName() + 
               " must be at least " + minAnnotation.value() + 
               " but was " + value;
    }
}

// Validator para @Max
class MaxValidator implements Validator {
    void validate(Object value, FieldMetadata field) {
        if (value == null) return;
        
        Max maxAnnotation = field.getAnnotation(Max.class);
        long maxValue = maxAnnotation.value();
        
        if (value instanceof Number) {
            long numValue = ((Number) value).longValue();
            if (numValue > maxValue) {
                throw new ValidationException(
                    getErrorMessage(value, field)
                );
            }
        }
    }
    
    String getErrorMessage(Object value, FieldMetadata field) {
        Max maxAnnotation = field.getAnnotation(Max.class);
        return "Field " + field.getFieldName() + 
               " must be at most " + maxAnnotation.value() + 
               " but was " + value;
    }
}
```

**Características:**
- Validators se asocian a anotaciones específicas
- Se ejecutan antes de INSERT y UPDATE
- Pueden acumularse múltiples errores antes de lanzar excepción
- El desarrollador puede crear validators custom

---

### 4.5 ValueGenerator (Interface)

**Responsabilidad:** Generar valores automáticamente para campos.

**Por qué existe:**
Algunos campos necesitan valores generados automáticamente:
- UUIDs
- Códigos de producto (ej: "PRD-12345")
- Timestamps de creación
- Valores calculados

**Métodos:**

```java
interface ValueGenerator {
    // Genera un valor para el campo
    Object generate(Object entity, FieldMetadata field);
    
    // Indica si debe generar en INSERT
    boolean generateOnInsert();
    
    // Indica si debe generar en UPDATE
    boolean generateOnUpdate();
}
```

**Ejemplos de implementación:**

```java
// Generator de UUID
class UuidGenerator implements ValueGenerator {
    public Object generate(Object entity, FieldMetadata field) {
        return UUID.randomUUID().toString();
    }
    
    public boolean generateOnInsert() { return true; }
    public boolean generateOnUpdate() { return false; }
}

// Generator de timestamp de creación
class CreatedAtGenerator implements ValueGenerator {
    public Object generate(Object entity, FieldMetadata field) {
        return LocalDateTime.now();
    }
    
    public boolean generateOnInsert() { return true; }
    public boolean generateOnUpdate() { return false; }
}

// Generator de timestamp de actualización
class UpdatedAtGenerator implements ValueGenerator {
    public Object generate(Object entity, FieldMetadata field) {
        return LocalDateTime.now();
    }
    
    public boolean generateOnInsert() { return true; }
    public boolean generateOnUpdate() { return true; }
}

// Generator de código de producto
class ProductCodeGenerator implements ValueGenerator {
    private AtomicLong counter = new AtomicLong(1000);
    
    public Object generate(Object entity, FieldMetadata field) {
        return "PRD-" + counter.incrementAndGet();
    }
    
    public boolean generateOnInsert() { return true; }
    public boolean generateOnUpdate() { return false; }
}
```

**Características:**
- Generators se registran con un nombre único
- Se referencian desde anotaciones (`@DefaultValue` o `@Generated`)
- Se ejecutan antes de validación
- El desarrollador puede crear generators custom

---

### 4.6 RowMapper

**Responsabilidad:** Convertir filas del `ResultSet` en objetos Java.

**Proceso:**
1. Para cada fila del `ResultSet`:
2. Crea una instancia del objeto destino
3. Para cada campo con `@Column`:
    - Lee el valor de la columna correspondiente
    - Si hay un `Handler` registrado para alguna anotación del campo, lo aplica
    - Asigna el valor al campo
4. Retorna el objeto completo

**Características:**
- Funciona con entidades (`@Table`) y DTOs (`@QueryResponse`)
- Solo mapea campos con `@Column`
- Ignora campos con `@Transient`
- Aplica handlers automáticamente

---

### 4.7 EntityMetadata

**Responsabilidad:** Analizar y almacenar información sobre una clase anotada.

**Información que extrae:**
- Nombre de tabla y schema (`@Table`)
- Lista de campos mapeables (con `@Column`, sin `@Transient`)
- Campo ID (`@Id`)
- Configuración de cada campo (nombre columna, anotaciones, etc.)

**Uso:**
```java
EntityMetadata meta = new EntityMetadata(User.class);
String tableName = meta.getTableName();
FieldMetadata idField = meta.getIdField();
List<FieldMetadata> allFields = meta.getFields();
```

---

### 4.8 FieldMetadata

**Responsabilidad:** Almacenar información sobre un campo específico.

**Información que contiene:**
- Nombre del campo Java
- Nombre de la columna SQL
- Si es ID
- Si es transient
- Anotaciones presentes
- Acceso reflectivo al campo

**Métodos útiles:**

```java
class FieldMetadata {
    String getFieldName();
    String getColumnName();
    boolean isId();
    boolean isTransient();
    boolean hasAnnotation(Class<? extends Annotation> annotationType);
    <A extends Annotation> A getAnnotation(Class<A> annotationType);
    Object getValue(Object entity);
    void setValue(Object entity, Object value);
}
```

---

### 4.9 Registries

#### HandlerRegistry

Almacena y recupera handlers por anotación y dialecto.

```java
HandlerRegistry registry = new HandlerRegistry();

// Handler genérico (para cualquier dialecto)
registry.register(JsonColumn.class, new JsonHandler());

// Handler específico de dialecto
registry.register(JsonColumn.class, new Dialect("POSTGRESQL"), new PostgresJsonHandler());
registry.register(JsonColumn.class, new Dialect("MYSQL"), new MySQLJsonHandler());

// Recuperar
Handler handler = registry.getHandler(JsonColumn.class, dialect);
```

#### SQLGeneratorRegistry

Almacena y recupera generadores SQL por dialecto.

```java
SQLGeneratorRegistry sqlRegistry = new SQLGeneratorRegistry();

sqlRegistry.register(new Dialect("POSTGRESQL"), new PostgreSQLGenerator());
sqlRegistry.register(new Dialect("MYSQL"), new MySQLGenerator());

// Si no hay generador registrado, usa GenericSQLGenerator
SQLGenerator generator = sqlRegistry.getGenerator(dialect);
```

#### ValidatorRegistry

Almacena y recupera validators por anotación.

```java
ValidatorRegistry validatorRegistry = new ValidatorRegistry();

// Registrar validators para anotaciones base
validatorRegistry.register(NotNull.class, new NotNullValidator());
validatorRegistry.register(Min.class, new MinValidator());
validatorRegistry.register(Max.class, new MaxValidator());

// Registrar validator custom
validatorRegistry.register(Email.class, new EmailValidator());

// Recuperar
List<Validator> validators = validatorRegistry.getValidators(fieldMetadata);
```

#### ValueGeneratorRegistry

Almacena y recupera value generators por nombre.

```java
ValueGeneratorRegistry generatorRegistry = new ValueGeneratorRegistry();

// Registrar generators
generatorRegistry.register("UUID", new UuidGenerator());
generatorRegistry.register("CREATED_AT", new CreatedAtGenerator());
generatorRegistry.register("UPDATED_AT", new UpdatedAtGenerator());
generatorRegistry.register("PRODUCT_CODE", new ProductCodeGenerator());

// Recuperar
ValueGenerator generator = generatorRegistry.getGenerator("UUID");
```

---

### 4.10 Repository<T>

**Responsabilidad:** Capa de abstracción para operaciones sobre una entidad específica.

**Características:**
- Clase base genérica que envuelve `KurubindDatabase`
- Reutiliza todos los métodos CRUD
- El desarrollador la extiende para agregar queries específicas de negocio

**Ejemplo:**

```java
public class Repository<T> {
    protected final KurubindDatabase db;
    protected final Class<T> type;

    public Repository(KurubindDatabase db, Class<T> type) {
        this.db = db;
        this.type = type;
    }

    public void insert(T obj) { db.insert(obj); }
    public void update(T obj) { db.update(obj); }
    public void delete(T obj) { db.delete(obj); }
    public List<T> list() { return db.list(type); }
    
    public <R> List<R> query(String sql, Class<R> resultType) { 
        return db.query(sql, resultType); 
    }
}
```

---

### 4.11 ValidationException

**Responsabilidad:** Encapsular errores de validación.

**Estructura:**

```java
class ValidationException extends RuntimeException {
    private final List<ValidationError> errors;
    
    ValidationException(List<ValidationError> errors) {
        super(buildMessage(errors));
        this.errors = errors;
    }
    
    ValidationException(String message) {
        super(message);
        this.errors = Collections.singletonList(
            new ValidationError(null, message)
        );
    }
    
    List<ValidationError> getErrors() {
        return errors;
    }
    
    private static String buildMessage(List<ValidationError> errors) {
        return "Validation failed: " + 
               errors.stream()
                   .map(ValidationError::getMessage)
                   .collect(Collectors.joining("; "));
    }
}

class ValidationError {
    private final String fieldName;
    private final String message;
    
    ValidationError(String fieldName, String message) {
        this.fieldName = fieldName;
        this.message = message;
    }
    
    String getFieldName() { return fieldName; }
    String getMessage() { return message; }
}
```

---

### 4.12 Dialect

**Responsabilidad:** Identificar una base de datos específica.

**Uso:**
```java
Dialect postgresql = new Dialect("POSTGRESQL");
Dialect mysql = new Dialect("MYSQL");
Dialect oracle = new Dialect("ORACLE");
```

Se usa para:
- Seleccionar el `SQLGenerator` apropiado
- Seleccionar `Handlers` específicos del dialecto

---

### 4.13 JdbiProvider (Interface)

**Responsabilidad:** Proporcionar instancias de JDBI dinámicamente.

**Por qué existe:** Multitenancy o selección dinámica de base de datos.

```java
interface JdbiProvider {
    Jdbi getJdbi();
}
```

---

## 5️⃣ Anotaciones Base

Estas son las **únicas** anotaciones que Kurubind conoce internamente.

### @Table

Define la tabla y schema de una entidad.

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
    String name();           // Nombre de la tabla
    String schema() default "";  // Schema (opcional)
}
```

**Uso:**
```java
@Table(name = "users")
@Table(name = "users", schema = "public")
```

---

### @Column

Mapea un campo Java a una columna SQL.

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Column {
    String value();  // Nombre de la columna
}
```

**Uso:**
```java
@Column("username")
private String username;
```

---

### @Id

Marca un campo como llave primaria.

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Id {
    boolean autoGenerated() default true;
}
```

**Uso:**
```java
@Id
@Column("id")
private Long id;

@Id(autoGenerated = false)
@Column("uuid")
private String uuid;  // UUID manual
```

---

### @Transient

Indica que un campo debe ser ignorado en persistencia.

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Transient {
}
```

**Uso:**
```java
@Transient
private String temporaryValue;
```

---

### @DefaultValue

Define un valor por defecto usando un literal o un ValueGenerator.

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DefaultValue {
    // Valor literal (ej: "ACTIVE", "0", "true")
    String value() default "";
    
    // Nombre del ValueGenerator registrado (ej: "UUID", "CREATED_AT")
    String generator() default "";
}
```

**Reglas:**
- Solo uno de `value` o `generator` debe estar presente
- Si ambos están vacíos o ambos tienen valor, se lanza excepción
- Si `value` está presente, se usa como literal
- Si `generator` está presente, se busca el generator registrado y se ejecuta

**Uso:**

```java
// Valor literal
@Column("status")
@DefaultValue(value = "ACTIVE")
private String status;

@Column("count")
@DefaultValue(value = "0")
private Integer count;

// Usando generator
@Column("uuid")
@DefaultValue(generator = "UUID")
private String uuid;

@Column("created_at")
@DefaultValue(generator = "CREATED_AT")
private LocalDateTime createdAt;

@Column("code")
@DefaultValue(generator = "PRODUCT_CODE")
private String code;
```

---

### @Generated

Marca un campo para generación automática con control fino sobre cuándo se genera.

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Generated {
    // Nombre del ValueGenerator registrado
    String generator();
    
    // Generar en INSERT
    boolean onInsert() default true;
    
    // Generar en UPDATE
    boolean onUpdate() default false;
}
```

**Uso:**

```java
// Solo en INSERT
@Column("uuid")
@Generated(generator = "UUID", onInsert = true, onUpdate = false)
private String uuid;

// Solo en UPDATE
@Column("updated_at")
@Generated(generator = "UPDATED_AT", onInsert = false, onUpdate = true)
private LocalDateTime updatedAt;

// En INSERT y UPDATE
@Column("version_code")
@Generated(generator = "VERSION_CODE", onInsert = true, onUpdate = true)
private String versionCode;
```

**Diferencia con @DefaultValue:**
- `@DefaultValue`: Solo se aplica si el campo es null
- `@Generated`: Siempre se aplica según `onInsert`/`onUpdate`, sobrescribe el valor existente

---

### @QueryResponse

Marca una clase como DTO para resultados de queries (no es una entidad persistible).

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryResponse {
}
```

**Uso:**
```java
@QueryResponse
public class UserSummary {
    @Column("id")
    private Long id;
    
    @Column("full_name")
    private String fullName;
}
```

---

### @NotNull

Validación: el campo no puede ser null.

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotNull {
    String message() default "Field cannot be null";
}
```

**Uso:**
```java
@Column("email")
@NotNull(message = "Email is required")
private String email;
```

---

### @Min

Validación: valor mínimo permitido para números.

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Min {
    long value();
    String message() default "Value must be at least {value}";
}
```

**Uso:**
```java
@Column("age")
@Min(value = 18, message = "Must be at least 18 years old")
private Integer age;

@Column("price")
@Min(0)
private Double price;
```

---

### @Max

Validación: valor máximo permitido para números.

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Max {
    long value();
    String message() default "Value must be at most {value}";
}
```

**Uso:**
```java
@Column("age")
@Max(value = 120, message = "Age cannot exceed 120")
private Integer age;

@Column("discount_percent")
@Min(0)
@Max(100)
private Integer discountPercent;
```

---

## 6️⃣ Flujo de Operaciones

### INSERT

```
1. Desarrollador: db.insert(user)
                     ↓
2. KurubindDatabase: Extrae EntityMetadata de User.class
                     ↓
3. VALUE GENERATION: Para cada campo con @DefaultValue o @Generated:
   - Si @DefaultValue y campo es null:
     * Si tiene literal: asigna literal parseado
     * Si tiene generator: ejecuta generator y asigna valor
   - Si @Generated con onInsert=true:
     * Ejecuta generator y asigna valor (sobrescribe existente)
                     ↓
4. VALIDATION: Para cada campo:
   - Busca validators registrados para sus anotaciones
   - Ejecuta cada validator
   - Si alguno falla, acumula error
   - Si hay errores, lanza ValidationException con todos
                     ↓
5. SQLGenerator: generateInsert(metadata, fields)
                     ↓ 
6. SQL generado: INSERT INTO users (username, email, uuid) VALUES (:username, :email, :uuid)
                     ↓
7. Para cada campo:
   - Si tiene handler: handler.handleWrite(valor)
   - Bind valor transformado al statement JDBI
                     ↓
8. JDBI: Ejecuta SQL
                     ↓
9. Si @Id(autoGenerated=true): Recupera ID generado y lo asigna al objeto
```

---

### UPDATE

```
1. Desarrollador: db.update(user)
                     ↓
2. KurubindDatabase: Extrae EntityMetadata
                     ↓
3. Valida: Entidad debe tener campo @Id
                     ↓
4. VALUE GENERATION: Para cada campo con @Generated:
   - Si onUpdate=true:
     * Ejecuta generator y asigna valor
                     ↓
5. VALIDATION: Para cada campo (igual que INSERT):
   - Busca validators
   - Ejecuta validaciones
   - Acumula errores
   - Lanza ValidationException si hay fallos
                     ↓
6. SQLGenerator: generateUpdate(metadata, fields)
                     ↓
7. SQL generado: UPDATE users SET username=:username, email=:email WHERE id=:id
                     ↓
8. Para cada campo (excepto @Id):
   - Si tiene handler: handler.handleWrite(valor)
   - Bind valor al statement
                     ↓
9. Bind ID al WHERE
                     ↓
10. JDBI: Ejecuta SQL
```

---

### DELETE

```
1. Desarrollador: db.delete(user)
                     ↓
2. KurubindDatabase: Extrae EntityMetadata
                     ↓
3. Valida: Entidad debe tener campo @Id
                     ↓
4. SQLGenerator: generateDelete(metadata)
                     ↓
5. SQL generado: DELETE FROM users WHERE id=:id
                     ↓
6. Bind ID
                     ↓
7. JDBI: Ejecuta SQL
```

**Nota:** DELETE no ejecuta validaciones ni generación de valores.

---

### SELECT (list)

```
1. Desarrollador: db.list(User.class)
                     ↓
2. KurubindDatabase: Extrae EntityMetadata
                     ↓
3. SQLGenerator: generateSelect(metadata)
                     ↓
4. SQL generado: SELECT * FROM users
                     ↓
5. JDBI: Ejecuta SQL → ResultSet
                     ↓
6. RowMapper: Para cada fila:
   - Crea instancia de User
   - Para cada campo @Column:
     * Lee valor de ResultSet
     * Si tiene handler: handler.handleRead(valor)
     * Asigna al campo
   - Retorna User completo
                     ↓
7. KurubindDatabase: Retorna List<User>
```

**Nota:** SELECT no ejecuta validaciones ni generación de valores.

---

### SELECT (query custom)

```
1. Desarrollador: db.query("SELECT * FROM users WHERE age > 18", User.class)
                     ↓
2. KurubindDatabase: Usa SQL proporcionado (sin generar)
                     ↓
3. JDBI: Ejecuta SQL → ResultSet
                     ↓
4. RowMapper: Mapea ResultSet → List<User> (mismo proceso que list)
                     ↓
5. KurubindDatabase: Retorna List<User>
```

---

## 7️⃣ Configuración y Construcción

### Builder Pattern

```java
KurubindDatabase db = KurubindDatabase.builder()
    .withJdbi(jdbi)                              // JDBI instance
    .withJdbiProvider(jdbiProvider)              // O JdbiProvider para multitenancy
    .withHandlerRegistry(handlerRegistry)        // Handlers custom
    .withSQLGeneratorRegistry(sqlRegistry)       // Generadores SQL custom
    .withValidatorRegistry(validatorRegistry)    // Validators (incluye los base)
    .withValueGeneratorRegistry(generatorRegistry) // Value generators
    .withDialect(new Dialect("POSTGRESQL"))      // Dialecto activo
    .build();
```

**Reglas:**
- `withJdbi()` y `withJdbiProvider()` son mutuamente excluyentes
- Debe haber al menos uno de los dos
- Si no se proporciona `HandlerRegistry`, usa uno vacío
- Si no se proporciona `SQLGeneratorRegistry`, usa `GenericSQLGenerator`
- Si no se proporciona `ValidatorRegistry`, usa uno con validators base (`NotNullValidator`, `MinValidator`, `MaxValidator`)
- Si no se proporciona `ValueGeneratorRegistry`, usa uno vacío
- El dialecto es opcional (solo necesario si hay handlers/generators específicos)

---

### Configuración Mínima

```java
Jdbi jdbi = Jdbi.create("jdbc:h2:mem:test");

KurubindDatabase db = KurubindDatabase.builder()
    .withJdbi(jdbi)
    .build();

// Usa:
// - GenericSQLGenerator
// - Sin handlers
custom
// - Validators base (NotNull, Min, Max)
// - Sin value generators
```

---

### Configuración Completa

```java
// 1. Crear JDBI
Jdbi jdbi = Jdbi.create(
    "jdbc:postgresql://localhost:5432/mydb",
    "user",
    "password"
);

// 2. Registrar Handlers
HandlerRegistry handlerRegistry = new HandlerRegistry();
handlerRegistry.register(
    JsonColumn.class,
    new Dialect("POSTGRESQL"),
    new PostgresJsonHandler()
);

// 3. Registrar SQL Generators
SQLGeneratorRegistry sqlRegistry = new SQLGeneratorRegistry();
sqlRegistry.register(
    new Dialect("POSTGRESQL"),
    new PostgreSQLGenerator()
);

// 4. Registrar Validators (los base ya están incluidos)
ValidatorRegistry validatorRegistry = new ValidatorRegistry();
// Validators base ya registrados automáticamente:
// - NotNullValidator para @NotNull
// - MinValidator para @Min
// - MaxValidator para @Max

// Registrar validators custom
validatorRegistry.register(Email.class, new EmailValidator());
validatorRegistry.register(Pattern.class, new PatternValidator());

// 5. Registrar Value Generators
ValueGeneratorRegistry generatorRegistry = new ValueGeneratorRegistry();
generatorRegistry.register("UUID", new UuidGenerator());
generatorRegistry.register("CREATED_AT", new CreatedAtGenerator());
generatorRegistry.register("UPDATED_AT", new UpdatedAtGenerator());
generatorRegistry.register("PRODUCT_CODE", new ProductCodeGenerator());

// 6. Construir KurubindDatabase
KurubindDatabase db = KurubindDatabase.builder()
    .withJdbi(jdbi)
    .withHandlerRegistry(handlerRegistry)
    .withSQLGeneratorRegistry(sqlRegistry)
    .withValidatorRegistry(validatorRegistry)
    .withValueGeneratorRegistry(generatorRegistry)
    .withDialect(new Dialect("POSTGRESQL"))
    .build();
```

---

## 8️⃣ Ejemplos de Uso

### Entidad Simple con Validación

```java
@Table(name = "products")
public class Product {
    @Id
    @Column("id")
    private Long id;

    @Column("name")
    @NotNull(message = "Product name is required")
    private String name;

    @Column("price")
    @NotNull
    @Min(value = 0, message = "Price cannot be negative")
    private Double price;

    @Column("stock")
    @Min(0)
    @Max(10000)
    private Integer stock;

    @Column("active")
    @DefaultValue(value = "true")
    private Boolean active;

    // Constructor, getters, setters
}
```

---

### Entidad con Value Generators

```java
@Table(name = "orders")
public class Order {
    @Id
    @Column("id")
    private Long id;

    @Column("order_code")
    @DefaultValue(generator = "ORDER_CODE")
    private String orderCode;

    @Column("customer_id")
    @NotNull
    private Long customerId;

    @Column("total")
    @NotNull
    @Min(0)
    private Double total;

    @Column("created_at")
    @DefaultValue(generator = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column("updated_at")
    @Generated(generator = "UPDATED_AT", onInsert = true, onUpdate = true)
    private LocalDateTime updatedAt;

    @Column("status")
    @DefaultValue(value = "PENDING")
    private String status;

    // Constructor, getters, setters
}

// Registrar generator de ORDER_CODE
class OrderCodeGenerator implements ValueGenerator {
    private AtomicLong counter = new AtomicLong(1000);
    
    public Object generate(Object entity, FieldMetadata field) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return "ORD-" + now.format(formatter) + "-" + counter.incrementAndGet();
    }
    
    public boolean generateOnInsert() { return true; }
    public boolean generateOnUpdate() { return false; }
}

// Registrar
generatorRegistry.register("ORDER_CODE", new OrderCodeGenerator());
```

---

### Uso CRUD con Validación

```java
// INSERT - con validación automática
Product product = new Product();
product.setName("Laptop");
product.setPrice(999.99);
product.setStock(50);

try {
    db.insert(product);
    // Validaciones OK, producto insertado
    // active = true (valor por defecto)
} catch (ValidationException e) {
    // Obtener todos los errores
    for (ValidationError error : e.getErrors()) {
        System.err.println(error.getFieldName() + ": " + error.getMessage());
    }
}

// INSERT - violación de validación
Product invalid = new Product();
invalid.setName(null);  // Viola @NotNull
invalid.setPrice(-100.0);  // Viola @Min(0)
invalid.setStock(15000);  // Viola @Max(10000)

try {
    db.insert(invalid);
} catch (ValidationException e) {
    // Captura TODOS los errores juntos:
    // name: Product name is required
    // price: Price cannot be negative
    // stock: Value must be at most 10000
    e.getErrors().forEach(err -> 
        System.err.println(err.getFieldName() + ": " + err.getMessage())
    );
}

// UPDATE - con generación automática
product.setPrice(899.99);
db.update(product);
// updated_at se genera automáticamente (si está configurado)

// LIST
List<Product> all = db.list(Product.class);
```

---

### Entidad con Generación Compleja

```java
@Table(name = "users")
public class User {
    @Id
    @Column("id")
    private Long id;

    @Column("uuid")
    @DefaultValue(generator = "UUID")
    private String uuid;

    @Column("username")
    @NotNull
    private String username;

    @Column("email")
    @NotNull(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @Column("password_hash")
    @NotNull
    private String passwordHash;

    @Column("created_at")
    @DefaultValue(generator = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column("updated_at")
    @Generated(generator = "UPDATED_AT", onInsert = true, onUpdate = true)
    private LocalDateTime updatedAt;

    @Column("status")
    @DefaultValue(value = "ACTIVE")
    private String status;

    @Transient
    private String plainPassword;  // No se persiste

    // Constructor, getters, setters
}

// Uso
User user = new User();
user.setUsername("johndoe");
user.setEmail("john@example.com");
user.setPasswordHash(hashPassword("secret123"));

db.insert(user);
// Después del insert:
// - uuid generado automáticamente
// - created_at = LocalDateTime.now()
// - updated_at = LocalDateTime.now()
// - status = "ACTIVE"
```

---

### DTO para Query Compleja

```java
@QueryResponse
public class OrderSummary {
    @Column("order_id")
    private Long orderId;
    
    @Column("order_code")
    private String orderCode;
    
    @Column("customer_name")
    private String customerName;
    
    @Column("total_amount")
    private Double totalAmount;
    
    @Column("item_count")
    private Integer itemCount;
    
    @Column("order_date")
    private LocalDateTime orderDate;

    // Getters, setters
}

// Query con JOIN y agregación
List<OrderSummary> summary = db.query("""
    SELECT 
        o.id as order_id,
        o.order_code,
        c.name as customer_name,
        SUM(oi.price * oi.quantity) as total_amount,
        COUNT(oi.id) as item_count,
        o.created_at as order_date
    FROM orders o
    JOIN customers c ON o.customer_id = c.id
    JOIN order_items oi ON o.id = oi.order_id
    WHERE o.status = 'COMPLETED'
    GROUP BY o.id, o.order_code, c.name, o.created_at
    ORDER BY o.created_at DESC
""", OrderSummary.class);
```

---

### Repositorio Especializado con Validación

```java
public class ProductRepository extends Repository<Product> {
    
    public ProductRepository(KurubindDatabase db) {
        super(db, Product.class);
    }
    
    public List<Product> findActiveProducts() {
        return query(
            "SELECT * FROM products WHERE active = true",
            Product.class
        );
    }
    
    public List<Product> findByPriceRange(double min, double max) {
        // Validar parámetros antes de query
        if (min < 0 || max < 0 || min > max) {
            throw new IllegalArgumentException("Invalid price range");
        }
        
        return db.query(
            "SELECT * FROM products WHERE price BETWEEN :min AND :max",
            Product.class
        );
    }
    
    public void decreaseStock(Long productId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        db.execute(handle -> {
            handle.createUpdate("""
                UPDATE products 
                SET stock = stock - :quantity,
                    updated_at = NOW()
                WHERE id = :id AND stock >= :quantity
            """)
            .bind("id", productId)
            .bind("quantity", quantity)
            .execute();
        });
    }
}

// Uso
ProductRepository repo = new ProductRepository(db);

Product product = new Product();
product.setName("Mouse");
product.setPrice(29.99);
product.setStock(100);

try {
    repo.insert(product);  // Validación automática
    System.out.println("Product inserted with ID: " + product.getId());
} catch (ValidationException e) {
    e.getErrors().forEach(err -> 
        System.err.println("Validation error: " + err.getMessage())
    );
}
```

---

### Multitenancy con Validación

```java
// Provider que selecciona JDBI según tenant
JdbiProvider multitenantProvider = () -> {
    String tenantId = TenantContext.getCurrentTenant();
    Jdbi jdbi = tenantConnectionPool.getJdbi(tenantId);
    if (jdbi == null) {
        throw new IllegalStateException("No JDBI for tenant: " + tenantId);
    }
    return jdbi;
};

// Configurar con validación
ValidatorRegistry validatorRegistry = new ValidatorRegistry();
// Base validators ya incluidos

ValueGeneratorRegistry generatorRegistry = new ValueGeneratorRegistry();
generatorRegistry.register("UUID", new UuidGenerator());
generatorRegistry.register("CREATED_AT", new CreatedAtGenerator());

KurubindDatabase db = KurubindDatabase.builder()
    .withJdbiProvider(multitenantProvider)
    .withValidatorRegistry(validatorRegistry)
    .withValueGeneratorRegistry(generatorRegistry)
    .build();

// Usar
TenantContext.setTenant("tenant1");

Product product = new Product();
product.setName("Product for Tenant 1");
product.setPrice(99.99);

try {
    db.insert(product);  
    // Se valida y se inserta en tenant1_db
} catch (ValidationException e) {
    // Manejar errores de validación
}

TenantContext.clear();
```

---

## 9️⃣ Extensión por el Desarrollador

### Crear Validator Custom

```java
// 1. Crear anotación
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Email {
    String message() default "Invalid email format";
}

// 2. Implementar validator
public class EmailValidator implements Validator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    @Override
    public void validate(Object value, FieldMetadata field) {
        if (value == null) return;  // @NotNull se encarga de nulls
        
        String email = value.toString();
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new ValidationException(
                getErrorMessage(value, field)
            );
        }
    }
    
    @Override
    public String getErrorMessage(Object value, FieldMetadata field) {
        Email emailAnnotation = field.getAnnotation(Email.class);
        return emailAnnotation.message() + ": " + value;
    }
}

// 3. Registrar
validatorRegistry.register(Email.class, new EmailValidator());

// 4. Usar
@Table(name = "users")
public class User {
    @Column("email")
    @NotNull
    @Email(message = "Please provide a valid email address")
    private String email;
}
```

---

### Crear ValueGenerator Custom

```java
// 1. Implementar generator
public class SkuGenerator implements ValueGenerator {
    private final String prefix;
    private final AtomicLong counter;
    
    public SkuGenerator(String prefix, long startValue) {
        this.prefix = prefix;
        this.counter = new AtomicLong(startValue);
    }
    
    @Override
    public Object generate(Object entity, FieldMetadata field) {
        // Formato: PREFIX-YYYYMMDD-SEQUENCE
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        long sequence = counter.incrementAndGet();
        
        return String.format(
            "%s-%s-%06d",
            prefix,
            now.format(formatter),
            sequence
        );
    }
    
    @Override
    public boolean generateOnInsert() {
        return true;
    }
    
    @Override
    public boolean generateOnUpdate() {
        return false;
    }
}

// 2. Registrar
generatorRegistry.register(
    "PRODUCT_SKU",
    new SkuGenerator("PRD", 1000)
);

generatorRegistry.register(
    "ORDER_SKU",
    new SkuGenerator("ORD", 5000)
);

// 3. Usar
@Table(name = "products")
public class Product {
    @Column("sku")
    @DefaultValue(generator = "PRODUCT_SKU")
    private String sku;
}

// Cuando se inserte:
// sku = "PRD-20250101-001001"
```

---

### Crear Handler Custom

```java
// 1. Crear anotación
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Encrypted {
    String value();  // Nombre de columna
}

// 2. Implementar handler
public class EncryptionHandler implements Handler {
    private final Cipher encryptCipher;
    private final Cipher decryptCipher;
    
    public EncryptionHandler(String key) {
        try {
            SecretKeySpec secretKey = new SecretKeySpec(
                key.getBytes("UTF-8"),
                "AES"
            );
            
            encryptCipher = Cipher.getInstance("AES");
            encryptCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            
            decryptCipher = Cipher.getInstance("AES");
            decryptCipher.init(Cipher.DECRYPT_MODE, secretKey);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption", e);
        }
    }
    
    @Override
    public Object handleWrite(Object javaValue) {
        if (javaValue == null) return null;
        try {
            byte[] encrypted = encryptCipher.doFinal(
                javaValue.toString().getBytes("UTF-8")
            );
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }
    
    @Override
    public Object handleRead(Object dbValue) {
        if (dbValue == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(dbValue.toString());
            byte[] decrypted = decryptCipher.doFinal(decoded);
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}

// 3. Registrar
handlerRegistry.register(
    Encrypted.class,
    new EncryptionHandler("my-secret-key-16b")
);

// 4. Usar
@Table(name = "users")
public class User {
    @Encrypted("ssn")
    private String socialSecurityNumber;
    
    @Encrypted("credit_card")
    private String creditCardNumber;
}
```

---

### Crear SQLGenerator Custom

```java
public class PostgreSQLGenerator implements SQLGenerator {
    
    @Override
    public String generateInsert(EntityMetadata meta, List<FieldMetadata> fields) {
        String tableName = meta.getFullTableName();
        
        String columns = fields.stream()
            .map(FieldMetadata::getColumnName)
            .collect(Collectors.joining(", "));
        
        String placeholders = fields.stream()
            .map(this::getPlaceholder)
            .collect(Collectors.joining(", "));
        
        String sql = String.format(
            "INSERT INTO %s (%s) VALUES (%s)",
            tableName, columns, placeholders
        );
        
        // PostgreSQL: usar RETURNING para obtener ID
        if (meta.hasAutoGeneratedId()) {
            sql += " RETURNING " + meta.getIdField().getColumnName();
        }
        
        return sql;
    }
    
    @Override
    public String generateUpdate(EntityMetadata meta, List<FieldMetadata> fields) {
        String tableName = meta.getFullTableName();
        
        String setClause = fields.stream()
            .filter(f -> !f.isId())
            .map(f -> f.getColumnName() + " = " + getPlaceholder(f))
            .collect(Collectors.joining(", "));
        
        return String.format(
            "UPDATE %s SET %s WHERE %s = :%s",
            tableName,
            setClause,
            meta.getIdField().getColumnName(),
            meta.getIdField().getColumnName()
        );
    }
    
    @Override
    public String generateDelete(EntityMetadata meta) {
        return String.format(
            "DELETE FROM %s WHERE %s = :%s",
            meta.getFullTableName(),
            meta.getIdField().getColumnName(),
            meta.getIdField().getColumnName()
        );
    }
    
    @Override
    public String generateSelect(EntityMetadata meta) {
        return "SELECT * FROM " + meta.getFullTableName();
    }
    
    @Override
    public String getPlaceholder(FieldMetadata field) {
        String placeholder = ":" + field.getColumnName();
        
        // Agregar cast para JSON en PostgreSQL
        if (field.hasAnnotation(JsonColumn.class)) {
            placeholder += "::jsonb";
        }
        
        return placeholder;
    }
}

// Registrar
sqlRegistry.register(
    new Dialect("POSTGRESQL"),
    new PostgreSQLGenerator()
);
```

---

## 🔟 Matriz de Responsabilidades

| Componente | Genera SQL | Ejecuta SQL | Transforma Datos | Valida Datos | Genera Valores | Mapea ResultSet | Extensible |
|------------|------------|-------------|------------------|--------------|----------------|-----------------|------------|
| **KurubindDatabase** | ❌ Delega | ✅ Sí | ❌ Delega | ❌ Delega | ❌ Delega | ❌ Delega | ✅ Builder |
| **SQLGenerator** | ✅ Sí | ❌ No | ❌ No | ❌ No | ❌ No | ❌ No | ✅ Implementable |
| **Handler** | ❌ No | ❌ No | ✅ Sí | ❌ No | ❌ No | ❌ No | ✅ Implementable |
| **Validator** | ❌ No | ❌ No | ❌ No | ✅ Sí | ❌ No | ❌ No | ✅ Implementable |
| **ValueGenerator** | ❌ No | ❌ No | ❌ No | ❌ No | ✅ Sí | ❌ No | ✅ Implementable |
| **RowMapper** | ❌ No | ❌ No | ❌ Delega | ❌ No | ❌ No | ✅ Sí | ❌ Interno |
| **Repository** | ❌ No | ❌ Delega | ❌ Delega | ❌ Delega | ❌ Delega | ❌ Delega | ✅ Extendible |

---

## 1️⃣1️⃣ Lo que Kurubind HACE y NO HACE

### ✅ Lo que Kurubind HACE

1. Proporciona anotaciones base para mapeo tabla-clase
2. Genera SQL para operaciones CRUD comunes (delegando al SQLGenerator)
3. **Valida datos antes de INSERT/UPDATE usando validators**
4. **Genera valores automáticamente usando value generators**
5. Ejecuta SQL usando JDBI
6. Mapea ResultSet a objetos Java
7. Aplica handlers para transformar datos
8. Proporciona interfaces para extensión (Handler, SQLGenerator, Validator, ValueGenerator)
9. Soporta entidades y DTOs
10. Permite SQL manual cuando se necesita
11. **Proporciona validators base (NotNull, Min, Max)**
12. **Acumula todos los errores de validación antes de lanzar excepción**

### ❌ Lo que Kurubind NO HACE

1. NO es un ORM completo (no lazy loading, no cascadas, no cache)
2. NO conoce anotaciones custom (más allá de las base)
3. NO genera SQL específico de dialectos internamente
4. NO transforma datos específicos internamente (delega a handlers)
5. NO maneja migraciones de schema
6. NO maneja transacciones (usar JDBI directamente)
7. NO genera clases en tiempo de compilación

### ✅ Lo que el DESARROLLADOR HACE

1. Crea anotaciones custom si las necesita
2. Implementa handlers para sus anotaciones
3. Implementa validators para validaciones custom
4. Implementa value generators para generación de valores custom
5. Implementa SQLGenerators si necesita optimización por dialecto
6. Registra handlers, validators, generators en las registries
7. Escribe SQL manual cuando necesita control total
8. Maneja transacciones usando JDBI

---

## 1️⃣2️⃣ Preguntas Frecuentes

### ¿Cómo se ejecutan las validaciones?

Las validaciones se ejecutan automáticamente antes de INSERT y UPDATE:

```java
Product product = new Product();
product.setPrice(-100.0);  // Viola @Min(0)
product.setStock(15000);   // Viola @Max(10000)

try {
    db.insert(product);
} catch (ValidationException e) {
    // Obtener TODOS los errores juntos
    List<ValidationError> errors = e.getErrors();
    // errors[0]: price - Price cannot be negative
    // errors[1]: stock - Value must be at most 10000
}
```

---

### ¿Puedo desactivar las validaciones?

No directamente, pero puedes crear un `ValidatorRegistry` vacío:

```java
ValidatorRegistry emptyRegistry = new ValidatorRegistry();
// No registrar ningún validator

KurubindDatabase db = KurubindDatabase.builder()
    .withJdbi(jdbi)
    .withValidatorRegistry(emptyRegistry)
    .build();

// Ahora no se ejecutan validaciones
```

---

### ¿Cuál es la diferencia entre @DefaultValue y @Generated?

**@DefaultValue:**
- Solo se aplica si el campo es `null`
- Solo en INSERT
- Puede usar un literal o un generator

**@Generated:**
- Siempre se aplica (sobrescribe el valor existente)
- Puede configurarse para INSERT, UPDATE o ambos
- Siempre usa un generator

```java
// DefaultValue: Solo si es null
@Column("status")
@DefaultValue(value = "ACTIVE")
private String status;

user.setStatus("INACTIVE");
db.insert(user);  // status = "INACTIVE" (no se sobrescribe)

user.setStatus(null);
db.insert(user);  // status = "ACTIVE" (se aplica default)

// Generated: Siempre se sobrescribe
@Column("updated_at")
@Generated(generator = "UPDATED_AT", onUpdate = true)
private LocalDateTime updatedAt;

user.setUpdatedAt(LocalDateTime.of(2020, 1, 1, 0, 0));
db.update(user);  // updated_at = LocalDateTime.now() (se sobrescribe)
```

---

### ¿Los value generators se ejecutan antes o después de la validación?

**ANTES.** El flujo es:

1. Generar valores (@DefaultValue, @Generated)
2. Validar datos (@NotNull, @Min, @Max, etc.)
3. Transformar datos (Handlers)
4. Ejecutar SQL

Esto permite que los validators validen valores generados automáticamente.

---

### ¿Puedo tener múltiples validators en un campo?

Sí, se ejecutan todos:

```java
@Column("email")
@NotNull(message = "Email is required")
@Email(message = "Invalid email format")
@Length(min = 5, max = 100)
private String email;

// Se ejecutan en orden:
// 1. NotNullValidator
// 2. EmailValidator
// 3. LengthValidator
```

---

### ¿Cómo creo un validator que valida múltiples campos?

Validators base validan campos individuales. Para validaciones complejas, usa lógica en el Repository:

```java
public class OrderRepository extends Repository<Order> {
    @Override
    public void insert(Order order) {
        // Validación custom multi-campo
        if (order.getEndDate().isBefore(order.getStartDate())) {
            throw new ValidationException(
                "End date cannot be before start date"
            );
        }
        
        super.insert(order);
    }
}
```

---

### ¿Los value generators tienen acceso a la entidad completa?

Sí, el método `generate` recibe la entidad:

```java
public class FullNameGenerator implements ValueGenerator {
    public Object generate(Object entity, FieldMetadata field) {
        if (entity instanceof User) {
            User user = (User) entity;
            return user.getFirstName() + " " + user.getLastName();
        }
        return null;
    }
    
    public boolean generateOnInsert() { return true; }
    public boolean generateOnUpdate() { return true; }
}
```

---

### ¿Puedo usar el mismo generator con diferentes configuraciones?

Sí, crea múltiples instancias:

```java
// Generator parametrizable
class PrefixedCodeGenerator implements ValueGenerator {
    private final String prefix;
    private final AtomicLong counter;
    
    public PrefixedCodeGenerator(String prefix, long start) {
        this.prefix = prefix;
        this.counter = new AtomicLong(start);
    }
    
    public Object generate(Object entity, FieldMetadata field) {
        return prefix + "-" + counter.incrementAndGet();
    }
    
    public boolean generateOnInsert() { return true; }
    public boolean generateOnUpdate() { return false; }
}

// Registrar múltiples instancias
generatorRegistry.register(
    "PRODUCT_CODE",
    new PrefixedCodeGenerator("PRD", 1000)
);

generatorRegistry.register(
    "ORDER_CODE",
    new PrefixedCodeGenerator("ORD", 5000)
);

generatorRegistry.register(
    "INVOICE_CODE",
    new PrefixedCodeGenerator("INV", 10000)
);
```
