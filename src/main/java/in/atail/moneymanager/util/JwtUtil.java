package in.atail.moneymanager.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component  //
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * 生成 JWT 令牌
     * 基于用户邮箱创建包含默认声明的 JWT 令牌
     *
     * @param email 用户的邮箱地址，作为 JWT 的主题（subject）标识符
     * @return String 生成的 JWT 令牌字符串，包含过期时间等标准声明
     */
    public String generateToken(String email) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, email);
    }


    /**
     * 验证 JWT 令牌
     * 验证令牌是否与指定用户邮箱匹配且未过期
     *
     * @param token 待验证的 JWT 令牌字符串
     * @param email 待验证的用户邮箱地址
     * @return boolean 如果令牌验证成功则返回 true，否则返回 false
     */
    public boolean validateToken(String token, String email) {
        final String tokenEmail = extractEmail(token);
        return (tokenEmail.equals(email) && !isTokenExpired(token));
    }


    /**
     * 从 JWT 令牌中提取用户邮箱
     * 解析令牌并获取 subject 字段（存储的是用户邮箱）
     *
     * @param token JWT 令牌字符串
     * @return String 从令牌中提取的用户邮箱地址
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }


    /**
     * 从 JWT 令牌中提取过期时间
     * 解析令牌并获取 exp 字段（存储的是令牌过期时间）
     *
     * @param token JWT 令牌字符串
     * @return Date 从令牌中提取的过期时间
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }


    /**
     * 从 JWT 令牌中提取指定声明
     * 解析令牌并获取指定声明的值
     *
     * @param token JWT 令牌字符串
     * @param claimsResolver 声明解析函数，用于从 Claims 中获取指定声明的值
     * @return T 从令牌中提取的指定声明的值
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }


    /**
     * 解析 JWT 令牌并获取 Claims
     * 解析令牌并获取 Claims 对象，包含令牌中的所有声明
     *
     * @param token JWT 令牌字符串
     * @return Claims 解析后的 Claims 对象，包含令牌中的所有声明
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    /**
     * 检查 JWT 令牌是否已过期
     * 检查令牌是否已过期，即当前时间是否超过令牌过期时间
     *
     * @param token JWT 令牌字符串
     * @return boolean 如果令牌已过期则返回 true，否则返回 false
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }


    /**
     * 创建 JWT 令牌
     * 基于 Claims 和用户邮箱创建 JWT 令牌
     *
     * @param claims 声明对象，包含令牌中的所有声明
     * @param subject 令牌的主题（subject），即用户邮箱地址
     * @return String 生成的 JWT 令牌字符串，包含过期时间等标准声明
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }


    /**
     * 获取签名密钥
     * 创建用于签名 JWT 令牌的密钥
     *
     * @return Key 签名密钥对象
     */
    private Key getSigningKey() {
        // 方式一：直接使用 UTF-8 字节（需确保 secret 长度 >= 32 个 ASCII 字符）
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}