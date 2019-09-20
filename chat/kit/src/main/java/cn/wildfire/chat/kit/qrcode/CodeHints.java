package cn.wildfire.chat.kit.qrcode;

import android.text.TextUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class CodeHints {
    private static Map<DecodeHintType, Object> DECODE_HINTS = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
    private static Map<EncodeHintType, Object> ENCODE_HINTS = new EnumMap<>(EncodeHintType.class);

    static {
        List<BarcodeFormat> formats = new ArrayList<BarcodeFormat>();
        formats.add(BarcodeFormat.QR_CODE);
        DECODE_HINTS.put(DecodeHintType.POSSIBLE_FORMATS, formats);
//      DECODE_HINTS.put(DecodeHintType.CHARACTER_SET, "UTF-8");

        ENCODE_HINTS.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
//      ENCODE_HINTS.put(EncodeHintType.CHARACTER_SET, "UTF-8");
    }

    /**
     * 获取默认解析QR参数
     *
     * @return
     */
    public static Map<DecodeHintType, Object> getDefaultDecodeHints() {
        return DECODE_HINTS;
    }

    /**
     * 获取自定义解析QR参数
     *
     * @param characterSet 编码方式
     * @return
     */
    public static Map<DecodeHintType, Object> getCustomDecodeHints(String characterSet) {
        Map<DecodeHintType, Object> decodeHints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        List<BarcodeFormat> formats = new ArrayList<BarcodeFormat>();
        formats.add(BarcodeFormat.QR_CODE);
        // 设置解码格式
        decodeHints.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        // 设置编码方式
        if (TextUtils.isEmpty(characterSet)) {
            characterSet = "UTF-8";
        }
        decodeHints.put(DecodeHintType.CHARACTER_SET, characterSet);
        return decodeHints;
    }

    /**
     * 获取默认生成QR参数
     *
     * @return
     */
    public static Map<EncodeHintType, Object> getDefaultEncodeHints() {
        return ENCODE_HINTS;
    }

    /**
     * 获取自定义生成QR参数
     *
     * @param level        容错率 L,M,Q,H
     * @param version      版本号 1-40
     * @param characterSet 编码方式
     * @return
     */
    public static Map<EncodeHintType, Object> getCustomEncodeHints(ErrorCorrectionLevel level, Integer version,
                                                                   String characterSet) {
        Map<EncodeHintType, Object> encodeHints = new EnumMap<>(EncodeHintType.class);
        // 设置容错率
        if (level != null) {
            encodeHints.put(EncodeHintType.ERROR_CORRECTION, level);
        }
        // 设置版本号
        if (version >= 1 && version <= 40) {
            encodeHints.put(EncodeHintType.QR_VERSION, version);
        }
        // 设置编码方式
        if (!TextUtils.isEmpty(characterSet)) {
//          characterSet = "UTF-8";
            encodeHints.put(EncodeHintType.CHARACTER_SET, characterSet);
        }
        return encodeHints;
    }

}

