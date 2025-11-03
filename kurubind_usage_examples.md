# KurubindDatabase - Complete Feature Guide

## Table of Contents
1. [Basic CRUD Operations](#1-basic-crud-operations)
2. [Extended CRUD Operations](#2-extended-crud-operations)
3. [Simple Queries](#3-simple-queries)
4. [Parameterized Queries](#4-parameterized-queries)
5. [Scalar Queries](#5-scalar-queries)
6. [Map-Based Results](#6-map-based-results)
7. [Batch Operations](#7-batch-operations)
8. [Pagination](#8-pagination)
9. [Transactions](#9-transactions)
10. [Streaming Large Datasets](#10-streaming-large-datasets)
11. [Stored Procedures](#11-stored-procedures)
12. [Database Functions](#12-database-functions)
13. [Table-Valued Functions](#13-table-valued-functions)
14. [Database Views](#14-database-views)
15. [Sequences](#15-sequences)
16. [Raw SQL Execution](#16-raw-sql-execution)
17. [Advanced Examples](#17-advanced-examples)

---

## 1. Basic CRUD Operations

### Insert Single Entity
```java
User user = new User("John", "john@example.com");
db.insert(user);
// ID is automatically generated and set if entity has @Id with auto-generation
System.out.println("Created user ID: " + user.getId());
```

### Insert Multiple Entities
```java
List<User> users = Arrays.asList(
        new User("Alice", "alice@example.com"),
        new User("Bob", "bob@example.com"),
        new User("Carol", "carol@example.com")
);
db.insertAll(users);
// All entities validated first, then inserted in a single transaction
```

### Update Entity
```java
User user = db.queryById(User.class, 123).orElseThrow();
user.setStatus("active");
db.update(user);
// Validates and updates the entity
```

### Delete Entity
```java
User user = db.queryById(User.class, 123).orElseThrow();
db.delete(user);
```

### List All Entities
```java
List<User> allUsers = db.list(User.class);
```

### Query By ID
```java
Optional<User> user = db.queryById(User.class, 123);
user.ifPresent(u -> System.out.println(u.getName()));
```

### Count Entities
```java
Long totalUsers = db.count(User.class);
```

---

## 2. Extended CRUD Operations

### Update Multiple Entities
```java
List<User> users = db.list(User.class);
users.forEach(u -> u.setStatus("active"));
        db.updateAll(users);
```

### Delete By ID
```java
// Delete without fetching the entity first
db.deleteById(User.class, 123);
```

### Delete Multiple By IDs
```java
db.deleteByIds(User.class, List.of(1, 2, 3, 4, 5));
```

### Delete Multiple Entities
```java
List<User> usersToDelete = db.query(
        "SELECT * FROM users WHERE status = 'inactive'",
        User.class
);
db.deleteAll(usersToDelete);
```

### Check If Entity Exists
```java
boolean exists = db.exists(User.class, 123);
if (exists) {
        System.out.println("User exists!");
}
```

### Query Multiple By IDs
```java
List<User> users = db.queryByIds(User.class, List.of(10, 20, 30, 40));
```

### Get First Entity
```java
// Useful for single-row tables or getting any example
Optional<User> firstUser = db.queryFirst(User.class);
```

---

## 3. Simple Queries

### Query with Custom SQL
```java
List<User> activeUsers = db.query(
        "SELECT * FROM users WHERE status = 'active'",
        User.class
);
```

### Query One Result
```java
Optional<User> user = db.queryOne(
        "SELECT * FROM users WHERE email = 'john@example.com'",
        User.class
);
```

### Query with ORDER BY
```java
List<User> orderedUsers = db.query(
        "SELECT * FROM users ORDER BY created_at DESC",
        User.class
);
```

### Query with LIMIT
```java
List<User> topUsers = db.query(
        "SELECT * FROM users ORDER BY score DESC LIMIT 10",
        User.class
);
```

---

## 4. Parameterized Queries

### Query with Parameters (Safe from SQL Injection)
```java
List<User> activeUsers = db.query(
        "SELECT * FROM users WHERE status = :status AND age > :minAge",
        User.class,
        Map.of("status", "active", "minAge", 18)
);
```

### Query One with Parameters
```java
Optional<User> user = db.queryOne(
        "SELECT * FROM users WHERE email = :email",
        User.class,
        Map.of("email", "john@example.com")
);
```

### Complex WHERE Clause
```java
List<User> users = db.query(
        "SELECT * FROM users " +
                "WHERE status = :status " +
                "AND created_at >= :startDate " +
                "AND created_at <= :endDate " +
                "AND country IN (:countries)",
        User.class,
        Map.of(
                "status", "active",
                "startDate", LocalDateTime.now().minusDays(30),
                "endDate", LocalDateTime.now(),
                "countries", List.of("US", "CA", "MX")
        )
);
```

### Query with LIKE
```java
List<User> users = db.query(
        "SELECT * FROM users WHERE name LIKE :pattern",
        User.class,
        Map.of("pattern", "%john%")
);
```

---

## 5. Scalar Queries

### Count Query
```java
Long totalUsers = db.queryForLong("SELECT COUNT(*) FROM users");

// With parameters
Long activeCount = db.queryForLong(
        "SELECT COUNT(*) FROM users WHERE status = :status",
        Map.of("status", "active")
);
```

### Integer Queries (AVG, SUM, etc.)
```java
Integer avgAge = db.queryForInt("SELECT AVG(age) FROM users");

Integer totalOrders = db.queryForInt(
        "SELECT SUM(quantity) FROM orders WHERE user_id = :userId",
        Map.of("userId", 123)
);
```

### String Queries
```java
String userName = db.queryForString(
        "SELECT name FROM users WHERE id = :id",
        Map.of("id", 123)
);
```

### Generic Object Queries
```java
Optional<Double> maxSalary = db.queryForObject(
        "SELECT MAX(salary) FROM employees WHERE department = :dept",
        Double.class,
        Map.of("dept", "Engineering")
);
```

### List of Scalars
```java
List<String> emails = db.queryForList(
        "SELECT email FROM users WHERE status = :status",
        String.class,
        Map.of("status", "active")
);

List<Integer> userIds = db.queryForList(
        "SELECT id FROM users WHERE age > :age",
        Integer.class,
        Map.of("age", 18)
);
```

---

## 6. Map-Based Results

**Perfect for: joins, aggregates, views, dynamic schemas, reports**

### Query as List of Maps
```java
// No DTO class needed!
List<Map<String, Object>> results = db.queryForMaps(
                "SELECT department, COUNT(*) as count, AVG(salary) as avg_salary " +
                        "FROM employees GROUP BY department"
        );

for (Map<String, Object> row : results) {
        System.out.println(row.get("department") + ": " + row.get("count"));
        }
```

### Query as Single Map
```java
Optional<Map<String, Object>> stats = db.queryForMap(
        "SELECT COUNT(*) as total, AVG(age) as avg_age FROM users"
);

stats.ifPresent(s -> {
        System.out.println("Total: " + s.get("total"));
        System.out.println("Average age: " + s.get("avg_age"));
        });
```

### Complex JOIN as Maps
```java
List<Map<String, Object>> orderSummary = db.queryForMaps(
        "SELECT u.name, u.email, COUNT(o.id) as order_count, " +
                "       SUM(o.total) as total_spent " +
                "FROM users u " +
                "LEFT JOIN orders o ON u.id = o.user_id " +
                "WHERE o.created_at >= :startDate " +
                "GROUP BY u.id " +
                "HAVING COUNT(o.id) > :minOrders " +
                "ORDER BY total_spent DESC",
        Map.of(
                "startDate", LocalDateTime.now().minusMonths(1),
                "minOrders", 5
        )
);
```

### Dashboard Statistics
```java
Map<String, Object> dashboard = db.queryForMap(
        "SELECT " +
                "  COUNT(*) as total_users, " +
                "  SUM(CASE WHEN status = 'active' THEN 1 ELSE 0 END) as active, " +
                "  SUM(CASE WHEN status = 'inactive' THEN 1 ELSE 0 END) as inactive, " +
                "  AVG(age) as avg_age " +
                "FROM users"
).orElse(Collections.emptyMap());
```

---

## 7. Batch Operations

### Batch Update
```java
List<Map<String, Object>> batchParams = List.of(
        Map.of("id", 1, "status", "completed"),
        Map.of("id", 2, "status", "completed"),
        Map.of("id", 3, "status", "completed"),
        Map.of("id", 4, "status", "completed")
);

List<Integer> results = db.executeBatch(
        "UPDATE orders SET status = :status WHERE id = :id",
        batchParams
);

System.out.println("Updated " + results.size() + " orders");
```

### Batch Insert
```java
List<Map<String, Object>> newUsers = List.of(
        Map.of("name", "Alice", "email", "alice@example.com"),
        Map.of("name", "Bob", "email", "bob@example.com"),
        Map.of("name", "Carol", "email", "carol@example.com")
);

db.executeBatch(
    "INSERT INTO users (name, email) VALUES (:name, :email)",
    newUsers
    );
```

### Insert Multiple Entities (Type-Safe)
```java
List<User> users = Arrays.asList(
        new User("Alice", "alice@example.com"),
        new User("Bob", "bob@example.com"),
        new User("Carol", "carol@example.com")
);

// Validates ALL entities first, then inserts in a single transaction
db.insertAll(users);
```

---

## 8. Pagination

### Paginate Entity List
```java
// Page 1, 20 items per page
PageResult<User> page = db.queryPage(User.class, 1, 20);

System.out.println("Page: " + page.getPage() + "/" + page.getTotalPages());
        System.out.println("Total items: " + page.getTotalElements());
        System.out.println("Results: " + page.getResults().size());

// Navigation
        if (page.hasNext()) {
PageResult<User> nextPage = db.queryPage(User.class, page.getPage() + 1, 20);
}

        if (page.hasPrevious()) {
PageResult<User> prevPage = db.queryPage(User.class, page.getPage() - 1, 20);
}
```

### Paginate Custom Query (No Parameters)
```java
PageResult<User> searchResults = db.queryPage(
    "SELECT * FROM users WHERE name LIKE '%john%' ORDER BY created_at DESC",
    User.class,
    2, // page 2
    10 // 10 per page
);
```

### Paginate Custom Query (With Parameters)
```java
// The CORRECT way - pass parameters separately
PageResult<User> searchResults = db.queryPage(
    "SELECT * FROM users WHERE status = :status AND age > :minAge ORDER BY name",
    User.class,
    Map.of("status", "active", "minAge", 18),
    1, // page 1
    20 // 20 per page
);
```

### PageResult Methods
```java
PageResult<User> page = db.queryPage(User.class, 1, 20);

List<User> results = page.getResults();      // Current page results
int currentPage = page.getPage();            // Current page number
int pageSize = page.getPageSize();           // Items per page
long totalElements = page.getTotalElements(); // Total items across all pages
long totalPages = page.getTotalPages();      // Total number of pages
boolean hasNext = page.hasNext();            // Has next page?
boolean hasPrev = page.hasPrevious();        // Has previous page?
```

### Complete Pagination Example
```java
public PageResult<User> searchUsers(String searchTerm, String status, int page) {
    return db.queryPage(
        "SELECT * FROM users " +
        "WHERE name LIKE :search " +
        "AND status = :status " +
        "ORDER BY created_at DESC",
        User.class,
        Map.of(
            "search", "%" + searchTerm + "%",
            "status", status
        ),
        page,
        20
    );
}
```

---

## 9. Transactions

### Simple Transaction (No Return Value)
```java
db.execute(handle -> {
    handle.createUpdate(
        "UPDATE accounts SET balance = balance - 100 WHERE id = 1"
    ).execute();
    
    handle.createUpdate(
        "UPDATE accounts SET balance = balance + 100 WHERE id = 2"
    ).execute();
});
```

### Transaction with Return Value
```java
User createdUser = db.executeInTransaction(handle -> {
    // Create user
    User user = new User("Alice", "alice@example.com");
    db.insert(user);
    
    // Create audit log
    handle.createUpdate(
        "INSERT INTO audit_log (action, user_id) VALUES (:action, :userId)"
    )
    .bind("action", "user_created")
    .bind("userId", user.getId())
    .execute();
    
    return user; // Return the created user!
});

System.out.println("Created user: " + createdUser.getId());
```

### Complex Transaction
```java
Integer updatedCount = db.executeInTransaction(handle -> {
    // Get users to update
    List<Integer> userIds = db.queryForList(
        "SELECT id FROM users WHERE last_login < :date",
        Integer.class,
        Map.of("date", LocalDateTime.now().minusDays(30))
    );
    
    // Batch update
    List<Map<String, Object>> params = userIds.stream()
        .map(id -> Map.of("id", (Object) id, "status", "inactive"))
        .toList();
    
    db.executeBatch(
        "UPDATE users SET status = :status WHERE id = :id",
        params
    );
    
    // Log audit
    handle.createUpdate(
        "INSERT INTO audit_log (action, count) VALUES (:action, :count)"
    )
    .bind("action", "bulk_deactivation")
    .bind("count", userIds.size())
    .execute();
    
    return userIds.size();
});
```

### Read-Only Operations (No Transaction)
```java
List<User> users = db.withHandle(handle -> 
    handle.createQuery("SELECT * FROM users WHERE status = 'active'")
          .mapToBean(User.class)
          .list()
);
```

### Direct Handle Access
```java
db.executeWithHandle(handle -> {
    // Direct JDBI operations without transaction
    handle.execute("ANALYZE users");
    handle.execute("VACUUM");
});
```

---

## 10. Streaming Large Datasets

### Stream Query Results
```java
// Avoid loading everything in memory
db.queryStream("SELECT * FROM large_table", User.class)
  .filter(u -> u.getAge() > 18)
  .limit(100)
  .forEach(user -> {
      // Process one at a time
      processUser(user);
  });
```

### Stream with Processing
```java
long count = db.queryStream(
    "SELECT * FROM users WHERE status = 'active'",
    User.class
)
.filter(u -> u.getEmail().contains("@example.com"))
.count();
```

---

## 11. Stored Procedures

### Call Procedure Without Parameters
```java
db.callProcedure("update_user_statistics");
```

### Call Procedure With Parameters
```java
db.callProcedure("archive_old_orders", Map.of(
    "days", 90,
    "status", "completed"
));
```

### Call Procedure in Transaction
```java
db.executeInTransaction(handle -> {
    db.callProcedure("process_monthly_reports", Map.of(
        "year", 2024,
        "month", 11
    ));
    
    // Other operations...
    return true;
});
```

---

## 12. Database Functions

### Call Function Returning Single Value
```java
// PostgreSQL: SELECT calculate_tax(1000, 0.21)
Optional<Double> tax = db.callFunction(
    "calculate_tax",
    Double.class,
    Map.of("amount", 1000.0, "rate", 0.21)
);

System.out.println("Tax: $" + tax.orElse(0.0));
```

### Call Function Without Parameters
```java
Optional<Integer> nextId = db.callFunction(
    "get_next_sequence",
    Integer.class
);
```

### Call Function with Multiple Parameters
```java
Optional<String> result = db.callFunction(
    "format_user_name",
    String.class,
    Map.of(
        "firstName", "John",
        "lastName", "Doe",
        "format", "last_first"
    )
);
```

### Use Function in Query
```java
// Direct SQL with function
Double discount = db.queryForObject(
    "SELECT calculate_discount(:customerType, :amount)",
    Double.class,
    Map.of("customerType", "premium", "amount", 1000.0)
).orElse(0.0);
```

---

## 13. Table-Valued Functions

### Query Function Returning Table (PostgreSQL)
```java
// Function: CREATE FUNCTION get_users_by_status(p_status TEXT) 
//           RETURNS TABLE(id INT, name TEXT, email TEXT)

List<User> activeUsers = db.query(
    "SELECT * FROM get_users_by_status(:status)",
    User.class,
    Map.of("status", "active")
);
```

### Query Function as Maps
```java
List<Map<String, Object>> salesData = db.queryForMaps(
    "SELECT * FROM get_sales_report(:startDate, :endDate)",
    Map.of(
        "startDate", LocalDateTime.now().minusMonths(1),
        "endDate", LocalDateTime.now()
    )
);
```

### JOIN with Table-Valued Function
```java
List<Map<String, Object>> report = db.queryForMaps(
    "SELECT u.name, u.email, o.* " +
    "FROM users u " +
    "JOIN get_user_orders(:userId) o ON u.id = o.user_id",
    Map.of("userId", 123)
);
```

### Paginate Function Results
```java
PageResult<User> page = db.queryPage(
    "SELECT * FROM search_users(:searchTerm) ORDER BY name",
    User.class,
    1,
    20
);
```

### MySQL Stored Procedure Returning Results
```java
// MySQL: CALL get_active_users()
List<User> users = db.query(
    "CALL get_active_users()",
    User.class
);
```

---

## 14. Database Views

### Query View Like a Table
```java
// View: CREATE VIEW active_users_view AS 
//       SELECT * FROM users WHERE status = 'active'

List<User> activeUsers = db.query(
    "SELECT * FROM active_users_view",
    User.class
);
```

### Query View with Parameters
```java
List<User> filteredView = db.query(
    "SELECT * FROM user_statistics_view WHERE age > :minAge",
    User.class,
    Map.of("minAge", 18)
);
```

### Query View as Maps
```java
// Perfect for views with computed columns
List<Map<String, Object>> stats = db.queryForMaps(
    "SELECT * FROM monthly_sales_view WHERE year = :year AND month = :month",
    Map.of("year", 2024, "month", 11)
);
```

### Paginate View Results
```java
PageResult<User> page = db.queryPage(
    "SELECT * FROM active_users_view ORDER BY created_at DESC",
    User.class,
    1,
    20
);
```

### JOIN with View
```java
List<Map<String, Object>> report = db.queryForMaps(
    "SELECT v.customer_name, v.total_orders, o.* " +
    "FROM customer_summary_view v " +
    "JOIN orders o ON v.customer_id = o.customer_id " +
    "WHERE o.created_at >= :date",
    Map.of("date", LocalDateTime.now().minusMonths(1))
);
```

### Refresh Materialized View (PostgreSQL)
```java
db.executeUpdate("REFRESH MATERIALIZED VIEW monthly_sales_summary");

// Or with CONCURRENTLY
db.executeUpdate("REFRESH MATERIALIZED VIEW CONCURRENTLY monthly_sales_summary");
```

---

## 15. Sequences

### PostgreSQL Sequences

#### Get Next Value
```java
Long nextId = db.queryForLong("SELECT nextval('users_id_seq')");
```

#### Get Current Value (Without Incrementing)
```java
Long currentId = db.queryForLong("SELECT currval('users_id_seq')");
```

#### Set Sequence Value
```java
db.executeUpdate(
    "SELECT setval('users_id_seq', :value)",
    Map.of("value", 1000)
);
```

#### Get Last Value
```java
Long lastValue = db.queryForLong("SELECT last_value FROM users_id_seq");
```

### MySQL AUTO_INCREMENT

#### Auto-Generated ID (Automatic)
```java
User user = new User("John", "john@example.com");
db.insert(user);
// ID is automatically set
System.out.println("Generated ID: " + user.getId());
```

#### Get Next AUTO_INCREMENT Value
```java
Optional<Long> nextId = db.queryForObject(
    "SELECT AUTO_INCREMENT FROM information_schema.TABLES " +
    "WHERE TABLE_SCHEMA = :schema AND TABLE_NAME = :table",
    Long.class,
    Map.of("schema", "mydb", "table", "users")
);
```

### SQLite Sequences

#### Get Sequence Value
```java
Long nextId = db.queryForLong(
    "SELECT seq FROM sqlite_sequence WHERE name = :table",
    Map.of("table", "users")
);
```

### Custom Sequence Table (All Databases)

#### Create Sequence Table
```java
db.executeUpdate(
    "CREATE TABLE IF NOT EXISTS sequences (" +
    "  name VARCHAR(100) PRIMARY KEY, " +
    "  value BIGINT NOT NULL" +
    ")"
);
```

#### Initialize Sequence
```java
db.executeUpdate(
    "INSERT INTO sequences (name, value) VALUES (:name, :value)",
    Map.of("name", "order_number", "value", 1000)
);
```

#### Get and Increment
```java
Long orderNumber = db.executeInTransaction(handle -> {
    // Get current value
    Long current = db.queryForLong(
        "SELECT value FROM sequences WHERE name = :name",
        Map.of("name", "order_number")
    );
    
    // Increment
    db.executeUpdate(
        "UPDATE sequences SET value = value + 1 WHERE name = :name",
        Map.of("name", "order_number")
    );
    
    return current;
});
```

---

## 16. Raw SQL Execution

### Execute Update
```java
int rowsAffected = db.executeUpdate(
    "UPDATE users SET status = 'active' WHERE last_login > NOW() - INTERVAL 7 DAY"
);
```

### Execute Update with Parameters
```java
int rows = db.executeUpdate(
    "UPDATE users SET status = :status WHERE created_at < :date",
    Map.of(
        "status", "inactive",
        "date", LocalDateTime.now().minusDays(30)
    )
);
```

### Execute DDL
```java
db.executeWithHandle(handle -> {
    handle.execute("CREATE INDEX idx_user_email ON users(email)");
    handle.execute("CREATE INDEX idx_user_status ON users(status)");
});
```

### Direct JDBI Access
```java
db.getJdbi().useHandle(handle -> {
    // Use any JDBI feature
    handle.execute("VACUUM");
    handle.execute("ANALYZE users");
});
```

---

## 17. Advanced Examples

### Example 1: Complete Order Processing
```java
Long orderNumber = db.executeInTransaction(handle -> {
    // 1. Get next order number from sequence
    Long orderNum = db.queryForLong("SELECT nextval('order_number_seq')");
    
    // 2. Query available products from view
    List<Map<String, Object>> products = db.queryForMaps(
        "SELECT * FROM available_products_view WHERE stock > :minStock",
        Map.of("minStock", 10)
    );
    
    // 3. Calculate discount using function
    Optional<Double> discount = db.callFunction(
        "calculate_discount",
        Double.class,
        Map.of("customerType", "premium", "totalAmount", 1000.0)
    );
    
    // 4. Insert order
    handle.createUpdate(
        "INSERT INTO orders (order_number, customer_id, total, discount) " +
        "VALUES (:orderNum, :customerId, :total, :discount)"
    )
    .bind("orderNum", orderNum)
    .bind("customerId", 123)
    .bind("total", 1000.0)
    .bind("discount", discount.orElse(0.0))
    .execute();
    
    // 5. Update inventory
    db.executeBatch(
        "UPDATE products SET stock = stock - :qty WHERE id = :productId",
        products.stream()
            .map(p -> Map.of("productId", p.get("id"), "qty", 1))
            .toList()
    );
    
    return orderNum;
});
```

### Example 2: Reporting Dashboard
```java
// Combine view, function, and aggregates
Map<String, Object> dashboard = db.queryForMap(
    "SELECT " +
    "  (SELECT COUNT(*) FROM active_users_view) as active_users, " +
    "  (SELECT SUM(total) FROM monthly_sales_view WHERE month = :month) as monthly_sales, " +
    "  calculate_growth_rate(:currentMonth, :previousMonth) as growth_rate",
    Map.of(
        "month", 11,
        "currentMonth", 11,
        "previousMonth", 10
    )
).orElse(Collections.emptyMap());
```

### Example 3: Bulk User Activation
```java
Integer activatedCount = db.executeInTransaction(handle -> {
    // 1. Find users from view
    List<Integer> userIds = db.queryForList(
        "SELECT user_id FROM eligible_activation_view",
        Integer.class
    );
    
    // 2. Batch update
    List<Map<String, Object>> params = userIds.stream()
        .map(id -> Map.of("id", (Object) id, "status", "active"))
        .toList();
    
    db.executeBatch("UPDATE users SET status = :status WHERE id = :id", params);
    
    // 3. Call procedure to send notifications
    db.callProcedure("send_activation_emails", Map.of("userIds", userIds));
    
    // 4. Log audit
    handle.createUpdate(
        "INSERT INTO audit_log (action, affected_count) VALUES (:action, :count)"
    )
    .bind("action", "bulk_activation")
    .bind("count", userIds.size())
    .execute();
    
    return userIds.size();
});
```

### Example 4: Paginated Search with Function
```java
String searchTerm = "john";
PageResult<User> results = db.queryPage(
    "SELECT u.* FROM users u " +
    "WHERE u.id IN (SELECT user_id FROM search_users_function(:term)) " +
    "ORDER BY u.name",
    User.class,
    Map.of("term", searchTerm),
    1,
    20
);

// Display results
System.out.println("Found " + results.getTotalElements() + " users");
for (User user : results.getResults()) {
    System.out.println(user.getName());
}
```

### Example 5: Data Migration with Sequences
```java
db.executeInTransaction(handle -> {
    // 1. Create new sequence
    handle.execute("CREATE SEQUENCE IF NOT EXISTS new_id_seq START 1000");
    
    // 2. Migrate data with new IDs
    List<Map<String, Object>> oldData = db.queryForMaps(
        "SELECT * FROM old_users"
    );
    
    for (Map<String, Object> row : oldData) {
        Long newId = db.queryForLong("SELECT nextval('new_id_seq')");
        
        handle.createUpdate(
            "INSERT INTO new_users (id, name, email) VALUES (:id, :name, :email)"
        )
        .bind("id", newId)
        .bind("name", row.get("name"))
        .bind("email", row.get("email"))
        .execute();
    }
    
    return oldData.size();
});
```

---

## Summary

KurubindDatabase provides:

✅ **CRUD**: insert, update, delete, batch operations  
✅ **Queries**: parameterized, scalar, maps, streaming  
✅ **Pagination**: Built-in with metadata  
✅ **Transactions**: Simple and complex with return values  
✅ **Procedures**: Call stored procedures with parameters  
✅ **Functions**: Scalar and table-valued functions  
✅ **Views**: Query views like tables  
✅ **Sequences**: PostgreSQL, MySQL, SQLite support  
✅ **Batch**: Efficient bulk operations  
✅ **Raw SQL**: Full control when needed

All features work across **MySQL**, **PostgreSQL**, and **SQLite**! 🚀