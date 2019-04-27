package cn.wildfire.chat.kit.conversation.ext;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfire.chat.kit.utils.FileUtils;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.TypingMessageContent;
import cn.wildfirechat.model.Conversation;

public class FileExt extends ConversationExt {

    /**
     * @param containerView 扩展view的container
     * @param conversation
     */
    @ExtContextMenuItem(title = "文件")
    public void pickFile(View containerView, Conversation conversation) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
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
            String path = FileUtils.getPath(context, uri);
            if (TextUtils.isEmpty(path)) {
                Toast.makeText(context, "选择文件错误", Toast.LENGTH_SHORT).show();
                return;
            }

            String type = path.substring(path.lastIndexOf("."));
            File file = new File(path);
            switch (type) {
                case ".png":
                case ".jpg":
                case ".jpeg":
                case ".gif":
                    File imageFileThumb = ImageUtils.genThumbImgFile(path);
                    conversationViewModel.sendImgMsg(imageFileThumb, file);
                    break;
                case ".3gp":
                case ".mpg":
                case ".mpeg":
                case ".mpe":
                case ".mp4":
                case ".avi":
                    conversationViewModel.sendVideoMsg(file);
                    break;
                default:
                    conversationViewModel.sendFileMsg(file);
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
}
