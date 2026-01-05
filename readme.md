# KuruBind 🚀

**Lightweight SQL-to-Object Mapper for Jdbi 3**

KuruBind is **NOT an ORM**. It's a high-performance, annotation-based mapper that works **on top of Jdbi 3**, providing:

- ✅ **Fast entity mapping** with MethodHandles (10x faster than reflection)
- ✅ **Lifecycle generators** with composable annotations (@CreatedAt, @UpdatedAt)
- ✅ **Multi-database support** (PostgreSQL, MySQL, H2, SQLite, SQL Server)
- ✅ **Query projections** using Java Records
- ✅ **Zero-config convenience** for common CRUD operations
- ✅ **Full Jdbi integration** for transactions, plugins, and complex queries

## Why KuruBind?

**KuruBind handles**: Fast mapping, common CRUD, SQL generation  
**Jdbi handles**: Connections, transactions, statement execution, plugins

This separation keeps KuruBind simple, fast, and maintainable.

### What KuruBind Does NOT Do

❌ Manage connections (use Jdbi's Handle)  
❌ Handle transactions (use Jdbi's `inTransaction()`)  
❌ Implement relationships (@OneToMany, lazy loading)  
❌ Track entity state (no dirty checking)  
❌ Generate schema (use Flyway/Liquibase)  
❌ Build complex queries (write SQL, we map results)

## Quick Start

### 1. Add Dependencies

```xml

<dependencies>
    <dependency>
        <groupId>org.jdbi</groupId>
        <artifactId>jdbi3-core</artifactId>
        <version>3.49.0</version>
    </dependency>

    <dependency>
        <groupId>com.kurubind</groupId>
        <artifactId>kurubind</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

### 2. Register KuruBind with Jdbi

```java
Jdbi jdbi = Jdbi.create("jdbc:postgresql://localhost/mydb", "user", "pass");
jdbi.

registerRowMapper(new KurubindRowMapper.Factory());
```

### 3. Create an Entity

```java

@Data
@NoArgsConstructor
@Kurubind
@Table("users")
public class User {
    @Id(generated = true)
    private Long id;

    private String username;
    private String email;

    @CreatedAt
    private Instant createdAt;

    @UpdatedAt
    private Instant updatedAt;
}
```

### 4. Use It!

```java
jdbi.useHandle(handle ->{
var db = KurubindDatabase.of(handle);

// CREATE
User user = new User();
    user.

setUsername("john");
    user.

setEmail("john@example.com");
    db.

save(user); // ID and timestamps auto-set

// READ
User found = db.findById(User.class, user.getId()).orElse(null);

// UPDATE
    found.

setEmail("new@example.com");
    db.

save(found); // updatedAt auto-updated

// DELETE
    db.

delete(found);
});
```

## Core Annotations

### `@Kurubind`

Marks a class as a KuruBind entity.

### `@Table`

Specifies the database table.

```java
@Table("users")              // Table: users
@Table(value = "users", schema = "auth")  // Table: auth.users
```

### `@Id`

Marks the primary key field.

```java
@Id(generated = true)   // Database auto-generates (SERIAL, AUTO_INCREMENT)
@Id                     // Application sets the value
```

### `@Column`

Maps field to column name.

```java
@Column("user_name")    // Field: username → Column: user_name
```

### `@Transient`

Excludes field from persistence.

```java

@Transient
private String temporaryData;
```

### `@Generated`

Core lifecycle annotation for value generation.

```java

@Generated("timestamp")
private Instant createdAt;

@Generated(value = "uuid", onInsert = true)
private String code;
```

### Meta-Annotations (Composable)

```java
@CreatedAt   // Timestamp on INSERT
@UpdatedAt   // Timestamp on INSERT and UPDATE
```

## Custom Generators

```java
// Register at application startup
GeneratorRegistry.register("order_code",(entity, field, handle) ->{
// Access to Jdbi Handle for database queries!
Integer count = handle.createQuery("SELECT COUNT(*) FROM orders")
        .mapTo(Integer.class)
        .one();
    
    return"ORD-"+String.

format("%08d",count +1);
});

// Use in entity
@Generated("order_code")
private String orderCode;
```

## API Reference

### Core Operations

```java
KurubindDatabase db = KurubindDatabase.of(handle);

// Save (INSERT or UPDATE)
db.

save(entity);

// Force INSERT
db.

insert(entity);

// Force UPDATE
db.

update(entity);

// Delete
db.

delete(entity);
db.

deleteById(User .class, 1L);

// Find
Optional<User> user = db.findById(User.class, 1L);
List<User> all = db.findAll(User.class);
Optional<User> first = db.findFirst(User.class);

// Count and Exists
long count = db.count(User.class);
boolean exists = db.existsById(User.class, 1L);
```

### Custom Queries

```java
// Query with parameters
List<User> users = db.query(
                "SELECT * FROM users WHERE email LIKE :pattern",
                User.class,
                Map.of("pattern", "%@gmail.com")
        );

// Query single result
Optional<User> user = db.queryOne(
        "SELECT * FROM users WHERE username = :username",
        User.class,
        Map.of("username", "john")
);
```

### Pagination

```java
// Paginate all entities
PageResult<User> page = db.findAllPaginated(User.class, 0, 20);

System.out.

println("Page: "+page.page() +"/"+page.

totalPages());
        System.out.

println("Total: "+page.totalElements());
        page.

content().

forEach(user ->System.out.

println(user));

// Paginate custom query
PageResult<User> filtered = db.findByPage(
        "SELECT * FROM users WHERE active = :active",
        User.class,
        Map.of("active", true),
        0,  // page
        20  // size
);
```

### Batch Operations

```java
List<User> users = List.of(user1, user2, user3);

db.

saveAll(users);     // Batch INSERT
db.

updateAll(users);   // Batch UPDATE
db.

deleteAll(users);   // Batch DELETE
```

## Records for Query Projections

```java

@Kurubind
public record UserSummary(
        Long id,
        String username,
        String email,
        Long orderCount,
        Double totalSpent
) {
}

// Usage
String sql = """
        SELECT 
            u.id,
            u.username,
            u.email,
            COUNT(o.id) as orderCount,
            SUM(o.total) as totalSpent
        FROM users u
        LEFT JOIN orders o ON u.id = o.user_id
        GROUP BY u.id, u.username, u.email
        """;

List<UserSummary> summaries = db.query(sql, UserSummary.class, null);
```

## Transactions

Jdbi handles transactions - KuruBind just works within them:

```java
jdbi.useTransaction(handle ->{
var db = KurubindDatabase.of(handle);
    
    db.

save(user);
    db.

save(order);

// If exception thrown, Jdbi rolls back
});
```

## Mixing KuruBind with Direct Jdbi

```java
jdbi.useHandle(handle ->{
var db = KurubindDatabase.of(handle);

// Use KuruBind for simple operations
    db.

save(user);

// Use Jdbi directly for complex queries
List<Map<String, Object>> stats = handle.createQuery("""
                SELECT DATE_TRUNC('day', created_at) as day, COUNT(*) as count
                FROM users
                GROUP BY DATE_TRUNC('day', created_at)
                """)
        .mapToMap()
        .list();

// Back to KuruBind
List<User> recent = db.query(
        "SELECT * FROM users WHERE created_at > :since",
        User.class,
        Map.of("since", Instant.now().minusSeconds(86400))
);
});
```

## Supported Databases

- ✅ PostgreSQL (with RETURNING support)
- ✅ MySQL / MariaDB
- ✅ H2
- ✅ SQLite
- ✅ SQL Server
- ✅ Generic (ANSI SQL fallback)

Dialect is auto-detected from JDBC connection metadata.

## Best Practices

### 1. Keep Entities Simple

KuruBind entities should be simple POJOs or Records. Avoid business logic.

### 2. Write SQL for Complex Queries

Don't try to force everything through entity operations. Write SQL for:

- Complex joins
- Aggregations
- Analytical queries

### 3. Use Records for Projections

Records are perfect for read-only DTOs from complex queries.

### 4. Let Jdbi Handle Transactions

Don't manage transactions yourself. Use `inTransaction()` or `@Transaction`.

### 5. Register Generators at Startup

Register all custom generators once at application startup.

## Migration Guide

### From Plain Jdbi

1. Add `@Kurubind` and field annotations
2. Register `KurubindRowMapper.Factory`
3. Replace manual queries with `db.save()`, `db.findById()`, etc.
4. Keep complex queries as-is

### From JPA/Hibernate

**What to Keep:**

- Entity classes (add KuruBind annotations)
- Database schema
- Business logic

**What to Remove:**

- `@Entity`, `@OneToMany`, etc.
- EntityManager
- JPQL queries → SQL
- Lazy loading → Explicit queries

**What to Change:**

- Write SQL instead of JPQL
- Explicit transaction boundaries
- Manual relationship loading

## Spring Boot Integration

```java

@Configuration
public class DatabaseConfig {

    @Bean
    public Jdbi jdbi(DataSource dataSource) {
        Jdbi jdbi = Jdbi.create(dataSource);
        jdbi.registerRowMapper(new KurubindRowMapper.Factory());

        // Register custom generators
        GeneratorRegistry.register("audit_user", (entity, field, handle) -> {
            Authentication auth = SecurityContextHolder.getContext()
                    .getAuthentication();
            return auth != null ? auth.getName() : "system";
        });

        return jdbi;
    }
}

@Repository
public class UserRepository {
    private final Jdbi jdbi;

    public User save(User user) {
        return jdbi.withHandle(h -> KurubindDatabase.of(h).save(user));
    }

    public Optional<User> findById(Long id) {
        return jdbi.withHandle(h ->
                KurubindDatabase.of(h).findById(User.class, id)
        );
    }
}
```

## License

MIT License

## Contributing

Contributions welcome! Please:

1. Follow existing code style
2. Add tests for new features
3. Update documentation
4. Keep it simple - this is NOT an ORM!

## Credits

Built on top of [Jdbi 3](https://jdbi.org/) - an excellent SQL convenience library for Java.