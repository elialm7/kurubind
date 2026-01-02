# KuruBind

[![Java](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
![Status](https://img.shields.io/badge/Status-Beta-blue.svg)

**KuruBind** is a lightweight, annotation-driven database library for Java that sits in the "sweet spot" between the
complexity of a full-blown ORM (like Hibernate/JPA) and the manual effort of raw SQL (like plain JDBI or JDBC).

It is built directly on top of Jdbi 3 and is designed for developers who want to work with existing databases ("
brownfield" projects) without the steep learning curve or configuration overhead of mapping every relationship and
constraint.

---

## The KuruBind Philosophy

KuruBind is for you if:

- You are working with an existing database and don't want to (or can't) let an ORM manage your schema.
- You find full ORMs too complex (no session management, lazy-loading proxies, or complex relationship graphs).
- You find writing raw SQL and manual mapping for every query tedious.
- You want a simple, convention-based Repository pattern for your entities.
- You want an extensible system for adding custom type conversions, validation, and value generation.

---

## Core Features

- **Annotation-Based Mapping**: Simple annotations (`@Table`, `@Column`, `@Id`) to map your Java POJOs.
- **Generic Repository**: Out-of-the-box `KuruRepository` for standard CRUD (insert, update, deleteById, findById,
  findAll).
- **Native JDBI Integration**: Transparently supports Jdbi plugins. If Jdbi can map it (JSON, UUID, Arrays, Vavr),
  KuruBind can map it.
- **Automatic SQL Generation**: Generates dialect-aware SQL for standard operations.
- **Full Extensibility**:
    - **Modules**: Package custom logic into reusable `KurubindModules`.
    - **Handlers**: Create custom type converters (e.g., Enum ↔ String, JSON ↔ Map).
    - **Validators**: Hook into insert/update operations to validate data.
    - **ValueGenerators**: Auto-generate values (UUIDs, timestamps) on insert/update.
    - **Dialects**: Customize generated SQL for specific databases.
    - **Meta-Annotations**: Compose annotations to create simple, reusable shortcuts.
- **Full SQL Access**: Drop down to raw SQL with parameter binding and POJO mapping (`@QueryResponse`) at any time.

---

## 1. Installation

Add KuruBind to your project. You also need to include the Jdbi extensions you plan to use (see **JDBI Ecosystem Support
** below for details).
**In order to use it, you must clone the repository and install it to your local Maven repository
through maven install command as it is not yet published to a public Maven repository.**

```xml

<dependencies>
    <!-- KuruBind Core -->
    <dependency>
        <groupId>com.roelias</groupId>
        <artifactId>kurubind</artifactId>
        <version>x.x.x</version>
    </dependency>

    <!-- Jdbi Core (Required) -->
    <dependency>
        <groupId>org.jdbi</groupId>
        <artifactId>jdbi3-core</artifactId>
        <version>3.49.0</version>
    </dependency>
</dependencies>
```

---

## 2. Configuration

The entry point is `KurubindDatabase`. We provide a `KurubindFactory` to get you started quickly with the best defaults
for your database type.

### Option A: Quick Start (Recommended)

Use `KurubindFactory` to create a pre-configured instance. This automatically installs relevant Jdbi plugins (like
Jackson for JSON or Postgres types) if they are on your classpath.

```java
import com.roelias.legacy.factory.KurubindFactory;
import com.roelias.legacy.KurubindDatabase;

// For PostgreSQL: Automatically enables JSON, UUID, Arrays, and Guava support
KurubindDatabase db = KurubindFactory.createPostgres(
        "jdbc:postgresql://localhost:5432/mydb", "user", "pass"
);

// For Generic DBs (MySQL, H2, MariaDB): Enables Jackson support for JSON mapping in text columns
// KurubindDatabase db = KurubindFactory.createGeneric("jdbc:h2:mem:test");
```

### Option B: Advanced / Multi-tenant

You can implement `JdbiProvider` to handle dynamic connection routing. This is ideal for multi-tenant applications where
the database connection changes based on the request context (e.g., Tenant ID in header).

```java
// 1. Create a provider (e.g., wrapping a RoutingDataSource)
JdbiProvider myProvider = new MultiTenantJdbiProvider(routingDataSource);

// 2. Pass it to the factory. 
// The factory will install plugins on the Jdbi instance provided by your implementation.
KurubindDatabase db = KurubindFactory.createPostgres(myProvider);
```

### Option C: Manual Configuration

Build the instance manually for fine-grained control over the Jdbi instance and plugins.

```java
import org.jdbi.v3.core.Jdbi;
import com.roelias.legacy.KurubindDatabase;
import com.roelias.legacy.base.Dialect;

Jdbi jdbi = Jdbi.create(dataSource);
// jdbi.installPlugin(new PostgresPlugin()); // Manual plugin installation

KurubindDatabase db = KurubindDatabase.builder()
        .withJdbi(jdbi)
        .withDialect(new Dialect("POSTGRESQL"))
        .build();
```

---

## 3. Defining Entities

Define entities as simple POJOs. KuruBind supports complex types natively if the underlying Jdbi plugin is installed.

### Annotations

- **`@Table`**: Table name and schema.
- **`@Id`**: Primary key. `autogenerate=true` fetches keys generated by the DB.
- **`@Column`**: Column name (optional if it matches field name).
- **`@Generated`**: Populates fields via a `ValueGenerator`.
- **`@DefaultValue`**: Provides a default value on insert if the field is null.
- **`@Transient`**: Ignores the field.

### Example Entity

```java
import com.roelias.legacy.annotations.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Table(name = "users", schema = "public")
public class User {

    @Id
    @Column("user_id")
    private Long userId;

    @Column("username")
    private String username;

    // Native support for UUIDs (requires jdbi3-postgres)
    @Column("api_key")
    private UUID apiKey;

    // Native support for JSON columns mapped to Maps or POJOs (requires jdbi3-jackson2)
    @Column("preferences")
    private Map<String, Object> preferences;

    @Generated(generator = "timestamp", onInsert = true, onUpdate = true)
    @Column("updated_at")
    private Instant updatedAt;

    @Transient
    private String someInternalState;

    // Getters and Setters...
}
```

---

## 4. Usage

### Basic CRUD (KuruRepository)

The `KuruRepository` provides a typed API for common operations.

```java
import com.roelias.legacy.repository.KuruRepository;

KuruRepository<User> userRepo = new KuruRepository<>(db, User.class);

// Create
User newUser = new User();
newUser.

setUsername("jdoe");
newUser.

setApiKey(UUID.randomUUID());
        newUser.

setPreferences(Map.of("theme", "dark"));
        userRepo.

insert(newUser);

// Read
Optional<User> user = userRepo.findById(newUser.getUserId());
List<User> allUsers = userRepo.findAll();

// Update
user.

ifPresent(u ->{
        u.

setUsername("jdoe_v2");
    userRepo.

update(u);
});

// Delete
        userRepo.

deleteById(newUser.getUserId());

// Check
boolean exists = userRepo.exists(1L);
long count = userRepo.countAll();
```

### Core Database Methods

You can also use `KurubindDatabase` directly for untyped access or operations across multiple entities.

```java
db.insert(newUser);

Optional<User> user = db.findById(User.class, 1L);
db.

deleteById(User .class, 1L);
```

### Custom SQL

When you need custom SQL, you get the power of Jdbi with KuruBind's row mapping. You can map results to `@Table`
entities, `@QueryResponse` POJOs, or simple primitives.

```java
// Map custom SQL results to your Entity
String sql = "SELECT * FROM users WHERE preferences ->> 'theme' = :theme";
List<User> darkThemeUsers = db.query(sql, User.class, Map.of("theme", "dark"));

// Map to primitives
List<String> usernames = db.queryForList("SELECT username FROM users", String.class);

// Map to Generic Maps
List<Map<String, Object>> rows = db.queryForMaps(sql, Map.of("theme", "dark"));

// Execute raw updates
db.

executeUpdate("DELETE FROM logs WHERE created_at < NOW() - INTERVAL '30 days'",Map.of());
```

---

## 5. JDBI Ecosystem Support

KuruBind sits on top of Jdbi. If Jdbi can map it, KuruBind can map it inside your entities.

To enable these features, simply add the corresponding Maven dependency. `KurubindFactory` will detect them and enable
them automatically.

### Compatibility Matrix

| Feature         | Jdbi Plugin | Maven Artifact   | Status                                              |
|-----------------|-------------|------------------|-----------------------------------------------------|
| **JSON**        | Jackson 2   | `jdbi3-jackson2` | ✅ Native. Map JSON columns directly to POJOs/Maps.  |
| **Postgres**    | Postgres    | `jdbi3-postgres` | ✅ Native. UUID, InetAddress, HStore, Arrays, Enums. |
| **Collections** | Guava       | `jdbi3-guava`    | ✅ Native. ImmutableList, Multimap.                  |
| **Vavr**        | Vavr        | `jdbi3-vavr`     | ✅ Native. Tuples, Option.                           |
| **Java Time**   | Core        | (None)           | ✅ Native. Instant, LocalDate, ZonedDateTime.        |

### Required Dependencies

Add these to your `pom.xml` to unlock the features above:

```xml
<!-- JSON Support (e.g., for @Column Map<String,Object> preferences) -->
<dependency>
    <groupId>org.jdbi</groupId>
    <artifactId>jdbi3-jackson2</artifactId>
    <version>${jdbi.version}</version>
</dependency>

        <!-- PostgreSQL Types (e.g., UUID, Arrays) -->
<dependency>
<groupId>org.jdbi</groupId>
<artifactId>jdbi3-postgres</artifactId>
<version>${jdbi.version}</version>
</dependency>

        <!-- Guava Collections -->
<dependency>
<groupId>org.jdbi</groupId>
<artifactId>jdbi3-guava</artifactId>
<version>${jdbi.version}</version>
</dependency>
```

---

## 6. Extending KuruBind

If native Jdbi support isn't enough, you can create custom extensions by packaging them into a `KurubindModule`.

### Creating and Installing Modules

A module allows you to bundle custom Handlers, Validators, and Generators together.

```java
import com.roelias.legacy.base.KurubindModule;
import com.roelias.legacy.base.RegistryCollector;

public class MyCustomModule implements KurubindModule {
    @Override
    public void configure(RegistryCollector registries) {
        // Register your components here
        registries.handlers().register(Encrypted.class, new EncryptionHandler());
        registries.valueGenerators().register("my-gen", new TimestampGenerator());
        registries.validators().register(NotNull.class, new NotNullValidator());
        // Register custom SQL generation logic
        registries.sqlGenerators().register(new Dialect("POSTGRESQL"), new PostgresSQLGenerator());
    }
}
```

To use it, simply install it during configuration:

```java
KurubindDatabase db = KurubindDatabase.builder()
        .withJdbi(jdbi)
        .installModule(new MyCustomModule()) // Install your module
        .build();
```

### Custom Handlers (Type Conversion)

Use Handlers when you need specific behavior triggered by an annotation (e.g., encryption) or for types Jdbi doesn't
support.

```java
// 1. Define annotation
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Encrypted {
}

// 2. Create Handler
public class EncryptionHandler implements Handler {
    @Override
    public Object handleWrite(Object javaValue, FieldMetadata fieldMeta) {
        return MyCrypto.encrypt((String) javaValue);
    }

    @Override
    public Object handleRead(Object dbValue, FieldMetadata fieldMeta) {
        return MyCrypto.decrypt((String) dbValue);
    }
}
```

### Custom Value Generators

For fields like "Created At" or "Updated At" or "Sequence ID".

```java
public class TimestampGenerator implements ValueGenerator {
    @Override
    public Object generate(Object entity, FieldMetadata field) {
        return Instant.now();
    }
    // ... implement generateOnInsert/Update
}
```

### Custom Validators

Run logic before persistence.

```java
public class NotNullValidator implements Validator {
    @Override
    public void validate(Object value, FieldMetadata field) throws ValidationException {
        if (value == null) throw new ValidationException("Field cannot be null");
    }
}
```

### Custom Dialects & SQL Generators

If KuruBind's default SQL generation doesn't match your database syntax, you can override it.

```java
import com.roelias.legacy.ootb.DefaultSQLGenerator;

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

// 2. Register it in your module (see above)
registries.

sqlGenerators().

register(new Dialect("POSTGRESQL"), new

PostgresSQLGenerator());
```

### Meta-Annotations (Composition)

KuruBind's annotation processor recursively scans for annotations. This allows you to create your own "shortcut"
annotations that bundle multiple KuruBind annotations together.

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

// Now your entity is much cleaner:
@Table(name = "users")
public class User {

    @Id
    @Column("user_id")
    private Long userId;

    @UpdatedTimestamp // KuruBind automatically applies @Generated and @Column
    private Instant updatedAt;
}
```

---

## License

Copyright 2025

Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at:

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "
AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
language governing permissions and limitations under the License.

---

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.