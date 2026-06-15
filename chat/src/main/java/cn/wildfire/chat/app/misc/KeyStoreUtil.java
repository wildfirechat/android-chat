package cn.wildfire.chat.app.misc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import cn.wildfire.chat.kit.Config;

public class KeyStoreUtil {
    private static final String PP_KEYSTORE_TYPE = "AndroidKeyStore";
    private static final String PP_KEYSTORE_ALIAS = "pp_keystore_alias";
    private static final String PP_RSA_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    /**
     * 触发生成密钥对.
     * <p>
     * 生成RSA 密钥对，包括公钥和私钥
     *
     * @return KeyPair 密钥对，包含公钥和私钥
     */
    private static KeyPair generateKey() throws Exception {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, PP_KEYSTORE_TYPE);
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(PP_KEYSTORE_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build();
            keyPairGenerator.initialize(spec);
            return keyPairGenerator.generateKeyPair();
        } else {
            return null;
        }
    }

    /**
     * 获取公钥.
     *
     * @return 公钥
     */
    private static PublicKey getPublicKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(PP_KEYSTORE_TYPE);
        keyStore.load(null);
        if (!keyStore.containsAlias(PP_KEYSTORE_ALIAS)) {
            return generateKey().getPublic();
        }
        // FYI https://stackoverflow.com/questions/52024752/android-9-keystore-exception-android-os-servicespecificexception
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return keyStore.getCertificate(PP_KEYSTORE_ALIAS).getPublicKey();
        } else {
            KeyStore.Entry entry = keyStore.getEntry(PP_KEYSTORE_ALIAS, null);
            return entry instanceof KeyStore.PrivateKeyEntry
                    ? ((KeyStore.PrivateKeyEntry) entry).getCertificate().getPublicKey()
                    : null;
        }
    }

    /**
     * 获取私钥.
     *
     * @return 密钥
     */
    private static PrivateKey getPrivateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(PP_KEYSTORE_TYPE);
        keyStore.load(null);
        if (!keyStore.containsAlias(PP_KEYSTORE_ALIAS)) {
            return generateKey().getPrivate();
        }
        // FYI https://stackoverflow.com/questions/52024752/android-9-keystore-exception-android-os-servicespecificexception
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return (PrivateKey) keyStore.getKey(PP_KEYSTORE_ALIAS, null);
        } else {
            KeyStore.Entry entry = keyStore.getEntry(PP_KEYSTORE_ALIAS, null);
            return entry instanceof KeyStore.PrivateKeyEntry
                    ? ((KeyStore.PrivateKeyEntry) entry).getPrivateKey()
                    : null;
        }
    }

    /**
     * 加密保存数据.
     *
     * 采用混合加密：AES-256-GCM 加密数据，RSA 加密 AES 密钥。
     * RSA 只需加密 32 字节的 AES 密钥，远低于 RSA-2048/PKCS1 的 245 字节上限，
     * 实际数据由 AES-GCM 加密，无长度限制。
     *
     * 存储格式：Base64( [2B: RSA密文长度 BE] [RSA密文] [1B: IV长度] [IV] [AES-GCM密文] )
     */
    public static void saveData(Context context, String key, String data) throws Exception {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // 1. 生成随机 AES-256 密钥
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(256);
            SecretKey aesKey = kg.generateKey();

            // 2. AES-256-GCM 加密数据
            Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
            aesCipher.init(Cipher.ENCRYPT_MODE, aesKey);
            byte[] iv = aesCipher.getIV();
            byte[] encryptedData = aesCipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

            // 3. RSA 加密 AES 密钥（32 bytes << RSA-2048 的 245 字节上限）
            PublicKey publicKey = getPublicKey();
            Cipher rsaCipher = Cipher.getInstance(PP_RSA_TRANSFORMATION);
            rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] encryptedAesKey = rsaCipher.doFinal(aesKey.getEncoded());

            // 4. 打包写入
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(ByteBuffer.allocate(2).putShort((short) encryptedAesKey.length).array());
            baos.write(encryptedAesKey);
            baos.write((byte) iv.length);
            baos.write(iv);
            baos.write(encryptedData);

            data = Base64.getEncoder().encodeToString(baos.toByteArray());
        }
        SharedPreferences sp = context.getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
        sp.edit().putString(key, data).commit();
    }

    /**
     * 解密读取数据.
     *
     * 兼容两种存储格式：
     *   新格式（混合加密）：Base64( [2B RSA密文长度] [RSA密文] [1B IV长度] [IV] [AES-GCM密文] )
     *   旧格式：Base64( RSA直接加密的原始数据 )
     */
    public static String getData(Context context, String key) throws Exception {
        SharedPreferences sp = context.getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
        String data = sp.getString(key, null);
        if (data == null) {
            return null;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            byte[] raw = Base64.getDecoder().decode(data);
            PrivateKey privateKey = getPrivateKey();
            try {
                // 新格式解析
                ByteBuffer buf = ByteBuffer.wrap(raw);
                int encAesKeyLen = buf.getShort() & 0xFFFF;
                byte[] encryptedAesKey = new byte[encAesKeyLen];
                buf.get(encryptedAesKey);
                int ivLen = buf.get() & 0xFF;
                byte[] iv = new byte[ivLen];
                buf.get(iv);
                byte[] encryptedData = new byte[buf.remaining()];
                buf.get(encryptedData);

                Cipher rsaCipher = Cipher.getInstance(PP_RSA_TRANSFORMATION);
                rsaCipher.init(Cipher.DECRYPT_MODE, privateKey);
                byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKey);

                SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");
                Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
                aesCipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
                data = new String(aesCipher.doFinal(encryptedData), StandardCharsets.UTF_8);
            } catch (Exception e) {
                // 旧格式兼容：RSA 直接加密原始数据
                Cipher cipher = Cipher.getInstance(PP_RSA_TRANSFORMATION);
                cipher.init(Cipher.DECRYPT_MODE, privateKey);
                data = new String(cipher.doFinal(raw), StandardCharsets.UTF_8);
            }
        }
        return data;
    }
}
