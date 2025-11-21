package com.exemplo.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // você já controla login/logout na mão com HttpSession,
            // então pode deixar o Spring Security “transparente”
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // LIBERA TUDO
                .anyRequest().permitAll()
            )
            // desativa formulários e basic auth padrão do Spring
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // continua usando BCrypt pro hash de senha
        return new BCryptPasswordEncoder();
    }
}
