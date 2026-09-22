/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.internal.tls.OkHostnameVerifier;

/**
 * 自签名证书工具类。
 * <p>
 * 证书统一放在 client/src/main/assets/certs 目录，协议栈（native 实现）通过 {@code ProtoLogic.useTls} 使用，
 * Java 层（OkHttp）通过这里使用，两边共用同一份证书。
 * <p>
 * 这里对自签名证书做了两件事，都严格限定在「证书链能追溯到 assets/certs 里的证书」这个前提下，其他证书的行为不受影响：
 * <ol>
 * <li>证书链校验：系统 CA 验不过时，再用 assets/certs 里的证书验一次。</li>
 * <li>域名校验：链到自签名证书时跳过域名校验，避免自签证书没写 subjectAltName 时报
 * No subject alternative names present / Hostname not verified。系统 CA 签发的证书仍然严格校验域名。</li>
 * </ol>
 * <b>注意：</b>跳过域名校验意味着，拿到这张自签 CA 私钥的人，可以对这个 OkHttpClient 访问的任意地址做中间人。
 * 只在自建服务、证书自己签发的场景下用。更规范的做法是签发证书时把域名和 IP 都写进 subjectAltName，
 * 这样这里的放行逻辑就不会触发。无论如何都不要换成恒返回 true 的 HostnameVerifier 或者什么都信任的 TrustManager，
 * 那等于关掉了 TLS 的身份认证。
 * <p>
 * demo app（chat 模块）另外配了 res/xml/network_security_config.xml，让整个 App 的 Java 网络库（OkHttp、WebView、
 * Glide 等）都信任这些证书，但 network security config <b>不会</b>放松域名校验。以 aar 方式集成 SDK、没有配置
 * network security config 时，可以用这里的方法给自己的 OkHttpClient 挂上自签证书。
 */
public class SelfSignedCertUtils {
    private static final String TAG = "SelfSignedCertUtils";

    /**
     * 自签名证书目录，协议栈（ProtoLogic.useTls）和 Java 层共用，对应 client/src/main/assets/certs
     */
    public static final String CERT_DIR_NAME = "certs";

    private SelfSignedCertUtils() {
    }

    /**
     * 同 {@link #trustSelfSignedCerts(Context, OkHttpClient.Builder, String)}，使用默认的 {@link #CERT_DIR_NAME} 目录
     */
    public static void trustSelfSignedCerts(Context context, OkHttpClient.Builder builder) {
        trustSelfSignedCerts(context, builder, CERT_DIR_NAME);
    }

