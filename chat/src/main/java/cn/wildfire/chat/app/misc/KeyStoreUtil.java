package cn.wildfire.chat.app.misc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import javax.crypto.Cipher;

import cn.wildfire.chat.kit.Config;

public class KeyStoreUtil {
    // 密钥库类型
    private static final String PP_KEYSTORE_TYPE = "AndroidKeyStore";
    // 密钥库别名
    private static final String PP_KEYSTORE_ALIAS = "pp_keystore_alias";
    // 加密算法标准算法名称
    private static final String PP_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    /**
     * 触发生成密钥对.
     * <p>
     * 生成RSA 密钥对，包括公钥和私钥
     *
     * @return KeyPair 密钥对，包含公钥和私钥
     */
    private static KeyPair generateKey() throws Exception {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // 创建密钥生成器
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, PP_KEYSTORE_TYPE);
            // 配置密钥生成器参数
            KeyGenParameterSpec builder = new KeyGenParameterSpec.Builder(PP_KEYSTORE_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build();

            keyPairGenerator.initialize(builder);
            // 生成密钥对
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
        // 判断密钥是否存在
        if (!keyStore.containsAlias(PP_KEYSTORE_ALIAS)) {
            return generateKey().getPublic();
        }

        // FYI https://stackoverflow.com/questions/52024752/android-9-keystore-exception-android-os-servicespecificexception
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return keyStore.getCertificate(PP_KEYSTORE_ALIAS).getPublicKey();
        } else {
            KeyStore.Entry kentry = keyStore.getEntry(PP_KEYSTORE_ALIAS, null);
            return kentry instanceof KeyStore.PrivateKeyEntry
                ? ((KeyStore.PrivateKeyEntry) kentry).getCertificate().getPublicKey()
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
        // 判断密钥是否存在
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
     * 加密保存数据
     *
     * @param context 上下文
     * @param key     数据的Key
     * @param data    数据
     */
    public static void saveData(Context context, String key, String data) throws Exception {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            PublicKey publicKey = getPublicKey();
            Cipher cipher = Cipher.getInstance(PP_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            bytes = cipher.doFinal(bytes);
            data = Base64.getEncoder().encodeToString(bytes);
        }
        SharedPreferences sharedPreferences = context.getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(key, data).commit();
    }

    /**
     * 获取保密数据
     *
     * @param context 上下文
     * @param key     数据的Key
     * @return 解密后的数据
     */
    public static String getData(Context context, String key) throws Exception {
        SharedPreferences sharedPreferences = context.getSharedPreferences(Config.SP_CONFIG_FILE_NAME, Context.MODE_PRIVATE);
        String data = sharedPreferences.getString(key, null);
        if (data == null) {
            return null;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PrivateKey privateKey = getPrivateKey();
            Cipher cipher = Cipher.getInstance(PP_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] bytes = cipher.doFinal(Base64.getDecoder().decode(data));
            data = new String(bytes, StandardCharsets.UTF_8);
        }
        return data;
    }
}
