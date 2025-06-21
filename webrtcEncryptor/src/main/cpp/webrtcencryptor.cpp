#include <jni.h>
#include <string>
#include <type_traits>

#ifdef __cplusplus
extern "C" {
#endif
typedef int (*WFFrameEncryptor_Encrypt)(int media_type, uint32_t ssrc,
                                        const uint8_t *additional_data, size_t additional_data_len,
                                        const uint8_t *frame, size_t frame_len,
                                        uint8_t *encrypted_frame, size_t encrypted_frame_len,
                                        size_t* bytes_written);
typedef size_t (*WFFrameEncryptor_GetMaxCiphertextByteSize)(int media_type, size_t frame_size);

typedef int (*WFFrameDecryptor_Decrypt)(int media_type,
                                        const uint32_t *csrcs, size_t csrcs_len,
                                        const uint8_t *additional_data, size_t additional_data_len,
                                        const uint8_t *encrypted_frame, size_t encrypted_frame_len,
                                        uint8_t *frame, size_t frame_len);
typedef size_t (*WFFrameDecryptor_GetMaxPlaintextByteSize)(int media_type, size_t encrypted_frame_size);
#ifdef __cplusplus
}
#endif


// Attempts to decrypt the encrypted frame. You may assume the frame size will
// be allocated to the size returned from GetMaxPlaintextSize. You may assume
// that the frames are in order if SRTP is enabled. The stream is not provided
// here and it is up to the implementor to transport this information to the
// receiver if they care about it. You must set bytes_written to how many
// bytes you wrote to in the frame buffer. kOk must be returned if successful,
// kRecoverable should be returned if the failure was due to something other
// than a decryption failure. kFailedToDecrypt should be returned in all other
// cases.

//示例加密混淆使用的key
const uint8_t example_key = 0xff;

//视频数据不能全部加密，有视频的头需要保留。这个头的大小没有研究，这里简单设置为240字节，如果需要准确，请仔细查阅资料。
const size_t video_frame_header_size = 240;

//media_type是数据类型，0是语音，1是视频，2是数据。
int Decrypt(int media_type,
            const uint32_t *csrcs, size_t csrcs_len,
            const uint8_t *additional_data, size_t additional_data_len,
            const uint8_t *encrypted_frame, size_t encrypted_frame_len,
            uint8_t *frame, size_t frame_len) {
    // 下面是解密的示例，对数据内容取反
    for (size_t i = 0; i < encrypted_frame_len; i++) {
        if(media_type == 1 && i < video_frame_header_size) {
            frame[i] = encrypted_frame[i];
        } else {
            frame[i] = encrypted_frame[i] ^ example_key;
        }
    }
    return 0;
}

// Returns the total required length in bytes for the output of the
// decryption. This can be larger than the actual number of bytes you need but
// must never be smaller as it informs the size of the frame buffer.
size_t GetMaxPlaintextByteSize(int media_type, size_t encrypted_frame_size) {
    return encrypted_frame_size;
}


// Attempts to encrypt the provided frame. You may assume the encrypted_frame
// will match the size returned by GetMaxCiphertextByteSize for a give frame.
// You may assume that the frames will arrive in order if SRTP is enabled.
// The ssrc will simply identify which stream the frame is travelling on. You
// must set bytes_written to the number of bytes you wrote in the
// encrypted_frame. 0 must be returned if successful all other numbers can be
// selected by the implementer to represent error codes.
int Encrypt(int media_type,
            uint32_t ssrc,
            const uint8_t *additional_data, size_t additional_data_len,
            const uint8_t *frame, size_t frame_len,
            uint8_t *encrypted_frame, size_t encrypted_frame_len,
            size_t *bytes_written) {
    // 下面是加密的示例，对数据内容取反
    for (size_t i = 0; i < encrypted_frame_len; i++) {
        if(media_type == 1 && i < video_frame_header_size) {
            encrypted_frame[i] = frame[i];
        } else {
            encrypted_frame[i] = frame[i] ^ example_key;
        }
    }
    *bytes_written = encrypted_frame_len;
    return 0;
}

// Returns the total required length in bytes for the output of the
// encryption. This can be larger than the actual number of bytes you need but
// must never be smaller as it informs the size of the encrypted_frame buffer.
size_t GetMaxCiphertextByteSize(int media_type, size_t frame_size) {
    return frame_size;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_webrtcencryptor_CryptNativeLib_loadNativeFrameDecryptor(JNIEnv *env, jobject thiz) {
    // TODO: implement loadNativeFrameDecryptor()
    return (long)Decrypt;
}
extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_webrtcencryptor_CryptNativeLib_loadNativeFrameDecryptSize(JNIEnv *env, jobject thiz) {
    return (long)GetMaxPlaintextByteSize;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_webrtcencryptor_CryptNativeLib_loadNativeFrameEncryptor(JNIEnv *env, jobject thiz) {
    return (long)Encrypt;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_example_webrtcencryptor_CryptNativeLib_loadNativeFrameEncryptSize(JNIEnv *env, jobject thiz) {
    return (long)GetMaxCiphertextByteSize;
}