package com.y5neko.dbapptools.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

public class AESCTRUtils {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private static final String ALGORITHM = "AES/CTR/PKCS7Padding";
    private static final int IV_SIZE = 16;

    /**
     * 生成随机256bit（32字节）密钥
     */
    public static byte[] generateKey() {
        try {
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(256);
            return kg.generateKey().getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("AES KeyGen失败", e);
        }
    }

    /**
     * 生成随机16字节IV
     */
    public static byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        return iv;
    }

    /**
     * AES-CTR加密
     * @param key 32字节AES密钥
     * @param iv 16字节初始化向量
     * @param plaintext 明文数据
     * @return 密文
     */
    public static byte[] encrypt(byte[] key, byte[] iv, byte[] plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(plaintext);
        } catch (Exception e) {
            throw new RuntimeException("AES加密失败", e);
        }
    }

    /**
     * AES-CTR解密
     * @param key 32字节AES密钥
     * @param iv 16字节初始化向量
     * @param ciphertext 密文数据
     * @return 明文
     */
    public static byte[] decrypt(byte[] key, byte[] iv, byte[] ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("AES解密失败", e);
        }
    }

    /**
     * Base64字符串转字节数组
     */
    public static byte[] base64ToBytes(String base64Str) {
        return Base64.getDecoder().decode(base64Str);
    }

    /**
     * 十六进制字符串转字节数组
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex字符串格式错误");
        }
        int len = hex.length() / 2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            int index = i * 2;
            int val = Integer.parseInt(hex.substring(index, index + 2), 16);
            result[i] = (byte) val;
        }
        return result;
    }

    /**
     * 字节数组转Base64字符串
     */
    public static String bytesToBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    /**
     * 字节数组转十六进制字符串
     */
    public static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // 方便调用的重载示例

    /**
     * 使用Base64密钥加密
     */
    public static byte[] encryptWithBase64Key(String base64Key, String base64Iv, byte[] plaintext) {
        byte[] key = base64ToBytes(base64Key);
        byte[] iv = base64ToBytes(base64Iv);
        return encrypt(key, iv, plaintext);
    }

    /**
     * 使用Hex密钥加密
     */
    public static byte[] encryptWithHexKey(String hexKey, String hexIv, byte[] plaintext) {
        byte[] key = hexToBytes(hexKey);
        byte[] iv = hexToBytes(hexIv);
        return encrypt(key, iv, plaintext);
    }

    /**
     * 使用Base64密钥解密
     */
    public static byte[] decryptWithBase64Key(String base64Key, String base64Iv, byte[] ciphertext) {
        byte[] key = base64ToBytes(base64Key);
        byte[] iv = base64ToBytes(base64Iv);
        return decrypt(key, iv, ciphertext);
    }

    /**
     * 使用Hex密钥解密
     */
    public static byte[] decryptWithHexKey(String hexKey, String hexIv, byte[] ciphertext) {
        byte[] key = hexToBytes(hexKey);
        byte[] iv = hexToBytes(hexIv);
        return decrypt(key, iv, ciphertext);
    }

    /**
     * 快速使用
     */
    public static String quickUse(String text, String key, String iv) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] ivBytes = iv.getBytes(StandardCharsets.UTF_8);
        byte[] ciphertext = encrypt(keyBytes, ivBytes, text.getBytes());
        return bytesToBase64(ciphertext);
    }

    public static void main(String[] args) {
        String text = "123123123";
        byte[] key = "e942ca56de46bd67edda30bf4c96f5e2".getBytes(StandardCharsets.UTF_8);
        byte[] iv = "DbappAPTLoginSpe".getBytes(StandardCharsets.UTF_8);
        byte[] ciphertext = encrypt(key, iv, text.getBytes());
        System.out.println(bytesToBase64(ciphertext));
        System.out.println(quickUse("123123123", "e942ca56de46bd67edda30bf4c96f5e2", "DbappAPTLoginSpe"));
    }
}
