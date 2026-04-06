package com.example.simpletiktok.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public final class JwtUtils {

    public static final long EXPIRE = 24L * 60 * 60 * 1000;
    public static final String APP_SECRET = "ukc8BDbRigUDaY6pZFfWus2jZWLPHO";
    private static final String USER_SUBJECT = "simple-tiktok-user";
    private static final String ADMIN_SUBJECT = "simple-tiktok-admin";

    private JwtUtils() {
    }

    public static String getJwtToken(Long id, String nickname) {
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setHeaderParam("alg", "HS256")
                .setSubject(USER_SUBJECT)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .claim("id", String.valueOf(id))
                .claim("nickname", nickname)
                .signWith(SignatureAlgorithm.HS256, APP_SECRET)
                .compact();
    }

    public static String getAdminJwtToken(Long id, String username) {
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setHeaderParam("alg", "HS256")
                .setSubject(ADMIN_SUBJECT)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE))
                .claim("id", String.valueOf(id))
                .claim("username", username)
                .signWith(SignatureAlgorithm.HS256, APP_SECRET)
                .compact();
    }

    public static boolean checkToken(String jwtToken) {
        return checkTokenBySubject(jwtToken, USER_SUBJECT);
    }

    public static boolean checkAdminToken(String jwtToken) {
        return checkTokenBySubject(jwtToken, ADMIN_SUBJECT);
    }

    public static Long getUserId(String jwtToken) {
        return getIdBySubject(jwtToken, USER_SUBJECT);
    }

    public static Long getAdminId(String jwtToken) {
        return getIdBySubject(jwtToken, ADMIN_SUBJECT);
    }

    private static boolean checkTokenBySubject(String jwtToken, String subject) {
        Claims claims = parseClaims(jwtToken);
        return claims != null && subject.equals(claims.getSubject());
    }

    private static Long getIdBySubject(String jwtToken, String subject) {
        Claims claims = parseClaims(jwtToken);
        if (claims == null || !subject.equals(claims.getSubject())) {
            return null;
        }
        Object id = claims.get("id");
        if (id == null) {
            return null;
        }
        try {
            return Long.valueOf(id.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private static Claims parseClaims(String jwtToken) {
        if (jwtToken == null || jwtToken.isBlank()) {
            return null;
        }
        try {
            Jws<Claims> claimsJws = Jwts.parser().setSigningKey(APP_SECRET).parseClaimsJws(jwtToken);
            return claimsJws.getBody();
        } catch (Exception e) {
            return null;
        }
    }
}
