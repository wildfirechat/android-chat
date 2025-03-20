/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.utils;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import cn.wildfire.chat.kit.Config;
import cn.wildfirechat.message.FileMessageContent;
import cn.wildfirechat.message.MediaMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.FileRecord;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallbackBytes;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by heavyrainlee on 20/02/2018.
 */

public class DownloadManager {

    private static final OkHttpClient okHttpClient = new OkHttpClient();

    public static void download(final String url, final String saveDir, final OnDownloadListener listener) {
        download(url, saveDir, null, listener);
    }

    public static void download(final String url, final String saveDir, String name, final OnDownloadListener listener) {
        Request request = new Request.Builder().url(url).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // 下载失败
                listener.onFail();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                URL _url = call.request().url().url();
                String query = _url.getQuery();
                String target = getQueryMap(query).get("target");
                boolean isSecret = Boolean.parseBoolean(getQueryMap(query).get("secret"));

                InputStream is = null;
                byte[] buf = new byte[2048];
                int len = 0;
                FileOutputStream fos = null;
                // 储存下载文件的目录
                String savePath = isExistDir(saveDir);
                String fileName = TextUtils.isEmpty(name) ? getNameFromUrl(url) : name;
                try {
                    is = response.body().byteStream();

                    long total = response.body().contentLength();
                    File file = new File(savePath, fileName);
                    fos = new FileOutputStream(file);
                    ByteArrayOutputStream bos = null;
                    if (isSecret) {
                        bos = new ByteArrayOutputStream();
                    }
                    long sum = 0;
                    while ((len = is.read(buf)) != -1) {
                        if (bos != null) {
                            bos.write(buf, 0, len);
                        } else {
                            fos.write(buf, 0, len);
                        }

                        sum += len;
                        int progress = (int) (sum * 1.0f / total * 100);
                        // 下载中
                        listener.onProgress(progress);
                    }
                    if (isSecret) {
                        CountDownLatch latch = new CountDownLatch(1);
                        FileOutputStream finalFos = fos;
                        ChatManager.Instance().decodeSecretDataAsync(target, bos.toByteArray(), new GeneralCallbackBytes() {
                            @Override
                            public void onSuccess(byte[] data) {
                                try {
                                    finalFos.write(data);
                                    finalFos.flush();
                                    listener.onSuccess(file);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                latch.countDown();
                            }

                            @Override
                            public void onFail(int errorCode) {
                                File file = new File(savePath, fileName);
                                if (file.exists()) {
                                    file.delete();
                                }
                                listener.onFail();
                                latch.countDown();
                            }
                        });
                        latch.await();
                    } else {
                        fos.flush();
                        // 下载完成
                        listener.onSuccess(file);
                    }
                } catch (Exception e) {
                    File file = new File(savePath, fileName);
                    if (file.exists()) {
                        file.delete();
                    }
                    listener.onFail();
                } finally {
                    try {
                        if (is != null) {
                            is.close();
                        }
                    } catch (IOException e) {
                    }
                    try {
                        if (fos != null) {
                            fos.close();
                        }
                    } catch (IOException e) {
                    }
                }
            }
        });
    }

    /**
     * 密聊时，媒体文件都是经过加密的，这个函数构造一个带特定参数的url，然后通过 {@link #download(String, String, String, OnDownloadListener)
     * 或者{@link #download(String, String, OnDownloadListener)}} 下载时，回调返回的文件是解密之后的
     *
     * @param mediaMessage
     * @return
     */
    public static String buildSecretChatMediaUrl(Message mediaMessage) {
        if (!(mediaMessage.content instanceof MediaMessageContent) || mediaMessage.conversation.type != Conversation.ConversationType.SecretChat) {
            return null;
        }
        String remoteUrl = ((MediaMessageContent) mediaMessage.content).remoteUrl;
        if (TextUtils.isEmpty(remoteUrl)) {
            return null;
        }
        if (remoteUrl.contains("?")) {
            return remoteUrl + "&target=" + mediaMessage.conversation.target + "&secret=true";
        } else {
            return remoteUrl + "?target=" + mediaMessage.conversation.target + "&secret=true";
        }
    }

    public static File fileRecordFile(FileRecord fileRecord) {
        String dir = Config.FILE_SAVE_DIR;
        String name = fileRecord.messageUid + "-" + fileRecord.name;
        return new File(dir, name);
    }

    public static File mediaMessageContentFile(Message message) {

        String dir = null;
        String name = null;
        MessageContent content = message.content;
        if (!(content instanceof MediaMessageContent)) {
            return null;
        }
        if (!TextUtils.isEmpty(((MediaMessageContent) content).localPath)) {
            File file = new File(((MediaMessageContent) content).localPath);
            if (file.exists()) {
                return file;
            }
        }

        switch (((MediaMessageContent) content).mediaType) {
            case VOICE:
                name = message.messageUid + ".mp3";
                dir = Config.AUDIO_SAVE_DIR;
                break;
            case IMAGE:
                name = message.messageUid + ".jpg";
                dir = Config.PHOTO_SAVE_DIR;
                break;
            case VIDEO:
                name = message.messageUid + ".mp4";
                dir = Config.VIDEO_SAVE_DIR;
                break;
            case FILE:
                if (message.content instanceof FileMessageContent) {
                    name = message.messageUid + "-" + ((FileMessageContent) message.content).getName();
                } else {
                    name = message.messageUid + ".data";
                }
                dir = Config.FILE_SAVE_DIR;
                break;
            default:
                dir = Config.FILE_SAVE_DIR;
                name = message.messageUid + "-" + ".data";
                break;
        }
        return new File(dir, name);
    }

    /**
     * @param saveDir
     * @return
     * @throws IOException 判断下载目录是否存在
     */
    private static String isExistDir(String saveDir) throws IOException {
        // 下载位置
        File downloadFile = new File(saveDir);
        if (!downloadFile.mkdirs()) {
            downloadFile.createNewFile();
        }
        return downloadFile.getAbsolutePath();
    }

    /**
     * @param url
     * @return 从下载连接中解析出文件名
     */
    @NonNull
    public static String getNameFromUrl(String url) {
        return url.substring(url.lastIndexOf("/") + 1);
    }

    public interface OnDownloadListener {
        /**
         * 下载成功
         */
        void onSuccess(File file);

        /**
         * @param progress 下载进度
         */
        void onProgress(int progress);

        /**
         * 下载失败
         */
        void onFail();
    }

    public interface OnDownloadListenerEx {
        /**
         * 下载成功
         */
        void onSuccess(byte[] bytes);

        /**
         * @param progress 下载进度
         */
        void onProgress(int progress);

        /**
         * 下载失败
         */
        void onFail();
    }

    public static class SimpleOnDownloadListener implements OnDownloadListener {

        @Override
        final public void onSuccess(File file) {
            ChatManager.Instance().getMainHandler().post(() -> onUiSuccess(file));
        }

        @Override
        final public void onProgress(int progress) {
            ChatManager.Instance().getMainHandler().post(() -> onUiProgress(progress));
        }

        @Override
        final public void onFail() {
            ChatManager.Instance().getMainHandler().post(this::onUiFail);
        }

        public void onUiSuccess(File file) {


        }

        public void onUiProgress(int progress) {

        }

        public void onUiFail() {

        }
    }

    private static Map<String, String> getQueryMap(String query) {
        Map<String, String> map = new HashMap<String, String>();
        if (TextUtils.isEmpty(query)) {
            return map;
        }

        String[] params = query.split("&");
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }
}