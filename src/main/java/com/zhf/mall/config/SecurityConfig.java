package com.zhf.mall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhf.mall.common.Result;
import com.zhf.mall.common.ResultCode;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    public static final String[] PUBLIC_URLS = {
            "/api/auth/register", "/api/auth/login","/api/categories"
    };
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JdbcTemplate jdbc, ObjectMapper json) throws Exception {
        http
                //禁用csrf
                .csrf(csrf -> csrf.disable())
                //配置会话管理为无状态（jwt需要）
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    json.writeValue(response.getOutputStream(), Result.error(ResultCode.UNAUTHORIZED));
                }))
                //配置请求授权
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/home/recommendations").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/media/avatars/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/goods", "/api/goods/{id}").permitAll()
                        .requestMatchers(PUBLIC_URLS).permitAll() // 公共请求
                        .anyRequest().authenticated() // 其他请求需要认证
                )
                .addFilterBefore(new JwtAuthenticationFilter(jdbc, json), UsernamePasswordAuthenticationFilter.class);// 添加JWT认证过滤器
        return http.build();
    }

}
