package com.kws.meercat.config;

import com.kws.meercat.admin.login.api.JwtUserDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@RequiredArgsConstructor
@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfiguration {

    private final JwtUserDetailService jwtUserDetailService;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean("authenticationManager")
    public AuthenticationManager authenticationManager(HttpSecurity http, BCryptPasswordEncoder passwordEncoder)
            throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.userDetailsService(jwtUserDetailService).passwordEncoder(passwordEncoder);
        return authenticationManagerBuilder.build();
    }

    // 정적 자원에는 인증을 적용하지 않음
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> {
            web.ignoring().requestMatchers("/css/**", "/js/**", "/img/**", "/lib/**");
        };
    }

    // SecurityFilterChain은 필터를 적용하는 방법을 정의하는 인터페이스
    // HttpSecurity를 사용하여 필터를 적용하는 방법을 정의
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // CSRF 토큰 비활성화
                .cors(AbstractHttpConfigurer::disable) // cors 설정 비활성화
                //인가 정책
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/rest/auth/**").permitAll()
                        .requestMatchers("user").hasRole("USER")
                        .requestMatchers("admin").hasRole("ADMIN")
                        .requestMatchers("/join", "/exception", "/denied").permitAll()
                        .anyRequest()
                        .authenticated()
                )
                // 로그인 설정
                .formLogin((formLogin) -> formLogin
                        .loginPage("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .loginProcessingUrl("/login_ process")
                        .failureForwardUrl("/login?error")
                        .successForwardUrl("/home")
                        .successHandler((request, response, authentication) -> response.sendRedirect("/home"))
                        .failureHandler((request, response, exception) -> response.sendRedirect("/login?error"))
                        .permitAll() //인증이 되어 있지 않아도 접근이 가능함
                )
                // 로그아웃
                .logout((logout) -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login")
                        .addLogoutHandler((request, response, authentication) -> {
                            request.getSession().invalidate();
                        })
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.sendRedirect("/login");
                        })
                        .deleteCookies("JSESSIONID") //해당 이름의 쿠키 삭제
                )
                // 세션
                .sessionManagement((sessionManagement) -> sessionManagement
                        .maximumSessions(1) // 최대 세션 1개
                        // 1개 이후에는 더이상 세션이 생기지 않도록 즉 2번째 요청부터 로그인을 막음 false일 땐 기존 사용자 로그아웃
                        .maxSessionsPreventsLogin(true)
                );
        return http.build();
    }

}
