/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.forward;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.pick.PickOrCreateConversationFragment;
import cn.wildfire.chat.kit.conversation.pick.PickOrCreateConversationPageFragment;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;

/**
 * 「转发到…」整页：挑一个（或多选若干个）会话，确认后把消息发过去。
 * <p>
 * 逐行搬自 {@link ForwardActivity}，页面部分在父类
 * {@link PickOrCreateConversationPageFragment} 里，本类只剩「多选模式」与「转发」。
 * 从会话里长按消息转发、从图片预览转发、从系统分享进来，右栏里都开在当前 tab 那条栈上。
 */
public class ForwardPageFragment extends PickOrCreateConversationPageFragment {

    private static final String ARG_MESSAGES = "messages";

    private List<Message> messages;
    private ForwardViewModel forwardViewModel;

    private LinearLayout multiSelectActionLayout;
    private TextView selectedCountTextView;
    private Button sendButton;

    /**
     * 由启动 intent 造页面，供 {@link ForwardActivity} 与 {@code PaneRegistry} 共用。
     * <p>
     * 兼容两种 extra：{@code messages}（多条，多选转发/合并转发）与 {@code message}（单条）。
     * 一条都没有时返回 null —— 改造前 {@code ForwardActivity} 在这种情况下直接 finish，
     * 这里等价于「不要开这个页面」。
     */
    @Nullable
    public static ForwardPageFragment fromIntent(Intent intent) {
        if (intent == null) {
            return null;
        }
        ArrayList<Message> messages = intent.getParcelableArrayListExtra(ARG_MESSAGES);
        if (messages == null || messages.isEmpty()) {
            Message message = intent.getParcelableExtra("message");
            if (message == null) {
                return null;
            }
            messages = new ArrayList<>();
            messages.add(message);
        }
        ForwardPageFragment fragment = new ForwardPageFragment();
        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_MESSAGES, messages);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        messages = getArguments() == null ? null : getArguments().getParcelableArrayList(ARG_MESSAGES);
        forwardViewModel = new ViewModelProvider(this).get(ForwardViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        multiSelectActionLayout = view.findViewById(R.id.multiSelectActionLayout);
        selectedCountTextView = view.findViewById(R.id.selectedCountTextView);
        sendButton = view.findViewById(R.id.sendButton);
        sendButton.setOnClickListener(v -> handleMultiSelectForward());

        // 视图重建后（旋转）恢复多选态：菜单文案、底栏、计数都要跟上
        multiSelectActionLayout.setVisibility(isMultiSelectMode ? View.VISIBLE : View.GONE);
        updateMultiSelectLabels(selectedCount());
    }

    @Override
    protected void onPickFragmentReady(PickOrCreateConversationFragment fragment) {
        fragment.setOnSelectionChangedListener(count -> {
            if (getView() == null) {
                return;
            }
            updateMultiSelectLabels(count);
            updateSelectedAvatars(fragment.getSelectedConversations());
        });
    }

    private int selectedCount() {
        PickOrCreateConversationFragment fragment = pickFragment();
        List<ConversationInfo> selected = fragment == null ? null : fragment.getSelectedConversations();
        return selected == null ? 0 : selected.size();
    }

    private void updateMultiSelectLabels(int count) {
        if (selectedCountTextView == null) {
            return;
        }
        selectedCountTextView.setText(getString(R.string.selected_count_format, count));
        sendButton.setText(getString(R.string.send_with_count, count));
        sendButton.setEnabled(count > 0);
    }

    // ==================== 多选模式 ====================

    @Override
    public int pageMenu() {
        return R.menu.forward;
    }

