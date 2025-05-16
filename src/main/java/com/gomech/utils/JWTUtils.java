package com.gomech.utils;

import com.gomech.model.User;
import com.gomech.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JWTUtils {

    @Autowired
    private TokenService tokenService;

    public boolean validateToken(String subject, Date expirationDate, User user) {
        return subject.equals(user.getUsername()) && !isTokenExpired(expirationDate);
    }

    private boolean isTokenExpired(Date expirationDate) {
        return expirationDate.before(new Date());
    }
}