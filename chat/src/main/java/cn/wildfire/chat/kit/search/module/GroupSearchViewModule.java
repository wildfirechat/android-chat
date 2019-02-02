package cn.wildfire.chat.kit.search.module;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfire.chat.kit.search.viewHolder.GroupViewHolder;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupSearchResult;
import cn.wildfirechat.remote.ChatManager;

public class GroupSearchViewModule extends SearchableModule<GroupSearchResult, GroupViewHolder> {
    @Override
    public GroupViewHolder onCreateViewHolder(Fragment fragment, @NonNull ViewGroup parent, int type) {
        View itemView;
        itemView = LayoutInflater.from(fragment.getActivity()).inflate(R.layout.search_item_group, parent, false);
        return new GroupViewHolder(fragment, itemView);
    }

    @Override
    public void onBind(Fragment fragment, GroupViewHolder holder, GroupSearchResult groupSearchResult) {
        holder.onBind(keyword, groupSearchResult);
    }

    @Override
    public void onClick(Fragment fragment, GroupViewHolder holder, View view, GroupSearchResult groupSearchResult) {
        Intent intent = new Intent(fragment.getActivity(), ConversationActivity.class);
        Conversation conversation = new Conversation(Conversation.ConversationType.Group, groupSearchResult.groupInfo.target, 0);
        intent.putExtra("conversation", conversation);
        fragment.startActivity(intent);
        fragment.getActivity().finish();
    }

    @Override
    public int getViewType(GroupSearchResult groupSearchResult) {
        return R.layout.search_item_group;
    }

    @Override
    public int priority() {
        return 90;
    }

    @Override
    public String category() {
        return "群组";
    }

    @Override
    public List<GroupSearchResult> search(String keyword) {
        return ChatManager.Instance().searchGroups(keyword);
    }

}
