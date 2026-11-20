/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.afollestad.materialdialogs.MaterialDialog;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContextMenuItem;
import cn.wildfire.chat.kit.conversation.message.MessageItemView;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfire.chat.kit.conversation.message.viewholder.ContextableNotificationMessageContentViewHolder;
import cn.wildfire.chat.kit.conversation.message.viewholder.LoadingViewHolder;
import cn.wildfire.chat.kit.conversation.message.viewholder.MessageContentViewHolder;
import cn.wildfire.chat.kit.conversation.message.viewholder.MessageViewHolderManager;
import cn.wildfire.chat.kit.conversation.message.viewholder.NormalMessageContentViewHolder;
import cn.wildfire.chat.kit.conversation.message.viewholder.NotificationMessageContentViewHolder;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.agent.AgentAnswerMessageContent;
import cn.wildfirechat.message.agent.AgentApprovalMessageContent;
import cn.wildfirechat.message.agent.AgentApprovalResultMessageContent;
import cn.wildfirechat.message.agent.AgentGoalMessageContent;
import cn.wildfirechat.message.agent.AgentQuestionMessageContent;
import cn.wildfirechat.message.agent.AgentTaskProgressMessageContent;
import cn.wildfirechat.message.StreamingTextGeneratedMessageContent;
import cn.wildfirechat.message.StreamingTextGeneratingMessageContent;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.uikit.menu.PopupMenu;

