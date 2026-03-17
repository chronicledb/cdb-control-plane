package io.github.grantchen2003.cdb.control.plane.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public class ApiKeyUtils {

    private static final int RANDOM_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static String generate() {
        final byte[] bytes = new byte[RANDOM_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public static String hash(String rawKey) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] bytes = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean verify(String rawKey, String hashedKey) {
        return MessageDigest.isEqual(
                hash(rawKey).getBytes(StandardCharsets.UTF_8),
                hashedKey.getBytes(StandardCharsets.UTF_8)
        );
    }
}
