package com.example.expensetracker.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    private final int expirationInSec;
    private final SecretKey signingKey;


    public JwtService( @Value("${app.jwt.expiration-minutes}") int expirationInMin, @Value("${app.jwt.secret}") String secret) {
        expirationInSec = expirationInMin * 60;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generateToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(expirationInSec)))
                .signWith(signingKey)
                .compact();
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(expirationInSec)))
                .signWith(signingKey)
                .compact();
    }


    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        String rawToken = stripBearer(token);

        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(rawToken)
                .getPayload();
    }

    private String stripBearer(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        return token;
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        if(token == null || userDetails == null) {
            return false;
        }
        Date exp;

        try {
            String username = extractUsername(token);
            if(!username.equals(userDetails.getUsername())) {
                return false;
            }
            exp = extractExpirationDate(token);
            if(exp.before(Date.from(Instant.now()))) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public Date extractExpirationDate(String token) {
        return extractClaim(token,Claims::getExpiration);
    }


}