    /**
     * 让 builder 构建出来的 OkHttpClient 在系统 CA 之外，额外信任 assets/certDirName 下的自签名证书，
     * 并对这些自签名证书跳过域名校验。没有证书时不做任何修改，使用系统默认配置。
     */
    public static void trustSelfSignedCerts(Context context, OkHttpClient.Builder builder, String certDirName) {
        SelfSignedTrustManager trustManager = buildTrustManager(context, certDirName);
        if (trustManager == null) {
            return;
        }
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{trustManager}, null);
            builder.sslSocketFactory(sslContext.getSocketFactory(), trustManager);
            builder.hostnameVerifier(buildHostnameVerifier(trustManager));
        } catch (GeneralSecurityException e) {
            Log.e(TAG, "init ssl context fail", e);
        }
    }

    /**
     * 服务端用的是 assets 证书目录里的自签名证书时，直接放过，不做域名校验；其他证书仍然走 OkHttp 默认的严格域名校验
     */
    public static HostnameVerifier buildHostnameVerifier(SelfSignedTrustManager trustManager) {
        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                X509Certificate[] chain = peerCertificateChain(session);
                if (chain != null && trustManager.isTrustedBySelfSignedCerts(chain)) {
                    // 自签名证书的 subjectAltName 里常常没写域名或 IP，直接放过，
                    // 否则会报 No subject alternative names present / Hostname not verified
                    Log.w(TAG, "skip hostname verify for self signed cert, host: " + hostname);
                    return true;
                }
                // 系统 CA 签发的证书，域名校验一点都不能少
                return OkHostnameVerifier.INSTANCE.verify(hostname, session);
            }
        };
    }

    private static X509Certificate[] peerCertificateChain(SSLSession session) {
        try {
            Certificate[] peerCertificates = session.getPeerCertificates();
            if (peerCertificates == null || peerCertificates.length == 0) {
                return null;
            }
            X509Certificate[] chain = new X509Certificate[peerCertificates.length];
            for (int i = 0; i < peerCertificates.length; i++) {
                if (!(peerCertificates[i] instanceof X509Certificate)) {
                    return null;
                }
                chain[i] = (X509Certificate) peerCertificates[i];
            }
            return chain;
        } catch (SSLPeerUnverifiedException e) {
            return null;
        }
    }

    /**
     * 在系统 CA 之外，额外信任 assets/certDirName 下的证书（如自签名证书）；没有证书时返回 null，使用系统默认。
     */
    public static SelfSignedTrustManager buildTrustManager(Context context, String certDirName) {
        try {
            AssetManager assetManager = context.getAssets();
            String[] certNames = assetManager.list(certDirName);
            if (certNames == null || certNames.length == 0) {
                return null;
            }

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            int certCount = 0;
            for (String certName : certNames) {
                try (InputStream is = assetManager.open(certDirName + "/" + certName)) {
                    for (Certificate cert : certificateFactory.generateCertificates(is)) {
                        keyStore.setCertificateEntry(certName + "-" + certCount++, cert);
                    }
                } catch (IOException | CertificateException e) {
                    Log.e(TAG, "load cert fail: " + certName, e);
                }
            }
            if (certCount == 0) {
                return null;
            }

            return new SelfSignedTrustManager(getX509TrustManager(null), getX509TrustManager(keyStore));
        } catch (IOException | GeneralSecurityException e) {
            Log.e(TAG, "build trust manager fail", e);
            return null;
        }
    }

    // keyStore 为 null 时返回系统默认的 TrustManager
    private static X509TrustManager getX509TrustManager(KeyStore keyStore) throws GeneralSecurityException {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore);
        for (TrustManager trustManager : factory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }
        throw new GeneralSecurityException("no X509TrustManager");
    }

    /**
     * 先用系统 CA 校验证书链，失败后再用自签名证书校验
     */
    public static class SelfSignedTrustManager implements X509TrustManager {
        private final X509TrustManager systemTrustManager;
        private final X509TrustManager selfSignedTrustManager;

        SelfSignedTrustManager(X509TrustManager systemTrustManager, X509TrustManager selfSignedTrustManager) {
            this.systemTrustManager = systemTrustManager;
            this.selfSignedTrustManager = selfSignedTrustManager;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            systemTrustManager.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            try {
                systemTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                selfSignedTrustManager.checkServerTrusted(chain, authType);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            X509Certificate[] systemIssuers = systemTrustManager.getAcceptedIssuers();
            X509Certificate[] certIssuers = selfSignedTrustManager.getAcceptedIssuers();
            X509Certificate[] issuers = Arrays.copyOf(systemIssuers, systemIssuers.length + certIssuers.length);
            System.arraycopy(certIssuers, 0, issuers, systemIssuers.length, certIssuers.length);
            return issuers;
        }

        /**
         * 证书链是不是 assets 证书目录里的自签名证书签发的。这里只校验证书链，不校验域名
         */
        public boolean isTrustedBySelfSignedCerts(X509Certificate[] chain) {
            if (chain == null || chain.length == 0) {
                return false;
            }
            try {
                selfSignedTrustManager.checkServerTrusted(chain, authType(chain[0]));
                return true;
            } catch (CertificateException e) {
                return false;
            }
        }

        // TrustManager 只要求 authType 非空，用证书公钥的算法即可
        private static String authType(X509Certificate cert) {
            String algorithm = cert.getPublicKey().getAlgorithm();
            return algorithm == null || algorithm.isEmpty() ? "RSA" : algorithm;
        }
    }
}
