package com.yunshu.zhixue.security;

import com.yunshu.zhixue.common.exception.BizException;
import com.yunshu.zhixue.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * JWT 签发与校验（D7）：
 * <ul>
 *   <li>学员端、管理端各自独立密钥与 audience，令牌互不通用；</li>
 *   <li>access token 短效、refresh token 长效，通过 {@code typ} 区分用途；</li>
 *   <li>每个令牌带 {@code jti}，登出时写入 Redis 黑名单。</li>
 * </ul>
 */
@Component
public class JwtTokenProvider {

    private static final String CLAIM_TYPE = "typ";
    private static final String CLAIM_ACCOUNT = "account";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final Map<TokenDomain, SecretKey> keys = new EnumMap<>(TokenDomain.class);

    public JwtTokenProvider(JwtProperties properties) {
        this.properties = properties;
        for (TokenDomain domain : TokenDomain.values()) {
            String secret = properties.forDomain(domain).getSecret();
            if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException(
                        "JWT 密钥长度不足（至少 32 字节）: domain=" + domain.getCode());
            }
            keys.put(domain, Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)));
        }
    }

    public TokenPair createTokenPair(TokenDomain domain, Long userId, String account) {
        String accessJti = UUID.randomUUID().toString();
        String refreshJti = UUID.randomUUID().toString();
        JwtProperties.Domain domainConfig = properties.forDomain(domain);

        String accessToken = buildToken(domain, domainConfig, userId, account,
                accessJti, TYPE_ACCESS, domainConfig.getAccessTokenTtl());
        String refreshToken = buildToken(domain, domainConfig, userId, account,
                refreshJti, TYPE_REFRESH, domainConfig.getRefreshTokenTtl());

        return new TokenPair(accessToken, refreshToken,
                domainConfig.getAccessTokenTtl().toSeconds(),
                domainConfig.getRefreshTokenTtl().toSeconds(),
                accessJti, refreshJti);
    }

    public JwtPrincipal parseAccessToken(String token, TokenDomain expectedDomain) {
        return parse(token, expectedDomain, TYPE_ACCESS);
    }

    public JwtPrincipal parseRefreshToken(String token, TokenDomain expectedDomain) {
        return parse(token, expectedDomain, TYPE_REFRESH);
    }

    public Duration refreshTokenTtl(TokenDomain domain) {
        return properties.forDomain(domain).getRefreshTokenTtl();
    }

    public Duration accessTokenTtl(TokenDomain domain) {
        return properties.forDomain(domain).getAccessTokenTtl();
    }

    private String buildToken(TokenDomain domain, JwtProperties.Domain domainConfig,
                              Long userId, String account, String jti,
                              String type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(domainConfig.getIssuer())
                .subject(String.valueOf(userId))
                .audience().add(domain.getCode()).and()
                .claim(CLAIM_ACCOUNT, account)
                .claim(CLAIM_TYPE, type)
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(keys.get(domain))
                .compact();
    }

    private JwtPrincipal parse(String token, TokenDomain expectedDomain, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(keys.get(expectedDomain))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Set<String> audience = claims.getAudience();
            if (audience == null || !audience.contains(expectedDomain.getCode())) {
                throw new JwtException("令牌域不匹配");
            }
            if (!expectedType.equals(claims.get(CLAIM_TYPE, String.class))) {
                throw new JwtException("令牌类型不匹配");
            }
            String expectedIssuer = properties.forDomain(expectedDomain).getIssuer();
            if (expectedIssuer != null && !expectedIssuer.equals(claims.getIssuer())) {
                throw new JwtException("令牌签发方不匹配");
            }
            Long userId = Long.valueOf(claims.getSubject());
            return new JwtPrincipal(userId, expectedDomain,
                    claims.get(CLAIM_ACCOUNT, String.class), claims.getId(),
                    claims.getExpiration().toInstant());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "登录状态无效或已过期", ex);
        }
    }
}
