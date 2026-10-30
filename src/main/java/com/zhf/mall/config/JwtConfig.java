package com.zhf.mall.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    private String secret;//
    private long expiration;// 访问令牌有效期（毫秒）
    private long refreshExpiration;// 刷新令牌有效期（毫秒）
    private String issuer;// 签发者标识
    private String tokenPrefix;// 令牌前缀
    private String headerString;// 头字符串
}
