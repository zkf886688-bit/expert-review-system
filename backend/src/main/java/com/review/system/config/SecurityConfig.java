package com.review.system.config;

import com.review.system.security.JwtAuthFilter;
import com.review.system.security.JsonAccessDeniedHandler;
import com.review.system.security.JsonAuthEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final JsonAuthEntryPoint authEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;

    @Value("${springdoc.api-docs.enabled:true}")
    private boolean apiDocsEnabled;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .headers(h -> h
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                        .contentTypeOptions(c -> {})
                        .cacheControl(c -> {}))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/", "/api/auth/login", "/api/auth/captcha", "/api/auth/captcha/**", "/api/auth/sso/**").permitAll()
                            .requestMatchers("/api/public/**").permitAll()
                            .requestMatchers("/actuator", "/actuator/", "/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll();
                    if (apiDocsEnabled) {
                        auth.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll();
                    }
                    auth
                        .requestMatchers("/api/reviews/**").hasRole("EXPERT")
                        .requestMatchers("/api/experts/**", "/api/dashboard/**", "/api/credit/**").hasAnyRole("ADMIN", "REVIEW_ADMIN")
                        .requestMatchers("/api/audit/**").hasAnyRole("ADMIN", "REVIEW_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/tasks").hasAnyRole("ADMIN", "REVIEW_ADMIN", "LEADER")
                        .requestMatchers("/api/tasks/**").hasAnyRole("ADMIN", "REVIEW_ADMIN")
                        .requestMatchers("/api/summary/**").hasAnyRole("ADMIN", "REVIEW_ADMIN", "LEADER")
                        .requestMatchers("/api/analytics/**").hasAnyRole("ADMIN", "REVIEW_ADMIN", "LEADER")
                        .requestMatchers("/api/system/**").hasRole("ADMIN")
                        .anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
