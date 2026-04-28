/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.Toast;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.uikit.permission.PermissionKit;

/**
 * 直播扩展
 * <p>
 * 在输入栏插件面板添加直播按钮，点击后跳转到直播主播页面，开始直播。
 * </p>
 */
public class LiveStreamingExt extends ConversationExt {

    @ExtContextMenuItem
    public void startLive(View containerView, Conversation conversation) {
        if (!AVEngineKit.isSupportConference()) {
            Toast.makeText(activity, R.string.live_streaming_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions = new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.BLUETOOTH_CONNECT
            };
        } else {
            permissions = new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            };
        }
        PermissionKit.PermissionReqTuple[] tuples = PermissionKit.buildRequestPermissionTuples(activity, permissions);
        PermissionKit.checkThenRequestPermission(activity, activity.getSupportFragmentManager(), tuples, granted -> {
            if (granted) {
                Intent intent = new Intent(activity, LiveHostActivity.class);
                intent.putExtra("conversation", conversation);
                startActivity(intent);
            } else {
                Toast.makeText(activity, R.string.voip_permission_required, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int priority() {
        return 80;
    }

    @Override
    public int iconResId() {
        return R.drawable.ic_ext_live_streaming;
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.live_streaming);
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        return null;
    }

    @Override
    public boolean filter(Conversation conversation) {
        // Only show for single and group conversations
        switch (conversation.type) {
            case Single:
            case Group:
                return false;
            default:
                return true;
        }
    }
}
