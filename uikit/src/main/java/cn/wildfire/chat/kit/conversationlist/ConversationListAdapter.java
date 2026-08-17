/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversationlist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.ConversationContextMenuItem;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.conversationlist.notification.StatusNotification;
import cn.wildfire.chat.kit.conversationlist.viewholder.ConversationViewHolder;
import cn.wildfire.chat.kit.conversationlist.viewholder.ConversationViewHolderManager;
import cn.wildfire.chat.kit.conversationlist.viewholder.StatusNotificationContainerViewHolder;
import cn.wildfirechat.message.core.MessageDirection;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.SecretChatInfo;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.uikit.menu.OnMenuItemClickListener;
import cn.wildfirechat.uikit.menu.VerticalContextMenu;

public class ConversationListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Fragment fragment;

    private List<ConversationInfo> conversationInfos = new ArrayList<>();
    private List<StatusNotification> statusNotifications;

    public ConversationListAdapter(Fragment context) {
        super();
        this.fragment = context;
    }

    private OnClickConversationItemListener onClickConversationItemListener;

    /**
     * 平板双栏下右栏当前打开的会话，在左栏列表里高亮。手机端不使用，恒为 null。
     */
    private Conversation selectedConversation;
    /**
     * 是否启用选中态。只有调用过 {@link #setSelectedConversation(Conversation)} 才会置为 true，
     * 即只有平板双栏宿主会打开它，手机端的绑定逻辑完全不受影响。
     */
    private boolean selectionEnabled;

    private boolean isEmpty(List list) {
        return list == null || list.isEmpty();
    }

    public void updateStatusNotification(List<StatusNotification> statusNotifications) {
        submit(statusNotifications, this.conversationInfos);
    }

    /**
     * 返回会话列表 header 的数量，由于 header 是一个 container，所有的 status notification 都是在这个 container里面实现，所以只可能返回 0 或 1
     *
     * @return 0 或 1
     */
    public int headerCount() {
        return isEmpty(this.statusNotifications) ? 0 : 1;
    }

    public void setConversationInfos(List<ConversationInfo> conversationInfos) {
        submit(this.statusNotifications, conversationInfos);
    }

    public int getNextUnreadConversationPosition(int position) {
        for (int i = position + 1; i < getItemCount(); i++) {
            ConversationInfo conversationInfo = conversationInfos.get(i - headerCount());
            if (!conversationInfo.isSilent && (conversationInfo.unreadCount.unread + conversationInfo.unreadCount.unreadMention + conversationInfo.unreadCount.unreadMentionAll) > 0) {
                return i;
            }
        }
        return -1;
    }

    public void setOnClickConversationItemListener(OnClickConversationItemListener onClickConversationItemListener) {
        this.onClickConversationItemListener = onClickConversationItemListener;
    }

    /**
     * 平板双栏专用：设置左栏中高亮的会话，传 null 表示右栏没有打开任何会话。
     */
    public void setSelectedConversation(Conversation conversation) {
        if (selectionEnabled && Conversation.equals(this.selectedConversation, conversation)) {
            return;
        }
        Conversation oldSelected = this.selectedConversation;
        this.selectedConversation = conversation;
        if (!selectionEnabled) {
            // 第一次启用选中态时，所有列表项都要从普通背景换成带选中态的背景，
            // 这里只能整体刷新一次；之后切换选中项只需要通知受影响的项。
            selectionEnabled = true;
            notifyDataSetChanged();
            return;
        }
        int oldPosition = findConversationPosition(oldSelected);
        if (oldPosition >= 0) {
            notifyItemChanged(oldPosition);
        }
        int newPosition = findConversationPosition(conversation);
        if (newPosition >= 0) {
            notifyItemChanged(newPosition);
        }
    }

    private int findConversationPosition(Conversation conversation) {
        if (conversation == null || conversationInfos == null) {
            return -1;
        }
        for (int i = 0; i < conversationInfos.size(); i++) {
            if (Conversation.equals(conversationInfos.get(i).conversation, conversation)) {
                return headerCount() + i;
            }
        }
        return -1;
    }

    /**
     * 只刷新可见范围内受用户信息更新影响的会话行，避免用户/群信息每次回调都把整个可见列表重绘一遍，
     * 在平板双栏左栏常驻时尤其明显（点开会话触发信息刷新会让左栏整体闪烁）。
     *
     * @param startVisible 可见区域第一个 item 的 position（含 header）
     * @param endVisible   可见区域最后一个 item 的 position（含 header）
     */
    public void notifyUserInfosUpdated(List<UserInfo> userInfos, int startVisible, int endVisible) {
        if (userInfos == null || userInfos.isEmpty() || conversationInfos == null
            || startVisible < 0 || endVisible < startVisible) {
            return;
        }
        Set<String> userIds = new HashSet<>();
        for (UserInfo userInfo : userInfos) {
            if (userInfo != null && userInfo.uid != null) {
                userIds.add(userInfo.uid);
            }
        }
        if (userIds.isEmpty()) {
            return;
        }
        int first = Math.max(startVisible, headerCount());
        int last = Math.min(endVisible, headerCount() + conversationInfos.size() - 1);
        for (int position = first; position <= last; position++) {
            if (isUserAffected(conversationInfos.get(position - headerCount()), userIds)) {
                notifyItemChanged(position);
            }
        }
    }

    /**
     * 只刷新可见范围内受群信息更新影响的会话行，避免整体重绘可见列表。
     *
     * @param startVisible 可见区域第一个 item 的 position（含 header）
     * @param endVisible   可见区域最后一个 item 的 position（含 header）
     */
    public void notifyGroupInfosUpdated(List<GroupInfo> groupInfos, int startVisible, int endVisible) {
        if (groupInfos == null || groupInfos.isEmpty() || conversationInfos == null
            || startVisible < 0 || endVisible < startVisible) {
            return;
        }
        Set<String> groupIds = new HashSet<>();
        for (GroupInfo groupInfo : groupInfos) {
            if (groupInfo != null && groupInfo.target != null) {
                groupIds.add(groupInfo.target);
            }
        }
        if (groupIds.isEmpty()) {
            return;
        }
        int first = Math.max(startVisible, headerCount());
        int last = Math.min(endVisible, headerCount() + conversationInfos.size() - 1);
        for (int position = first; position <= last; position++) {
            ConversationInfo conversationInfo = conversationInfos.get(position - headerCount());
            if (conversationInfo.conversation.type == Conversation.ConversationType.Group
                && groupIds.contains(conversationInfo.conversation.target)) {
                notifyItemChanged(position);
            }
        }
    }

    private boolean isUserAffected(ConversationInfo conversationInfo, Set<String> userIds) {
        Conversation conversation = conversationInfo.conversation;
        if (conversation.type == Conversation.ConversationType.Single
            && userIds.contains(conversation.target)) {
            return true;
        }
        if ((conversation.type == Conversation.ConversationType.Group || conversation.type == Conversation.ConversationType.Channel)
            && conversationInfo.lastMessage != null
            && conversationInfo.lastMessage.direction == MessageDirection.Receive
            && userIds.contains(conversationInfo.lastMessage.sender)) {
            return true;
        }
        if (conversation.type == Conversation.ConversationType.SecretChat) {
            SecretChatInfo secretChatInfo = ChatManager.Instance().getSecretChatInfo(conversation.target);
            return secretChatInfo != null && userIds.contains(secretChatInfo.getUserId());
        }
        return false;
    }

    private void submit(List<StatusNotification> notifications, List<ConversationInfo> conversationInfos) {
        List<StatusNotification> oldNotifications = this.statusNotifications;
        List<ConversationInfo> oldConversationInfos = this.conversationInfos;

        this.conversationInfos = conversationInfos;
        int oldHeaderCount = headerCount();
        this.statusNotifications = notifications;
        // 设置 statusNotifications 后，重新计算
        int newHeaderCount = headerCount();

        if (listSize(oldConversationInfos) == 0) {
            notifyDataSetChanged();
            return;
        }

        // TODO work thread 做 diff
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return oldHeaderCount + listSize(oldConversationInfos);
            }

            @Override
            public int getNewListSize() {
                return newHeaderCount + listSize(conversationInfos);
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                if (oldItemPosition < oldHeaderCount && newItemPosition < newHeaderCount) {
                    return true;
                } else if (oldItemPosition >= oldHeaderCount && newItemPosition >= newHeaderCount) {
                    oldItemPosition = oldItemPosition - oldHeaderCount;
                    newItemPosition = newItemPosition - newHeaderCount;

                    ConversationInfo oldInfo = oldConversationInfos.get(oldItemPosition);
                    ConversationInfo newInfo = conversationInfos.get(newItemPosition);

                    return Conversation.equals(oldInfo.conversation, newInfo.conversation);
                } else {
                    return false;
                }
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                if (oldItemPosition < oldHeaderCount && newItemPosition < newHeaderCount) {
                    return areStatusNotificationsTheSame(oldNotifications, statusNotifications);
                } else if (oldItemPosition >= oldHeaderCount && newItemPosition >= newHeaderCount) {
                    oldItemPosition = oldItemPosition - oldHeaderCount;
                    newItemPosition = newItemPosition - newHeaderCount;

                    ConversationInfo oldInfo = oldConversationInfos.get(oldItemPosition);
                    ConversationInfo newInfo = conversationInfos.get(newItemPosition);

                    return oldInfo.equals(newInfo);
                } else {
                    return false;
                }
            }
        }, true);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == R.layout.conversationlist_item_notification_container) {
            View view = LayoutInflater.from(fragment.getContext()).inflate(R.layout.conversationlist_item_notification_container, parent, false);
            return new StatusNotificationContainerViewHolder(view);
        }
        Class<? extends ConversationViewHolder> viewHolderClazz = ConversationViewHolderManager.getInstance().getConversationContentViewHolder(viewType);

        View itemView;
        itemView = LayoutInflater.from(fragment.getContext()).inflate(R.layout.conversationlist_item_conversation, parent, false);

        try {
            Constructor constructor = viewHolderClazz.getConstructor(Fragment.class, RecyclerView.Adapter.class, View.class);
            ConversationViewHolder viewHolder = (ConversationViewHolder) constructor.newInstance(fragment, this, itemView);
            processConversationClick(viewHolder, itemView);
            processConversationLongClick(viewHolderClazz, viewHolder, itemView);
            return viewHolder;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof ConversationViewHolder) {
            ((ConversationViewHolder) holder).removeLiveDataObserver();
        }
    }

    private void processConversationClick(ConversationViewHolder viewHolder, View itemView) {
        if (onClickConversationItemListener != null) {
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = viewHolder.getAdapterPosition();
                    ConversationInfo conversationInfo = conversationInfos.get(position - headerCount());
                    markConversationSelectedOptimistically(conversationInfo.conversation, position, v);
                    onClickConversationItemListener.onClickConversationItem(conversationInfo);
                }
            });
        } else {
            itemView.setOnClickListener(viewHolder::onClick);
        }
    }

    /**
     * 平板双栏：点击时先同步把背景切成选中态，不等右栏 Fragment 事务异步执行完再回调。
     * 打开右栏页面走的是 FragmentTransaction.commitAllowingStateLoss()，真正执行要等到下一个
     * Looper 消息；而系统的按压态（selector 的 state_pressed）会在那之前自行褪去，
     * 露出一帧普通背景后才等到选中态生效，看起来像背景闪了一下。这里提前应用，等
     * {@link #setSelectedConversation} 真正被调用时会因状态已一致而直接跳过。
     */
    private void markConversationSelectedOptimistically(Conversation conversation, int clickedPosition, View clickedItemView) {
        if (!selectionEnabled || Conversation.equals(selectedConversation, conversation)) {
            return;
        }
        int oldPosition = findConversationPosition(selectedConversation);
        selectedConversation = conversation;
        if (oldPosition >= 0 && oldPosition != clickedPosition) {
            notifyItemChanged(oldPosition);
        }
        clickedItemView.setActivated(true);
    }

    private static class ContextMenuItemWrapper {
        ConversationContextMenuItem contextMenuItem;
        Method method;

        public ContextMenuItemWrapper(ConversationContextMenuItem contextMenuItem, Method method) {
            this.contextMenuItem = contextMenuItem;
            this.method = method;
        }
    }


    private void processConversationLongClick(Class<? extends ConversationViewHolder> viewHolderClazz, ConversationViewHolder viewHolder, View itemView) {
        if (!viewHolderClazz.isAnnotationPresent(EnableContextMenu.class)) {
            return;
        }
        VerticalContextMenu verticalContextMenu = new VerticalContextMenu(fragment.getActivity(), itemView);
        View.OnLongClickListener listener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Method[] allMethods = viewHolderClazz.getDeclaredMethods();
                List<ContextMenuItemWrapper> contextMenus = new ArrayList<>();
                for (final Method method : allMethods) {
                    if (method.isAnnotationPresent(ConversationContextMenuItem.class)) {
                        contextMenus.add(new ContextMenuItemWrapper(method.getAnnotation(ConversationContextMenuItem.class), method));
                    }
                }
                // handle annotated method in ConversationViewHolder
                allMethods = ConversationViewHolder.class.getDeclaredMethods();
                for (final Method method : allMethods) {
                    if (method.isAnnotationPresent(ConversationContextMenuItem.class)) {
                        contextMenus.add(new ContextMenuItemWrapper(method.getAnnotation(ConversationContextMenuItem.class), method));
                    }
                }

                if (contextMenus.isEmpty()) {
                    return false;
                }

                int position = viewHolder.getAdapterPosition();
                ConversationInfo conversationInfo = conversationInfos.get(position - headerCount());
                Iterator<ContextMenuItemWrapper> iterator = contextMenus.iterator();
                ConversationContextMenuItem item;
                while (iterator.hasNext()) {
                    item = iterator.next().contextMenuItem;
                    if (viewHolder.contextMenuItemFilter(conversationInfo, item.tag())) {
                        iterator.remove();
                    }
                }

                if (contextMenus.isEmpty()) {
                    return false;
                }
                Collections.sort(contextMenus, (o1, o2) -> o1.contextMenuItem.priority() - o2.contextMenuItem.priority());
                List<String> titles = new ArrayList<>(contextMenus.size());
                for (ContextMenuItemWrapper itemWrapper : contextMenus) {
                    titles.add(viewHolder.contextMenuTitle(fragment.getContext(), itemWrapper.contextMenuItem.tag()));
                }

                verticalContextMenu.items(titles);
                verticalContextMenu.setOnItemClickListener(new OnMenuItemClickListener() {
                    @Override
                    public void onClick(int position) {
                        try {
                            ContextMenuItemWrapper menuItem = contextMenus.get(position);
                            if (menuItem.contextMenuItem.confirm()) {
                                String content;
                                content = viewHolder.contextConfirmPrompt(fragment.getContext(), menuItem.contextMenuItem.tag());
                                new MaterialDialog.Builder(fragment.getActivity())
                                    .content(content)
                                    .negativeText(R.string.action_cancel)
                                    .positiveText(R.string.action_confirm)
                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            try {
                                                menuItem.method.invoke(viewHolder, itemView, conversationInfo);
                                            } catch (IllegalAccessException e) {
                                                e.printStackTrace();
                                            } catch (InvocationTargetException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    })
                                    .build()
                                    .show();

                            } else {
                                contextMenus.get(position).method.invoke(viewHolder, itemView, conversationInfo);
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }

                    }
                });
                verticalContextMenu.show();
                return true;
            }
        };
        itemView.setOnLongClickListener(listener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (isStatusNotificationHeader(position)) {
            ((StatusNotificationContainerViewHolder) holder).onBind(fragment, holder.itemView, statusNotifications);
            return;
        }
        int conversationItemPosition = position - headerCount();
        ConversationInfo conversationInfo = conversationInfos.get(conversationItemPosition);
        ((ConversationViewHolder) holder).onBind(conversationInfo, conversationItemPosition);

        // 平板双栏：标记右栏当前打开的会话。手机端从不调用 setSelectedConversation，
        // selectionEnabled 恒为 false，这一段不会执行，列表项背景与改造前一致。
        if (selectionEnabled) {
            holder.itemView.setBackgroundResource(R.drawable.selector_conversation_item_two_pane);
            holder.itemView.setActivated(selectedConversation != null
                && Conversation.equals(selectedConversation, conversationInfo.conversation));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        onBindViewHolder(holder, position);
    }

    @Override
    public int getItemViewType(int position) {
        if (isStatusNotificationHeader(position)) {
            return R.layout.conversationlist_item_notification_container;
        }
        Conversation conversation = conversationInfos.get(position - headerCount()).conversation;
        return conversation.type.getValue() << 24 | conversation.line;
    }

    @Override
    public int getItemCount() {
        return headerCount() + (conversationInfos == null ? 0 : conversationInfos.size());
    }

    private boolean isStatusNotificationHeader(int position) {
        return position < headerCount();
    }

    private static int listSize(List list) {
        return list == null ? 0 : list.size();
    }

    private boolean areStatusNotificationsTheSame(List<StatusNotification> oldNotifications, List<StatusNotification> newNotifications) {
        boolean eq = Objects.equals(oldNotifications, newNotifications);
        if (eq) {
            return true;
        } else if (listSize(oldNotifications) != listSize(newNotifications)) {
            return false;
        } else {
            for (int i = 0; i < oldNotifications.size(); i++) {
                if (!oldNotifications.get(i).equals(newNotifications.get(i))) {
                    return false;
                }
            }
        }
        return true;
    }
}
