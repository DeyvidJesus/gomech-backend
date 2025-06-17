package com.gomech.configuration;

import com.gomech.model.User;
import com.gomech.repository.UserRepository;
import com.gomech.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            String tokenJWT = getToken(request);
            
            if (tokenJWT != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Validar o token usando o TokenService
                if (tokenService.validateToken(tokenJWT)) {
                    String email = tokenService.getSubject(tokenJWT);
                    Optional<User> userOptional = userRepository.findByEmail(email);
                    
                    if (userOptional.isPresent()) {
                        User user = userOptional.get();
                        
                        // Criar autenticação
                        UsernamePasswordAuthenticationToken authenticationToken = 
                            new UsernamePasswordAuthenticationToken(
                                user, 
                                null, 
                                user.getAuthorities()
                            );
                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        // Definir no contexto de segurança
                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    }
                }
            }
        } catch (Exception e) {
            // Log do erro sem quebrar o filtro
            logger.error("Erro na validação do token JWT: " + e.getMessage());
        }
        
        filterChain.doFilter(request, response);
    }

    private String getToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7); // Remove "Bearer "
        }
        return null;
    }
}