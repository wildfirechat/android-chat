# 音视频通话自定义加密。

## 实现自定义加密。
请检查源码webrtcencryptor.cpp中的 Encrypt/GetMaxCiphertextByteSize 和 Decrypt/GetMaxPlaintextByteSize 方法。重新这4个方法来实现自定义加解密。

## 开启加密
1. 在uikit开module中添加对本module的依赖。在uikit的build.gradle中，打开注释添加```implementation project(':webrtcEncryptor')```。
2. 在WfcUIKIt类中，初始化音视频SDK的下面，打开如下注释：
```
            AVEngineKit.init(application, this);
            //下面这这两句是添加加密库.
            CryptNativeLib cryptNativeLib = new CryptNativeLib();
            AVEngineKit.Instance().setCryptor(cryptNativeLib, cryptNativeLib);
```

## WebRTC库
WebRTC是标准WebRTC做了加密的修改，项目中的[libwebrtc.aar](../webrtc/libwebrtc.aar)库的的tag是```branch-heads/6978```，修改的文件在[webrtc_patch](./webrtc_patch)目录下。
