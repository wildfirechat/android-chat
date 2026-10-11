/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.http.SslCertificate;
import android.net.http.SslError;
import android.net.http.X509TrustManagerExtensions;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.internal.tls.OkHostnameVerifier;

/**
 * 自签名证书工具类。
 * <p>
 * 证书统一放在 client/src/main/assets/certs 目录，协议栈（native 实现）通过 {@code ProtoLogic.useTls} 使用，
 * Java 层通过这里使用，两边共用同一份证书。Java 层分三种接入方式：
 * <ul>
 * <li>OkHttp：{@link #trustSelfSignedCerts(Context, OkHttpClient.Builder)}，SDK 和 demo 里的 OkHttpClient
 * （上传、应用服务、下载、投票、收藏、归档、语音识别等）都已经接入，新增 OkHttpClient 时也要记得调用。</li>
 * <li>HttpURLConnection：{@link #trustSelfSignedCerts(Context, HttpURLConnection)}，如 Glide 加载图片。</li>
 * <li>WebView：在 WebViewClient#onReceivedSslError 里调用 {@link #isTrustedBySelfSignedCerts(Context, SslError)}。</li>
 * </ul>
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
 * demo app（chat 模块）另外配了 res/xml/network_security_config.xml，兜底视频在线播放（MediaPlayer）等没法单独配置
 * 证书的组件，但它只对列出的地址生效，也<b>不会</b>放松域名校验。以 aar 方式集成 SDK 时，自己的 OkHttpClient
 * 也用这里的方法挂上自签证书即可。
 */
public class SelfSignedCertUtils {
    private static final String TAG = "SelfSignedCertUtils";

    /**
     * 自签名证书目录，协议栈（ProtoLogic.useTls）和 Java 层共用，对应 client/src/main/assets/certs
     */
    public static final String CERT_DIR_NAME = "certs";

    // 证书目录 -> TLS 配置。assets 运行期间不会变，每个目录只构建一次，Glide 等每个请求都要用，不能每次都去读证书；
    // 没有证书的目录对应 null
    private static final Map<String, SslConfig> sslConfigs = new HashMap<>();

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
        SslConfig sslConfig = getSslConfig(context, certDirName);
        if (sslConfig == null) {
            return;
        }
        builder.sslSocketFactory(sslConfig.sslSocketFactory, sslConfig.trustManager);
        builder.hostnameVerifier(sslConfig.hostnameVerifier);
    }

    /**
     * {@link #trustSelfSignedCerts(Context, OkHttpClient.Builder)} 的 HttpURLConnection 版本，要在连接之前调用。
     * 不是 https 连接、或者没有证书时不做任何修改
     */
    public static void trustSelfSignedCerts(Context context, HttpURLConnection connection) {
        if (!(connection instanceof HttpsURLConnection)) {
            return;
        }
        SslConfig sslConfig = getSslConfig(context, CERT_DIR_NAME);
        if (sslConfig == null) {
            return;
        }
        HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
        httpsConnection.setSSLSocketFactory(sslConfig.sslSocketFactory);
        httpsConnection.setHostnameVerifier(sslConfig.hostnameVerifier);
    }

    /**
     * 给 WebViewClient#onReceivedSslError 用：出错的证书是 assets 证书目录里的自签名证书签发的，返回 true，这时可以
     * handler.proceed()，否则应该按默认逻辑 cancel。和 OkHttp 一样，不校验域名。
     * <p>
     * WebView 只给出服务端证书本身，不带中间证书，所以只支持服务端证书就是 assets 里的证书、
     * 或者由 assets 里的证书直接签发的情况。
     */
    public static boolean isTrustedBySelfSignedCerts(Context context, SslError error) {
        if (error == null) {
            return false;
        }
        SslConfig sslConfig = getSslConfig(context, CERT_DIR_NAME);
        if (sslConfig == null) {
            return false;
        }
        X509Certificate cert = toX509Certificate(error.getCertificate());
        return cert != null && sslConfig.trustManager.isTrustedBySelfSignedCerts(new X509Certificate[]{cert});
    }

    private static X509Certificate toX509Certificate(SslCertificate sslCertificate) {
        if (sslCertificate == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return sslCertificate.getX509Certificate();
        }
        // Android 10 以下没有 getX509Certificate，证书的 DER 编码保存在 saveState 返回的 Bundle 里
        Bundle bundle = SslCertificate.saveState(sslCertificate);
        byte[] der = bundle == null ? null : bundle.getByteArray("x509-certificate");
        if (der == null) {
            return null;
        }
        try {
            return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(der));
        } catch (CertificateException e) {
            Log.e(TAG, "parse ssl certificate fail", e);
            return null;
        }
    }

    private static synchronized SslConfig getSslConfig(Context context, String certDirName) {
        if (sslConfigs.containsKey(certDirName)) {
            return sslConfigs.get(certDirName);
        }
        SslConfig sslConfig = null;
        SelfSignedTrustManager trustManager = buildTrustManager(context, certDirName);
        if (trustManager != null) {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{trustManager}, null);
                sslConfig = new SslConfig(trustManager, sslContext.getSocketFactory(), buildHostnameVerifier(trustManager));
            } catch (GeneralSecurityException e) {
                Log.e(TAG, "init ssl context fail", e);
            }
        }
        sslConfigs.put(certDirName, sslConfig);
        return sslConfig;
    }

    private static class SslConfig {
        final SelfSignedTrustManager trustManager;
        final SSLSocketFactory sslSocketFactory;
        final HostnameVerifier hostnameVerifier;

        SslConfig(SelfSignedTrustManager trustManager, SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier) {
            this.trustManager = trustManager;
            this.sslSocketFactory = sslSocketFactory;
            this.hostnameVerifier = hostnameVerifier;
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
        // 系统 TrustManager 带域名的校验接口，见 checkServerTrusted(X509Certificate[], String, String)
        private final X509TrustManagerExtensions systemTrustManagerExtensions;

        SelfSignedTrustManager(X509TrustManager systemTrustManager, X509TrustManager selfSignedTrustManager) {
            this.systemTrustManager = systemTrustManager;
            this.selfSignedTrustManager = selfSignedTrustManager;
            X509TrustManagerExtensions extensions = null;
            try {
                extensions = new X509TrustManagerExtensions(systemTrustManager);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "system trust manager not support hostname aware check", e);
            }
            this.systemTrustManagerExtensions = extensions;
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

        /**
         * 带域名的证书链校验。TLS 握手时 Conscrypt 会通过反射优先调用这个方法，而不是上面不带域名的版本，
         * X509TrustManagerExtensions（OkHttp 清理证书链时用）也一样。
         * <p>
         * 一定要有这个方法：App 的 network security config 里有 domain-config 时（demo 就有），
         * 系统 TrustManager 不带域名的 checkServerTrusted 会直接抛异常，只靠上面那个方法的话，
         * 系统 CA 签发的正规证书也会校验失败。
         */
        public List<X509Certificate> checkServerTrusted(X509Certificate[] chain, String authType, String host) throws CertificateException {
            try {
                if (systemTrustManagerExtensions != null) {
                    return systemTrustManagerExtensions.checkServerTrusted(chain, authType, host);
                }
                systemTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                selfSignedTrustManager.checkServerTrusted(chain, authType);
            }
            return Arrays.asList(chain);
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
