package cn.wildfire.chat.kit.conversation.ext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import java.io.File;

import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfire.chat.kit.utils.FilePathUtil;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.TypingMessageContent;
import cn.wildfirechat.model.Conversation;

public class FileExt extends ConversationExt {

    /**
     * @param containerView 扩展view的container
     * @param conversation
     */
    @ExtContextMenuItem(title = "文件")
    public void image(View containerView, Conversation conversation) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //intent.setType(“image/*”);//选择图片
        //intent.setType(“audio/*”); //选择音频
        //intent.setType(“video/*”); //选择视频 （mp4 3gp 是android支持的视频格式）
        //intent.setType(“video/*;image/*”);//同时选择视频和图片
        intent.setType("*/*");//无类型限制
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 100);
        TypingMessageContent content = new TypingMessageContent(TypingMessageContent.TYPING_FILE);
        conversationViewModel.sendMessage(content);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            String path = FilePathUtil.getRealPath(context, uri);
            conversationViewModel.sendFileMsg(new File(path));
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
}
