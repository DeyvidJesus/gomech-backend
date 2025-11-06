package com.gomech.configuration;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.gomech.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {
    @Value("${api.security.token.secret}")
    private String secret;

    private final Duration accessTokenTtl;

    public TokenService(@Value("${security.access-token.ttl-minutes:15}") long accessTokenMinutes) {
        this.accessTokenTtl = Duration.ofMinutes(accessTokenMinutes);
    }

    public String generateToken(User user){
        return generateAccessToken(user);
    }

    public String generateAccessToken(User user){
        try{
            Algorithm algorithm = Algorithm.HMAC256(secret);
            var jwtBuilder = JWT.create()
                    .withIssuer("auth-api")
                    .withSubject(user.getEmail())
                    .withClaim("role", user.getRole().name())
                    .withExpiresAt(genExpirationDate());

            if (user.getOrganization() != null && user.getOrganization().getId() != null) {
                jwtBuilder.withClaim("organizationId", user.getOrganization().getId());
            }

            return jwtBuilder.sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Error while generating token", exception);
        }
    }

    public String validateToken(String token){
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("auth-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception){
            return "";
        }
    }

    private Instant genExpirationDate(){
        return Instant.now().plus(accessTokenTtl);
    }
}
