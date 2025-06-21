package com.example.webrtcencryptor;

import org.webrtc.FrameDecryptor;
import org.webrtc.FrameEncryptor;

public class CryptNativeLib implements FrameEncryptor, FrameDecryptor {

    // Used to load the 'webrtcencryptor' library on application startup.
    static {
        System.loadLibrary("webrtcencryptor");
    }

    private native long loadNativeFrameDecryptor();
    private native long loadNativeFrameDecryptSize();
    private native long loadNativeFrameEncryptor();
    private native long loadNativeFrameEncryptSize();

    @Override
    public long getNativeFrameDecryptor() {
        return loadNativeFrameDecryptor();
    }

    @Override
    public long getNativeFrameDecryptSize() {
        return loadNativeFrameDecryptSize();
    }

    @Override
    public long getNativeFrameEncryptor() {
        return loadNativeFrameEncryptor();
    }


    @Override
    public long getNativeFrameEncryptSize() {
        return loadNativeFrameEncryptSize();
    }
}