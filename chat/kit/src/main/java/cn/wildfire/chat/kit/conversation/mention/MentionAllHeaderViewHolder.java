package cn.wildfire.chat.kit.conversation.mention;

import android.view.View;

import androidx.fragment.app.Fragment;
import cn.wildfire.chat.kit.annotation.LayoutRes;
import cn.wildfire.chat.kit.contact.UserListAdapter;
import cn.wildfire.chat.kit.contact.model.HeaderValue;
import cn.wildfire.chat.kit.contact.viewholder.header.HeaderViewHolder;
import cn.wildfirechat.chat.R;

@LayoutRes(resId = R.layout.conversation_header_mention_all)
public class MentionAllHeaderViewHolder extends HeaderViewHolder<HeaderValue> {
    public MentionAllHeaderViewHolder(Fragment fragment, UserListAdapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(HeaderValue value) {

    }
}
