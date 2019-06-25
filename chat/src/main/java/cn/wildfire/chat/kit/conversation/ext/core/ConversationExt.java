package cn.wildfire.chat.kit.conversation.ext.core;

import android.content.Context;
import android.content.Intent;

import androidx.fragment.app.FragmentActivity;

import cn.wildfire.chat.kit.conversation.ConversationViewModel;
import cn.wildfirechat.model.Conversation;

public abstract class ConversationExt {
    protected FragmentActivity context;
    protected ConversationExtension extension;
    private int index;
    protected Conversation conversation;
    protected ConversationViewModel conversationViewModel;

    public abstract int priority();

    public abstract int iconResId();

    public abstract String title(Context context);

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
     * @param activity
     */

    protected final void onBind(FragmentActivity activity, ConversationViewModel conversationViewModel, Conversation conversation, ConversationExtension conversationExtension, int index) {
        this.context = activity;
        this.conversationViewModel = conversationViewModel;
        this.conversation = conversation;
        this.extension = conversationExtension;
        this.index = index;
    }

    protected final void onDestroy() {
        this.context = null;
        this.conversationViewModel = null;
        this.conversation = null;
        this.extension = null;
    }

    protected final void startActivity(Intent intent) {
        context.startActivity(intent);
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
