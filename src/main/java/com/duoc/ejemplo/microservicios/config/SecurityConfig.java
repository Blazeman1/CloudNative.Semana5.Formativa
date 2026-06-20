package com.duoc.ejemplo.microservicios.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración de Spring Security para el microservicio de inscripción de cursos.
 *
 * Securitiza todos los endpoints del API (/api/**) exigiendo un JWT válido,
 * emitido y firmado por Azure AD B2C (IDaaS). Spring Security valida
 * automáticamente la firma del token contra el issuer configurado en
 * application.properties (spring.security.oauth2.resourceserver.jwt.issuer-uri).
 *
 * Se deja público únicamente el endpoint de Actuator Health, utilizado por
 * el pipeline de GitHub Actions y por Docker para comprobar que el contenedor
 * está disponible sin necesidad de generar un token JWT en cada despliegue.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
            // usando el issuer de Azure AD B2C configurado en application.properties
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));

        return http.build();
    }
}
