package com.duoc.ejemplo.microservicios.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;

/**
 * Configuración de Spring Security — Semana 6: Custom Claims y control de acceso por rol.
 *
 * Extiende la configuración de la Semana 5 agregando autorización basada en el
 * custom claim "extension_consultaRole" emitido por Azure AD B2C (IDaaS).
 *
 * Roles definidos:
 *   - "consulta"  → accede a GET /api/cursos/consulta (listado de cursos)
 *   - "admin"     → accede a POST /api/cursos        (creación de cursos)
 *
 * Si el token es válido pero el claim no tiene el rol requerido: 403 Forbidden.
 * Si no hay token o el token es inválido:                       401 Unauthorized.
 *
 * Nota sobre Azure AD B2C y auto-discovery OIDC (Semana 5):
 * Se usa jwk-set-uri en vez de issuer-uri porque B2C no implementa el endpoint
 * de auto-discovery estándar en la ruta del issuer. Ver sección 2.4 del informe S5. 
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${app.security.expected-issuer}")
    private String expectedIssuer;

    /**
     * Construye el JwtDecoder manualmente apuntando al jwk-set-uri de la policy de B2C,
     * y agrega validadores explícitos de issuer y expiración.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> withIssuer    = new JwtIssuerValidator(expectedIssuer);
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> validator     =
                new DelegatingOAuth2TokenValidator<>(List.of(withIssuer, withTimestamp));

        decoder.setJwtValidator(validator);
        return decoder;
    }

    /**
     * Extrae el custom claim "extension_consultaRole" del JWT y lo convierte en
     * una GrantedAuthority de Spring Security con prefijo "ROLE_".
     * Esto permite usar hasRole("consulta") o hasRole("admin") en los matchers.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new java.util.ArrayList<>();
            // Azure AD B2C emite el custom claim directamente como "extension_consultaRole"
            String role = jwt.getClaimAsString("extension_consultaRole");
            if (role != null && !role.isBlank()) {
                // Prefijo ROLE_ para usar hasRole() en los matchers de Spring Security
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // API stateless: CSRF no aplica (autenticación por token, no por sesión)
            .csrf(csrf -> csrf.disable())

            // No se mantienen sesiones HTTP
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // Health check público para el pipeline CI/CD y Docker healthcheck
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                // Endpoint de consulta de cursos: requiere JWT válido + rol "consulta"
                // -> 401 si no hay token, 403 si el rol no coincide
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                                 "/api/cursos/consulta")
                    .hasRole("consulta")

                // Creación de cursos: requiere JWT válido + rol "admin"
                // -> 401 si no hay token, 403 si el rol no coincide
                .requestMatchers(org.springframework.http.HttpMethod.POST,
                                 "/api/cursos")
                    .hasRole("admin")

                // Resto de endpoints del API: JWT válido, cualquier rol
                .requestMatchers("/api/**").authenticated()

                .anyRequest().authenticated()
            )

            // Resource Server OAuth2: usa el JwtDecoder y el converter de roles
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                ));

        return http.build();
    }
}
