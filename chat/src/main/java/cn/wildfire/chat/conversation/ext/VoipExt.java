package cn.wildfire.chat.conversation.ext;

import android.content.Context;
import android.view.View;

import cn.wildfire.chat.annotation.ExtContextMenuItem;
import cn.wildfire.chat.app.MyApp;
import cn.wildfire.chat.conversation.ext.core.ConversationExt;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.Conversation;

public class VoipExt extends ConversationExt {

    @ExtContextMenuItem(title = "视频通话")
    public void voip(View containerView, Conversation conversation) {
        MyApp.onCall(context, conversation.target, true, false);
    }

    @ExtContextMenuItem(title = "语音通话")
    public void audio(View containerView, Conversation conversation) {
        MyApp.onCall(context, conversation.target, true, true);
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
