/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.pan;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.File;
import java.util.List;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.pan.model.PanFile;
import cn.wildfire.chat.kit.pan.model.PanSpace;
import cn.wildfire.chat.kit.utils.DownloadManager;
import cn.wildfire.chat.kit.utils.FileUtils;

/**
 * 「存到网盘」：把一个已上传到 IM 媒体桶的文件（storageUrl）注册到「我的个人空间」根目录，
 * {@code copy = true} 让服务端把物理对象拷进网盘桶。也负责保存后的打开（在线文档 / 系统应用）。
 */
public class PanSaveHelper {

    public static void saveUrlToMyPan(PanService service, String name, long size, String storageUrl,
                                      String mimeType, String md5,
                                      final PanService.ResultCallback<PanFile> callback) {
        if (service == null || TextUtils.isEmpty(storageUrl)) {
            callback.onError(-1, "参数错误");
            return;
        }
        service.getMySpaces(new PanService.ResultCallback<List<PanSpace>>() {
            @Override
            public void onSuccess(List<PanSpace> spaces) {
                PanSpace target = null;
                if (spaces != null) {
                    for (PanSpace space : spaces) {
                        if (space.isUserPrivate()) {
                            target = space;
                            break;
                        }
                    }
                    if (target == null && !spaces.isEmpty()) {
                        target = spaces.get(0);
                    }
                }
                if (target == null) {
                    callback.onError(-1, "没有可用的网盘空间");
                    return;
                }
                final PanSpace space = target;
                service.getSpaceFiles(space.spaceId, 0, new PanService.ResultCallback<List<PanFile>>() {
                    @Override
                    public void onSuccess(List<PanFile> files) {
                        String uniqueName = uniqueName(files, name);
                        service.createFile(space.spaceId, 0, uniqueName, size, mimeType, md5, storageUrl, true, callback);
                    }

                    @Override
                    public void onError(int code, String message) {
                        // 列目录失败也尝试直接创建
                        service.createFile(space.spaceId, 0, name, size, mimeType, md5, storageUrl, true, callback);
                    }
                });
            }

            @Override
            public void onError(int code, String message) {
                callback.onError(code, message);
            }
        });
    }

    /**
     * 存到网盘并（可选）打开。
     */
    public static void saveFileMessageToMyPan(Context context, String name, long size, String storageUrl,
                                              String mimeType, String md5, boolean openAfterSave) {
        PanService service = PanServiceProvider.getInstance().getService();
        if (service == null) {
            return;
        }
        saveUrlToMyPan(service, name, size, storageUrl, mimeType, md5, new PanService.ResultCallback<PanFile>() {
            @Override
            public void onSuccess(PanFile file) {
                Toast.makeText(context, R.string.pan_save_success, Toast.LENGTH_SHORT).show();
                if (openAfterSave) {
                    openPanFile(context, service, file);
                }
            }

            @Override
            public void onError(int code, String message) {
                Toast.makeText(context, R.string.pan_save_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 打开网盘文件：在线文档走内置网页，其余取签名下载地址后交系统应用。
     */
    public static void openPanFile(Context context, PanService service, PanFile file) {
        String docUrl = PanService.docOpenUrl(file.fileId);
        if (docUrl != null && PanService.isOnlineDocName(file.name)) {
            WfcWebViewActivity.loadUrl(context, file.name, docUrl);
            return;
        }
        service.getFileDownloadUrl(file.fileId, 0, new PanService.ResultCallback<cn.wildfire.chat.kit.pan.model.PanFileUrl>() {
            @Override
            public void onSuccess(cn.wildfire.chat.kit.pan.model.PanFileUrl url) {
                if (url == null || TextUtils.isEmpty(url.storageUrl)) {
                    Toast.makeText(context, R.string.pan_download_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
                File target = new File(Config.FILE_SAVE_DIR, file.name);
                DownloadManager.download(url.storageUrl, target.getParent(), target.getName(),
                    new DownloadManager.SimpleOnDownloadListener() {
                        @Override
                        public void onUiSuccess(File downloaded) {
                            try {
                                context.startActivity(FileUtils.getViewIntent(context, downloaded));
                            } catch (ActivityNotFoundException e) {
                                Toast.makeText(context, R.string.pan_no_app_to_open, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onUiFail() {
                            Toast.makeText(context, R.string.pan_download_failed, Toast.LENGTH_SHORT).show();
                        }
                    });
            }

            @Override
            public void onError(int code, String message) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String uniqueName(List<PanFile> files, String originalName) {
        if (files == null || files.isEmpty() || TextUtils.isEmpty(originalName)) {
            return originalName;
        }
        java.util.Set<String> names = new java.util.HashSet<>();
        for (PanFile file : files) {
            names.add(file.name);
        }
        if (!names.contains(originalName)) {
            return originalName;
        }
        int extIndex = originalName.lastIndexOf('.');
        String base = extIndex > 0 ? originalName.substring(0, extIndex) : originalName;
        String ext = extIndex > 0 ? originalName.substring(extIndex) : "";
        int index = 1;
        String candidate;
        do {
            candidate = base + "(" + index + ")" + ext;
            index++;
        } while (names.contains(candidate));
        return candidate;
    }
}
