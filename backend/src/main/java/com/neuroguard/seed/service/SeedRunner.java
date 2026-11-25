package com.neuroguard.seed.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;

/**
 * Loads demo data when the "seed" profile is active (never otherwise). Refuses to run against the shared
 * Neon database unless {@code seed.allow-shared-db=true} is set explicitly. See docs/clinical-billing.md.
 */
@Component
@Profile("seed")
public class SeedRunner implements CommandLineRunner {

    private final SeedService seedService;
    private final DataSource dataSource;
    private final boolean allowSharedDb;

    public SeedRunner(SeedService seedService, DataSource dataSource,
                      @Value("${seed.allow-shared-db:false}") boolean allowSharedDb) {
        this.seedService = seedService;
        this.dataSource = dataSource;
        this.allowSharedDb = allowSharedDb;
    }

    /**
     * Checks the URL of the connection actually in use (not just a property), because .env, environment
     * variables and profiles can all change where the app points; seeding the team database by accident
     * would mix demo rows into everyone's data.
     */
    @Override
    public void run(String... args) throws SQLException {
        String url;
        try (Connection connection = dataSource.getConnection()) {
            url = connection.getMetaData().getURL();
        }
        if (isSharedDatabase(url) && !allowSharedDb) {
            throw new IllegalStateException("Refusing to seed: the database looks like the shared Neon DB (" + host(url)
                    + "). Point DB_URL at a local or throwaway database, or set seed.allow-shared-db=true if the team "
                    + "has agreed.");
        }
        seedService.seed();
    }

    static boolean isSharedDatabase(String jdbcUrl) {
        return jdbcUrl != null && jdbcUrl.toLowerCase(Locale.ROOT).contains("neon.tech");
    }

    /** Host part only, so the log never shows credentials that might be in the URL. */
    static String host(String jdbcUrl) {
        String authority = jdbcUrl.replaceFirst("^jdbc:[a-z]+://", "");
        for (char stop : new char[]{'/', '?'}) {
            int i = authority.indexOf(stop);
            if (i >= 0) {
                authority = authority.substring(0, i);
            }
        }
        authority = authority.substring(authority.lastIndexOf('@') + 1); // drop "user:password@"
        int colon = authority.indexOf(':');
        return colon >= 0 ? authority.substring(0, colon) : authority;
    }
}
