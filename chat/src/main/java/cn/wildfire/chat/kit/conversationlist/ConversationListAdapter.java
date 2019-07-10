package cn.wildfire.chat.kit.conversationlist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import cn.wildfire.chat.kit.annotation.ConversationContextMenuItem;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.conversationlist.notification.StatusNotification;
import cn.wildfire.chat.kit.conversationlist.viewholder.ConversationViewHolder;
import cn.wildfire.chat.kit.conversationlist.viewholder.ConversationViewHolderManager;
import cn.wildfire.chat.kit.conversationlist.viewholder.StatusNotificationContainerViewHolder;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.UserInfo;

public class ConversationListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Fragment fragment;

    private List<ConversationInfo> conversationInfos = new ArrayList<>();
    private List<StatusNotification> statusNotifications;

    public ConversationListAdapter(Fragment context) {
        super();
        this.fragment = context;
    }

    public List<ConversationInfo> getConversationInfos() {
        return conversationInfos;
    }

    private boolean isEmpty(List list) {
        return list == null || list.isEmpty();
    }

    public void updateStatusNotification(List<StatusNotification> statusNotifications) {
        if (isEmpty(this.statusNotifications) && !isEmpty(statusNotifications)) {
            this.statusNotifications = statusNotifications;
            notifyItemInserted(0);
            return;
        }

        if (!isEmpty(this.statusNotifications) && isEmpty(statusNotifications)) {
            this.statusNotifications = null;
            notifyItemRemoved(0);
            return;
        }

        if (!isEmpty(this.statusNotifications) && !isEmpty(statusNotifications)) {
            this.statusNotifications = statusNotifications;
            notifyItemChanged(0);
        }
    }

    private int headerCount() {
        return isEmpty(this.statusNotifications) ? 0 : 1;
    }

    // TODO 其实只有更新可见的那部分就可以了
    public void updateUserInfos(List<UserInfo> userInfos) {
        if (conversationInfos == null || conversationInfos.isEmpty()) {
            return;
        }

        for (int i = 0; i < conversationInfos.size(); i++) {
            for (UserInfo userInfo : userInfos) {
                if (conversationInfos.get(i).lastMessage == null) {
                    continue;
                }
                ConversationInfo conversationInfo = conversationInfos.get(i);
                if (conversationInfo.conversation.type == Conversation.ConversationType.Single
                        && conversationInfo.conversation.target.equals(userInfo.uid)) {
                    notifyItemChanged(headerCount() + i);
                } else if (conversationInfo.lastMessage.sender.equals(userInfo.uid)) {
                    // TODO 以后可能会添加header
                    notifyItemChanged(headerCount() + i);
                    break;
                }
            }
        }
    }

    public void setConversationInfos(List<ConversationInfo> conversationInfos) {
        this.conversationInfos = conversationInfos;
    }

    public void submitConversationInfo(ConversationInfo conversationInfo) {
        if (conversationInfos == null) {
            return;
        }

        int currentPosition, targetPosition;
        currentPosition = currentPosition(conversationInfo);
        targetPosition = targetPosition(conversationInfo, currentPosition);

        if (currentPosition >= 0) {
            if (currentPosition == targetPosition) {
                conversationInfos.set(targetPosition, conversationInfo);
                notifyItemChanged(headerCount() + targetPosition);
            } else {
                conversationInfos.remove(currentPosition);
                conversationInfos.add(targetPosition, conversationInfo);
                notifyItemRemoved(headerCount() + currentPosition);
                notifyItemInserted(headerCount() + targetPosition);
            }
        } else {
            conversationInfos.add(targetPosition, conversationInfo);
            notifyItemInserted(headerCount() + targetPosition);
        }
    }

    public void removeConversation(Conversation conversation) {
        boolean found = false;
        Iterator<ConversationInfo> iterator = conversationInfos.iterator();
        int index = 0;
        Conversation c;
        while (iterator.hasNext()) {
            c = iterator.next().conversation;
            if (c.type == conversation.type && c.line == conversation.line && c.target.equals(conversation.target)) {
                iterator.remove();
                found = true;
                break;
            }
            index++;
        }
        if (found) {
            notifyItemRemoved(headerCount() + index);
        }
    }

    public void clearAllUnreadStatus() {
        if (conversationInfos == null) {
            return;
        }

        ConversationInfo info;
        for (int i = 0; i < conversationInfos.size(); i++) {
            info = conversationInfos.get(i);
            if (info.unreadCount.unread > 0 || info.unreadCount.unreadMention > 0 || info.unreadCount.unreadMentionAll > 0) {
                info.unreadCount.unread = 0;
                info.unreadCount.unreadMention = 0;
                info.unreadCount.unreadMentionAll = 0;
            }
            notifyItemChanged(headerCount() + i);
        }
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

    private void processConversationClick(ConversationViewHolder viewHolder, View itemView) {
        itemView.setOnClickListener(viewHolder::onClick);
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
                ConversationInfo conversationInfo = getItem(position);
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
                    titles.add(itemWrapper.contextMenuItem.title());
                }
                new MaterialDialog.Builder(fragment.getContext()).items(titles).itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View v, int position, CharSequence text) {
                        try {
                            ContextMenuItemWrapper menuItem = contextMenus.get(position);
                            if (menuItem.contextMenuItem.confirm()) {
                                new MaterialDialog.Builder(fragment.getActivity())
                                        .content(menuItem.contextMenuItem.confirmPrompt())
                                        .negativeText("取消")
                                        .positiveText("确认")
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
                }).show();
                return true;
            }
        };
        itemView.setOnLongClickListener(listener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (headerCount() > 0 && position == 0) {
            ((StatusNotificationContainerViewHolder) holder).onBind(fragment, holder.itemView, statusNotifications);
            return;
        }
        ((ConversationViewHolder) holder).onBind(getItem(position), position - headerCount());
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (headerCount() > 0 && position == 0) {
            ((StatusNotificationContainerViewHolder) holder).onBind(fragment, holder.itemView, statusNotifications);
            return;
        }
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public int getItemCount() {
        return headerCount() + (conversationInfos == null ? 0 : conversationInfos.size());
    }

    public ConversationInfo getItem(int position) {
        return conversationInfos.get(position - headerCount());
    }

    @Override
    public int getItemViewType(int position) {
        if (headerCount() > 0 && position == 0) {
            return R.layout.conversationlist_item_notification_container;
        }
        Conversation conversation = getItem(position).conversation;
        return conversation.type.getValue() << 24 | conversation.line;
    }

    private int currentPosition(ConversationInfo conversationInfo) {
        if (conversationInfos == null || conversationInfos.isEmpty()) {
            return -1;
        }
        ConversationInfo info;
        int position = -1;
        for (int i = 0; i < conversationInfos.size(); i++) {
            info = conversationInfos.get(i);
            if (info.conversation.equals(conversationInfo.conversation)) {
                position = i;
                break;
            }
        }
        return position;
    }

    private int targetPosition(ConversationInfo conversationInfo, int currentPosition) {
        if (conversationInfos == null) {
            return -1;
        }
        if (conversationInfos.isEmpty()) {
            return 0;
        }
        // isTop
        ConversationInfo info;
        int position = 0;
        if (conversationInfo.isTop) {
            for (int i = 0; i < conversationInfos.size(); i++) {
                info = conversationInfos.get(i);
                if (info.isTop) {
                    if (conversationInfo.timestamp >= info.timestamp) {
                        position = i;
                        break;
                    }
                } else {
                    position = i;
                    break;
                }
            }
        } else {
            for (int i = 0; i < conversationInfos.size(); i++) {
                info = conversationInfos.get(i);
                if (info.isTop) {
                    // do nothing, just continue
                    position++;
                } else {
                    if (conversationInfo.timestamp >= info.timestamp) {
                        position = i;
                        break;
                    }
                }
            }
        }
        if (currentPosition >= 0 && position > currentPosition) {
            position = position - 1;
        }
        return position;
    }
}
