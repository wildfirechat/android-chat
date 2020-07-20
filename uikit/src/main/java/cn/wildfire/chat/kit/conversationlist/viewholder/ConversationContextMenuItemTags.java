package cn.wildfire.chat.kit.conversationlist.viewholder;

import android.content.Context;

import butterknife.BindString;
import cn.wildfirechat.chat.R2;

public class ConversationContextMenuItemTags {
    public static final String TAG_REMOVE = "remove";
    public static final String TAG_CLEAR = "clear";
    public static final String TAG_TOP = "stick_top";
    public static final String TAG_CANCEL_TOP = "cancel_stick_top";
    public static final String TAG_UNSUBSCRIBE = "unSubscribe_channel";

    @BindString(R2.string.add_friend)
    String addFriend;

    // todo
    // HOW
    /**
     * {@link cn.wildfire.chat.kit.annotation.LayoutRes}
     * {@link cn.wildfire.chat.kit.annotation.SendLayoutRes}
     * {@link cn.wildfire.chat.kit.annotation.ReceiveLayoutRes}
     * 移除上面上面三个注解，两个抽象方法 sendLayoutResId(), receiveLayoutResId()
     *
     * {@link cn.wildfire.chat.kit.annotation.ConversationContextMenuItem}
     * {@link cn.wildfire.chat.kit.annotation.MessageContextMenuItem}
     * 上面上个移除 title、titleResId
     *
     * {@link ConversationContextMenuItemTags}
     * {@link cn.wildfire.chat.kit.conversation.message.viewholder.MessageContextMenuItemTags}
     * 上面两个类中，分别添加 getTitle(tag) 根据tag，获取title
     *
     * {@link cn.wildfire.chat.kit.annotation.ExtContextMenuItem} 才去类似上面的处理
     */

    public String getTile(Context context, String tag) {
        String title = null;
        switch (tag) {
            case TAG_REMOVE:
                title = addFriend;
                break;
            default:
                break;
        }
        return title;
    }
}
