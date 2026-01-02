package com.roelias.legacy.factory;

import com.roelias.legacy.KurubindDatabase;
import com.roelias.legacy.base.Dialect;
import com.roelias.legacy.base.JdbiProvider;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.guava.GuavaPlugin;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import org.jdbi.v3.postgres.PostgresPlugin;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * Factory for creating pre-configured {@link KurubindDatabase} instances.
 * <p>
 * This class simplifies Jdbi configuration by automatically installing
 * supported plugins based on the database profile and providing
 * multiple connection options (DataSource, JDBC URL, JdbiProvider, etc).
 * <p>
 * <b>Note:</b> Using specific methods (like createPostgres) requires optional dependencies
 * (jdbi3-jackson2, jdbi3-postgres, jdbi3-guava) to be present in the classpath.
 */
public class KurubindFactory {

    // ============================================================================================
    // POSTGRESQL FACTORY METHODS
    // Automatically enables: PostgresPlugin (UUID, Inet, Arrays), Jackson2Plugin (JSON), GuavaPlugin
    // ============================================================================================

    public static KurubindDatabase createPostgres(DataSource dataSource) {
        Jdbi jdbi = Jdbi.create(dataSource);
        return buildPostgres(jdbi);
    }

    public static KurubindDatabase createPostgres(String url) {
        Jdbi jdbi = Jdbi.create(url);
        return buildPostgres(jdbi);
    }

    public static KurubindDatabase createPostgres(String url, String user, String password) {
        Jdbi jdbi = Jdbi.create(url, user, password);
        return buildPostgres(jdbi);
    }

    public static KurubindDatabase createPostgres(String url, Properties properties) {
        Jdbi jdbi = Jdbi.create(url, properties);
        return buildPostgres(jdbi);
    }

    /**
     * Creates a configured Postgres instance using an existing JdbiProvider.
     * Useful for frameworks where Jdbi is managed externally/lazily.
     * Note: This method will install required plugins on the provided Jdbi instance.
     */
    public static KurubindDatabase createPostgres(JdbiProvider provider) {
        applyPostgresPlugins(provider.getJdbi());
        return KurubindDatabase.builder()
                .withJdbiProvider(provider)
                .withDialect(new Dialect("POSTGRESQL"))
                .build();
    }

    private static KurubindDatabase buildPostgres(Jdbi jdbi) {
        applyPostgresPlugins(jdbi);
        return KurubindDatabase.builder()
                .withJdbi(jdbi)
                .withDialect(new Dialect("POSTGRESQL"))
                .build();
    }

    // ============================================================================================
    // GENERIC FACTORY METHODS (MySQL, H2, MariaDB, etc.)
    // Automatically enables: Jackson2Plugin (JSON mapping for text columns)
    // Uses ANSI Dialect by default.
    // ============================================================================================

    public static KurubindDatabase createGeneric(DataSource dataSource) {
        Jdbi jdbi = Jdbi.create(dataSource);
        return buildGeneric(jdbi);
    }

    public static KurubindDatabase createGeneric(String url) {
        Jdbi jdbi = Jdbi.create(url);
        return buildGeneric(jdbi);
    }

    public static KurubindDatabase createGeneric(String url, String user, String password) {
        Jdbi jdbi = Jdbi.create(url, user, password);
        return buildGeneric(jdbi);
    }

    public static KurubindDatabase createGeneric(String url, Properties properties) {
        Jdbi jdbi = Jdbi.create(url, properties);
        return buildGeneric(jdbi);
    }

    /**
     * Creates a configured Generic instance using an existing JdbiProvider.
     * Note: This method will install Jackson support on the provided Jdbi instance.
     */
    public static KurubindDatabase createGeneric(JdbiProvider provider) {
        applyGenericPlugins(provider.getJdbi());
        return KurubindDatabase.builder()
                .withJdbiProvider(provider)
                .withDialect(new Dialect("ANSI"))
                .build();
    }

    private static KurubindDatabase buildGeneric(Jdbi jdbi) {
        applyGenericPlugins(jdbi);
        return KurubindDatabase.builder()
                .withJdbi(jdbi)
                .withDialect(new Dialect("ANSI"))
                .build();
    }

    // ============================================================================================
    // RAW FACTORY METHODS
    // No extra plugins installed. Pure Jdbi + Kurubind.
    // ============================================================================================

    public static KurubindDatabase createRaw(DataSource dataSource) {
        return buildRaw(Jdbi.create(dataSource));
    }

    public static KurubindDatabase createRaw(String url) {
        return buildRaw(Jdbi.create(url));
    }

    public static KurubindDatabase createRaw(String url, String user, String password) {
        return buildRaw(Jdbi.create(url, user, password));
    }

    public static KurubindDatabase createRaw(String url, Properties properties) {
        return buildRaw(Jdbi.create(url, properties));
    }

    public static KurubindDatabase createRaw(JdbiProvider provider) {
        return KurubindDatabase.builder()
                .withJdbiProvider(provider)
                .withDialect(new Dialect("ANSI"))
                .build();
    }

    private static KurubindDatabase buildRaw(Jdbi jdbi) {
        return KurubindDatabase.builder()
                .withJdbi(jdbi)
                .withDialect(new Dialect("ANSI"))
                .build();
    }

    // ============================================================================================
    // INTERNAL HELPERS
    // ============================================================================================

    private static void applyPostgresPlugins(Jdbi jdbi) {
        try {
            jdbi.installPlugin(new PostgresPlugin());
            jdbi.installPlugin(new Jackson2Plugin());
            jdbi.installPlugin(new GuavaPlugin());
        } catch (NoClassDefFoundError e) {
            throw new RuntimeException("Missing Jdbi dependencies on classpath. " +
                    "Ensure you include 'jdbi3-postgres', 'jdbi3-jackson2', and 'jdbi3-guava'.", e);
        }
    }

    private static void applyGenericPlugins(Jdbi jdbi) {
        try {
            jdbi.installPlugin(new Jackson2Plugin());
        } catch (NoClassDefFoundError e) {
            throw new RuntimeException("Missing dependency 'jdbi3-jackson2' on classpath.", e);
        }
    }
}