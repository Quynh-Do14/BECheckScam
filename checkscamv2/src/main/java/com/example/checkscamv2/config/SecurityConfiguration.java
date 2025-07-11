package com.example.checkscamv2.config;

import com.example.checkscamv2.util.SecurityUtil;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {

    @Value("${checkscam.jwt.base64-secret}")
    private String jwtKey;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/api/v1/check-scam/**",
            "/api/v1/ranking/**",
            "/ws/**",
            "/ws-simple/**",
            "/sockjs-node/**",
            "/topic/**",
            "/app/**",
            "/api/v1/activities/**"
    };

    private static final String[] PUBLIC_GET_ENDPOINTS = {
            "/api/v1/news/**",
            "/api/v1/users/profiles/**",
            "/api/v1/users/**",
            "/api/v1/report/image/**",
            "/api/v1/report/ranking/**",
            "/api/v1/report/ranking",
            "/api/v1/activities/**",
    };

    private static final String[] PUBLIC_POST_ENDPOINTS = {
            "/api/v1/report/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


    @Bean
    public BearerTokenResolver smartBearerTokenResolver() {
        return new BearerTokenResolver() {
            @Override
            public String resolve(HttpServletRequest request) {
                String uri = request.getRequestURI();
                String method = request.getMethod();

                if (isPublicEndpoint(uri, method)) {
                    return null; // Không extract JWT token
                }

                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    System.out.println("PROTECTED ENDPOINT: " + method + " " + uri + " → QUÉT THẺ: " +
                            (token.length() > 20 ? token.substring(0, 20) + "..." : token));
                    return token;
                }

                System.out.println(" PROTECTED ENDPOINT: " + method + " " + uri + " → KHÔNG CÓ THẺ");
                return null;
            }
        };
    }


    private boolean isPublicEndpoint(String uri, String method) {
        for (String pattern : PUBLIC_ENDPOINTS) {
            if (matchesPattern(uri, pattern)) {
                return true;
            }
        }

        if ("GET".equalsIgnoreCase(method)) {
            for (String pattern : PUBLIC_GET_ENDPOINTS) {
                if (matchesPattern(uri, pattern)) {
                    return true;
                }
            }
        }

        if ("POST".equalsIgnoreCase(method)) {
            for (String pattern : PUBLIC_POST_ENDPOINTS) {
                if (matchesPattern(uri, pattern)) {
                    return true;
                }
            }
        }

        return false;
    }


    private boolean matchesPattern(String uri, String pattern) {
        if (pattern.endsWith("/**")) {
            String prefix = pattern.substring(0, pattern.length() - 3);
            return uri.startsWith(prefix);
        }
        return uri.equals(pattern);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/v1/check-scam/**").permitAll()
                        .requestMatchers("/api/v1/ranking/**").permitAll()
                        .requestMatchers("/api/v1/activities/**").permitAll()
                        .requestMatchers("/ws/**", "/ws-simple/**", "/sockjs-node/**", "/topic/**", "/app/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/v1/news/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/profiles/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/report/image/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/report/ranking/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/report/ranking").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/report/**").permitAll()

                        .requestMatchers("/api/v1/news/**").authenticated()
                        .requestMatchers("/api/v1/users/**").authenticated()
                        .requestMatchers("/api/v1/report/**").authenticated()

                        .anyRequest().authenticated()
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(smartBearerTokenResolver())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:4200",
                "http://127.0.0.1:4200",
                "https://localhost:4200",
                "https://ai6.vn",
                "https://www.ai6.vn"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "Accept", "Origin",
                "X-Requested-With", "Cache-Control", "x-auth-token"
        ));

        configuration.setExposedHeaders(Arrays.asList("Authorization", "x-auth-token"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(getSecretKey())
                .macAlgorithm(SecurityUtil.JWT_ALGORITHM).build();

        return token -> {
            try {
                System.out.println(" Decoding JWT token...");
                return jwtDecoder.decode(token);
            } catch (Exception e) {
                System.out.println(" JWT decode error: " + e.getMessage());
                throw e;
            }
        };
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(getSecretKey()));
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, SecurityUtil.JWT_ALGORITHM.getName());
    }
}