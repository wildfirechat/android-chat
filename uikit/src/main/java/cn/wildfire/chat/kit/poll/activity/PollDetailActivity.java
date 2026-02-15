/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.poll.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import cn.wildfire.chat.kit.ChatManagerHolder;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.poll.model.Poll;
import cn.wildfire.chat.kit.poll.model.PollOption;
import cn.wildfire.chat.kit.poll.model.PollVoterDetail;
import cn.wildfire.chat.kit.poll.service.PollService;
import cn.wildfire.chat.kit.poll.service.PollServiceProvider;
import cn.wildfirechat.message.PollMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.UserInfo;

/**
 * 投票详情页面
 * <p>
 * 支持两种场景：
 * 1. 投票场景（从消息点击进入）: 可以参与投票
 * 2. 管理场景（从列表点击进入且是创建者）: 不可投票，显示管理按钮
 * </p>
 */
public class PollDetailActivity extends WfcBaseActivity {

    // Intent 参数
    public static final String EXTRA_POLL_ID = "pollId";
    public static final String EXTRA_GROUP_ID = "groupId";
    public static final String EXTRA_MESSAGE = "message";

    private RecyclerView recyclerView;
    private PollOptionAdapter adapter;
    private View headerView;
    private View footerView;
    private View bottomBarView;

    // Header 视图
    private ImageView creatorAvatarImageView;
    private TextView creatorNameTextView;
    private TextView statusTextView;
    private TextView titleTextView;
    private TextView descTextView;

    // Footer 视图
    private TextView footerStatusTextView;

    // 底部操作栏
    private Button exportButton;
    private Button closeButton;
    private Button deleteButton;

    // 数据
    private long pollId;
    private String groupId;
    private Message message;
    private Poll poll;
    private String currentUserId;

    // 选中的选项（投票前）
    private Set<Long> selectedOptions = new HashSet<>();

    // 菜单项
    private MenuItem submitMenuItem;
    private MenuItem forwardMenuItem;

    private RequestOptions glideOptions;

    /**
     * 从消息进入（投票场景）
     */
    public static Intent buildIntent(Context context, Message message, long pollId, String groupId) {
        Intent intent = new Intent(context, PollDetailActivity.class);
        intent.putExtra(EXTRA_MESSAGE, message);
        intent.putExtra(EXTRA_POLL_ID, pollId);
        intent.putExtra(EXTRA_GROUP_ID, groupId);
        return intent;
    }

    /**
     * 从列表进入（可能是管理场景）
     */
    public static Intent buildIntent(Context context, long pollId, String groupId) {
        Intent intent = new Intent(context, PollDetailActivity.class);
        intent.putExtra(EXTRA_POLL_ID, pollId);
        intent.putExtra(EXTRA_GROUP_ID, groupId);
        return intent;
    }

    @Override
    protected int contentLayout() {
        return R.layout.activity_poll_detail;
    }

    @Override
    protected void beforeViews() {
        super.beforeViews();
        glideOptions = new RequestOptions()
            .placeholder(R.mipmap.avatar_def)
            .transforms(new CenterCrop(), new RoundedCorners(8));
    }

    @Override
    protected void afterViews() {
        super.afterViews();

        // 获取参数
        parseIntent();

        // 初始化视图
        initViews();

        // 加载数据
        loadPollDetail();
    }

