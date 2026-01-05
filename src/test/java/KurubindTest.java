import com.roelias.kurubind.KurubindDatabase;
import com.roelias.kurubind.annotation.*;
import com.roelias.kurubind.common.PageResult;
import com.roelias.kurubind.generator.GeneratorRegistry;
import com.roelias.kurubind.mapper.KurubindRowMapper;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive test suite for KuruBind V2.
 */
class KurubindTest {

    private static Jdbi jdbi;

    @BeforeAll
    static void setupDatabase() {
        // In-memory H2 database for testing
        //  jdbi = Jdbi.create("jdbc:h2:mem:test;;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE");
        //String url = "jdbc:postgresql://localhost:5432/test_crud";
        String url = "jdbc:mysql://localhost:3306/test_crud";
        String user = "root";
        String password = "admin";
        jdbi = Jdbi.create(url, user, password);
        jdbi.registerRowMapper(new KurubindRowMapper.Factory());

        // Create tables
        jdbi.useHandle(handle -> {
            // Tabla de Usuarios
            handle.execute("""
                        CREATE TABLE IF NOT EXISTS users (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            username VARCHAR(255) NOT NULL,
                            email VARCHAR(255) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            // Tabla de Productos
            handle.execute("""
                        CREATE TABLE IF NOT EXISTS products (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            name VARCHAR(255) NOT NULL,
                            price DECIMAL(19, 4) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);

            // Tabla de Órdenes
            handle.execute("""
                        CREATE TABLE IF NOT EXISTS orders (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            user_id BIGINT NOT NULL,
                            order_code VARCHAR(255),
                            total DECIMAL(19, 4) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id)
                        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """);
        });

        // Register custom generator for tests
        GeneratorRegistry.register("test_code", (entity, field, handle) -> {
            return "TEST-" + System.currentTimeMillis();
        });
    }

    @AfterEach
    void cleanup() {
        jdbi.useHandle(handle -> {
            handle.execute("SET FOREIGN_KEY_CHECKS = 0");
            handle.execute("DELETE FROM orders");
            handle.execute("DELETE FROM products");
            handle.execute("DELETE FROM users");
            handle.execute("SET FOREIGN_KEY_CHECKS = 1");
        });


    }

    @AfterAll
    static void teardown() {
        jdbi.useTransaction(handle -> {
            handle.execute("DROP TABLE IF EXISTS orders");
            handle.execute("DROP TABLE IF EXISTS products");
            handle.execute("DROP TABLE IF EXISTS users");


        });
        ;
    }

    @Test
    void testBasicInsert() {
        jdbi.useHandle(handle -> {
            var db = KurubindDatabase.of(handle);

            TestUser user = new TestUser();
            user.setUsername("john");
            user.setEmail("john@example.com");

            db.insert(user);

            assertThat(user.getId()).isNotNull();
            assertThat(user.getCreatedAt()).isNotNull();
            assertThat(user.getUpdatedAt()).isNotNull();
        });
    }

    @Test
    void testFindById() {
        jdbi.useHandle(handle -> {
            var db = KurubindDatabase.of(handle);

            TestUser user = new TestUser();
            user.setUsername("john");
            user.setEmail("john@example.com");
            db.insert(user);

            Optional<TestUser> found = db.findById(TestUser.class, user.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo("john");
            assertThat(found.get().getEmail()).isEqualTo("john@example.com");
        });
    }

    @Test
    void testUpdate() {
        jdbi.useHandle(handle -> {
            var db = KurubindDatabase.of(handle);

            TestUser user = new TestUser();
            user.setUsername("john");
            user.setEmail("john@example.com");
            db.insert(user);

            Instant createdAt = user.getCreatedAt();
            Instant updatedAt = user.getUpdatedAt();

            // Wait a bit to ensure timestamp difference
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }

            user.setEmail("newemail@example.com");
            db.update(user);

            assertThat(user.getEmail()).isEqualTo("newemail@example.com");
            assertThat(user.getCreatedAt()).isEqualTo(createdAt);
            assertThat(user.getUpdatedAt()).isAfter(updatedAt);
        });
    }

    @Test
    void testSave_insertsWhenNew() {
        jdbi.useHandle(handle -> {
            var db = KurubindDatabase.of(handle);

            TestUser user = new TestUser();
            user.setUsername("john");
            user.setEmail("john@example.com");

            db.save(user);

            assertThat(user.getId()).isNotNull();
        });
    }

    @Test
    void testSave_updatesWhenExists() {
        jdbi.useHandle(handle -> {
            var db = KurubindDatabase.of(handle);

            TestUser user = new TestUser();
            user.setUsername("john");
            user.setEmail("john@example.com");
            db.save(user);

            Long id = user.getId();
            user.setEmail("updated@example.com");
            db.save(user);

            assertThat(user.getId()).isEqualTo(id);

            TestUser found = db.findById(TestUser.class, id).orElseThrow();
            assertThat(found.getEmail()).isEqualTo("updated@example.com");
        });
    }

    @Test
    void testDelete() {
        jdbi.useHandle(handle -> {
            var db = KurubindDatabase.of(handle);

            TestUser user = new TestUser();
            user.setUsername("john");
            user.setEmail("john@example.com");
            db.insert(user);

            Long id = user.getId();
            db.delete(user);

            Optional<TestUser> found = db.findById(TestUser.class, id);
            assertThat(found).isEmpty();
        });
    }

    @Test
    void testDeleteById() {
        jdbi.useHandle(handle -> {
            var db = KurubindDatabase.of(handle);

            TestUser user = new TestUser();
            user.setUsername("john");
            user.setEmail("john@example.com");
            db.insert(user);

            Long id = user.getId();
            db.deleteById(TestUser.class, id);

            Optional<TestUser> found = db.findById(TestUser.class, id);
            assertThat(found).isEmpty();
        });
    }

    @Test
    void testExistsById() {
        jdbi.useHandle(handle -> {
            var db = KurubindDatabase.of(handle);

            TestUser user = new TestUser();
            user.setUsername("john");
            user.setEmail("john@example.com");
            db.insert(user);

            assertThat(db.existsById(TestUser.class, user.getId())).isTrue();
            assertThat(db.existsById(TestUser.class, 99999L)).isFalse();
        });
    }

    @Test
    void testFindAll() {
        jdbi.useHandle(handle -> {
            var db = KurubindDatabase.of(handle);

            for (int i = 1; i <= 5; i++) {
                TestUser user = new TestUser();
                user.setUsername("user" + i);
                user.setEmail("user" + i + "@example.com");
                db.insert(user);
            }

            List<TestUser> users = db.findAll(TestUser.class);
            assertThat(users).hasSize(5);
        });
    }

    @Test
    void testCount() {
        jdbi.useHandle(handle -> {
            var db = KurubindDatabase.of(handle);

            for (int i = 1; i <= 3; i++) {
                TestUser user = new TestUser();
                user.setUsername("user" + i);
                user.setEmail("user" + i + "@example.com");
                db.insert(user);
            }

            long count = db.count(TestUser.class);
            assertThat(count).isEqualTo(3);
        });
    }

    @Test
    void testPagination() {
        jdbi.useHandle(handle -> {
            var db = KurubindDatabase.of(handle);

            // Create 25 users
            for (int i = 1; i <= 25; i++) {
                TestUser user = new TestUser();
                user.setUsername("user" + i);
                user.setEmail("user" + i + "@example.com");
                db.insert(user);
            }

            // Get first page
            PageResult<TestUser> page1 = db.findAllPaginated(TestUser.class, 0, 10);
            assertThat(page1.content()).hasSize(10);
            assertThat(page1.page()).isEqualTo(0);
            assertThat(page1.totalElements()).isEqualTo(25);
            assertThat(page1.totalPages()).isEqualTo(3);
            assertThat(page1.hasNext()).isTrue();
            assertThat(page1.hasPrevious()).isFalse();

            // Get second page
            PageResult<TestUser> page2 = db.findAllPaginated(TestUser.class, 1, 10);
            assertThat(page2.content()).hasSize(10);
            assertThat(page2.hasNext()).isTrue();
            assertThat(page2.hasPrevious()).isTrue();

            // Get last page
            PageResult<TestUser> page3 = db.findAllPaginated(TestUser.class, 2, 10);
            assertThat(page3.content()).hasSize(5);
            assertThat(page3.hasNext()).isFalse();
            assertThat(page3.hasPrevious()).isTrue();
        });
    }

    @Test
    void testCustomQuery() {
        jdbi.useHandle(handle -> {
            var db = KurubindDatabase.of(handle);

            TestUser user1 = new TestUser();
            user1.setUsername("john");
            user1.setEmail("john@gmail.com");
            db.insert(user1);

            TestUser user2 = new TestUser();
            user2.setUsername("jane");
            user2.setEmail("jane@yahoo.com");
            db.insert(user2);

            List<TestUser> gmailUsers = db.query(
                    "SELECT * FROM users WHERE email LIKE :pattern",
                    TestUser.class,
                    Map.of("pattern", "%@gmail.com")
            );

            assertThat(gmailUsers).hasSize(1);
            assertThat(gmailUsers.get(0).getUsername()).isEqualTo("john");
        });
    }

    @Test
    void testQueryOne() {
        jdbi.useHandle(handle -> {
            var db = KurubindDatabase.of(handle);

            TestUser user = new TestUser();
            user.setUsername("john");
            user.setEmail("john@example.com");
            db.insert(user);

            Optional<TestUser> found = db.queryOne(
                    "SELECT * FROM users WHERE username = :username",
                    TestUser.class,
                    Map.of("username", "john")
            );

            assertThat(found).isPresent();
            assertThat(found.get().getEmail()).isEqualTo("john@example.com");
        });
    }

    @Test
    void testBatchInsert() {
        jdbi.useHandle(handle -> {
            var db = KurubindDatabase.of(handle);

            List<TestUser> users = List.of(
                    createUser("user1", "user1@example.com"),
                    createUser("user2", "user2@example.com"),
                    createUser("user3", "user3@example.com")
            );

            db.saveAll(users);

            long count = db.count(TestUser.class);
            assertThat(count).isEqualTo(3);

            // Verify all have IDs
            users.forEach(u -> assertThat(u.getId()).isNotNull());
        });
    }

    @Test
    void testCustomGenerator() {
        jdbi.useHandle(handle -> {
            var db = KurubindDatabase.of(handle);

            var user = new TestUser();
            user.setUsername("john");
            user.setEmail("jhon@gmail.com");
            db.insert(user);

            TestOrder order = new TestOrder();
            order.setUserId(user.getId());
            order.setTotal(99.99);
            db.insert(order);

            assertThat(order.getOrderCode()).startsWith("TEST-");
        });
    }

    @Test
    void testRecordMapping() {
        jdbi.useHandle(handle -> {
            var db = KurubindDatabase.of(handle);

            TestUser user = new TestUser();
            user.setUsername("john");
            user.setEmail("john@example.com");
            db.insert(user);

            List<TestUserDto> dtos = db.query(
                    "SELECT id, username, email FROM users",
                    TestUserDto.class,
                    null
            );

            assertThat(dtos).hasSize(1);
            assertThat(dtos.get(0).username()).isEqualTo("john");
        });
    }

    @Test
    void testTransaction() {
        assertThatThrownBy(() -> {
            jdbi.useTransaction(handle -> {
                var db = KurubindDatabase.of(handle);

                TestUser user = new TestUser();
                user.setUsername("john");
                user.setEmail("john@example.com");
                db.insert(user);

                // Throw exception to trigger rollback
                throw new RuntimeException("Rollback!");
            });
        }).isInstanceOf(RuntimeException.class);

        // Verify rollback worked
        jdbi.useHandle(handle -> {
            var db = KurubindDatabase.of(handle);
            long count = db.count(TestUser.class);
            assertThat(count).isEqualTo(0);
        });
    }

    private TestUser createUser(String username, String email) {
        TestUser user = new TestUser();
        user.setUsername(username);
        user.setEmail(email);
        return user;
    }
}

// Test entities

@Kurubind
@Table("users")
class TestUser {
    @Id(generated = true)
    private Long id;

    @Column("username")
    private String username;
    @Column("email")
    private String email;

    @CreatedAt
    @Column("created_at")
    private Instant createdAt;

    @UpdatedAt
    @Column("updated_at")
    private Instant updatedAt;

    public TestUser() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

@Kurubind
@Table("orders")
class TestOrder {
    @Id(generated = true)
    private Long id;

    @Column("user_id")
    private Long userId;

    @Generated("test_code")
    @Column("order_code")
    private String orderCode;

    private Double total;

    @CreatedAt
    @Column("created_at")
    private Instant createdAt;

    public TestOrder() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOrderCode() {
        return orderCode;
    }

    public void setOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
