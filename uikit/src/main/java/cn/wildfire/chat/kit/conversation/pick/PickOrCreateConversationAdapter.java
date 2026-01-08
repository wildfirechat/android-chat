/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.pick;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.forward.viewholder.CategoryViewHolder;
import cn.wildfire.chat.kit.conversation.forward.viewholder.ConversationViewHolder;
import cn.wildfire.chat.kit.conversation.forward.viewholder.CreateConversationViewHolder;
import cn.wildfirechat.model.ConversationInfo;

public class PickOrCreateConversationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int MODE_SINGLE = 0;
    public static final int MODE_MULTI = 1;

    private Fragment fragment;
    private List<ConversationInfo> conversations;
    private OnConversationItemClickListener conversationItemClickListener;
    private OnNewConversationItemClickListener newConversationItemClickListener;
    private OnSelectionChangedListener selectionChangedListener;
    private int mode = MODE_SINGLE;
    private Map<String, ConversationInfo> selectedConversationsMap = new LinkedHashMap<>();

    public PickOrCreateConversationAdapter(Fragment fragment) {
        this.fragment = fragment;
    }

    public void setConversations(List<ConversationInfo> conversations) {
        this.conversations = conversations;
    }

    public void setOnConversationItemClickListener(OnConversationItemClickListener listener) {
        this.conversationItemClickListener = listener;
    }

    public void setNewConversationItemClickListener(OnNewConversationItemClickListener listener) {
        this.newConversationItemClickListener = listener;
    }

    public void setMode(int mode) {
        this.mode = mode;
        notifyDataSetChanged();
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    public void toggleSelection(ConversationInfo conversationInfo) {
        String key = getConversationKey(conversationInfo);
        if (selectedConversationsMap.containsKey(key)) {
            selectedConversationsMap.remove(key);
        } else {
            selectedConversationsMap.put(key, conversationInfo);
        }
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedConversationsMap.size());
        }
        notifyDataSetChanged();
    }

    private String getConversationKey(ConversationInfo info) {
        return info.conversation.type + "_" + info.conversation.target;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        RecyclerView.ViewHolder holder;
        if (viewType == R.layout.forward_item_create_conversation) {
            holder = new CreateConversationViewHolder(view);
            processOnClick(holder);
        } else if (viewType == R.layout.forward_item_category) {
            holder = new CategoryViewHolder(view);
        } else {
            holder = new ConversationViewHolder(fragment, view);
            processOnClick(holder);
        }
        return holder;
    }

    private void processOnClick(RecyclerView.ViewHolder holder) {
        holder.itemView.setOnClickListener((v) -> {
            if (holder instanceof ConversationViewHolder) {
                int position = holder.getAdapterPosition();
                ConversationInfo conversationInfo;

                if (mode == MODE_MULTI) {
                    conversationInfo = conversations.get(position - 1);
                } else {
                    conversationInfo = conversations.get(position - 2);
                }

                if (mode == MODE_MULTI) {
                    toggleSelection(conversationInfo);
                    notifyItemChanged(position);
                } else if (conversationItemClickListener != null) {
                    conversationItemClickListener.onConversationItemClick(conversationInfo);
                }
            } else if (holder instanceof CreateConversationViewHolder && newConversationItemClickListener != null) {
                newConversationItemClickListener.onNewConversationItemClick();
            }
        });
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (mode == MODE_MULTI) {
            switch (position) {
                case 0:
                    break;
                default:
                    ConversationViewHolder conversationViewHolder = (ConversationViewHolder) holder;
                    ConversationInfo conversationInfo = conversations.get(position - 1);
                    conversationViewHolder.onBind(conversationInfo);

                    CheckBox checkBox = holder.itemView.findViewById(R.id.checkbox);
                    if (checkBox != null) {
                        if (mode == MODE_MULTI) {
                            checkBox.setVisibility(View.VISIBLE);
                            String key = getConversationKey(conversationInfo);
                            checkBox.setChecked(selectedConversationsMap.containsKey(key));
                        } else {
                            checkBox.setVisibility(View.GONE);
                        }
                    }
                    break;
            }
        } else {
            switch (position) {
                case 0:
                    break;
                case 1:
                    break;
                default:
                    ConversationViewHolder conversationViewHolder = (ConversationViewHolder) holder;
                    ConversationInfo conversationInfo = conversations.get(position - 2);
                    conversationViewHolder.onBind(conversationInfo);

                    CheckBox checkBox = holder.itemView.findViewById(R.id.checkbox);
                    if (checkBox != null) {
                        if (mode == MODE_MULTI) {
                            checkBox.setVisibility(View.VISIBLE);
                            String key = getConversationKey(conversationInfo);
                            checkBox.setChecked(selectedConversationsMap.containsKey(key));
                        } else {
                            checkBox.setVisibility(View.GONE);
                        }
                    }
                    break;
            }
        }
    }

    @Override
    public int getItemCount() {
        if (mode == MODE_MULTI) {
            return conversations == null ? 1 : 1 + conversations.size();
        }
        return conversations == null ? 2 : 2 + conversations.size();
    }

    @Override
    public int getItemViewType(int position) {
        int type;
        if (mode == MODE_MULTI) {
            switch (position) {
                case 0:
                    type = R.layout.forward_item_category;
                    break;
                default:
                    type = R.layout.forward_item_recent_conversation;
                    break;
            }
        } else {
            switch (position) {
                case 0:
                    type = R.layout.forward_item_create_conversation;
                    break;
                case 1:
                    type = R.layout.forward_item_category;
                    break;
                default:
                    type = R.layout.forward_item_recent_conversation;
                    break;
            }
        }
        return type;
    }

    public List<ConversationInfo> getSelectedConversations() {
        return new ArrayList<>(selectedConversationsMap.values());
    }

    public void clearSelections() {
        selectedConversationsMap.clear();
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(0);
        }
        notifyDataSetChanged();
    }

    public interface OnNewConversationItemClickListener {
        void onNewConversationItemClick();
    }

    public interface OnConversationItemClickListener {
        void onConversationItemClick(ConversationInfo conversationInfo);
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int count);
    }
}