    @Override
    public void onPreparePageMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menuMultiSelect);
        if (item != null) {
            item.setTitle(isMultiSelectMode ? R.string.cancel_multi_select : R.string.multi_select);
        }
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuMultiSelect) {
            toggleMultiSelectMode();
            return true;
        }
        return false;
    }

    private void toggleMultiSelectMode() {
        setMultiSelectMode(!isMultiSelectMode);

        PickOrCreateConversationFragment fragment = pickFragment();
        if (fragment != null) {
            fragment.setMultiSelectMode(isMultiSelectMode);
        }
        multiSelectActionLayout.setVisibility(isMultiSelectMode ? View.VISIBLE : View.GONE);
        updateMultiSelectLabels(0);

        if (!isMultiSelectMode) {
            updateSelectedAvatars(null);
        }
        // 菜单文案要在「多选/取消多选」之间切，等价于改造前的 invalidateOptionsMenu()
        WfcPageCompat.invalidatePageMenu(this);
    }

    @Override
    protected void onSearchResultClicked(Conversation conversation, String name, String portrait) {
        if (!isMultiSelectMode) {
            super.onSearchResultClicked(conversation, name, portrait);
            return;
        }
        clearSearch();

        PickOrCreateConversationFragment fragment = pickFragment();
        if (fragment == null) {
            return;
        }
        ConversationInfo info = new ConversationInfo();
        info.conversation = conversation;
        // 搜索结果不在会话列表里，先把头像存下来，已选栏才显示得出来
        tempPortraitMap.put(conversation.type + "_" + conversation.target, portrait);
        fragment.toggleConversationSelection(info);
    }

    // ==================== 转发 ====================

    @Override
    protected void onPickOrCreateConversation(Conversation conversation) {
        if (isMultiSelectMode) {
            return;
        }
        List<Conversation> targets = new ArrayList<>();
        targets.add(conversation);
        confirmAndForward(targets, false);
    }

    private void handleMultiSelectForward() {
        PickOrCreateConversationFragment fragment = pickFragment();
        List<ConversationInfo> selectedConversations = fragment == null ? null : fragment.getSelectedConversations();
        if (selectedConversations == null || selectedConversations.isEmpty()) {
            Toast.makeText(getActivity(), R.string.select_forward_target, Toast.LENGTH_SHORT).show();
            return;
        }
        List<Conversation> targets = new ArrayList<>();
        for (ConversationInfo info : selectedConversations) {
            targets.add(info.conversation);
        }
        confirmAndForward(targets, true);
    }

    /**
     * 弹确认框（可附一句留言），确认后发送。
     *
     * @param multiple true 走批量接口 {@code forwardToMultipleTargets}，false 走单目标接口 ——
     *                 与改造前一致，单目标那条路径没有被批量接口替代
     */
    private void confirmAndForward(List<Conversation> targets, boolean multiple) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        ForwardBottomSheetDialogFragment dialog =
            ForwardBottomSheetDialogFragment.newInstance(new ArrayList<>(targets), messages);
        dialog.setOnSendListener(extraMessage -> {
            List<Message> msgList = new ArrayList<>(messages);
            if (!TextUtils.isEmpty(extraMessage)) {
                Message extraMsg = new Message();
                extraMsg.content = new TextMessageContent(extraMessage);
                msgList.add(extraMsg);
            }
            Message[] array = msgList.toArray(new Message[0]);
            (multiple ? forwardViewModel.forwardToMultipleTargets(targets, array)
                : forwardViewModel.forward(targets.get(0), array))
                .observe(getViewLifecycleOwner(), result -> {
                    if (result.isSuccess()) {
                        Toast.makeText(getActivity(), R.string.forward_success, Toast.LENGTH_SHORT).show();
                        // 手机端是 finish()；右栏里把本页出栈，绝不能 finish 双栏主界面
                        WfcPageCompat.finishPage(this);
                    } else {
                        Toast.makeText(getActivity(),
                            getString(R.string.forward_failed, result.getErrorCode()), Toast.LENGTH_SHORT).show();
                    }
                });
        });
        dialog.show(getChildFragmentManager(), "forward_dialog");
    }
}
