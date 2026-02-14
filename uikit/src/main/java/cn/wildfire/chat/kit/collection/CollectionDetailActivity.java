/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.collection;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.collection.model.Collection;
import cn.wildfire.chat.kit.collection.model.CollectionEntry;
import cn.wildfirechat.message.CollectionMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 接龙详情Activity
 * <p>
 * 与iOS WFCUCollectionDetailViewController对应
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class CollectionDetailActivity extends WfcBaseActivity {

    public static final String EXTRA_MESSAGE = "message";

    private Message message;
    private long collectionId;
    private String groupId;
    private String currentUserId;

    private Collection collection;
    private boolean hasJoined = false;
    private boolean isCreator = false;
    private int myEntryIndex = -1;
    private String myEntryContent = null;

    private RecyclerView recyclerView;
    private EntryAdapter adapter;
    private MenuItem submitMenuItem;

    // Header views
    private ImageView creatorAvatarView;
    private TextView creatorLabel;
    private TextView titleLabel;
    private TextView descLabel;
    private TextView templateLabel;

    private CollectionService collectionService;

    @Override
    protected int contentLayout() {
        return R.layout.activity_collection_detail;
    }

    @Override
    protected int menu() {
        return R.menu.collection_detail_menu;
    }

    @Override
    protected void afterViews() {
        message = getIntent().getParcelableExtra(EXTRA_MESSAGE);
        if (message == null) {
            finish();
            return;
        }

        CollectionMessageContent content = (CollectionMessageContent) message.content;
        collectionId = Long.parseLong(content.getCollectionId());
        groupId = message.conversation.target;
        currentUserId = ChatManager.Instance().getUserId();

        collectionService = CollectionServiceProvider.getInstance().getService();
        if (collectionService == null) {
            Toast.makeText(this, R.string.collection_service_not_configured, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        fetchCollectionDetail();
    }

    @Override
    protected void afterMenus(Menu menu) {
        submitMenuItem = menu.findItem(R.id.menu_submit);
        updateSubmitButtonState();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create header
        View headerView = LayoutInflater.from(this).inflate(R.layout.collection_detail_header, recyclerView, false);
        creatorAvatarView = headerView.findViewById(R.id.creatorAvatarView);
        creatorLabel = headerView.findViewById(R.id.creatorLabel);
        titleLabel = headerView.findViewById(R.id.titleLabel);
        descLabel = headerView.findViewById(R.id.descLabel);
        templateLabel = headerView.findViewById(R.id.templateLabel);

        adapter = new EntryAdapter(headerView);
        recyclerView.setAdapter(adapter);
    }

    private void fetchCollectionDetail() {
        collectionService.getCollection(collectionId, groupId,
                new CollectionService.GetCollectionCallback() {
                    @Override
                    public void onSuccess(Collection result) {
                        runOnUiThread(() -> updateWithCollection(result));
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(CollectionDetailActivity.this,
                                    message != null ? message : getString(R.string.collection_join_failed),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void updateWithCollection(Collection collection) {
        this.collection = collection;

        // Check join status
        hasJoined = false;
        myEntryIndex = -1;
        myEntryContent = null;

        for (int i = 0; i < collection.getEntries().size(); i++) {
            CollectionEntry entry = collection.getEntries().get(i);
            if (currentUserId.equals(entry.getUserId())) {
                hasJoined = true;
                myEntryIndex = i;
                myEntryContent = entry.getContent();
                break;
            }
        }

        // Check if creator
        isCreator = currentUserId.equals(collection.getCreatorId());

        refreshUI();

        // Auto fill display name and focus after delay
        recyclerView.postDelayed(() -> {
            scrollToMyEntry();

            // Fill display name if content is empty
            if (!hasJoined || TextUtils.isEmpty(myEntryContent)) {
                UserInfo userInfo = ChatManager.Instance().getUserInfo(currentUserId, false);
                String displayName = userInfo != null ? (userInfo.displayName != null ? userInfo.displayName : userInfo.name) : currentUserId;
                if (adapter.editCell != null) {
                    adapter.editCell.setAutoFillContent(displayName + " ");
                }
            }

            // Focus edit cell
            if (adapter.editCell != null) {
                adapter.editCell.focusTextField();
            }

            updateSubmitButtonState();
        }, 500);
    }

    private void refreshUI() {
        if (collection == null) return;

        // Creator info
        UserInfo creatorInfo = ChatManager.Instance().getUserInfo(collection.getCreatorId(), false);
        String creatorName = creatorInfo != null ? (creatorInfo.displayName != null ? creatorInfo.displayName : creatorInfo.name) : collection.getCreatorId();
        creatorLabel.setText(String.format(getString(R.string.collection_creator_info), creatorName, collection.getParticipantCount()));

        // Load avatar
        if (creatorInfo != null && creatorInfo.portrait != null) {
            Glide.with(this).load(creatorInfo.portrait).placeholder(R.mipmap.avatar_def).into(creatorAvatarView);
        } else {
            creatorAvatarView.setImageResource(R.mipmap.avatar_def);
        }

        // Title
        titleLabel.setText(collection.getTitle());

        // Desc
        if (!TextUtils.isEmpty(collection.getDesc())) {
            descLabel.setText(collection.getDesc());
            descLabel.setVisibility(View.VISIBLE);
        } else {
            descLabel.setVisibility(View.GONE);
        }

        // Template
        if (!TextUtils.isEmpty(collection.getTemplate())) {
            templateLabel.setText(getString(R.string.collection_template) + ": " + collection.getTemplate());
            templateLabel.setVisibility(View.VISIBLE);
        } else {
            templateLabel.setVisibility(View.GONE);
        }

        adapter.notifyDataSetChanged();
        updateSubmitButtonState();
    }

    private void scrollToMyEntry() {
        if (myEntryIndex >= 0 && collection.getStatus() == 0) {
            recyclerView.smoothScrollToPosition(myEntryIndex + 1); // +1 for header
        }
    }

    private void updateSubmitButtonState() {
        if (submitMenuItem == null || adapter.editCell == null) return;

        boolean hasChanged = adapter.editCell.hasContentChanged();
        String currentContent = adapter.editCell.getContent().trim();

        // Can submit if content changed, or if joined and content is empty (delete)
        boolean canSubmit = hasChanged || (hasJoined && currentContent.isEmpty() && !TextUtils.isEmpty(myEntryContent));

        submitMenuItem.setEnabled(canSubmit);
    }

    private void onSubmit() {
        if (adapter.editCell == null) return;

        String content = adapter.editCell.getContent().trim();

        if (hasJoined) {
            // Already joined
            if (content.isEmpty()) {
                // Delete entry
                confirmDeleteEntry();
            } else {
                // Update entry
                updateEntry(content);
            }
        } else {
            // New join
            if (content.isEmpty()) {
                Toast.makeText(this, R.string.collection_join_hint, Toast.LENGTH_SHORT).show();
                return;
            }
            joinCollection(content);
        }
    }

    private void joinCollection(String content) {
        collectionService.joinCollection(collectionId, groupId, content,
                new CollectionService.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(CollectionDetailActivity.this, R.string.collection_join_success, Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(CollectionDetailActivity.this,
                                    message != null ? message : getString(R.string.collection_join_failed),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void updateEntry(String content) {
        collectionService.joinCollection(collectionId, groupId, content,
                new CollectionService.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(CollectionDetailActivity.this, R.string.collection_join_success, Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(CollectionDetailActivity.this,
                                    message != null ? message : getString(R.string.collection_join_failed),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void confirmDeleteEntry() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm)
                .setMessage(R.string.confirm_delete_entry)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> doDeleteEntry())
                .show();
    }

    private void doDeleteEntry() {
        collectionService.deleteCollectionEntry(collectionId, groupId,
                new CollectionService.OperationCallback() {
                    @Override
                    public void onSuccess() {
                        runOnUiThread(() -> {
                            Toast.makeText(CollectionDetailActivity.this, R.string.collection_delete_success, Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }

                    @Override
                    public void onError(int errorCode, String message) {
                        runOnUiThread(() -> {
                            Toast.makeText(CollectionDetailActivity.this,
                                    message != null ? message : getString(R.string.delete_failed),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_submit) {
            onSubmit();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 参与记录Adapter
     */
    private class EntryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ENTRY = 1;
        private static final int TYPE_EDIT = 2;

        private View headerView;
        EditViewHolder editCell;

        EntryAdapter(View headerView) {
            this.headerView = headerView;
        }

        @Override
        public int getItemViewType(int position) {
            if (position == 0) return TYPE_HEADER;

            if (collection == null || collection.getStatus() != 0) {
                // Not active, show normal entry
                return TYPE_ENTRY;
            }

            // Active
            if (hasJoined) {
                // Joined: show edit at my index
                return (position - 1 == myEntryIndex) ? TYPE_EDIT : TYPE_ENTRY;
            } else {
                // Not joined: show edit at last row
                return (position == getItemCount() - 1) ? TYPE_EDIT : TYPE_ENTRY;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == TYPE_HEADER) {
                return new HeaderViewHolder(headerView);
            } else if (viewType == TYPE_EDIT) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_collection_edit, parent, false);
                editCell = new EditViewHolder(view);
                return editCell;
            } else {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_collection_entry, parent, false);
                return new EntryViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof HeaderViewHolder) {
                // Header already bound
            } else if (holder instanceof EntryViewHolder) {
                int entryIndex = position - 1;
                CollectionEntry entry = collection.getEntries().get(entryIndex);
                ((EntryViewHolder) holder).bind(entryIndex + 1, entry.getContent());
            } else if (holder instanceof EditViewHolder) {
                String placeholder = !TextUtils.isEmpty(collection.getTemplate())
                        ? collection.getTemplate()
                        : getString(R.string.collection_join_hint);
                String content = hasJoined ? myEntryContent : "";
                int displayIndex = hasJoined ? myEntryIndex + 1 : collection.getEntries().size() + 1;

                ((EditViewHolder) holder).bind(displayIndex, placeholder, content);
            }
        }

        @Override
        public int getItemCount() {
            if (collection == null) return 1; // Header only

            int count = 1; // Header

            if (collection.getStatus() != 0) {
                // Not active, only show entries
                count += collection.getEntries().size();
            } else {
                // Active
                if (hasJoined) {
                    count += collection.getEntries().size(); // Replace my entry with edit
                } else {
                    count += collection.getEntries().size() + 1; // Add edit row
                }
            }

            return count;
        }
    }

    private static class HeaderViewHolder extends RecyclerView.ViewHolder {
        HeaderViewHolder(View itemView) {
            super(itemView);
        }
    }

    private static class EntryViewHolder extends RecyclerView.ViewHolder {
        TextView indexLabel;
        TextView contentLabel;

        EntryViewHolder(View itemView) {
            super(itemView);
            indexLabel = itemView.findViewById(R.id.indexLabel);
            contentLabel = itemView.findViewById(R.id.contentLabel);
        }

        void bind(int index, String content) {
            indexLabel.setText(String.valueOf(index));
            contentLabel.setText(content);
        }
    }

    private class EditViewHolder extends RecyclerView.ViewHolder {
        TextView indexLabel;
        EditText contentEditText;
        String originalContent;
        String autoFillContent;

        EditViewHolder(View itemView) {
            super(itemView);
            indexLabel = itemView.findViewById(R.id.indexLabel);
            contentEditText = itemView.findViewById(R.id.contentEditText);

            contentEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    updateSubmitButtonState();
                }
            });
        }

        void bind(int index, String placeholder, String content) {
            indexLabel.setText(String.valueOf(index));
            contentEditText.setHint(placeholder);
            contentEditText.setText(content != null ? content : "");
            originalContent = content != null ? content : "";
            // 为编辑框添加边框背景（不包含序号）
            contentEditText.setBackgroundResource(R.drawable.shape_collection_edit_border);
            contentEditText.setPadding(12, 8, 12, 8);
        }

        void setAutoFillContent(String content) {
            if (TextUtils.isEmpty(contentEditText.getText())) {
                contentEditText.setText(content);
                autoFillContent = content;
            }
        }

        String getContent() {
            return contentEditText.getText().toString();
        }

        boolean hasContentChanged() {
            String current = getContent().trim();
            return !current.equals(originalContent) && !current.equals(autoFillContent != null ? autoFillContent.trim() : "");
        }

        void focusTextField() {
            contentEditText.requestFocus();
            // 将光标移到最后
            contentEditText.post(() -> {
                contentEditText.setSelection(contentEditText.getText().length());
            });
        }
    }
}
