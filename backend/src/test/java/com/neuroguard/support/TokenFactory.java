package com.neuroguard.support;

import com.neuroguard.security.Role;
import com.neuroguard.security.UserType;
import com.neuroguard.staff.entity.PermissionScope;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/** Mints valid and deliberately broken JWTs for tests. */
public class TokenFactory {

    public static final String TEST_SECRET = "test-secret-for-neuroguard-tests-0123456789";
    public static final String OTHER_SECRET = "a-completely-different-secret-0123456789!!";

    private TokenFactory() {
    }

    public static String valid(UserType type, UUID sub, Role role, PermissionScope scope) {
        return build(type, sub, role, scope, Instant.now(), Instant.now().plus(Duration.ofHours(1)),
                "neuroguard", TEST_SECRET);
    }

    /** Expired an hour ago (well beyond the decoder's clock skew). */
    public static String expired(UserType type, UUID sub, Role role, PermissionScope scope) {
        return build(type, sub, role, scope, Instant.now().minus(Duration.ofHours(2)),
                Instant.now().minus(Duration.ofHours(1)), "neuroguard", TEST_SECRET);
    }

    public static String signedWithWrongKey(UserType type, UUID sub, Role role, PermissionScope scope) {
        return build(type, sub, role, scope, Instant.now(), Instant.now().plus(Duration.ofHours(1)),
                "neuroguard", OTHER_SECRET);
    }

    public static String wrongIssuer(UserType type, UUID sub, Role role, PermissionScope scope) {
        return build(type, sub, role, scope, Instant.now(), Instant.now().plus(Duration.ofHours(1)),
                "someone-else", TEST_SECRET);
    }

    /** Valid signature for a low-privilege token, but the payload swapped for an ADMIN one. */
    public static String tamperedPayload(UUID patientSub) {
        String original = valid(UserType.PATIENT, patientSub, Role.PATIENT, null);
        String forgedPayload = valid(UserType.STAFF, UUID.randomUUID(), Role.ADMIN, null).split("\\.")[1];
        String[] parts = original.split("\\.");
        return parts[0] + "." + forgedPayload + "." + parts[2];
    }

    /** Unsigned token with alg "none" claiming to be an admin. */
    public static String algNone() {
        JWTClaimsSet claims = claims(UserType.STAFF, UUID.randomUUID(), Role.ADMIN, null,
                Instant.now(), Instant.now().plus(Duration.ofHours(1)), "neuroguard");
        return new PlainJWT(claims).serialize();
    }

    private static JWTClaimsSet claims(UserType type, UUID sub, Role role, PermissionScope scope,
                                       Instant iat, Instant exp, String issuer) {
        JWTClaimsSet.Builder b = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .subject(sub.toString())
                .issueTime(Date.from(iat))
                .expirationTime(Date.from(exp))
                .claim("user_type", type.name())
                .claim("role", role.name());
        if (scope != null) {
            b.claim("perm_scope", scope.name());
        }
        return b.build();
    }

    private static String build(UserType type, UUID sub, Role role, PermissionScope scope,
                                Instant iat, Instant exp, String issuer, String secret) {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256),
                    claims(type, sub, role, scope, iat, exp, issuer));
            jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }
}