    private void parseIntent() {
        Intent intent = getIntent();
        pollId = intent.getLongExtra(EXTRA_POLL_ID, 0);
        groupId = intent.getStringExtra(EXTRA_GROUP_ID);
        message = intent.getParcelableExtra(EXTRA_MESSAGE);
        currentUserId = ChatManagerHolder.gChatManager.getUserId();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 创建 Header
        headerView = LayoutInflater.from(this).inflate(R.layout.poll_detail_header, recyclerView, false);
        creatorAvatarImageView = headerView.findViewById(R.id.creatorAvatarImageView);
        creatorNameTextView = headerView.findViewById(R.id.creatorNameTextView);
        statusTextView = headerView.findViewById(R.id.statusTextView);
        titleTextView = headerView.findViewById(R.id.titleTextView);
        descTextView = headerView.findViewById(R.id.descTextView);

        // 创建 Footer
        footerView = LayoutInflater.from(this).inflate(R.layout.poll_detail_footer, recyclerView, false);
        footerStatusTextView = footerView.findViewById(R.id.footerStatusTextView);

        // 底部操作栏
        bottomBarView = findViewById(R.id.bottomBar);
        exportButton = findViewById(R.id.exportButton);
        closeButton = findViewById(R.id.closeButton);
        deleteButton = findViewById(R.id.deleteButton);

        exportButton.setOnClickListener(v -> onExport());
        closeButton.setOnClickListener(v -> onClosePoll());
        deleteButton.setOnClickListener(v -> onDeletePoll());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_poll_detail, menu);
        submitMenuItem = menu.findItem(R.id.menu_submit);
        forwardMenuItem = menu.findItem(R.id.menu_forward);
        updateMenuState();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_submit) {
            onSubmit();
            return true;
        } else if (itemId == R.id.menu_forward) {
            onForward();
            return true;
        } else if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (message != null) {
            // 从消息点击进入，返回时关闭页面
            finish();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * 是否是管理场景（从列表进入且是创建者）
     */
    private boolean isManagerMode() {
        return poll != null && poll.isCreator() && message == null;
    }

    /**
     * 是否可以投票
     */
    private boolean canVote() {
        if (poll == null) return false;
        if (isManagerMode()) return false;
        if (poll.isHasVoted()) return false;
        if (poll.getStatus() == 1) return false;
        if (poll.isExpired()) return false;
        return true;
    }

    /**
     * 更新菜单状态
     */
    private void updateMenuState() {
        if (submitMenuItem == null || forwardMenuItem == null) return;

        if (isManagerMode()) {
            // 管理场景：显示转发按钮
            submitMenuItem.setVisible(false);
            forwardMenuItem.setVisible(true);
        } else {
            // 投票场景：根据是否可以投票显示提交按钮
            forwardMenuItem.setVisible(false);
            boolean showSubmit = canVote();
            submitMenuItem.setVisible(showSubmit);
            submitMenuItem.setEnabled(!selectedOptions.isEmpty());
        }
    }

    /**
     * 更新标题显示已选数量（多选时）
     */
    private void updateTitle() {
        if (poll != null && canVote() && poll.getType() == 2 && !selectedOptions.isEmpty()) {
            setTitle(getString(R.string.poll_selected_count, selectedOptions.size(), poll.getMaxSelect()));
        } else {
            setTitle(R.string.poll_detail_title);
        }
    }

    /**
     * 更新底部操作栏
     */
    private void updateBottomBar() {
        if (poll == null) {
            bottomBarView.setVisibility(View.GONE);
            return;
        }

        if (!isManagerMode()) {
            bottomBarView.setVisibility(View.GONE);
            return;
        }

        bottomBarView.setVisibility(View.VISIBLE);

        boolean isAnonymous = poll.getAnonymous() != 0;
        boolean isEnded = poll.getStatus() == 1 || poll.isExpired();

        if (isAnonymous) {
            // 匿名投票：只显示关闭/删除按钮，居中显示
            exportButton.setVisibility(View.GONE);

            if (isEnded) {
                // 已结束：显示删除按钮
                closeButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.VISIBLE);

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) deleteButton.getLayoutParams();
                params.width = getResources().getDimensionPixelSize(R.dimen.poll_button_width_large);
                deleteButton.setLayoutParams(params);
            } else {
                // 进行中：显示关闭按钮
                closeButton.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(View.GONE);

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) closeButton.getLayoutParams();
                params.width = getResources().getDimensionPixelSize(R.dimen.poll_button_width_large);
                closeButton.setLayoutParams(params);
            }
        } else {
            // 实名投票：显示导出按钮 + 关闭/删除按钮（并排）
            exportButton.setVisibility(View.VISIBLE);

            if (isEnded) {
                closeButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.VISIBLE);
            } else {
                closeButton.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 加载投票详情
     */
    private void loadPollDetail() {
        PollService service = PollServiceProvider.getInstance().getService();
        if (service == null) {
            Toast.makeText(this, R.string.poll_service_not_available, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.loading));
        dialog.setCancelable(false);
        dialog.show();

        service.getPoll(pollId, new PollService.OnPollCallback<Poll>() {
            @Override
            public void onSuccess(Poll result) {
                dialog.dismiss();
                poll = result;
                
                // 更新本地消息（如果有变化）
                updateLocalMessageIfNeeded();
                
                runOnUiThread(() -> {
                    updateHeader();
                    updateFooter();
                    updateBottomBar();
                    updateMenuState();
                    updateTitle();
                    setupRecyclerView();
                });
            }

            @Override
            public void onError(int errorCode, String message) {
                dialog.dismiss();
                runOnUiThread(() -> {
                    Toast.makeText(PollDetailActivity.this,
                        message != null ? message : getString(R.string.load_failed),
                        Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    /**
     * 如果本地消息内容有变化，更新本地消息
     * <p>
     * 参考 iOS 实现：获取投票详情后，如果内容有变化，更新本地消息并刷新 UI
     */
    private void updateLocalMessageIfNeeded() {
        if (message == null || !(message.content instanceof PollMessageContent)) {
            return;
        }

        PollMessageContent content = (PollMessageContent) message.content;
        boolean needUpdate = false;

        // 检查状态是否有变化
        if (poll.getStatus() != content.getStatus()) {
            content.setStatus(poll.getStatus());
            needUpdate = true;
        }

        // 检查总票数是否有变化
        if (poll.getTotalVotes() != content.getTotalVotes()) {
            content.setTotalVotes(poll.getTotalVotes());
            needUpdate = true;
        }

        // 检查是否已过期
        long now = System.currentTimeMillis();
        if (poll.getEndTime() > 0 && poll.getEndTime() < now && content.getEndTime() >= now) {
            // 投票已过期，更新状态
            content.setStatus(1);
            needUpdate = true;
        }

        // 如果有变化，更新本地消息
        if (needUpdate) {
            ChatManagerHolder.gChatManager.updateMessage(message.messageId, content);
        }
    }

    /**
     * 更新 Header 视图
     */
    private void updateHeader() {
        // 加载创建者信息
        UserInfo creatorInfo = ChatManagerHolder.gChatManager.getUserInfo(poll.getCreatorId(), false);
        if (creatorInfo != null && !TextUtils.isEmpty(creatorInfo.portrait)) {
            Glide.with(this).load(creatorInfo.portrait).apply(glideOptions).into(creatorAvatarImageView);
        } else {
            creatorAvatarImageView.setImageResource(R.mipmap.avatar_def);
        }

        // 创建者名称
        String creatorName = creatorInfo != null ?
            (!TextUtils.isEmpty(creatorInfo.displayName) ? creatorInfo.displayName : creatorInfo.name) :
            poll.getCreatorId();
        creatorNameTextView.setText(getString(R.string.poll_creator_format, creatorName));

        // 状态标签
        String remainingTime = poll.getRemainingTimeText();
        if (remainingTime != null) {
            statusTextView.setText(remainingTime);
            statusTextView.setTextColor(getResources().getColor(R.color.orange));
        } else if (poll.getStatus() == 1) {
            statusTextView.setText(R.string.poll_status_ended);
            statusTextView.setTextColor(getResources().getColor(R.color.gray));
        } else {
            statusTextView.setText("");
        }

        // 标题
        titleTextView.setText(poll.getTitle());

        // 描述
        if (!TextUtils.isEmpty(poll.getDesc())) {
            descTextView.setText(poll.getDesc());
            descTextView.setVisibility(View.VISIBLE);
        } else {
            descTextView.setVisibility(View.GONE);
        }
    }

    /**
     * 更新 Footer 视图
     */
    private void updateFooter() {
        List<String> statusParts = new ArrayList<>();

        // 投票类型
        statusParts.add(poll.getAnonymous() == 1 ?
            getString(R.string.poll_anonymous) : getString(R.string.poll_named));

        // 参与人数
        statusParts.add(getString(R.string.poll_voter_count, poll.getVoterCount()));

        // 剩余时间
        String remainingTime = formatRemainingTime();
        if (!TextUtils.isEmpty(remainingTime)) {
            statusParts.add(remainingTime);
        }

        // 状态
        if (poll.getStatus() == 1) {
            statusParts.add(getString(R.string.poll_status_ended));
        } else if (poll.isExpired()) {
            statusParts.add(getString(R.string.poll_expired));
        } else if (poll.isHasVoted()) {
            statusParts.add(getString(R.string.poll_already_voted));
        }

        footerStatusTextView.setText(TextUtils.join(" · ", statusParts));
    }

    /**
     * 格式化剩余时间
     */
    private String formatRemainingTime() {
        if (poll.getStatus() == 1 || poll.isExpired()) {
            return "";
        }

        if (poll.getEndTime() <= 0) {
            return getString(R.string.poll_no_deadline);
        }

        long now = System.currentTimeMillis();
        long remaining = poll.getEndTime() - now;

        if (remaining <= 0) {
            return "";
        }

        long minutes = remaining / 60000;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return getString(R.string.poll_days_left, days);
        } else if (hours > 0) {
            return getString(R.string.poll_hours_left, hours);
        } else if (minutes > 0) {
            return getString(R.string.poll_minutes_left, minutes);
        } else {
            return getString(R.string.poll_less_than_one_minute);
        }
    }

    /**
     * 设置 RecyclerView
     */
    private void setupRecyclerView() {
        adapter = new PollOptionAdapter(poll.getOptions());
        recyclerView.setAdapter(adapter);
    }

    /**
     * 提交投票
     */
    private void onSubmit() {
        if (selectedOptions.isEmpty()) {
            Toast.makeText(this, R.string.poll_please_select_option, Toast.LENGTH_SHORT).show();
            return;
        }

        PollService service = PollServiceProvider.getInstance().getService();
        if (service == null) return;

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.submitting));
        dialog.setCancelable(false);
        dialog.show();

        List<Long> optionIds = new ArrayList<>(selectedOptions);
        service.vote(pollId, optionIds, new PollService.OnPollCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                dialog.dismiss();
                runOnUiThread(() -> {
                    Toast.makeText(PollDetailActivity.this,
                        R.string.poll_vote_success, Toast.LENGTH_SHORT).show();
                    // 刷新详情
                    loadPollDetail();
                });
            }

            @Override
            public void onError(int errorCode, String message) {
                dialog.dismiss();
                runOnUiThread(() -> {
                    String errorMsg = message;
                    if (errorCode == 4005) {
                        errorMsg = getString(R.string.poll_not_in_group);
                    } else if (errorCode == 4002) {
                        errorMsg = getString(R.string.poll_already_voted_error);
                    }
                    Toast.makeText(PollDetailActivity.this,
                        errorMsg != null ? errorMsg : getString(R.string.operation_failed),
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 结束投票
     */
    private void onClosePoll() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.confirm)
            .setMessage(R.string.poll_confirm_close)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.close, (dialog, which) -> doClosePoll())
            .show();
    }

    private void doClosePoll() {
        PollService service = PollServiceProvider.getInstance().getService();
        if (service == null) return;

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.processing));
        dialog.setCancelable(false);
        dialog.show();

        service.closePoll(pollId, new PollService.OnPollCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                dialog.dismiss();
                runOnUiThread(() -> {
                    Toast.makeText(PollDetailActivity.this,
                        R.string.poll_closed_success, Toast.LENGTH_SHORT).show();
                    loadPollDetail();
                });
            }

            @Override
            public void onError(int errorCode, String message) {
                dialog.dismiss();
                runOnUiThread(() -> {
                    Toast.makeText(PollDetailActivity.this,
                        message != null ? message : getString(R.string.operation_failed),
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 删除投票
     */
    private void onDeletePoll() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.confirm)
            .setMessage(R.string.poll_confirm_delete)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete, (dialog, which) -> doDeletePoll())
            .show();
    }

    private void doDeletePoll() {
        PollService service = PollServiceProvider.getInstance().getService();
        if (service == null) return;

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.processing));
        dialog.setCancelable(false);
        dialog.show();

        service.deletePoll(pollId, new PollService.OnPollCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                dialog.dismiss();
                runOnUiThread(() -> {
                    Toast.makeText(PollDetailActivity.this,
                        R.string.poll_deleted_success, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }

            @Override
            public void onError(int errorCode, String message) {
                dialog.dismiss();
                runOnUiThread(() -> {
                    Toast.makeText(PollDetailActivity.this,
                        message != null ? message : getString(R.string.operation_failed),
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 导出投票明细
     */
    private void onExport() {
        PollService service = PollServiceProvider.getInstance().getService();
        if (service == null) return;

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.loading));
        dialog.setCancelable(false);
        dialog.show();

        service.exportPollDetails(pollId, new PollService.OnPollCallback<List<PollVoterDetail>>() {
            @Override
            public void onSuccess(List<PollVoterDetail> result) {
                dialog.dismiss();
                runOnUiThread(() -> showExportResult(result));
            }

            @Override
            public void onError(int errorCode, String message) {
                dialog.dismiss();
                runOnUiThread(() -> {
                    Toast.makeText(PollDetailActivity.this,
                        message != null ? message : getString(R.string.operation_failed),
                        Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 显示导出结果
     */
    private void showExportResult(List<PollVoterDetail> details) {
        if (details == null || details.isEmpty()) {
            Toast.makeText(this, R.string.poll_no_voter_details, Toast.LENGTH_SHORT).show();
            return;
        }

        // 生成 CSV 内容
        StringBuilder csv = new StringBuilder();
        // UTF-8 BOM
        csv.append("\uFEFF");
        csv.append(getString(R.string.poll_csv_option)).append(",")
           .append(getString(R.string.poll_csv_user)).append(",")
           .append(getString(R.string.poll_csv_time)).append("\n");

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        for (PollVoterDetail detail : details) {
            String timeStr = formatter.format(new Date(detail.getCreatedAt()));
            csv.append(escapeCsv(detail.getOptionText())).append(",")
               .append(escapeCsv(detail.getUserName())).append(",")
               .append(timeStr).append("\n");
        }

        // 生成安全的文件名
        String safeTitle = safeFileName(poll.getTitle());
        String fileName = safeTitle + "_" + getString(R.string.poll_details_suffix) + ".csv";

        // 写入文件
        File cacheDir = getCacheDir();
        File csvFile = new File(cacheDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(csvFile);
            fos.write(csv.toString().getBytes("UTF-8"));
            fos.close();
        } catch (IOException e) {
            Toast.makeText(this, R.string.poll_export_failed, Toast.LENGTH_SHORT).show();
            return;
        }

        // 分享文件
        shareCsvFile(csvFile);
    }

    /**
     * 转义 CSV 字段
     */
    private String escapeCsv(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    /**
     * 生成安全的文件名
     */
    private String safeFileName(String fileName) {
        if (TextUtils.isEmpty(fileName)) {
            return getString(R.string.poll_default_file_name);
        }

        // 替换文件系统中的非法字符
        String safeName = fileName.replaceAll("[/\\\\?%*|\"<>]", "_");

        // 限制文件名长度
        if (safeName.length() > 50) {
            safeName = safeName.substring(0, 50);
        }

        return safeName.isEmpty() ? getString(R.string.poll_default_file_name) : safeName;
    }

    /**
     * 分享 CSV 文件
     */
    private void shareCsvFile(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    /**
     * 转发投票
     */
    private void onForward() {
        if (poll == null) return;

        // 构建投票消息内容
        PollMessageContent content = new PollMessageContent();
        content.setPollId(String.valueOf(poll.getPollId()));
        content.setGroupId(poll.getGroupId());
        content.setCreatorId(poll.getCreatorId());
        content.setTitle(poll.getTitle());
        content.setDesc(poll.getDesc());
        content.setVisibility(poll.getVisibility());
        content.setType(poll.getType());
        content.setAnonymous(poll.getAnonymous());
        content.setStatus(poll.getStatus());
        content.setEndTime(poll.getEndTime());
        content.setTotalVotes(poll.getTotalVotes());

        // 构建消息对象
        cn.wildfirechat.message.Message forwardMessage = new cn.wildfirechat.message.Message();
        forwardMessage.content = content;
        forwardMessage.conversation = new cn.wildfirechat.model.Conversation(
            cn.wildfirechat.model.Conversation.ConversationType.Group, 
            poll.getGroupId()
        );

        // 跳转到转发页面
        Intent intent = new Intent(this, cn.wildfire.chat.kit.conversation.forward.ForwardActivity.class);
        intent.putExtra("message", forwardMessage);
        startActivity(intent);
    }

    /**
     * 选项适配器
     */
    private class PollOptionAdapter extends RecyclerView.Adapter<PollOptionAdapter.ViewHolder> {

        private List<PollOption> options;

        public PollOptionAdapter(List<PollOption> options) {
            this.options = options;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_poll_vote_option, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(options.get(position), position);
        }

        @Override
        public int getItemCount() {
            return options == null ? 0 : options.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView optionTextView;
            TextView checkmarkTextView;
            TextView percentTextView;
            View progressBarView;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                optionTextView = itemView.findViewById(R.id.optionTextView);
                checkmarkTextView = itemView.findViewById(R.id.checkmarkTextView);
                percentTextView = itemView.findViewById(R.id.percentTextView);
                progressBarView = itemView.findViewById(R.id.progressBarView);
            }

            void bind(PollOption option, int position) {
                // 选项文字
                optionTextView.setText(option.getOptionText());

                // 是否显示结果
                boolean showResult = poll.shouldShowResult();

                // 判断是否已投票/已结束/管理场景
                boolean hasVoted = !canVote();

                // 选中的选项
                long optionId = option.getOptionId();
                boolean isSelected = selectedOptions.contains(optionId);
                boolean isMyVote = poll.getMyOptionIds() != null &&
                    poll.getMyOptionIds().contains((int) optionId);

                // 显示百分比和进度条
                if (showResult) {
                    percentTextView.setVisibility(View.VISIBLE);
                    percentTextView.setText(getString(R.string.poll_vote_count_format,
                        option.getVotePercent(), option.getVoteCount()));
                    progressBarView.setVisibility(View.VISIBLE);
                    ViewGroup.LayoutParams params = progressBarView.getLayoutParams();
                    params.width = (int) (itemView.getWidth() * option.getVotePercent() / 100.0);
                    progressBarView.setLayoutParams(params);
                } else {
                    percentTextView.setVisibility(View.GONE);
                    progressBarView.setVisibility(View.GONE);
                }

                // 选中状态
                if (isMyVote) {
                    // 已投票选项：对勾紧跟选项
                    checkmarkTextView.setVisibility(View.VISIBLE);
                    checkmarkTextView.setTextColor(getResources().getColor(R.color.blue));
                    optionTextView.setTextColor(getResources().getColor(R.color.blue));
                    optionTextView.post(() -> {
                        int textWidth = optionTextView.getMeasuredWidth();
                        checkmarkTextView.setX(optionTextView.getX() +
                            Math.min(textWidth, optionTextView.getPaint().measureText(option.getOptionText())));
                    });
                } else if (isSelected && !hasVoted) {
                    // 投票前选中：对勾在最右侧
                    checkmarkTextView.setVisibility(View.VISIBLE);
                    checkmarkTextView.setTextColor(getResources().getColor(R.color.blue));
                    optionTextView.setTextColor(getResources().getColor(R.color.black));
                    checkmarkTextView.setX(itemView.getWidth() - checkmarkTextView.getWidth() - 16);
                } else if (isSelected && hasVoted) {
                    // 已投票后选中：对勾紧跟选项
                    checkmarkTextView.setVisibility(View.VISIBLE);
                    checkmarkTextView.setTextColor(getResources().getColor(R.color.blue));
                    optionTextView.setTextColor(getResources().getColor(R.color.black));
                    optionTextView.post(() -> {
                        int textWidth = optionTextView.getMeasuredWidth();
                        checkmarkTextView.setX(optionTextView.getX() +
                            Math.min(textWidth, optionTextView.getPaint().measureText(option.getOptionText())));
                    });
                } else {
                    checkmarkTextView.setVisibility(View.GONE);
                    optionTextView.setTextColor(getResources().getColor(R.color.black));
                }

                // 点击事件
                if (canVote()) {
                    itemView.setOnClickListener(v -> {
                        if (poll.getType() == 1) {
                            // 单选
                            selectedOptions.clear();
                            selectedOptions.add(optionId);
                        } else {
                            // 多选
                            if (selectedOptions.contains(optionId)) {
                                selectedOptions.remove(optionId);
                            } else {
                                // 检查最大选择数
                                if (poll.getMaxSelect() > 0 &&
                                    selectedOptions.size() >= poll.getMaxSelect()) {
                                    Toast.makeText(PollDetailActivity.this,
                                        getString(R.string.poll_max_select_limit, poll.getMaxSelect()),
                                        Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                selectedOptions.add(optionId);
                            }
                        }
                        notifyDataSetChanged();
                        updateMenuState();
                        updateTitle();
                    });
                } else {
                    itemView.setOnClickListener(null);
                }
            }
        }
    }
}
