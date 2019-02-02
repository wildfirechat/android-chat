package cn.wildfire.chat.kit.conversation.ext;

import android.content.Context;
import android.view.View;

import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.annotation.ExtContextMenuItem;
import cn.wildfire.chat.kit.conversation.ext.core.ConversationExt;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.Conversation;

public class VoipExt extends ConversationExt {

    @ExtContextMenuItem(title = "视频通话")
    public void voip(View containerView, Conversation conversation) {
        WfcUIKit.onCall(context, conversation.target, true, false);
    }

    @ExtContextMenuItem(title = "语音通话")
    public void audio(View containerView, Conversation conversation) {
        WfcUIKit.onCall(context, conversation.target, true, true);
    }

    @Override
    public int priority() {
        return 99;
    }

    @Override
    public int iconResId() {
        return R.mipmap.ic_func_video;
    }

    @Override
    public boolean filter(Conversation conversation) {
        if (conversation.type == Conversation.ConversationType.Single) {
            return false;
        }
        return true;
    }


    @Override
    public String title(Context context) {
        return "视频通话";
    }
}
