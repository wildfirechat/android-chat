/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan.api;

import java.util.List;

import cn.wildfire.chat.kit.pan.model.CreateFileRequest;
import cn.wildfire.chat.kit.pan.model.PanFile;
import cn.wildfire.chat.kit.pan.model.PanSpace;
import cn.wildfire.chat.kit.pan.model.Result;

/**
 * 网盘服务接口
 */
public interface PanService {
    
    /**
     * 回调接口
     */
    interface Callback<T> {
        void onSuccess(T data);
        void onError(int errorCode, String message);
    }
    
    /**
     * 简单回调
     */
    interface SimpleCallback {
        void onSuccess();
        void onError(int errorCode, String message);
    }
    
    /**
     * 获取空间列表
     * GET /api/v1/spaces
     */
    void getSpaces(Callback<List<PanSpace>> callback);
    
    /**
     * 获取指定用户的公共空间
     * POST /api/v1/spaces/user/public
     */
    void getUserPublicSpace(String userId, Callback<PanSpace> callback);
    
    /**
     * 获取空间内文件列表
     * GET /api/v1/spaces/{spaceId}/files?parentId={parentId}
     */
    void getFiles(Long spaceId, Long parentId, Callback<List<PanFile>> callback);
    
    /**
     * 创建文件夹
     * POST /api/v1/files/folder
     */
    void createFolder(Long spaceId, Long parentId, String name, Callback<PanFile> callback);
    
    /**
     * 创建文件记录
     * POST /api/v1/files
     */
    void createFile(CreateFileRequest request, Callback<PanFile> callback);
    
    /**
     * 删除文件/文件夹
     * POST /api/v1/files/{id}/delete
     */
    void deleteFile(Long fileId, SimpleCallback callback);
    
    /**
     * 移动文件/文件夹
     * POST /api/v1/files/{id}/move
     */
    void moveFile(Long fileId, Long targetSpaceId, Long targetParentId, Callback<PanFile> callback);
    
    /**
     * 复制文件/文件夹
     * POST /api/v1/files/{id}/copy
     */
    void copyFile(Long fileId, Long targetSpaceId, Long targetParentId, boolean copy, Callback<PanFile> callback);
    
    /**
     * 重命名文件/文件夹
     * POST /api/v1/files/{id}/rename
     */
    void renameFile(Long fileId, String newName, Callback<PanFile> callback);
    
    /**
     * 获取文件下载URL
     * POST /api/v1/files/url
     */
    void getFileUrl(Long fileId, Callback<String> callback);
    
    /**
     * 检查空间写入权限
     * POST /api/v1/files/check-permission
     */
    void checkSpaceWritePermission(Long spaceId, Callback<Boolean> callback);
    
    /**
     * 检查上传权限（与 checkSpaceWritePermission 相同）
     */
    void checkUploadPermission(Long spaceId, Callback<Boolean> callback);
    
    /**
     * 转存文件（复制到自己的空间）
     * POST /api/v1/files/duplicate
     */
    void duplicateFile(Long fileId, Long targetSpaceId, Long targetParentId, SimpleCallback callback);
}
