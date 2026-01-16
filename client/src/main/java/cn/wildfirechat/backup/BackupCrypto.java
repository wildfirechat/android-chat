package cn.wildfirechat.backup;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 备份加密工具类
 * 实现与 iOS 完全一致的加密方案：AES-256-CBC + PBKDF2-SHA256
 */
public class BackupCrypto {

    /**
     * 加密数据
     * @param data 要加密的数据
     * @param password 密码
     * @return 加密后的 JSON 对象，包含 salt、iv、data 和 iterations
     */
    public static JSONObject encryptData(byte[] data, String password) throws Exception {
        if (data == null || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Data and password must not be null or empty");
        }

        // 1. 生成随机 Salt
        byte[] salt = generateRandomData(BackupConstants.SALT_LENGTH);

        // 2. 使用 PBKDF2-SHA256 派生密钥
        byte[] key = deriveKeyFromPassword(password, salt, BackupConstants.PBKDF2_ITERATIONS);

        // 3. 生成随机 IV
        byte[] iv = generateRandomData(BackupConstants.IV_LENGTH);

        // 4. 使用 AES-256-CBC 加密
        byte[] encryptedData = encryptAES256CBC(data, key, iv);

        // 5. 返回加密结果（Base64 编码）
        JSONObject result = new JSONObject();
        result.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP));
        result.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP));
        result.put("data", Base64.encodeToString(encryptedData, Base64.NO_WRAP));
        result.put("iterations", BackupConstants.PBKDF2_ITERATIONS);

        return result;
    }

    /**
     * 解密数据
     * @param encryptedData 加密的 JSON 对象，包含 salt、iv、data 和 iterations
     * @param password 密码
     * @return 解密后的原始数据
     */
    public static byte[] decryptData(JSONObject encryptedData, String password) throws Exception {
        if (encryptedData == null || password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Encrypted data and password must not be null or empty");
        }

        try {
            // 1. 提取 salt、iv、data 和 iterations
            byte[] salt = Base64.decode(encryptedData.getString("salt"), Base64.NO_WRAP);
            byte[] iv = Base64.decode(encryptedData.getString("iv"), Base64.NO_WRAP);
            byte[] data = Base64.decode(encryptedData.getString("data"), Base64.NO_WRAP);
            int iterations = encryptedData.optInt("iterations", BackupConstants.PBKDF2_ITERATIONS);

            // 2. 使用 PBKDF2-SHA256 派生密钥
            byte[] key = deriveKeyFromPassword(password, salt, iterations);

            // 3. 使用 AES-256-CBC 解密
            return decryptAES256CBC(data, key, iv);
        } catch (JSONException e) {
            throw new Exception("Invalid encrypted data format", e);
        }
    }

    /**
     * 使用 PBKDF2-SHA256 从密码派生密钥
     */
    private static byte[] deriveKeyFromPassword(String password, byte[] salt, int iterations) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                BackupConstants.KEY_LENGTH
        );

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] key = factory.generateSecret(spec).getEncoded();

        spec.clearPassword();
        return key;
    }

    /**
     * AES-256-CBC 加密
     */
    private static byte[] encryptAES256CBC(byte[] data, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec);
        return cipher.doFinal(data);
    }

    /**
     * AES-256-CBC 解密
     */
    private static byte[] decryptAES256CBC(byte[] encryptedData, byte[] key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
        return cipher.doFinal(encryptedData);
    }

    /**
     * 生成随机数据
     */
    public static byte[] generateRandomData(int length) {
        byte[] data = new byte[length];
        new SecureRandom().nextBytes(data);
        return data;
    }

    /**
     * 计算文件的 MD5 哈希值
     */
    public static String calculateMD5ForFile(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File does not exist");
        }

        MessageDigest md = MessageDigest.getInstance("MD5");
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

        byte[] hash = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 计算字节数组的 MD5 哈希值
     */
    public static String calculateMD5(byte[] data) throws Exception {
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(data);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 验证密码（用于测试密码是否正确）
     * @param encryptedData 加密的数据
     * @param password 密码
     * @return 密码是否正确
     */
    public static boolean verifyPassword(JSONObject encryptedData, String password) {
        try {
            decryptData(encryptedData, password);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private BackupCrypto() {
        // 私有构造函数，防止实例化
    }
}
