package com.gomech.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class MfaService {

    private static final int TIME_STEP_SECONDS = 30;
    private static final int TOTP_DIGITS = 6;

    private final SecureRandom secureRandom = new SecureRandom();
    private final EncryptionService encryptionService;

    public MfaService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public String generateSecret() {
        byte[] buffer = new byte[32];
        secureRandom.nextBytes(buffer);
        return Base64.getEncoder().encodeToString(buffer);
    }

    public String encryptSecret(String secret) {
        return encryptionService.encrypt(secret);
    }

    public boolean verifyCode(String encryptedSecret, String code) {
        if (encryptedSecret == null || code == null) {
            return false;
        }
        String secret = encryptionService.decrypt(encryptedSecret);
        long timeWindow = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        return verifyCodeForWindow(secret, code, timeWindow) || verifyCodeForWindow(secret, code, timeWindow - 1) || verifyCodeForWindow(secret, code, timeWindow + 1);
    }

    public String generateCode(String secret) {
        long timeWindow = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        return generateCodeForWindow(secret, timeWindow);
    }

    private boolean verifyCodeForWindow(String secret, String code, long window) {
        try {
            String expected = generateCodeForWindow(secret, window);
            return constantTimeEquals(expected, code);
        } catch (Exception e) {
            return false;
        }
    }

    private String generateCodeForWindow(String secret, long window) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secret);
            ByteBuffer buffer = ByteBuffer.allocate(8);
            buffer.putLong(window);
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA1"));
            byte[] hash = mac.doFinal(buffer.array());
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);
            int otp = binary % (int) Math.pow(10, TOTP_DIGITS);
            return String.format("%0" + TOTP_DIGITS + "d", otp);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to generate TOTP code", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
