package com.ethiorental.backend.IAM.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint unauthorizedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(daoAuthenticationProvider());
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://192.168.*:*",
                "http://10.*:*",
                "http://172.*:*"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .exceptionHandling(ex -> ex.authenticationEntryPoint(unauthorizedHandler))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Allow CORS preflight
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // Public auth endpoints
                .requestMatchers("/api/v1/auth/**").permitAll()
                // Fayda verification (public — used during registration)
                .requestMatchers("/api/v1/fayda/**").permitAll()
                // Location reference data (sub-cities & woredas — used by registration forms)
                .requestMatchers("/api/v1/locations/**").permitAll()
                // Admin only
                .requestMatchers("/api/v1/admin/**").hasRole("SYSTEM_ADMINISTRATOR")
                // Officer portal — government employees with any officer role
                .requestMatchers("/api/v1/officer/**").hasAnyRole(
                        "WOREDA_OFFICER", "WOREDA_SUPERVISOR",
                        "SUB_CITY_ADMINISTRATOR", "CITY_ADMINISTRATOR",
                        "TAX_OFFICER", "SYSTEM_ADMINISTRATOR", "AUDITOR")
                // Authenticated users profile
                .requestMatchers("/api/v1/users/**").authenticated()
                // Properties — /my endpoint requires LANDLORD or BOTH, other GETs are public for LISTED, everything else requires auth
                .requestMatchers(HttpMethod.GET, "/api/v1/properties/my").hasAnyRole("LANDLORD", "BOTH")
                .requestMatchers(HttpMethod.GET, "/api/v1/properties/**").permitAll()
                .requestMatchers("/api/v1/properties/**").authenticated()
                // Lease requests — applicant (tenant) vs landlord
                .requestMatchers(HttpMethod.GET, "/api/v1/lease-requests/my").hasAnyRole("TENANT", "BOTH")
                .requestMatchers(HttpMethod.GET, "/api/v1/lease-requests/landlord").hasAnyRole("LANDLORD", "BOTH")
                // Tax — landlord only
                .requestMatchers("/api/v1/tax/**").hasAnyRole("LANDLORD", "BOTH", "TAX_OFFICER", "SYSTEM_ADMINISTRATOR")
                // Agreements — tenant vs landlord
                .requestMatchers(HttpMethod.GET, "/api/v1/agreements/tenant/active").hasRole("TENANT")
                .requestMatchers(HttpMethod.GET, "/api/v1/agreements/landlord").hasAnyRole("LANDLORD", "BOTH", "ADMIN", "SYSTEM_ADMINISTRATOR")
                .requestMatchers(HttpMethod.GET, "/api/v1/agreements/tenant").hasAnyRole("TENANT", "BOTH", "ADMIN", "SYSTEM_ADMINISTRATOR")
                // Complaints — citizens submit/view own; officers view all (method security enforces roles)
                .requestMatchers(HttpMethod.POST, "/api/v1/complaints").hasRole("CITIZEN")
                .requestMatchers(HttpMethod.GET, "/api/v1/complaints/my").hasRole("CITIZEN")
                .requestMatchers("/api/v1/complaints/**").authenticated()
                // Reports — admin and auditor roles only (method security also enforces per-endpoint)
                .requestMatchers("/api/v1/reports/**").hasAnyRole(
                        "SYSTEM_ADMINISTRATOR", "CITY_ADMINISTRATOR", "AUDITOR")
                // Public Chapa payment webhook & payment verification settlement
                .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhook").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/payments/verify/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/payments/trigger-reminders").permitAll()
                // SSE notifications — permitAll for now (EventSource doesn't support Authorization headers)
                .requestMatchers("/api/notifications/**").permitAll()
                .anyRequest().authenticated()
            )
            .authenticationProvider(daoAuthenticationProvider())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
