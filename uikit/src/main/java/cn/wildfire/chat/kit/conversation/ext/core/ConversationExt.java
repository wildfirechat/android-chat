package cn.wildfire.chat.kit.conversation.ext.core;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import cn.wildfire.chat.kit.viewmodel.MessageViewModel;
import cn.wildfirechat.model.Conversation;

public abstract class ConversationExt {
    protected FragmentActivity activity;
    protected ConversationExtension extension;
    protected Fragment fragment;
    private int index;
    protected Conversation conversation;
    protected MessageViewModel messageViewModel;

    /**
     * ext 优先级
     *
     * @return
     */
    public abstract int priority();

    /**
     * ext icon资源id
     *
     * @return
     */
    public abstract int iconResId();

    /**
     * ext 标题
     *
     * @param context
     * @return
     */
    public abstract String title(Context context);

    /**
     * 长按Ext，弹出的 context menu 的标题
     *
     * @param tag
     * @return
     */
    public abstract String contextMenuTitle(Context context, String tag);

    /**
     * 当前会话是否显示此扩展
     *
     * @param conversation
     * @return 返回true，表示不显示
     */
    public boolean filter(Conversation conversation) {
        return false;
    }

    /**
     * 和会话界面绑定之后调用
     *
     * @param fragment
     */

    protected final void onBind(Fragment fragment, MessageViewModel messageViewModel, Conversation conversation, ConversationExtension conversationExtension, int index) {
        this.activity = fragment.getActivity();
        this.fragment = fragment;
        this.messageViewModel = messageViewModel;
        this.conversation = conversation;
        this.extension = conversationExtension;
        this.index = index;
    }

    protected final void onDestroy() {
        this.activity = null;
        this.fragment = null;
        this.messageViewModel = null;
        this.conversation = null;
        this.extension = null;
    }

    protected final void startActivity(Intent intent) {
        activity.startActivity(intent);
    }

    /**
     * @param requestCode
     * @param resultCode
     * @param data
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        throw new IllegalStateException("show override this method");
    }

    /**
     * @param intent
     * @param requestCode 必须在0-256范围之内, 扩展{@link ConversationExt}内部唯一即可
     */
    protected final void startActivityForResult(Intent intent, int requestCode) {
        if (requestCode < 0 || requestCode > 256) {
            throw new IllegalArgumentException("request code should in [0, 256]");
        }
        extension.startActivityForResult(intent, requestCode, index);
    }

}
