package com.gomech.utils;

import com.gomech.model.User;
import org.springframework.stereotype.Component;
import java.util.Date;

@Component
public class JWTUtils {

    public boolean validateToken(String subject, Date expirationDate, User user) {
        return subject != null && 
               user != null && 
               subject.equals(user.getUsername()) && 
               !isTokenExpired(expirationDate);
    }

    private boolean isTokenExpired(Date expirationDate) {
        return expirationDate != null && expirationDate.before(new Date());
    }
}