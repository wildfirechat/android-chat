package cn.wildfire.chat.kit.conversation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContextMenuItem;
import cn.wildfire.chat.kit.conversation.message.MessageItemView;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.conversation.message.viewholder.LoadingViewHolder;
import cn.wildfire.chat.kit.conversation.message.viewholder.MessageContentViewHolder;
import cn.wildfire.chat.kit.conversation.message.viewholder.MessageViewHolderManager;
import cn.wildfire.chat.kit.conversation.message.viewholder.NormalMessageContentViewHolder;
import cn.wildfire.chat.kit.conversation.message.viewholder.NotificationMessageContentViewHolder;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class ConversationMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ConversationFragment fragment;

    public static int MODE_NORMAL = 0;
    public static int MODE_CHECKABLE = 1;

    // check or normal
    private int mode;
    private List<UiMessage> messages = new ArrayList<>();
    private Map<String, Long> deliveries;
    private Map<String, Long> readEntries;
    private OnPortraitClickListener onPortraitClickListener;
    private OnMessageCheckListener onMessageCheckListener;
    private OnPortraitLongClickListener onPortraitLongClickListener;
    private OnMessageReceiptClickListener onMessageReceiptClickListener;

    public ConversationMessageAdapter(ConversationFragment fragment) {
        super();
        this.fragment = fragment;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void clearMessageCheckStatus() {
        if (messages == null) {
            return;
        }
        for (UiMessage message : messages) {
            message.isChecked = false;
        }
    }

    public List<UiMessage> getCheckedMessages() {
        List<UiMessage> checkedMessages = new ArrayList<>();
        if (this.messages != null) {
            for (UiMessage msg : this.messages) {
                if (msg.isChecked) {
                    checkedMessages.add(msg);
                }
            }
        }
        return checkedMessages;
    }

    public List<UiMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<UiMessage> messages) {
        this.messages = messages;
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
    }

    public void setDeliveries(Map<String, Long> deliveries) {
        // TODO diff
        this.deliveries = deliveries;
        notifyDataSetChanged();
    }

    public void setReadEntries(Map<String, Long> readEntries) {
        // TODO diff
        this.readEntries = readEntries;
        notifyDataSetChanged();
    }

    public Map<String, Long> getDeliveries() {
        return deliveries;
    }

    public Map<String, Long> getReadEntries() {
        return readEntries;
    }

    public void setOnPortraitClickListener(OnPortraitClickListener onPortraitClickListener) {
        this.onPortraitClickListener = onPortraitClickListener;
    }

    public void setOnMessageCheckListener(OnMessageCheckListener onMessageCheckListener) {
        this.onMessageCheckListener = onMessageCheckListener;
    }

    public void setOnPortraitLongClickListener(OnPortraitLongClickListener onPortraitLongClickListener) {
        this.onPortraitLongClickListener = onPortraitLongClickListener;
    }

    public void setOnMessageReceiptClickListener(OnMessageReceiptClickListener onMessageReceiptClickListener) {
        this.onMessageReceiptClickListener = onMessageReceiptClickListener;
    }

    public void addNewMessage(UiMessage message) {
        if (message == null) {
            return;
        }
        if (contains(message)) {
            updateMessage(message);
            return;
        }
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    public void addMessagesAtHead(List<UiMessage> newMessages) {
        if (newMessages == null || newMessages.isEmpty()) {
            return;
        }
        this.messages.addAll(0, newMessages);
        notifyItemRangeInserted(0, newMessages.size());
    }

    public void addMessagesAtTail(List<UiMessage> newMessages) {
        if (newMessages == null || newMessages.isEmpty()) {
            return;
        }
        int insertStartPosition = this.messages.size();
        this.messages.addAll(newMessages);
        notifyItemRangeInserted(insertStartPosition, newMessages.size());
    }

    public void updateMessage(UiMessage message) {
        int index = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (message.message.messageUid > 0) {
                // 聊天室消息收到的消息
                if (messages.get(i).message.messageUid == message.message.messageUid) {
                    messages.set(i, message);
                    index = i;
                    break;
                }
            } else if (message.message.messageId > 0) {
                if (messages.get(i).message.messageId == message.message.messageId) {
                    messages.set(i, message);
                    index = i;
                    break;
                }
            }
        }
        if (index > -1) {
            notifyItemChanged(index);
        }
    }

    public void removeMessage(UiMessage message) {
        if (message == null || messages == null || messages.isEmpty()) {
            return;
        }
        UiMessage msg;
        int position = -1;
        for (int i = 0; i < messages.size(); i++) {
            msg = messages.get(i);

            if (msg.message.messageUid > 0 || message.message.messageUid > 0) {
                if (msg.message.messageUid == message.message.messageUid) {
                    messages.remove(msg);
                    position = i;
                    break;
                }
            } else {
                if (msg.message.messageId == message.message.messageId) {
                    messages.remove(msg);
                    position = i;
                    break;
                }
            }
        }
        if (position >= 0) {
            notifyItemRemoved(position);
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof MessageContentViewHolder) {
            ((MessageContentViewHolder) holder).onViewRecycled();
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == R.layout.conversation_item_loading) {
            View view = LayoutInflater.from(fragment.getContext()).inflate(R.layout.conversation_item_loading, parent, false);
            return new LoadingViewHolder(view);
        }

        int direction = viewType >> 24;
        int messageType = viewType & 0x7FFFFF;
        Class<? extends MessageContentViewHolder> viewHolderClazz = MessageViewHolderManager.getInstance().getMessageContentViewHolder(messageType);

        int sendResId = MessageViewHolderManager.getInstance().sendLayoutResId(messageType);
        int receiveResId = MessageViewHolderManager.getInstance().receiveLayoutResId(messageType);

        View itemView;
        ViewStub viewStub;
        if (NotificationMessageContentViewHolder.class.isAssignableFrom(viewHolderClazz)) {
            itemView = LayoutInflater.from(fragment.getContext()).inflate(R.layout.conversation_item_notification_containr, parent, false);
            viewStub = itemView.findViewById(R.id.contentViewStub);
            viewStub.setLayoutResource(direction == 0 ? sendResId : receiveResId);
        } else {
            if (direction == 0) {
                itemView = LayoutInflater.from(fragment.getContext()).inflate(R.layout.conversation_item_message_container_send, parent, false);
                viewStub = itemView.findViewById(R.id.contentViewStub);
                viewStub.setLayoutResource(sendResId > 0 ? sendResId : R.layout.conversation_item_unknown_send);
            } else {
                itemView = LayoutInflater.from(fragment.getContext()).inflate(R.layout.conversation_item_message_container_receive, parent, false);
                viewStub = itemView.findViewById(R.id.contentViewStub);
                viewStub.setLayoutResource(receiveResId > 0 ? receiveResId : R.layout.conversation_item_unknown_receive);
            }
        }
        try {
            View view = viewStub.inflate();
            if (view instanceof ImageView) {
                ((ImageView) view).setImageDrawable(null);
            }
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("webview")) {
                Toast.makeText(fragment.getContext(), "请安装: Android System WebView", Toast.LENGTH_SHORT).show();
            }
        }

        try {
            Constructor constructor = viewHolderClazz.getConstructor(ConversationFragment.class, RecyclerView.Adapter.class, View.class);
            MessageContentViewHolder viewHolder = (MessageContentViewHolder) constructor.newInstance(fragment, this, itemView);
            if (viewHolder instanceof NotificationMessageContentViewHolder) {
                return viewHolder;
            }
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

    private static class ContextMenuItemWrapper {
        MessageContextMenuItem contextMenuItem;
        Method method;

        public ContextMenuItemWrapper(MessageContextMenuItem contextMenuItem, Method method) {
            this.contextMenuItem = contextMenuItem;
            this.method = method;
        }
    }

    private void setOnLongClickListenerForAllClickableChildView(View view, View.OnLongClickListener listener) {
        if (view instanceof ViewGroup) {
            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
                setOnLongClickListenerForAllClickableChildView(((ViewGroup) view).getChildAt(i), listener);
            }
        }
        if (view.isClickable()) {
            view.setOnLongClickListener(listener);
        }
    }

    private void processPortraitClick(MessageContentViewHolder viewHolder, View itemView) {
        itemView.findViewById(R.id.portraitImageView).setOnClickListener(v -> {
            if (onPortraitClickListener != null) {
                int position = viewHolder.getAdapterPosition();
                UiMessage message = getItem(position);
                // FIXME: 2019/2/15 getUserInfo可能返回null
                onPortraitClickListener.onPortraitClick(ChatManager.Instance().getUserInfo(message.message.sender, false));
            }
        });
    }

    public void onGroupMessageReceiptClick(Message message) {
        if (onMessageReceiptClickListener != null) {
            onMessageReceiptClickListener.onMessageReceiptCLick(message);
        }
    }

    private void processCheckClick(MessageContentViewHolder holder, View itemView) {
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = holder.getAdapterPosition();
                UiMessage message = getItem(position);
                message.isChecked = !message.isChecked;
                CheckBox checkBox = itemView.findViewById(R.id.checkbox);
                checkBox.setChecked(message.isChecked);
                if (onMessageCheckListener != null) {
                    onMessageCheckListener.onMessageCheck(message, message.isChecked);
                }
                notifyItemChanged(position);
            }
        });
    }

    private void processPortraitLongClick(MessageContentViewHolder viewHolder, View itemView) {
        itemView.findViewById(R.id.portraitImageView).setOnLongClickListener(v -> {
                if (onPortraitLongClickListener != null) {
                    int position = viewHolder.getAdapterPosition();
                    UiMessage message = getItem(position);
                    onPortraitLongClickListener.onPortraitLongClick(ChatManager.Instance().getUserInfo(message.message.sender, false));
                    return true;
                }
                return false;
            }
        );
    }

    /**
     * 和{@link Class#getDeclaredMethods()}类似，但包括父类方法
     *
     * @param clazz
     * @return
     */
    private List<Method> getDeclaredMethodsEx(Class clazz) {
        List<Method> methods = new ArrayList<>();
        if (MessageContentViewHolder.class.isAssignableFrom(clazz)) {
            Method[] m = clazz.getDeclaredMethods();
            methods.addAll(Arrays.asList(m));

            methods.addAll(getDeclaredMethodsEx(clazz.getSuperclass()));
        }
        return methods;
    }

    // refer to https://stackoverflow.com/questions/21217397/android-issue-with-onclicklistener-and-onlongclicklistener?noredirect=1&lq=1
    private void processContentLongClick(Class<? extends MessageContentViewHolder> viewHolderClazz, MessageContentViewHolder viewHolder, View itemView) {
        if (!viewHolderClazz.isAnnotationPresent(EnableContextMenu.class)) {
            return;
        }
        View.OnLongClickListener listener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                List<Method> allMethods = getDeclaredMethodsEx(viewHolderClazz);
                List<ContextMenuItemWrapper> contextMenus = new ArrayList<>();
                for (final Method method : allMethods) {
                    if (method.isAnnotationPresent(MessageContextMenuItem.class)) {
                        contextMenus.add(new ContextMenuItemWrapper(method.getAnnotation(MessageContextMenuItem.class), method));
                    }
                }

                if (contextMenus.isEmpty()) {
                    return false;
                }

                int position = viewHolder.getAdapterPosition();
                UiMessage message = getItem(position);
                Iterator<ContextMenuItemWrapper> iterator = contextMenus.iterator();
                MessageContextMenuItem item;
                while (iterator.hasNext()) {
                    item = iterator.next().contextMenuItem;
                    if (viewHolder.contextMenuItemFilter(message, item.tag())) {
                        iterator.remove();
                    }
                }

                if (contextMenus.isEmpty()) {
                    return false;
                }

                Collections.sort(contextMenus, (o1, o2) -> o1.contextMenuItem.priority() - o2.contextMenuItem.priority());
                List<String> titles = new ArrayList<>(contextMenus.size());
                for (ContextMenuItemWrapper itemWrapper : contextMenus) {
                    if (itemWrapper.contextMenuItem.titleResId() != 0) {
                        titles.add(fragment.getString(itemWrapper.contextMenuItem.titleResId()));
                    } else {
                        titles.add(itemWrapper.contextMenuItem.title());
                    }
                }
                new MaterialDialog.Builder(fragment.getContext()).items(titles).itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View v, int position, CharSequence text) {
                        try {
                            ContextMenuItemWrapper menuItem = contextMenus.get(position);
                            if (menuItem.contextMenuItem.confirm()) {
                                String content;
                                if (menuItem.contextMenuItem.confirmPromptResId() != 0) {
                                    content = fragment.getString(menuItem.contextMenuItem.confirmPromptResId());
                                } else {
                                    content = menuItem.contextMenuItem.confirmPrompt();
                                }
                                new MaterialDialog.Builder(fragment.getContext())
                                    .content(content)
                                    .negativeText("取消")
                                    .positiveText("确认")
                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                            try {
                                                menuItem.method.invoke(viewHolder, itemView, message);
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
                                contextMenus.get(position).method.invoke(viewHolder, itemView, message);
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
        View contentLayout = itemView.findViewById(R.id.contentFrameLayout);
        contentLayout.setOnLongClickListener(listener);
        setOnLongClickListenerForAllClickableChildView(contentLayout, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MessageContentViewHolder) {
            MessageContentViewHolder viewHolder = (MessageContentViewHolder) holder;
            ((MessageContentViewHolder) holder).onBind(getItem(position), position);
            MessageItemView itemView = (MessageItemView) holder.itemView;
            CheckBox checkBox = itemView.findViewById(R.id.checkbox);
            if (checkBox == null) {
                return;
            }
            itemView.setCheckable(getMode() == MODE_CHECKABLE);
            if (getMode() == MODE_CHECKABLE) {
                checkBox.setVisibility(View.VISIBLE);
                UiMessage message = getItem(position);
                checkBox.setChecked(message.isChecked);
            } else {
                checkBox.setVisibility(View.GONE);
            }

            if (getMode() == MODE_CHECKABLE) {
                processCheckClick(viewHolder, itemView);
            } else {
                processContentLongClick(viewHolder.getClass(), viewHolder, itemView);
                if (holder instanceof NormalMessageContentViewHolder) {
                    processPortraitClick(viewHolder, itemView);
                    processPortraitLongClick(viewHolder, itemView);
                }
            }
        } else {
            // bottom loading progress bar, do nothing
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
    }

    // 返回message type + message direction
    @Override
    public int getItemViewType(int position) {
        if (getItem(position) == null) {
            return R.layout.conversation_item_loading;
        }
        Message msg = getItem(position).message;
        return msg.direction.value() << 24 | msg.content.getType();
    }

    @Override
    public int getItemCount() {
        return messages == null ? 0 : messages.size();
    }

    public void showLoadingNewMessageProgressBar() {
        if (messages == null) {
            return;
        }

        messages.add(null);
        notifyItemInserted(messages.size() - 1);
    }

    public void dismissLoadingNewMessageProgressBar() {
        if (messages == null || messages.isEmpty() || messages.get(messages.size() - 1) != null) {
            return;
        }
        int position = messages.size() - 1;
        messages.remove(position);
        notifyItemRemoved(position);
    }

    public int getMessagePosition(long messageId) {
        if (messages == null) {
            return -1;
        }
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).message.messageId == messageId) {
                return i;
            }
        }
        return -1;
    }

    public UiMessage getItem(int position) {
        return messages.get(position);
    }

    public void highlightFocusMessage(int position) {
        messages.get(position).isFocus = true;
        notifyItemChanged(position);
    }

    private boolean contains(UiMessage message) {
        for (UiMessage msg : messages) {
            // 消息发送成功之前，messageUid都是0
            if (message.message.messageId > 0) {
                if (msg.message.messageId == message.message.messageId) {
                    return true;
                }
                // 聊天室里面，由于消息不存储，messageId都是0
            } else if (message.message.messageUid > 0) {
                if (msg.message.messageUid == message.message.messageUid) {
                    return true;
                }
            }
        }
        return false;
    }

    public interface OnPortraitClickListener {
        void onPortraitClick(UserInfo userInfo);
    }

    public interface OnPortraitLongClickListener {
        void onPortraitLongClick(UserInfo userInfo);
    }

    public interface OnMessageCheckListener {
        void onMessageCheck(UiMessage uiMessage, boolean checked);
    }

    public interface OnMessageReceiptClickListener {
        void onMessageReceiptCLick(Message message);
    }
}
