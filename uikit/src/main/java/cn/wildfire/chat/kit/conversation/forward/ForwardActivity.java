/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.forward;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.pick.PickOrCreateConversationActivity;
import cn.wildfire.chat.kit.conversation.pick.PickOrCreateConversationFragment;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;

public class ForwardActivity extends PickOrCreateConversationActivity {
    private List<Message> messages;
    private ForwardViewModel forwardViewModel;

    private LinearLayout multiSelectActionLayout;
    private TextView selectedCountTextView;
    private Button sendButton;

    @Override
    protected void afterViews() {
        super.afterViews();
        messages = getIntent().getParcelableArrayListExtra("messages");
        if (messages == null || messages.isEmpty()) {
            Message message = getIntent().getParcelableExtra("message");
            if (message != null) {
                messages = new ArrayList<>();
                messages.add(message);
            }
        }
        if (messages == null || messages.isEmpty()) {
            finish();
        }
        forwardViewModel =new ViewModelProvider(this).get(ForwardViewModel.class);

        multiSelectActionLayout = findViewById(R.id.multiSelectActionLayout);
        selectedCountTextView = findViewById(R.id.selectedCountTextView);
        sendButton = findViewById(R.id.sendButton);

        sendButton.setOnClickListener(v -> handleMultiSelectForward());

        setOnConversationRemovedListener(conversation -> {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.containerFrameLayout);
            if (fragment instanceof PickOrCreateConversationFragment) {
                PickOrCreateConversationFragment pickFragment = (PickOrCreateConversationFragment) fragment;
                pickFragment.toggleConversationSelection(conversation);
            }
        });

        new Handler(Looper.getMainLooper()).post(() -> setupFragmentListener());
    }

    private void setupFragmentListener() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.containerFrameLayout);
        if (fragment instanceof PickOrCreateConversationFragment) {
            PickOrCreateConversationFragment pickFragment = (PickOrCreateConversationFragment) fragment;
            pickFragment.setOnSelectionChangedListener(count -> {
                runOnUiThread(() -> {
                    selectedCountTextView.setText(getString(R.string.selected_count_format, count));
                    sendButton.setText(getString(R.string.send_with_count, count));
                    sendButton.setEnabled(count > 0);

                    List<ConversationInfo> selected = pickFragment.getSelectedConversations();
                    updateSelectedAvatars(selected);
                });
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.forward, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menuMultiSelect) {
            toggleMultiSelectMode();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem multiSelectItem = menu.findItem(R.id.menuMultiSelect);
        if (multiSelectItem != null) {
            multiSelectItem.setTitle(isMultiSelectMode ? R.string.cancel_multi_select : R.string.multi_select);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private void toggleMultiSelectMode() {
        isMultiSelectMode = !isMultiSelectMode;
        setMultiSelectMode(isMultiSelectMode);

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.containerFrameLayout);
        if (fragment instanceof PickOrCreateConversationFragment) {
            PickOrCreateConversationFragment pickFragment = (PickOrCreateConversationFragment) fragment;
            pickFragment.setMultiSelectMode(isMultiSelectMode);
        }

        multiSelectActionLayout.setVisibility(isMultiSelectMode ? LinearLayout.VISIBLE : LinearLayout.GONE);

        if (!isMultiSelectMode) {
            updateSelectedAvatars(null);
        }

        invalidateOptionsMenu();
    }

    @Override
    protected void onSearchResultClicked(Conversation conversation, String name, String portrait) {
        if (!isMultiSelectMode) {
            super.onSearchResultClicked(conversation, name, portrait);
            return;
        }

        // 退出搜索
        clearSearch();

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.containerFrameLayout);
        if (fragment instanceof PickOrCreateConversationFragment) {
            PickOrCreateConversationFragment pickFragment = (PickOrCreateConversationFragment) fragment;
            ConversationInfo info = new ConversationInfo();
            info.conversation = conversation;

            // 如果是搜索结果，暂时把头像存起来，以便在已选列表中显示
            String key = conversation.type + "_" + conversation.target;
            tempPortraitMap.put(key, portrait);

            pickFragment.toggleConversationSelection(info);
        }
    }

    @Override
    protected void onPickOrCreateConversation(Conversation conversation) {
        if (isMultiSelectMode) {
            return;
        }
        forward(conversation);
    }

    private void handleMultiSelectForward() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.containerFrameLayout);
        if (!(fragment instanceof PickOrCreateConversationFragment)) {
            return;
        }

        PickOrCreateConversationFragment pickFragment = (PickOrCreateConversationFragment) fragment;
        List<ConversationInfo> selectedConversations = pickFragment.getSelectedConversations();

        if (selectedConversations == null || selectedConversations.isEmpty()) {
            Toast.makeText(this, R.string.select_forward_target, Toast.LENGTH_SHORT).show();
            return;
        }

        List<Conversation> targetConversations = new ArrayList<>();
        for (ConversationInfo info : selectedConversations) {
            targetConversations.add(info.conversation);
        }


        ForwardBottomSheetDialogFragment fragmentDialog = ForwardBottomSheetDialogFragment.newInstance(targetConversations, messages);
        fragmentDialog.setOnSendListener(extraMessage -> {
            List<Message> msgList = new ArrayList<>(messages);
            if (!TextUtils.isEmpty(extraMessage)) {
                TextMessageContent content = new TextMessageContent(extraMessage);
                Message extraMsg = new Message();
                extraMsg.content = content;
                msgList.add(extraMsg);
            }

            forwardViewModel.forwardToMultipleTargets(targetConversations, msgList.toArray(new Message[0]))
                .observe(ForwardActivity.this, result -> {
                    if (result.isSuccess()) {
                        Toast.makeText(ForwardActivity.this, R.string.forward_success, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ForwardActivity.this, getString(R.string.forward_failed, result.getErrorCode()), Toast.LENGTH_SHORT).show();
                    }
                });
        });
        fragmentDialog.show(getSupportFragmentManager(), "forward_dialog");
    }

    public void forward(Conversation targetConversation) {
        List<Conversation> targetConversations = new ArrayList<>();
        targetConversations.add(targetConversation);

        ForwardBottomSheetDialogFragment fragmentDialog = ForwardBottomSheetDialogFragment.newInstance(targetConversations, messages);
        fragmentDialog.setOnSendListener(extraMessage -> {
            List<Message> msgList = new ArrayList<>(messages);
            if (!TextUtils.isEmpty(extraMessage)) {
                TextMessageContent content = new TextMessageContent(extraMessage);
                Message extraMsg = new Message();
                extraMsg.content = content;
                msgList.add(extraMsg);
            }
            forwardViewModel.forward(targetConversation, msgList.toArray(new Message[0]))
                .observe(ForwardActivity.this, integerOperateResult -> {
                    if (integerOperateResult.isSuccess()) {
                        Toast.makeText(ForwardActivity.this, R.string.forward_success, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ForwardActivity.this, getString(R.string.forward_failed, integerOperateResult.getErrorCode()), Toast.LENGTH_SHORT).show();
                    }
                });
        });
        fragmentDialog.show(getSupportFragmentManager(), "forward_dialog");
    }
}
