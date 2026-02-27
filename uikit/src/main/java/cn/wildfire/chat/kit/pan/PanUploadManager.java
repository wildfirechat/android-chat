/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.MessageContentMediaType;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback;
import cn.wildfirechat.remote.UploadMediaCallback;

/**
 * 网盘文件上传管理器
 * 参考 iOS 的 WFCUPanUploadManager
 */
public class PanUploadManager {
    
    private static PanUploadManager instance;
    
    public static synchronized PanUploadManager getInstance() {
        if (instance == null) {
            instance = new PanUploadManager();
        }
        return instance;
    }
    
    private PanUploadManager() {
    }
    
    /**
     * 上传文件
     * 
     * @param filePath 文件路径
     * @param progressCallback 进度回调 (0-100)
     * @param successCallback 成功回调 (remoteUrl, size, md5)
     * @param errorCallback 失败回调 (errorMessage)
     */
    public void uploadFile(String filePath, 
                          ProgressCallback progressCallback,
                          SuccessCallback successCallback,
                          ErrorCallback errorCallback) {
        
        File file = new File(filePath);
        if (!file.exists()) {
            if (errorCallback != null) {
                errorCallback.onError("文件不存在");
            }
            return;
        }
        
        final long fileSize = file.length();
        final String fileName = file.getName();
        
        // 计算 MD5
        final String md5 = calculateMD5(file);
        
        // 使用 WildFireChat SDK 上传文件
        // Media_Type_CUSTOM1 用于通用文件上传
        ChatManager.Instance().uploadMediaFile(filePath, MessageContentMediaType.PAN.getValue(),
            new UploadMediaCallback() {
                @Override
                public void onSuccess(String remoteUrl) {
                    if (successCallback != null) {
                        successCallback.onSuccess(remoteUrl, fileSize, md5);
                    }
                }
                
                @Override
                public void onProgress(long uploaded, long total) {
                    if (progressCallback != null && total > 0) {
                        int progress = (int) ((uploaded * 100) / total);
                        progressCallback.onProgress(progress);
                    }
                }
                
                @Override
                public void onFail(int errorCode) {
                    if (errorCallback != null) {
                        errorCallback.onError("上传失败(错误码:" + errorCode + ")");
                    }
                }
            });
    }
    
    /**
     * 计算文件 MD5
     */
    public String calculateMD5(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            fis.close();
            
            BigInteger md5BigInt = new BigInteger(1, digest.digest());
            return md5BigInt.toString(16);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 根据文件路径获取 MIME 类型
     */
    public String getMimeTypeForFile(String filePath) {
        if (filePath == null) {
            return "application/octet-stream";
        }
        
        String extension = "";
        int lastDot = filePath.lastIndexOf('.');
        if (lastDot > 0) {
            extension = filePath.substring(lastDot + 1);
        }
        
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        if (mimeType == null) {
            mimeType = "application/octet-stream";
        }
        
        return mimeType;
    }
    
    /**
     * 根据文件名推断 MIME 类型（与 iOS 兼容）
     */
    public String getMimeTypeFromFileName(String fileName) {
        return getMimeTypeForFile(fileName);
    }
    
    // ============ 回调接口 ============
    
    public interface ProgressCallback {
        void onProgress(int progress);
    }
    
    public interface SuccessCallback {
        void onSuccess(String remoteUrl, long size, String md5);
    }
    
    public interface ErrorCallback {
        void onError(String errorMessage);
    }
}
