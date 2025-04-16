/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.ext;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfire.chat.kit.mm.TakePhotoActivity;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfirechat.uikit.permission.PermissionKit;
import cn.wildfirechat.message.TypingMessageContent;
import cn.wildfirechat.model.Conversation;

public class ShootExt extends ConversationExt {

    /**
     * @param containerView 扩展view的container
     * @param conversation
     */
    @ExtContextMenuItem
    public void shoot(View containerView, Conversation conversation) {
        String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
        PermissionKit.PermissionReqTuple[] tuples = PermissionKit.buildRequestPermissionTuples(activity, permissions);
        PermissionKit.checkThenRequestPermission(activity, activity.getSupportFragmentManager(), tuples, granted -> {
            if (granted) {
                Intent intent = new Intent(activity, TakePhotoActivity.class);
                startActivityForResult(intent, 100);
                TypingMessageContent content = new TypingMessageContent(TypingMessageContent.TYPING_CAMERA);
                messageViewModel.sendMessage(conversation, toUsers(), content);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            String path = data.getStringExtra("path");
            if (TextUtils.isEmpty(path)) {
                Toast.makeText(activity, R.string.shoot_error_feedback, Toast.LENGTH_SHORT).show();
                return;
            }
            if (data.getBooleanExtra("take_photo", true)) {
                //照片
                File file = new File(path);
                messageViewModel.sendImgMsg(conversation, toUsers(), ImageUtils.genThumbImgFile(path), file);
                ImageUtils.saveMedia2Album(fragment.getContext(), file, true);
            } else {
                //小视频
                File file = new File(path);
                messageViewModel.sendVideoMsg(conversation, toUsers(), new File(path));
                ImageUtils.saveMedia2Album(fragment.getContext(), file, false);
            }
        }
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public int iconResId() {
        return R.mipmap.ic_func_shot;
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.shoot_ext_title);
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        return title(context);
    }
}
