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
    // KeyStore type
    private static final String PP_KEYSTORE_TYPE = "AndroidKeyStore";
    // KeyStore alias
    private static final String PP_KEYSTORE_ALIAS = "pp_keystore_alias";
    // Encryption algorithm standard name
    private static final String PP_TRANSFORMATION = "RSA/ECB/PKCS1Padding";

    /**
     * Trigger key pair generation.
     * <p>
     * Generate RSA key pair, including public key and private key
     *
     * @return KeyPair key pair, containing public key and private key
     */
    private static KeyPair generateKey() throws Exception {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // Create key pair generator
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, PP_KEYSTORE_TYPE);
            // Configure key pair generator parameters
            KeyGenParameterSpec builder = new KeyGenParameterSpec.Builder(PP_KEYSTORE_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .build();

            keyPairGenerator.initialize(builder);
            // Generate key pair
            return keyPairGenerator.generateKeyPair();
        } else {
            return null;
        }
    }

    /**
     * Get public key.
     *
     * @return public key
     */
    private static PublicKey getPublicKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(PP_KEYSTORE_TYPE);
        keyStore.load(null);
        // Check if key exists
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
     * Get private key.
     *
     * @return private key
     */
    private static PrivateKey getPrivateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(PP_KEYSTORE_TYPE);
        keyStore.load(null);
        // Check if key exists
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
     * Encrypt and save data
     *
     * @param context context
     * @param key     data key
     * @param data    data
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
     * Get confidential data
     *
     * @param context context
     * @param key     data key
     * @return decrypted data
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
