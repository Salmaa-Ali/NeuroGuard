package com.neuroguard.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Maps claims to authorities: role -> ROLE_ADMIN / ROLE_CLINICAL_STAFF / ROLE_PATIENT and
 * perm_scope -> PERM_NEUROLOGIST etc. Replaces Spring's default "scope"-based mapping.
 */
public final class JwtClaimsConverter {

    private JwtClaimsConverter() {
    }

    public static JwtAuthenticationConverter create() {
        Converter<Jwt, Collection<GrantedAuthority>> authorities = jwt -> {
            List<GrantedAuthority> result = new ArrayList<>();
            String role = jwt.getClaimAsString(CurrentUser.CLAIM_ROLE);
            if (role != null) {
                result.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            String scope = jwt.getClaimAsString(CurrentUser.CLAIM_PERM_SCOPE);
            if (scope != null) {
                result.add(new SimpleGrantedAuthority("PERM_" + scope));
            }
            return result;
        };
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
