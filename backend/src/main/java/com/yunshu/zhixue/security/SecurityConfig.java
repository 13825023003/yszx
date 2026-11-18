package com.yunshu.zhixue.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 安全配置：两端各一条独立过滤链，令牌域互不通用（D7/D12/D20）。
 *
 * <p>同域部署下不配置 CORS 白名单；Nginx 统一入口完成 /api 反向代理。</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** 学员端公开接口：认证入口、首页公开数据、题库浏览（D4-2）。 */
    private static final String[] PORTAL_PUBLIC_PATHS = {
            "/api/portal/v1/auth/sms-code",
            "/api/portal/v1/auth/register",
            "/api/portal/v1/auth/login/**",
            "/api/portal/v1/auth/reset/**",
            "/api/portal/v1/auth/refresh",
            "/api/portal/v1/auth/logout",
            "/api/portal/v1/home/platform-stats",
            "/api/portal/v1/home/hot-stages",
            "/api/portal/v1/subjects/**",
            "/api/portal/v1/questions/**"
    };

    /** 管理端公开接口：登录、刷新、登出（登出幂等）。 */
    private static final String[] ADMIN_PUBLIC_PATHS = {
            "/api/admin/v1/auth/login",
            "/api/admin/v1/auth/refresh",
            "/api/admin/v1/auth/logout"
    };

    /** 非业务路径：OpenAPI 文档、健康检查、对象存储直链、错误页。 */
    private static final String[] INFRASTRUCTURE_PATHS = {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/webjars/**",
            "/actuator/health",
            "/actuator/health/**",
            "/actuator/info",
            "/files/**",
            "/error"
    };

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final ObjectProvider<AccountStatusValidator> accountStatusValidatorProvider;

    /** 管理端密码使用 BCrypt 存储与校验。 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain portalSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/portal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PORTAL_PUBLIC_PATHS).permitAll()
                        .requestMatchers("/api/portal/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter(TokenDomain.PORTAL),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/admin/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(ADMIN_PUBLIC_PATHS).permitAll()
                        .requestMatchers("/api/admin/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter(TokenDomain.ADMIN),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(3)
    public SecurityFilterChain infrastructureSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(INFRASTRUCTURE_PATHS).permitAll()
                        .anyRequest().permitAll())
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    private JwtAuthenticationFilter jwtAuthenticationFilter(TokenDomain domain) {
        return new JwtAuthenticationFilter(jwtTokenProvider, tokenBlacklistService,
                domain, authenticationEntryPoint, accountStatusValidatorProvider);
    }
}
