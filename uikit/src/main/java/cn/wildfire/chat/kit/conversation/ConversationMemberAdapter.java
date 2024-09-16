/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.Iterator;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.DomainInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.utils.WfcUtils;

public class ConversationMemberAdapter extends RecyclerView.Adapter<ConversationMemberAdapter.MemberViewHolder> {
    private List<UserInfo> members;
    private ConversationInfo conversationInfo;
    private boolean enableAddMember;
    private boolean enableRemoveMember;
    private OnMemberClickListener onMemberClickListener;

    public ConversationMemberAdapter(ConversationInfo conversationInfo, boolean enableAddMember, boolean enableRemoveMember) {
        this.conversationInfo = conversationInfo;
        this.enableAddMember = enableAddMember;
        this.enableRemoveMember = enableRemoveMember;
    }

    public void setMembers(List<UserInfo> members) {
        this.members = members;
    }


    public void addMembers(List<UserInfo> members) {
        int startIndex = this.members.size();
        this.members.addAll(members);
        notifyItemRangeInserted(startIndex, members.size());
    }

    public void updateMember(UserInfo userInfo) {
        if (this.members == null) {
            return;
        }
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).uid.equals(userInfo.uid)) {
                members.set(i, userInfo);
                notifyItemChanged(i);
                break;
            }
        }
    }

    public void removeMembers(List<String> memberIds) {
        Iterator<UserInfo> iterator = members.iterator();
        while (iterator.hasNext()) {
            UserInfo userInfo = iterator.next();
            if (memberIds.contains(userInfo.uid)) {
                iterator.remove();
                memberIds.remove(userInfo.uid);
            }

            if (memberIds.size() == 0) {
                break;
            }
        }
        notifyDataSetChanged();
    }

    public void setOnMemberClickListener(OnMemberClickListener onMemberClickListener) {
        this.onMemberClickListener = onMemberClickListener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.conversation_item_member_info, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        if (position < members.size()) {
            holder.bindUserInfo(members.get(position));
        } else {
            if (position == members.size()) {
                if (enableAddMember) {
                    holder.bindAddMember();
                } else if (enableRemoveMember) {
                    holder.bindRemoveMember();
                }
            } else if (position == members.size() + 1 && enableRemoveMember) {
                holder.bindRemoveMember();
            }
        }
    }

    @Override
    public int getItemCount() {
        if (members == null) {
            return 0;
        }
        int count = members.size();
        if (enableAddMember) {
            count++;
        }
        if (enableRemoveMember) {
            count++;
        }
        return count;
    }

    class MemberViewHolder extends RecyclerView.ViewHolder {
        ImageView portraitImageView;
        TextView nameTextView;
        TextView externalDomainTextView;
        private UserInfo userInfo;
        private int type = TYPE_USER;
        private static final int TYPE_USER = 0;
        private static final int TYPE_ADD = 1;
        private static final int TYPE_REMOVE = 2;

        void onClick() {
            if (onMemberClickListener == null) {
                return;
            }
            switch (type) {
                case TYPE_USER:
                    if (userInfo != null) {
                        onMemberClickListener.onUserMemberClick(userInfo);
                    }
                    break;
                case TYPE_ADD:
                    onMemberClickListener.onAddMemberClick();
                    break;
                case TYPE_REMOVE:
                    onMemberClickListener.onRemoveMemberClick();
                    break;
                default:
                    break;
            }
        }

        public MemberViewHolder(View itemView) {
            super(itemView);
            bindViews(itemView);
            bindEvents(itemView);
        }

        private void bindViews(View itemView) {
            portraitImageView = itemView.findViewById(R.id.portraitImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            externalDomainTextView = itemView.findViewById(R.id.externalDomainTextView);
        }

        private void bindEvents(View itemView) {
            itemView.findViewById(R.id.portraitImageView).setOnClickListener(_v -> onClick());
        }

        public void bindUserInfo(UserInfo userInfo) {
            if (userInfo == null) {
                nameTextView.setText("");
                portraitImageView.setImageResource(R.mipmap.avatar_def);
                return;
            }
            this.userInfo = userInfo;
            this.type = TYPE_USER;
            nameTextView.setVisibility(View.VISIBLE);
            if (conversationInfo.conversation.type == Conversation.ConversationType.Group) {
                nameTextView.setText(ChatManager.Instance().getGroupMemberDisplayName(conversationInfo.conversation.target, userInfo.uid));
            } else {
                nameTextView.setText(ChatManager.Instance().getUserDisplayName(userInfo.uid));
            }

            String externalDomainId = WfcUtils.getExternalDomainId(userInfo.uid);
            externalDomainTextView.setVisibility(View.GONE);
            if (externalDomainId != null) {
                DomainInfo domainInfo = ChatManager.Instance().getDomainInfo(externalDomainId, false);
                if (domainInfo != null) {
                    externalDomainTextView.setVisibility(View.VISIBLE);
                    externalDomainTextView.setText("@" + domainInfo.name);
                }
            }
            Glide.with(portraitImageView).load(userInfo.portrait).apply(new RequestOptions().centerCrop().placeholder(R.mipmap.avatar_def)).into(portraitImageView);
        }

        public void bindAddMember() {
            nameTextView.setVisibility(View.GONE);
            portraitImageView.setImageResource(R.mipmap.ic_add_team_member);
            this.type = TYPE_ADD;

        }

        public void bindRemoveMember() {
            nameTextView.setVisibility(View.GONE);
            portraitImageView.setImageResource(R.mipmap.ic_remove_team_member);
            this.type = TYPE_REMOVE;
        }
    }

    public interface OnMemberClickListener {
        void onUserMemberClick(UserInfo userInfo);

        void onAddMemberClick();

        void onRemoveMemberClick();
    }
}
