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
}
