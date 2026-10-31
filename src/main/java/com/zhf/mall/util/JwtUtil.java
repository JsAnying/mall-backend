package com.zhf.mall.util;

import com.zhf.mall.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.InvalidKeyException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil implements ApplicationContextAware {

    // 声明静态上下文，供静态方法使用
    private static ApplicationContext context;
    //在静态工具类中获取Spring管理的Bean
    @Override
    public void setApplicationContext(ApplicationContext context) {
        JwtUtil.context = context;
    }

    public static JwtConfig getJwtConfig() {
        return context.getBean(JwtConfig.class);
    }
    // 创建 JWT Token
    public static String createJwtToken(String username) {
        //获取JWT配置
        JwtConfig jwtConfig = getJwtConfig();
        //生成签名算法
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        //生成过期时间
        Date expirationDate = new Date(System.currentTimeMillis() + jwtConfig.getExpiration());
        try {
            return Jwts.builder()
                    .setSubject(username)
                    .setId(UUID.randomUUID().toString())
                    .setExpiration(expirationDate)
                    .signWith(signatureAlgorithm, jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8))
                    .compact();
        } catch (InvalidKeyException e) {
            throw new RuntimeException("生成JWT令牌失败" + e);
        }
    }

    /** 验证签名和有效期后返回声明；旧格式令牌没有 jti，不能用于撤销。 */
    public static Claims verify(String token) {
        SecretKey key = Keys.hmacShaKeyFor(getJwtConfig().getSecret().getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (claims.getId() == null || claims.getId().isBlank()
                || claims.getSubject() == null || claims.getSubject().isBlank()
                || claims.getExpiration() == null) {
            throw new io.jsonwebtoken.MalformedJwtException("JWT 缺少必要声明");
        }
        return claims;
    }
}
