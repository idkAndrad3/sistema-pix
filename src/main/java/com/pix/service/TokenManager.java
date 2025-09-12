package com.pix.service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class TokenManager {
    private static final ConcurrentHashMap<String, TokenInfo> tokens = new ConcurrentHashMap<>();
    private static final long TOKEN_EXPIRY_HOURS = 24; // Token expira em 24 horas
    
    private static class TokenInfo {
        String cpf;
        LocalDateTime criadoEm;
        
        TokenInfo(String cpf) {
            this.cpf = cpf;
            this.criadoEm = LocalDateTime.now();
        }
        
        boolean isExpired() {
            return ChronoUnit.HOURS.between(criadoEm, LocalDateTime.now()) > TOKEN_EXPIRY_HOURS;
        }
    }
    
    /**
     * Gera um novo token para o usuário
     */
    public static String generateToken(String cpf) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, new TokenInfo(cpf));
        return token;
    }
    
    /**
     * Valida um token e retorna o CPF do usuário se válido
     */
    public static String validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        
        TokenInfo tokenInfo = tokens.get(token);
        if (tokenInfo == null) {
            return null;
        }
        
        if (tokenInfo.isExpired()) {
            tokens.remove(token);
            return null;
        }
        
        return tokenInfo.cpf;
    }
    
    /**
     * Remove um token (logout)
     */
    public static boolean removeToken(String token) {
        return tokens.remove(token) != null;
    }
    
    /**
     * Remove todos os tokens expirados
     */
    public static void cleanupExpiredTokens() {
        tokens.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}

