package com.project.animalface_web.config;

import com.project.animalface_web.security.APIUserDetailsService;
import com.project.animalface_web.security.CustomAuthenticationProvider;
import com.project.animalface_web.security.filter.APILoginFilter;
import com.project.animalface_web.security.filter.RefreshTokenFilter;
import com.project.animalface_web.security.filter.TokenCheckFilter;
import com.project.animalface_web.security.handler.APILoginSuccessHandler;
import com.project.animalface_web.service.MemberService;
import com.project.animalface_web.util.JWTUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Log4j2
@Configuration
@EnableMethodSecurity
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JWTUtil jwtUtil;

    private final APIUserDetailsService apiUserDetailsService;

    private final CustomAuthenticationProvider customAuthenticationProvider;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private TokenCheckFilter tokenCheckFilter(JWTUtil jwtUtil, APIUserDetailsService
            apiUserDetailsService){
        return new TokenCheckFilter(apiUserDetailsService, jwtUtil);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, MemberService memberService) throws Exception {
        log.info("-----------------------configuration---------------------");

        http.authenticationProvider(customAuthenticationProvider);

        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(apiUserDetailsService).passwordEncoder(passwordEncoder());

        AuthenticationManager authenticationManager = authenticationManagerBuilder.build();

        http.authenticationManager(authenticationManager);

        APILoginFilter apiLoginFilter = new APILoginFilter("/generateToken");
        apiLoginFilter.setAuthenticationManager(authenticationManager);

        APILoginSuccessHandler successHandler = new APILoginSuccessHandler(jwtUtil);

        apiLoginFilter.setAuthenticationSuccessHandler(successHandler);

        http.addFilterBefore(apiLoginFilter, UsernamePasswordAuthenticationFilter.class);

        http.addFilterBefore(
                tokenCheckFilter(jwtUtil, apiUserDetailsService),

                UsernamePasswordAuthenticationFilter.class
        );

        http.addFilterBefore(new RefreshTokenFilter("/refreshToken", jwtUtil),
                TokenCheckFilter.class);

        http.csrf(csrf -> csrf.disable());
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http
                .formLogin(formLogin ->
                        formLogin
                                .loginPage("/member/login")
                                .permitAll()
                )
                .authorizeRequests(authorizeRequests ->
                        authorizeRequests
                                .requestMatchers("/main","/member/register","/api/member/**","/api/notices/**","/apiLogin.html", "/static/**").permitAll()
                                .requestMatchers("/reservations/**","/payments/**","/member/delete").authenticated()

                )
                .sessionManagement(sessionManagement ->
                        sessionManagement
                                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                );

        http.logout(
                logout -> logout.logoutUrl("/member/logout")
                        .logoutSuccessUrl("/member/login")
                        .invalidateHttpSession(true)  // 세션 무효화
                        .deleteCookies("JSESSIONID").permitAll()  // 쿠키 삭제
        );

        http.formLogin(formLogin ->
                formLogin.defaultSuccessUrl("/main",true)
        );

        http.cors(httpSecurityCorsConfigurer -> {
            httpSecurityCorsConfigurer.configurationSource(corsConfigurationSource());
        });

        return http.build();
    }


    @Bean

    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("HEAD", "GET", "POST", "PUT", "DELETE"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}