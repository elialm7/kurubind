# KuruBind

[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![Thread-Safe](https://img.shields.io/badge/Thread--Safe-100%25-green.svg)]()

KuruBind is a lightweight, annotation-driven database library for Java that sits in the "sweet spot" between the
complexity of a full-blown ORM (like Hibernate/JPA) and the manual effort of raw SQL (like plain JDBI or JDBC).

It is built on top of JDBI 3 and is designed for developers who want to work with existing databases ("brownfield"
projects) without the steep learning curve or configuration overhead of mapping every relationship and constraint.

KuruBind lets you map POJOs to tables, handles 90% of your CRUD operations automatically, and gets out of your way when
you need to write powerful, custom SQL.

## The KuruBind Philosophy

KuruBind is for you if:

- You are working with an existing database and don't want to (or can't) let an ORM manage your schema.
- You find full ORMs too complex for your project's needs. You don't want to deal with session management, lazy-loading
  proxies, or complex relationship mapping.
- You find writing raw SQL and manual mapping for every query tedious and repetitive.
- You want a simple, convention-based Repository pattern for your entities.
- You want an extensible system for adding custom type conversions, validation, and value generation.

KuruBind gives you the best of both worlds: the simplicity of POJO mapping for common operations and the full power of
JDBI for everything else.

## Core Features

- **Annotation-Based Mapping**: Simple annotations (`@Table`, `@Column`, `@Id`) to map your Java POJOs to database
  tables.
- **Generic Repository**: An out-of-the-box `KuruRepository` for all your standard CRUD operations (insert, update,
  deleteById, findById, findAll, etc.).
- **Automatic SQL Generation**: Generates simple, dialect-aware SQL for insert, update, delete, and select operations.
- **Powerful Extensibility**:
    - **Modules**: Package your custom logic into reusable `KurubindModules`.
    - **Handlers**: Create custom type converters (e.g., Java Enum <-> String, JSON <-> Map).
    - **Validators**: Hook into insert/update operations to validate data.
    - **ValueGenerators**: Automatically generate values on insert/update (e.g., UUIDs, timestamps).
    - **Dialects & SQLGenerators**: Customize the generated SQL for specific databases.
    - **Meta-Annotations**: Compose annotations to create simple, reusable annotations for your entities (e.g., create a
      `@CreatedAt` annotation that bundles `@Generated`).
- **Full SQL Access**: Drop down to raw SQL with full parameter binding and POJO mapping (`@QueryResponse`) at any time.

## Getting Started: Configuration

The main entry point is `KurubindDatabase`. You configure it using a builder, providing it with a JDBI instance.

```java
import org.jdbi.v3.core.Jdbi;
import com.roelias.kurubind.KurubindDatabase;
import com.roelias.kurubind.core.Dialect;

// 1. Create and configure your Jdbi instance (e.g., with a DataSource)
Jdbi jdbi = Jdbi.create("jdbc:postgresql://localhost:5432/mydb", "user", "pass");

        // 2. Build your KurubindDatabase instance
        KurubindDatabase db = KurubindDatabase.builder()
                .withJdbi(jdbi)
                .withDialect(new Dialect("POSTGRESQL")) // Optional, defaults to ANSI
                // .installModule(new MyCustomModule()) // Optional: Add your extensions
                .build();

// 3. You are ready to go!
```

## Defining Entities

You define entities as simple POJOs.

- `@Table`: (On class) Specifies the table name and (optional) schema.
- `@QueryResponse`: (On class) Marks a POJO as a container for custom query results, not a physical table. CRUD
  operations will be disabled for these.
- `@Column`: (On field) Specifies the column name. If omitted, the field name is used.
- `@Id`: (On field) Marks the primary key field. `autogenerate=true` (default) will fetch the generated key from the DB
  on insert.
- `@Transient`: (On field) Ignores this field completely; it will not be mapped or persisted.
- `@Generated`: (On field) Uses a `ValueGenerator` to populate this field. You can specify `onInsert` and `onUpdate`.
- `@DefaultValue`: (On field) Provides a default value if the field is null during an insert, either from a literal
  value or a generator.

### Example Entity

```java
import com.roelias.kurubind.annotations.*;

import java.time.Instant;

@Table(name = "users", schema = "public")
public class User {

    @Id
    @Column("user_id")
    private Long userId;

    @Column("username")
    private String username;

    @Column("email")
    private String email;

    // This field will be populated by a ValueGenerator named "timestamp"
    @Generated(generator = "timestamp", onInsert = true, onUpdate = true)
    @Column("updated_at")
    private Instant updatedAt;

    // This field is not stored in the database
    @Transient
    private String someInternalState;

    // Getters and Setters...
}
```

## Using KuruRepository (The Easy Way)

For most use cases, you'll interact with a `KuruRepository`. It provides a typed API for all common operations.

```java
import com.roelias.kurubind.repository.KuruRepository;

// 1. Create a repository for your entity
KuruRepository<User> userRepo = new KuruRepository<>(db, User.class);

        // 2. Use the built-in methods
        User newUser = new User();
newUser.

        setUsername("testuser");
newUser.

        setEmail("test@example.com");

// --- Create ---
userRepo.

        insert(newUser);
// newUser.getUserId() is now populated (if @Id(autogenerate=true))

        // --- Read ---
        Optional<User> foundUser = userRepo.findById(newUser.getUserId());
        List<User> allUsers = userRepo.findAll();

// --- Update ---
foundUser.

        ifPresent(user ->{
        user.

        setEmail("new-email@example.com");
    userRepo.

        update(user);
});

// --- Delete ---
        userRepo.

        deleteById(newUser.getUserId());

        // --- Count ---
        long count = userRepo.countAll();

        // --- Exists ---
        boolean exists = userRepo.exists(1L);
```

## Using KurubindDatabase (Core Methods)

The `KuruRepository` is just a convenience wrapper. You can also use the `KurubindDatabase` object directly. This is
useful if you don't want to create a repository class or if you are working with multiple entity types.

The method signatures are intuitive:

- Operations on a specific, known entity: `insert(T entity)`, `update(T entity)`, `delete(T entity)`
- Operations that need a target type: `findById(Class<T> type, Object id)`, `findAll(Class<T> type)`,
  `deleteById(Class<T> type, Object id)`

```java
// --- Insert ---
db.insert(newUser);

// --- Find By Id ---
Optional<User> user = db.findById(User.class, 1L);

// --- Find All ---
List<User> users = db.findAll(User.class);

// --- Delete By Id ---
db.

deleteById(User .class, 1L);

// --- Count ---
long count = db.countAll(User.class);
```

## Advanced: Custom Queries

This is where KuruBind shines. When you need to write custom SQL, you get all the power of JDBI with KuruBind's row
mapping.

You can map results to your `@Table` entities or create dedicated `@QueryResponse` POJOs.

```java
// 1. Define a @QueryResponse POJO (or just use your @Table entity)
@QueryResponse
public class UserEmail {
    @Column("username")
    private String username;

    @Column("email")
    private String email;

    // Getters/Setters...
}

// 2. Write your SQL
String sql = "SELECT username, email FROM users WHERE username LIKE :prefix";
Map<String, Object> params = Map.of("prefix", "test%");

// 3. Query and map to your POJO
List<UserEmail> results = db.query(sql, UserEmail.class, params);

// Or query for a single object
Optional<User> user = db.queryOne("SELECT * FROM users WHERE user_id = :id", User.class, Map.of("id", 1L));

// Or query for generic Maps
List<Map<String, Object>> maps = db.queryForMaps(sql, params);

// Or query for a single primitive
Optional<String> email = db.queryForObject(
        "SELECT email FROM users WHERE user_id = :id",
        String.class,
        Map.of("id", 1L)
);

// Execute updates
int rowsAffected = db.executeUpdate(
        "UPDATE users SET email = :email WHERE user_id = :id",
        Map.of("email", "new@example.com", "id", 1L)
);
```

## Extending KuruBind

KuruBind is built on a modular, extensible core. You can customize almost any part of its behavior.

All extensions are registered by creating a `KurubindModule` and installing it with the builder.

```java
import com.roelias.kurubind.core.KurubindModule;
import com.roelias.kurubind.core.RegistryCollector;

public class MyCustomModule implements KurubindModule {
    @Override
    public void configure(RegistryCollector registries) {
        // Register all your custom components here
        registries.valueGenerators().register("uuid", new UuidGenerator());
        registries.valueGenerators().register("timestamp", new TimestampGenerator());

        registries.handlers().register(Jsonb.class, new JsonbHandler());

        registries.validators().register(NotNull.class, new NotNullValidator());

        registries.sqlGenerators().register(new Dialect("POSTGRESQL"), new PostgresSQLGenerator());
    }
}

// Then install it:
KurubindDatabase db = KurubindDatabase.builder()
        .withJdbi(jdbi)
        .installModule(new MyCustomModule())
        .build();
```

### 1. ValueGenerator

Used by `@Generated` and `@DefaultValue(generator=...)`.

```java
import com.roelias.kurubind.core.ValueGenerator;
import com.roelias.kurubind.metadata.FieldMetadata;

// Generates an Instant on insert or update
public class TimestampGenerator implements ValueGenerator {
    @Override
    public Object generate(Object entity, FieldMetadata field) {
        return Instant.now();
    }

    // Note: @Generated annotation controls onInsert/onUpdate,
    // these methods are for @DefaultValue
    @Override
    public boolean generateOnInsert() {
        return true;
    }

    @Override
    public boolean generateOnUpdate() {
        return true;
    }
}
```

### 2. Handler

Used for custom type conversion between Java and SQL. It's triggered by annotations on a field.

```java
import com.roelias.kurubind.core.Handler;
import com.roelias.kurubind.metadata.FieldMetadata;

// 1. Create an annotation

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Jsonb {
}

// 2. Create the Handler
// (Assumes a 'PGobject' from Postgres and a 'Map' in Java)
public class JsonbHandler implements Handler {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Object handleWrite(Object javaValue, FieldMetadata fieldMeta) {
        // Convert Map -> JSON String -> PGobject
        try {
            PGobject pgObject = new PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue(mapper.writeValueAsString(javaValue));
            return pgObject;
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize JSONB", e);
        }
    }

    @Override
    public Object handleRead(Object dbValue, FieldMetadata fieldMeta) {
        // Convert PGobject -> JSON String -> Map
        try {
            String json = ((PGobject) dbValue).getValue();
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSONB", e);
        }
    }
}

// 3. Register it in your module
registries.

handlers().

register(Jsonb .class, new JsonbHandler());

// 4. Use it in your entity
@Jsonb
@Column("metadata")
private Map<String, Object> metadata;
```

### 3. Validator

Used to run validation logic before an insert or update.

```java
import com.roelias.kurubind.core.Validator;
import com.roelias.kurubind.metadata.FieldMetadata;
import com.roelias.kurubind.exceptions.ValidationException;

// 1. Create an annotation

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NotNullOrEmpty {
}

// 2. Create the Validator
public class NotNullOrEmptyValidator implements Validator {
    @Override
    public void validate(Object value, FieldMetadata field) throws ValidationException {
        if (value == null) {
            throw new ValidationException("Field '" + field.getFieldName() + "' cannot be null");
        }
        if (value instanceof String && ((String) value).isEmpty()) {
            throw new ValidationException("Field '" + field.getFieldName() + "' cannot be empty");
        }
    }
}

// 3. Register it
registries.

validators().

register(NotNullOrEmpty .class, new NotNullOrEmptyValidator());

// 4. Use it
@NotNullOrEmpty
@Column("username")
private String username;
```

### 4. Dialect and SQLGenerator

You can override the default SQL generation logic for a specific dialect.

```java
import com.roelias.kurubind.ootb.DefaultSQLGenerator;

// 1. Create a custom SQL Generator

public class PostgresSQLGenerator extends DefaultSQLGenerator {

    // Override just the insert statement to add 'RETURNING *'
    @Override
    public String generateInsert(EntityMetadata meta, List<FieldMetadata> fields) {
        String baseSql = super.generateInsert(meta, fields);
        if (meta.hasAutoGeneratedId()) {
            return baseSql + " RETURNING " + meta.getIdField().getColumnName();
        }
        return baseSql;
    }
}

// 2. Register it
registries.

sqlGenerators().

register(new Dialect("POSTGRESQL"), new

PostgresSQLGenerator());
```

### 5. Meta-Annotations (Composition)

KuruBind's annotation processor recursively scans for annotations. This means you can create your own "shortcut"
annotations.

```java
import java.lang.annotation.*;

/**
 * A custom annotation that combines @Generated and @Column
 * to mark a field as an "updated at" timestamp.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Generated(generator = "timestamp", onInsert = true, onUpdate = true)
@Column("updated_at")
public @interface UpdatedTimestamp {
}

// Now your entity is even cleaner:
@Table(name = "users")
public class User {

    @Id
    @Column("user_id")
    private Long userId;

    @UpdatedTimestamp // KuruBind will find the @Generated and @Column annotations inside
    private Instant updatedAt;

    // ...
}
```

## JDBI Integration (Data Types)

KuruBind is built directly on JDBI 3. This means:

**Any data type that JDBI 3 can map, KuruBind can map.**

This includes all standard JDBC types out-of-the-box:

- String
- Integer, Long, Short, Byte
- Double, Float
- Boolean
- BigDecimal
- byte[]
- java.sql.Date, java.sql.Time, java.sql.Timestamp
- java.time.LocalDate, java.time.LocalDateTime, java.time.Instant (with jdbi3-sqlobject plugin or manual registration)

For any other non-standard or database-specific type (like JSON, Enum, InetAddress, etc.), you can simply create a
Handler (as shown above) or a JDBI ColumnMapper to handle the conversion.

## License

Copyright 2025

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.