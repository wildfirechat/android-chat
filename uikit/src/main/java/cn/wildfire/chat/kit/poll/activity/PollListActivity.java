/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.poll.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.poll.model.Poll;
import cn.wildfire.chat.kit.poll.service.PollService;
import cn.wildfire.chat.kit.poll.service.PollServiceProvider;

/**
 * 我的投票列表页面
 * <p>
 * 参考iOS WFCUPollListViewController实现
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class PollListActivity extends WfcBaseActivity {
    
    public static final String EXTRA_GROUP_ID = "groupId";

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView emptyView;
    
    private PollAdapter adapter;
    private List<Poll> pollList = new ArrayList<>();
    private boolean isLoading = false;
    private ProgressDialog progressDialog;

    @Override
    protected int contentLayout() {
        return R.layout.activity_poll_list;
    }

    @Override
    protected void afterViews() {
        setTitle(R.string.my_polls);
        
        initViews();
        loadPolls();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 从详情页返回时刷新列表
        loadPolls();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyView = findViewById(R.id.emptyView);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PollAdapter();
        recyclerView.setAdapter(adapter);
        
        // 下拉刷新
        swipeRefreshLayout.setOnRefreshListener(this::loadPolls);
    }

    private void loadPolls() {
        if (isLoading) return;
        
        PollService service = PollServiceProvider.getInstance().getService();
        if (service == null) {
            Toast.makeText(this, R.string.poll_service_not_configured, Toast.LENGTH_SHORT).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }
        
        isLoading = true;
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.loading));
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        service.getMyPolls(new PollService.OnPollCallback<List<Poll>>() {
            @Override
            public void onSuccess(List<Poll> result) {
                runOnUiThread(() -> {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                    swipeRefreshLayout.setRefreshing(false);
                    isLoading = false;
                    
                    pollList.clear();
                    if (result != null) {
                        pollList.addAll(result);
                    }
                    adapter.notifyDataSetChanged();
                    
                    updateEmptyView();
                });
            }

            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                    swipeRefreshLayout.setRefreshing(false);
                    isLoading = false;
                    Toast.makeText(PollListActivity.this, 
                            message != null ? message : getString(R.string.network_error), 
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateEmptyView() {
        if (pollList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 结束投票
     */
    private void closePoll(Poll poll) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm)
                .setMessage(R.string.confirm_close_poll)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    doClosePoll(poll);
                })
                .show();
    }

    private void doClosePoll(Poll poll) {
        PollService service = PollServiceProvider.getInstance().getService();
        if (service == null) return;
        
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.loading));
        progressDialog.setCancelable(false);
        progressDialog.show();
        service.closePoll(poll.getPollId(), new PollService.OnPollCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                    Toast.makeText(PollListActivity.this, R.string.poll_closed, Toast.LENGTH_SHORT).show();
                    loadPolls();
                });
            }

            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                    Toast.makeText(PollListActivity.this, 
                            message != null ? message : getString(R.string.operation_failed), 
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 删除投票
     */
    private void deletePoll(Poll poll) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm)
                .setMessage(R.string.confirm_delete_poll)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    doDeletePoll(poll);
                })
                .show();
    }

    private void doDeletePoll(Poll poll) {
        PollService service = PollServiceProvider.getInstance().getService();
        if (service == null) return;
        
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.loading));
        progressDialog.setCancelable(false);
        progressDialog.show();
        service.deletePoll(poll.getPollId(), new PollService.OnPollCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                runOnUiThread(() -> {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                    Toast.makeText(PollListActivity.this, R.string.poll_deleted, Toast.LENGTH_SHORT).show();
                    loadPolls();
                });
            }

            @Override
            public void onError(int errorCode, String message) {
                runOnUiThread(() -> {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                        progressDialog = null;
                    }
                    Toast.makeText(PollListActivity.this, 
                            message != null ? message : getString(R.string.operation_failed), 
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 跳转到投票详情
     */
    private void navigateToPollDetail(Poll poll) {
        Intent intent = new Intent(this, PollDetailActivity.class);
        intent.putExtra(PollDetailActivity.EXTRA_POLL_ID, poll.getPollId());
        if (poll.getGroupId() != null) {
            intent.putExtra(PollDetailActivity.EXTRA_GROUP_ID, poll.getGroupId());
        }
        startActivity(intent);
    }

    /**
     * 格式化时间
     */
    private String formatTime(long time) {
        if (time <= 0) return "";
        return DateFormat.format("MM-dd HH:mm", time).toString();
    }

    /**
     * Poll列表Adapter
     */
    private class PollAdapter extends RecyclerView.Adapter<PollViewHolder> {

        @NonNull
        @Override
        public PollViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_poll_list, parent, false);
            return new PollViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull PollViewHolder holder, int position) {
            Poll poll = pollList.get(position);
            holder.bind(poll);
        }

        @Override
        public int getItemCount() {
            return pollList.size();
        }
    }

    /**
     * Poll列表ViewHolder
     */
    private class PollViewHolder extends RecyclerView.ViewHolder {
        
        private final ImageView typeIcon;
        private final TextView titleLabel;
        private final TextView statusLabel;
        private final TextView countLabel;
        private final TextView timeLabel;

        public PollViewHolder(@NonNull View itemView) {
            super(itemView);
            typeIcon = itemView.findViewById(R.id.typeIcon);
            titleLabel = itemView.findViewById(R.id.titleLabel);
            statusLabel = itemView.findViewById(R.id.statusLabel);
            countLabel = itemView.findViewById(R.id.countLabel);
            timeLabel = itemView.findViewById(R.id.timeLabel);
            
            // 点击跳转详情
            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    navigateToPollDetail(pollList.get(position));
                }
            });
        }

        public void bind(Poll poll) {
            titleLabel.setText(poll.getTitle());
            
            // 类型图标
            if (poll.getType() == 1) {
                typeIcon.setImageResource(R.drawable.ic_poll_single);
            } else {
                typeIcon.setImageResource(R.drawable.ic_poll_multiple);
            }
            
            // 状态标签
            boolean isEnded = (poll.getStatus() == 1 || poll.isExpired());
            if (isEnded) {
                statusLabel.setText(R.string.poll_ended);
                statusLabel.setBackgroundResource(R.drawable.shape_poll_status_ended);
            } else {
                statusLabel.setText(R.string.poll_in_progress);
                statusLabel.setBackgroundResource(R.drawable.shape_poll_status_progress);
            }
            
            // 参与人数
            countLabel.setText(getString(R.string.voter_count, poll.getVoterCount()));
            
            // 创建时间
            timeLabel.setText(formatTime(poll.getCreatedAt()));
            
            // 左滑操作（只有创建者才显示）
            if (poll.isCreator()) {
                if (isEnded) {
                    // 已结束：删除按钮
                    itemView.setOnLongClickListener(v -> {
                        deletePoll(poll);
                        return true;
                    });
                } else {
                    // 进行中：结束按钮
                    itemView.setOnLongClickListener(v -> {
                        closePoll(poll);
                        return true;
                    });
                }
            }
        }
    }
}
