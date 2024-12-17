package com.commerce.backend.config;

import com.commerce.backend.jwt.JwtAuthenticationFilter;
import com.commerce.backend.jwt.JwtUtil;
import com.commerce.backend.service.JwtBlacklistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
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
public class SecurityConfig {


    private UserDetailsService userDetailsService;


    private JwtUtil jwtUtil;


    private JwtBlacklistService jwtBlacklistService;

    @Autowired
    public SecurityConfig (UserDetailsService userDetailsService
            , JwtUtil jwtUtil
            ,JwtBlacklistService jwtBlacklistService){
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
        this.jwtBlacklistService = jwtBlacklistService;

    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults()) // `Customizer.withDefaults()`를 사용해 기본 CORS 설정을 적용합니다.
                .csrf(csrf -> csrf.disable())    // 람다 스타일로 CSRF 비활성화
                .authorizeHttpRequests((requests) -> requests
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**","/api/games", "/api/users/signUp", "/api/users/login", "/api/ads", "/api/ads/**").permitAll()
                        .anyRequest().authenticated()
                ).addFilterBefore(new JwtAuthenticationFilter(jwtUtil, userDetailsService, jwtBlacklistService), UsernamePasswordAuthenticationFilter.class)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // 람다 스타일로 세션 정책 설정
//                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {

        return http.getSharedObject(AuthenticationManagerBuilder.class)
                .userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder())
                .and()
                .build();
//        auth.userDetailsService(userDetailsService)
//                .passwordEncoder(passwordEncoder());
//        return auth.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173")); 
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(true); // 인증 정보 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
