package com.pawanpoudel.powerauth.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

public class EncryptionUtil {

    private static final KeyPair keyPair;

    static {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(1024);
            keyPair = generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    public static KeyPair getKeyPair() {
        return keyPair;
    }

    public static byte[] generateVerifyToken() {
        byte[] token = new byte[4];
        new java.security.SecureRandom().nextBytes(token);
        return token;
    }

    public static String getServerIdHash(String serverId, PublicKey publicKey, SecretKey secretKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(serverId.getBytes("ISO_8859_1"));
            digest.update(secretKey.getEncoded());
            digest.update(publicKey.getEncoded());

            // Minecraft uses a special hex encoding for negative BigIntegers
            byte[] hash = digest.digest();
            java.math.BigInteger bigInt = new java.math.BigInteger(hash);

            // Handle negative values (two's complement)
            if (bigInt.compareTo(java.math.BigInteger.ZERO) < 0) {
                return "-" + bigInt.negate().toString(16);
            }
            return bigInt.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PublicKey getPublicKey() {
        return keyPair.getPublic();
    }

    public static SecretKey decryptSharedSecret(PublicKey publicKey, byte[] encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            return new SecretKeySpec(cipher.doFinal(encrypted), "AES");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decryptVerifyToken(PublicKey publicKey, byte[] encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            return cipher.doFinal(encrypted);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