public class ConversationMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final String TAG = "ConvMsgAdapter";
    private ConversationFragment fragment;

    public static int MODE_NORMAL = 0;
    public static int MODE_CHECKABLE = 1;

    private int mode;
    private List<UiMessage> messages = new ArrayList<>();
    private Map<String, Long> readEntries;
    private OnPortraitClickListener onPortraitClickListener;
    private OnMessageCheckListener onMessageCheckListener;
    private OnPortraitLongClickListener onPortraitLongClickListener;
    private OnMessageReceiptClickListener onMessageReceiptClickListener;

    // 消息高亮
    private long highlightMessageId = 0;

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

    public long getOldestMessageUid() {
        if (this.messages == null || this.messages.isEmpty()) {
            return Long.MAX_VALUE;
        }
        return this.messages.get(0).message.messageUid;
    }

    public long getOldestMessageId() {
        if (this.messages == null || this.messages.isEmpty()) {
            return Long.MAX_VALUE;
        }
        long id = this.messages.get(0).message.messageId;
        // 以下是新消息 那条提示消息的 messageId 是 Long.MAX_VALUE
        if (id == Long.MAX_VALUE && this.messages.size() > 1) {
            id = this.messages.get(1).message.messageId;
        }
        return id;
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
        // 加载历史消息时按 streamId 归一化去重，避免切换会话后同一 AI 回复的多条「生成中(14)」重复显示
        messages = normalizeStreamingMessages(messages);
        if (messages != null && !messages.isEmpty()) {
            this.messages = messages;
        } else {
            this.messages = new ArrayList<>();
        }
        // 整体重设数据后按时间排序（临时移除 AI 元素钉底，验证纯时间顺序效果）
        sortMessagesByTime();
    }

    public void setReadEntries(Map<String, Long> readEntries) {
        // TODO diff
        this.readEntries = readEntries;
        notifyDataSetChanged();
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

    public void setHighlightMessage(long messageId) {
        this.highlightMessageId = messageId;
        notifyDataSetChanged();
    }

    public void clearHighlight() {
        this.highlightMessageId = 0;
        notifyDataSetChanged();
    }

    public void addNewMessage(UiMessage message) {
        if (message == null) {
            return;
        }
        // 201 回答 / 203 审批结果是 Transparent 的本地动作回执：发送端本地回显也不要
        // 进入消息列表，否则会出现额外气泡；卡片状态由机器人 updateMessage 更新。
        if (message.message.content instanceof AgentAnswerMessageContent
            || message.message.content instanceof AgentApprovalResultMessageContent) {
            return;
        }
        int index = contains(message);
        if (index >= 0) {
            updateMessage(index, message);
            return;
        }
        // 服务端更新卡片（200/202）时个别情况下会以“新消息”回调到达；按 qid/aid
        // 找到原卡片并原地更新，避免聊天里再出现一条问题/审批卡片。
        int cardIndex = containsAgentCard(message);
        if (cardIndex >= 0) {
            updateMessage(cardIndex, message);
            return;
        }
        {
            if (!messages.isEmpty() && messages.get(0).message.serverTime > message.message.serverTime) {
                Log.d(TAG, "msg timestamp < the first msg's timestamp, maybe update old message content, ignore");
                return;
            }
        }
        messages.add(message);
        notifyItemInserted(messages.size() - 1);
        // 收到新消息/自己发送消息追加后，按时间重新排序
        sortMessagesByTime();
    }

    public void addMessagesAtHead(List<UiMessage> newMessages) {
        if (newMessages == null || newMessages.isEmpty()) {
            return;
        }
        List<UiMessage> filteredMsgs = new ArrayList<>();
        for (UiMessage m : newMessages) {
            if (m.message.messageId != 0 && indexOfMessage(m) == -1) {
                filteredMsgs.add(m);
            }
        }
        // 向上翻页加载更早历史消息时，同样按 streamId 归一化去重（与 setMessages 一致）
        newMessages = normalizeStreamingMessages(filteredMsgs);
        if (newMessages.isEmpty()) {
            return;
        }
        this.messages.addAll(0, newMessages);
        notifyItemRangeInserted(0, newMessages.size());
        sortMessagesByTime();
    }

    public void addMessagesAtTail(List<UiMessage> newMessages) {
        if (newMessages == null || newMessages.isEmpty()) {
            return;
        }
        int insertStartPosition = this.messages.size();
        this.messages.addAll(newMessages);
        notifyItemRangeInserted(insertStartPosition, newMessages.size());
        // 尾部追加（定位模式加载更新消息）后，按时间重新排序
        sortMessagesByTime();
    }

    public void updateMessage(int index, UiMessage message) {
        if (index >= 0) {
            messages.set(index, message);
            notifyItemChanged(index);
            // 消息内容/状态更新后（含流式分片更新、卡片状态流转），按时间重新排序
            sortMessagesByTime();
        }
    }

    public void updateMessage(UiMessage message) {
        int index = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (message.message.messageUid > 0) {
                // 聊天室消息收到的消息，或者消息被远程更新
                if (messages.get(i).message.messageUid == message.message.messageUid) {
                    messages.set(i, message);
                    index = i;
                    break;
                }
            }
            if (message.message.messageId > 0) {
                if (messages.get(i).message.messageId == message.message.messageId) {
                    messages.set(i, message);
                    index = i;
                    break;
                }
            }
        }
        if (index < 0) {
            // 更新事件未带可匹配的 messageId/messageUid 时，按 200 qid / 202 aid 兜底匹配
            index = containsAgentCard(message);
        }
        if (index > -1) {
            if (index != -1) messages.set(index, message);
            notifyItemChanged(index);
            // 消息内容/状态更新后（含流式分片更新、卡片状态流转），按时间重新排序
            sortMessagesByTime();
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

    public void removeMessageById(long messageId) {
        int position = -1;
        UiMessage msg;
        for (int i = 0; i < messages.size(); i++) {
            msg = messages.get(i);
            if (msg.message.messageId == messageId) {
                messages.remove(msg);
                position = i;
                break;
            }
        }
        if (position >= 0) {
            notifyItemRemoved(position);
        }
    }

    public void removeMessageByUid(long messageUid) {
        int position = -1;
        UiMessage msg;
        for (int i = 0; i < messages.size(); i++) {
            msg = messages.get(i);
            if (msg.message.messageUid == messageUid) {
                messages.remove(msg);
                position = i;
                break;
            }
        }
        if (position >= 0) {
            notifyItemRemoved(position);
        }
    }

    /**
     * 取消消息（20）处理：按 streamId 从消息列表移除正在生成(14)/已生成(15)消息，
     * 界面上的"生成中/生成完成"气泡直接消失（与 PC 端 store._removeStreamingMessage 语义一致）。
     *
     * @param streamId 被取消的流式响应 streamId
     */
    public void removeStreamingMessage(String streamId) {
        if (streamId == null || messages == null || messages.isEmpty()) {
            return;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            UiMessage uiMessage = messages.get(i);
            String msgStreamId;
            if (uiMessage.message.content instanceof StreamingTextGeneratingMessageContent) {
                msgStreamId = ((StreamingTextGeneratingMessageContent) uiMessage.message.content).getStreamId();
            } else if (uiMessage.message.content instanceof StreamingTextGeneratedMessageContent) {
                msgStreamId = ((StreamingTextGeneratedMessageContent) uiMessage.message.content).getStreamId();
            } else {
                continue;
            }
            if (TextUtils.equals(msgStreamId, streamId)) {
                messages.remove(i);
                notifyItemRemoved(i);
            }
        }
    }

    /**
     * 加载历史消息时对流式消息按 streamId 归一化去重（与 HarmonyOS 版 ConversationPage.normalizeStreaming 逻辑一致）。
     * <p>
     * 背景：AI 机器人流式回复时，会逐字推送多条同 streamId 的「生成中(14) StreamingTextGeneratingMessageContent」
     * 更新消息（每条都入库），最后推送「已生成(15) StreamingTextGeneratedMessageContent」。实时接收时已按 streamId
     * 替换/移除（见 {@link #removeStreamingMessage(String)}），但切换会话后重新加载历史消息时，数据库里同 streamId
     * 的多条消息会重复显示成多条"生成中"气泡。这里在整体设置消息列表时统一归一化：
     * <ul>
     *   <li>同 streamId 组内存在「已生成(15)」→ 丢弃组内所有「生成中(14)」，只保留最终结果；</li>
     *   <li>同 streamId 只有「生成中(14)」→ 只保留组内最后一条（最新文本）；</li>
     *   <li>非 streaming 消息全部保留；保持原有相对顺序。</li>
     * </ul>
     *
     * @param messages 待设置的消息列表（UiMessage）
     * @return 归一化去重后的消息列表；没有 streaming 消息时返回原列表引用，避免无谓拷贝
     */
    private List<UiMessage> normalizeStreamingMessages(List<UiMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return messages;
        }
        // 第一遍：按 streamId 统计「已生成」是否存在、该组最后一条「生成中」的下标
        Map<String, Boolean> hasGenerated = new HashMap<>();
        Map<String, Integer> lastGeneratingIndex = new HashMap<>();
        List<String> streamIds = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            UiMessage uiMessage = messages.get(i);
            MessageContent content = uiMessage.message.content;
            String streamId = null;
            if (content instanceof StreamingTextGeneratingMessageContent) {
                streamId = ((StreamingTextGeneratingMessageContent) content).getStreamId();
            } else if (content instanceof StreamingTextGeneratedMessageContent) {
                streamId = ((StreamingTextGeneratedMessageContent) content).getStreamId();
            }
            if (TextUtils.isEmpty(streamId)) {
                continue;
            }
            if (!hasGenerated.containsKey(streamId)) {
                hasGenerated.put(streamId, false);
                lastGeneratingIndex.put(streamId, -1);
                streamIds.add(streamId);
            }
            if (content instanceof StreamingTextGeneratedMessageContent) {
                hasGenerated.put(streamId, true);
            } else if (content instanceof StreamingTextGeneratingMessageContent) {
                lastGeneratingIndex.put(streamId, i);
            }
        }
        if (streamIds.isEmpty()) {
            return messages;
        }
        // 第二遍：过滤重复的「生成中」，非 streaming 消息与「已生成」消息原样保留
        List<UiMessage> result = new ArrayList<>(messages.size());
        for (int i = 0; i < messages.size(); i++) {
            UiMessage uiMessage = messages.get(i);
            MessageContent content = uiMessage.message.content;
            if (content instanceof StreamingTextGeneratingMessageContent) {
                String streamId = ((StreamingTextGeneratingMessageContent) content).getStreamId();
                Integer lastIndex = lastGeneratingIndex.get(streamId);
                if (Boolean.TRUE.equals(hasGenerated.get(streamId)) || lastIndex == null || lastIndex != i) {
                    continue;
                }
            }
            result.add(uiMessage);
        }
        return result;
    }

    /**
     * 按消息时间升序排序（临时行为：先取消「进行中的 AI 元素钉底」，观察纯时间顺序效果）。
     * <p>
     * 14 流式生成中、200/202 pending 卡片不再强制移到最新，所有消息统一按
     * {@link Message#serverTime} 升序；serverTime 缺失（<=0）时保持稳定顺序。
     * 列表中的 null 是底部加载占位，永远排在最后。
     * </p>
     */
    public void sortMessagesByTime() {
        if (messages == null || messages.size() <= 1) {
            return;
        }
        try {
            Collections.sort(messages, new Comparator<UiMessage>() {
                @Override
                public int compare(UiMessage a, UiMessage b) {
                    long ta = messageTime(a);
                    long tb = messageTime(b);
                    if (ta == tb) {
                        return 0;
                    }
                    return ta < tb ? -1 : 1;
                }
            });
            notifyDataSetChanged();
        } catch (Exception e) {
            Log.w(TAG, "sortMessagesByTime failed: " + e.getMessage());
        }
    }

    /** 排序时间：serverTime 有效时用它；加载占位(null)放最后。 */
    private long messageTime(UiMessage uiMessage) {
        if (uiMessage == null || uiMessage.message == null) {
            return Long.MAX_VALUE;
        }
        long time = uiMessage.message.serverTime;
        return time > 0 ? time : Long.MAX_VALUE - 1;
    }

    private int indexOfMessage(UiMessage message) {
        if (this.messages != null) {
            for (int i = 0; i < this.messages.size(); i++) {
                if (this.messages.get(i).message.messageUid == message.message.messageUid) {
                    return i;
                }
            }
        }
        return -1;
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
        Class<? extends MessageContentViewHolder> viewHolderClazz = MessageViewHolderManager.getInstance().getMessageContentViewHolder(messageType, direction);

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
                viewStub.setLayoutResource(sendResId);
            } else {
                itemView = LayoutInflater.from(fragment.getContext()).inflate(R.layout.conversation_item_message_container_receive, parent, false);
                viewStub = itemView.findViewById(R.id.contentViewStub);
                viewStub.setLayoutResource(receiveResId);
            }
        }
        try {
            View view = viewStub.inflate();
            view.setTag("messageContentView");
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
        // Don't skip text selectable TextViews anymore - we want both text selection AND context menu
        if (view.isClickable()) {
            // 华为 harmonyOS 2.0 版本，没有 hasOnLongClickListeners
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

    public PopupMenu popupMenuForMessageViewHolder(Class<? extends MessageContentViewHolder> viewHolderClazz, MessageContentViewHolder viewHolder, View itemView) {

        List<Method> allMethods = getDeclaredMethodsEx(viewHolderClazz);
        List<ContextMenuItemWrapper> contextMenus = new ArrayList<>();
        for (final Method method : allMethods) {
            if (method.isAnnotationPresent(MessageContextMenuItem.class)) {
                contextMenus.add(new ContextMenuItemWrapper(method.getAnnotation(MessageContextMenuItem.class), method));
            }
        }

        if (contextMenus.isEmpty()) {
            return null;
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
            return null;
        }

        List<Pair<Integer, String>> items = new ArrayList<>(contextMenus.size());
        Collections.sort(contextMenus, Comparator.comparingInt(o -> o.contextMenuItem.priority()));
        for (ContextMenuItemWrapper itemWrapper : contextMenus) {
            items.add(new Pair<>(viewHolder.contextMenuIcon(fragment.getContext(), itemWrapper.contextMenuItem.tag()), viewHolder.contextMenuTitle(fragment.getContext(), itemWrapper.contextMenuItem.tag())));
        }
        return new PopupMenu(fragment.getContext(), items, position1 -> {
            try {
                ContextMenuItemWrapper menuItem = contextMenus.get(position1);
                if (menuItem.contextMenuItem.confirm()) {
                    String content;
                    content = viewHolder.contextConfirmPrompt(fragment.getContext(), menuItem.contextMenuItem.tag());
                    new MaterialDialog.Builder(fragment.getContext())
                        .content(content)
                        .negativeText(R.string.delete_message_dialog_cancel)
                        .positiveText(R.string.delete_message_dialog_confirm)
                        .onPositive((dialog, which) -> {
                            try {
                                menuItem.method.invoke(viewHolder, itemView, message);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            } catch (InvocationTargetException e) {
                                e.printStackTrace();
                            }
                        })
                        .build()
                        .show();

                } else {
                    contextMenus.get(position1).method.invoke(viewHolder, itemView, message);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        });
    }

    // refer to https://stackoverflow.com/questions/21217397/android-issue-with-onclicklistener-and-onlongclicklistener?noredirect=1&lq=1
    private void processContentLongClick(Class<? extends MessageContentViewHolder> viewHolderClazz, MessageContentViewHolder viewHolder, View itemView) {
        if (!viewHolderClazz.isAnnotationPresent(EnableContextMenu.class)) {
            return;
        }
        View.OnLongClickListener listener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                PopupMenu popupMenu = popupMenuForMessageViewHolder(viewHolderClazz, viewHolder, itemView);
                if (popupMenu == null) {
                    return false;
                }
                popupMenu.showAsGridMenu(itemView.findViewWithTag("messageContentView"), 5);
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
            UiMessage uiMessage = getItem(position);
            MessageItemView itemView = (MessageItemView) holder.itemView;

            // 设置高亮
            if (highlightMessageId != 0 && uiMessage.message.messageId == highlightMessageId) {
                itemView.setBackgroundResource(R.drawable.bg_message_highlight);
            } else {
                itemView.setBackground(null);
            }

            CheckBox checkBox = itemView.findViewById(R.id.checkbox);
            if (checkBox != null) {
                if (holder instanceof NotificationMessageContentViewHolder && !(holder instanceof ContextableNotificationMessageContentViewHolder)) {
                    checkBox.setVisibility(View.GONE);
                } else {
                    itemView.setCheckable(getMode() == MODE_CHECKABLE);
                    if (getMode() == MODE_CHECKABLE) {
                        checkBox.setVisibility(View.VISIBLE);
                        UiMessage message = getItem(position);
                        checkBox.setChecked(message.isChecked);
                    } else {
                        checkBox.setVisibility(View.GONE);
                    }
                }
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
            ((MessageContentViewHolder) holder).onBind(uiMessage, position);
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
        int contentType = msg.content.getMessageContentType();
        // 正在生成的流式文本消息和已经生成完毕的流式文本消息使用同一个ViewHolder
        if (contentType == MessageContentType.ContentType_Streaming_Text_Generated) {
            contentType = MessageContentType.ContentType_Streaming_Text_Generating;
        }
        return msg.direction.value() << 24 | contentType;
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

    /** 按 200 qid / 202 aid 找原卡片（服务端更新被误当作新消息时兜底）。 */
    private int containsAgentCard(UiMessage message) {
        if (message == null || message.message == null || messages == null) {
            return -1;
        }
        MessageContent incoming = message.message.content;
        if (incoming instanceof AgentQuestionMessageContent) {
            String qid = ((AgentQuestionMessageContent) incoming).getQid();
            if (TextUtils.isEmpty(qid)) return -1;
            for (int i = 0; i < messages.size(); i++) {
                UiMessage item = messages.get(i);
                if (item != null && item.message != null
                    && item.message.content instanceof AgentQuestionMessageContent
                    && TextUtils.equals(((AgentQuestionMessageContent) item.message.content).getQid(), qid)) {
                    return i;
                }
            }
        } else if (incoming instanceof AgentApprovalMessageContent) {
            String aid = ((AgentApprovalMessageContent) incoming).getAid();
            if (TextUtils.isEmpty(aid)) return -1;
            for (int i = 0; i < messages.size(); i++) {
                UiMessage item = messages.get(i);
                if (item != null && item.message != null
                    && item.message.content instanceof AgentApprovalMessageContent
                    && TextUtils.equals(((AgentApprovalMessageContent) item.message.content).getAid(), aid)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int contains(UiMessage message) {
        int index = -1;
        for (int i = 0; i < messages.size(); i++) {
            UiMessage msg = messages.get(i);
            // 消息发送成功之前，messageUid都是0
            if (message.message.messageId > 0) {
                if (msg.message.messageId == message.message.messageId) {
                    index = i;
                    break;
                }
            }
            // 聊天室里面，由于消息不存储，messageId都是0，被远程更新的消息，也会走这儿
            if (message.message.messageUid > 0) {
                if (msg.message.messageUid == message.message.messageUid) {
                    index = i;
                    break;
                }
            }
            if (msg.message.messageId == 0
                && msg.message.content instanceof StreamingTextGeneratingMessageContent
                && (message.message.content instanceof StreamingTextGeneratingMessageContent || message.message.content instanceof StreamingTextGeneratedMessageContent)) {
                String streamId;
                if (message.message.content instanceof StreamingTextGeneratingMessageContent) {
                    streamId = ((StreamingTextGeneratingMessageContent) message.message.content).getStreamId();
                } else {
                    streamId = ((StreamingTextGeneratedMessageContent) message.message.content).getStreamId();
                }

                if (TextUtils.equals(((StreamingTextGeneratingMessageContent) msg.message.content).getStreamId(), streamId)) {
                    index = i;
                    break;
                }

            }
        }
        return index;
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
