package com.duoc.ejemplo.microservicios.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

/**
 * Configuración de Spring Security para el microservicio de inscripción de cursos.
 *
 * Securitiza todos los endpoints del API (/api/**) exigiendo un JWT válido,
 * emitido y firmado por Azure AD B2C (IDaaS).
 *
 * NOTA IMPORTANTE (Semana 5): Azure AD B2C no implementa el endpoint de
 * auto-discovery estándar en la ruta del issuer (".well-known/openid-configuration"
 * bajo el "iss" del token). Por eso NO se usa
 * spring.security.oauth2.resourceserver.jwt.issuer-uri (que dispara ese
 * auto-discovery y falla con "Unable to resolve the Configuration with the
 * provided Issuer"). En su lugar, se construye el JwtDecoder manualmente
 * apuntando directo al jwk-set-uri (claves públicas de firma de la policy),
 * y se valida el claim "iss" de forma explícita contra el issuer esperado.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${app.security.expected-issuer}")
    private String expectedIssuer;

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> withIssuer = new JwtIssuerValidator(expectedIssuer);
        OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> validator =
                new DelegatingOAuth2TokenValidator<>(List.of(withIssuer, withTimestamp));

        decoder.setJwtValidator(validator);
        return decoder;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // API stateless protegida con JWT: no se requiere protección CSRF basada en sesión
            .csrf(csrf -> csrf.disable())

            // No se mantienen sesiones HTTP: cada request se autentica con su propio JWT
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // Health check público para el pipeline CI/CD y Docker healthcheck
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                // Todo el resto de los endpoints de negocio requiere JWT válido
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )

            // Habilita la validación de JWT como Resource Server OAuth2,
            // usando el JwtDecoder configurado arriba (jwk-set-uri + issuer manual)
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(jwtDecoder())));

        return http.build();
    }
}
