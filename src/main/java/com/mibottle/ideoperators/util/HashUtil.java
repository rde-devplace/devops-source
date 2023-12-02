package com.mibottle.ideoperators.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

    /**
     * Generates a SHA-256 hash for the given object.
     *
     * @param object The object to be hashed.
     * @return The generated SHA-256 hash.
     */
    public static String generateSHA256Hash(Object object) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String objectAsString = objectMapper.writeValueAsString(object);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(objectAsString.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                hexString.append(Integer.toHexString(0xFF & b));
            }

            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate SHA-256 hash", e);
        }
    }
}

