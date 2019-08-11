package cn.wildfire.chat.kit.conversation.ext;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfire.chat.kit.mm.TakePhotoActivity;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.TypingMessageContent;
import cn.wildfirechat.model.Conversation;

import static android.app.Activity.RESULT_OK;

public class ShootExt extends ConversationExt {

    /**
     * @param containerView 扩展view的container
     * @param conversation
     */
    @ExtContextMenuItem(title = "拍摄")
    public void shoot(View containerView, Conversation conversation) {
        String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!((WfcBaseActivity) context).checkPermission(permissions)) {
                context.requestPermissions(permissions, 100);
                return;
            }
        }
        Intent intent = new Intent(context, TakePhotoActivity.class);
        startActivityForResult(intent, 100);
        TypingMessageContent content = new TypingMessageContent(TypingMessageContent.TYPING_CAMERA);
        conversationViewModel.sendMessage(content);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String path = data.getStringExtra("path");
            if (TextUtils.isEmpty(path)) {
                Toast.makeText(context, "拍照错误, 请向我们反馈", Toast.LENGTH_SHORT).show();
                return;
            }
            if (data.getBooleanExtra("take_photo", true)) {
                //照片
                conversationViewModel.sendImgMsg(ImageUtils.genThumbImgFile(path), new File(path));
            } else {
                //小视频
                conversationViewModel.sendVideoMsg(new File(path));
            }
        }
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public int iconResId() {
        return R.mipmap.ic_func_pic;
    }

    @Override
    public String title(Context context) {
        return "拍摄";
    }
}
