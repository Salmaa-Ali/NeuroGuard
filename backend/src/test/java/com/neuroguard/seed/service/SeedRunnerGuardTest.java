package com.neuroguard.seed.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The shared-DB guard, without a database. */
class SeedRunnerGuardTest {

    @Test
    void neonUrlsAreRecognised() {
        assertTrue(SeedRunner.isSharedDatabase(
                "jdbc:postgresql://ep-cool-name-123456.eu-central-1.aws.neon.tech:5432/neondb?sslmode=require"));
        assertTrue(SeedRunner.isSharedDatabase("jdbc:postgresql://EP-X.NEON.TECH/db"));
        assertFalse(SeedRunner.isSharedDatabase("jdbc:postgresql://localhost:5432/neuroguard"));
        assertFalse(SeedRunner.isSharedDatabase("jdbc:postgresql://127.0.0.1:55432/test"));
        assertFalse(SeedRunner.isSharedDatabase(null));
    }

    @Test
    void hostNeverIncludesCredentials() {
        assertEquals("ep-x.eu-central-1.aws.neon.tech",
                SeedRunner.host("jdbc:postgresql://ep-x.eu-central-1.aws.neon.tech:5432/neondb?user=u&password=secret"));
        assertEquals("db.example.com", SeedRunner.host("jdbc:postgresql://user:secret@db.example.com/x"));
        assertEquals("localhost", SeedRunner.host("jdbc:postgresql://localhost/neuroguard"));
    }
}
