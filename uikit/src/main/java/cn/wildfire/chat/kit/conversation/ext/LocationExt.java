/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.ext;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfire.chat.kit.third.location.data.LocationData;
import cn.wildfire.chat.kit.third.location.ui.activity.MyLocationActivity;
import cn.wildfire.chat.kit.utils.AgentState;
import cn.wildfirechat.uikit.permission.PermissionKit;
import cn.wildfirechat.message.TypingMessageContent;
import cn.wildfirechat.model.Conversation;

public class LocationExt extends ConversationExt {

    /**
     * @param containerView 扩展view的container
     * @param conversation
     */
    @ExtContextMenuItem
    public void pickLocation(View containerView, Conversation conversation) {
        String[] permissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        };
        PermissionKit.PermissionReqTuple[] tuples = PermissionKit.buildRequestPermissionTuples(activity, permissions);
        PermissionKit.checkThenRequestPermission(activity, fragment.getChildFragmentManager(), tuples, o -> {
            if (o) {
                Intent intent = new Intent(activity, MyLocationActivity.class);
                startActivityForResult(intent, 100);
                TypingMessageContent content = new TypingMessageContent(TypingMessageContent.TYPING_LOCATION);
                messageViewModel.sendMessage(conversation, toUsers(), content);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            LocationData locationData = (LocationData) data.getSerializableExtra("location");
            messageViewModel.sendLocationMessage(conversation, toUsers(), locationData);
        }
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public int iconResId() {
        return R.mipmap.ic_func_location;
    }

    @Override
    public String title(Context context) {
        return context.getString(R.string.location_ext_title);
    }

    @Override
    public String contextMenuTitle(Context context, String tag) {
        return title(context);
    }

    /**
     * AI 会话（line=2 的 AI 群、与本人 DSH 机器人的单聊）里隐藏「位置」：机器人无法使用
     * 位置卡片，扩展面板只保留图片、拍照、文件等机器人能消费的入口（与 PC 端 DSH 会话
     * 精简输入区一致）。
     * <p>
     * 单聊 DSH 的机器人 id 是异步查询/缓存的，会话打开时这里可能暂时判不出 AI 会话；
     * {@code ConversationInputPanel} 每次打开「+」面板前会按最新状态重算一次可见扩展。
     * </p>
     *
     * @param conversation 会话
     * @return true=不显示
     */
    @Override
    public boolean filter(Conversation conversation) {
        return AgentState.isAgentConversation(conversation);
    }
}
