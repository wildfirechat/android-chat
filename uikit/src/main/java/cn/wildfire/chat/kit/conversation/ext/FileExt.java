/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.ext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.io.File;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.conversation.bigfile.UploadBigFileActivity;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfire.chat.kit.utils.FileUtils;
import cn.wildfirechat.message.TypingMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

public class FileExt extends ConversationExt {

    /**
     * @param containerView 扩展view的container
     * @param conversation
     */
    @ExtContextMenuItem
    public void pickFile(View containerView, Conversation conversation) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");//无类型限制
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 100);
        TypingMessageContent content = new TypingMessageContent(TypingMessageContent.TYPING_FILE);
        messageViewModel.sendMessage(conversation, content);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            // TODO Android10之后，可能不能直接通过uri拿到path，这时候，会进行文件拷贝
            // 当大文件上时，不应当进行拷贝，可以直接通过InputStream进行上传
            String path = FileUtils.getPath(activity, uri);
            if (TextUtils.isEmpty(path)) {
                Toast.makeText(activity, "选择文件错误", Toast.LENGTH_SHORT).show();
                return;
            }

            String type = path.substring(path.lastIndexOf("."));
            File file = new File(path);
            if(file.length() > 80 * 1024 * 1024) {
                if (ChatManager.Instance().isSupportBigFilesUpload()) {
                    new MaterialDialog.Builder(activity)
                        .content("文件太大，是否先上传？")
                        .cancelable(true)
                        .negativeText("取消")
                        .positiveText("确定")
                        .onPositive((dialog, which) -> {
                    Intent intent = new Intent(activity, UploadBigFileActivity.class);
                    intent.putExtra("filePath", file.getAbsolutePath());
                    intent.putExtra("conversation", conversation);
                    activity.startActivity(intent);

                        })
                        .show();
                } else {
                    Toast.makeText(activity, "文件太大无法发送！", Toast.LENGTH_LONG).show();
                }
                return;
            }

            switch (type) {
                case ".png":
                case ".jpg":
                case ".jpeg":
                case ".gif":
                    File imageFileThumb = ImageUtils.genThumbImgFile(path);
                    messageViewModel.sendImgMsg(conversation, imageFileThumb, file);
                    break;
                case ".3gp":
                case ".mpg":
                case ".mpeg":
                case ".mpe":
                case ".mp4":
                case ".avi":
                    messageViewModel.sendVideoMsg(conversation, file);
                    break;
                default:
                    messageViewModel.sendFileMsg(conversation, file);
                    break;
            }
        }
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public int iconResId() {
        return R.mipmap.ic_func_file;
    }

    @Override
    public String title(Context context) {
        return "文件";
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        return title(context);
    }
}
