/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.poll.activity;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.conversation.forward.ForwardActivity;
import cn.wildfirechat.message.PollMessageContent;
import cn.wildfire.chat.kit.poll.model.Poll;
import cn.wildfire.chat.kit.poll.service.PollService;
import cn.wildfire.chat.kit.poll.service.PollServiceProvider;
import cn.wildfirechat.model.Conversation;

/**
 * 创建投票Activity
 * <p>
 * 与iOS WFCUCreatePollViewController对应
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class CreatePollActivity extends WfcBaseActivity {

    public static final String EXTRA_CONVERSATION = "conversation";

    private static final int MIN_OPTIONS = 2;
    private static final int MAX_OPTIONS = 10;

    private Conversation conversation;

    // 视图
    private EditText titleEditText;
    private EditText descEditText;
    private LinearLayout optionsContainer;
    private TextView addOptionButton;
    private TextView pollTypeText;
    private View pollTypeLayout;
    private Switch anonymousSwitch;
    private Switch showResultSwitch;
    private TextView endTimeText;
    private View endTimeLayout;
    private TextView visibilityText;
    private View visibilityLayout;
    private Button publishButton;

    // 数据
    private List<String> options = new ArrayList<>();
    private int type = 1;           // 1=单选, 2=多选
    private int maxSelect = 1;      // 多选时最多选几项
    private int anonymous = 0;      // 0=实名, 1=匿名
    private long endTime = 0;       // 截止时间（毫秒时间戳，0表示无截止时间）
    private int visibility = 1;     // 1=仅群内, 2=公开
    private int showResult = 0;     // 0=投票前隐藏, 1=始终显示

    private ProgressDialog progressDialog;

    @Override
    protected int contentLayout() {
        return R.layout.activity_create_poll;
    }

    @Override
    protected void afterViews() {
        conversation = getIntent().getParcelableExtra(EXTRA_CONVERSATION);
        if (conversation == null || conversation.type != Conversation.ConversationType.Group) {
            Toast.makeText(this, R.string.poll_only_for_group, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        initData();
        setupListeners();
    }

    private void initViews() {
        titleEditText = findViewById(R.id.titleEditText);
        descEditText = findViewById(R.id.descEditText);
        optionsContainer = findViewById(R.id.optionsContainer);
        addOptionButton = findViewById(R.id.addOptionButton);
        pollTypeText = findViewById(R.id.pollTypeText);
        pollTypeLayout = findViewById(R.id.pollTypeLayout);
        anonymousSwitch = findViewById(R.id.anonymousSwitch);
        showResultSwitch = findViewById(R.id.showResultSwitch);
        endTimeText = findViewById(R.id.endTimeText);
        endTimeLayout = findViewById(R.id.endTimeLayout);
        visibilityText = findViewById(R.id.visibilityText);
        visibilityLayout = findViewById(R.id.visibilityLayout);
        publishButton = findViewById(R.id.publishButton);
    }

    private void initData() {
        // 默认两个空选项
        options.add("");
        options.add("");
        refreshOptionsView();
        updatePollTypeText();
        updateEndTimeText();
        updateVisibilityText();
    }

    private void setupListeners() {
        // 添加选项按钮
        addOptionButton.setOnClickListener(v -> {
            if (options.size() >= MAX_OPTIONS) {
                Toast.makeText(this, getString(R.string.max_options_limit, MAX_OPTIONS), Toast.LENGTH_SHORT).show();
                return;
            }
            options.add("");
            refreshOptionsView();
        });

        // 投票类型选择
        pollTypeLayout.setOnClickListener(v -> showPollTypeDialog());

        // 匿名投票开关
        anonymousSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            anonymous = isChecked ? 1 : 0;
        });

        // 始终显示结果开关
        showResultSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showResult = isChecked ? 1 : 0;
        });

        // 截止时间选择
        endTimeLayout.setOnClickListener(v -> showEndTimePicker());

        // 可见性选择
        visibilityLayout.setOnClickListener(v -> showVisibilityDialog());

        // 发布按钮
        publishButton.setOnClickListener(v -> createPoll());

        // 标题输入监听，用于验证
        titleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    /**
     * 刷新选项列表视图
     */
    private void refreshOptionsView() {
        optionsContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < options.size(); i++) {
            final int index = i;
            View optionView = inflater.inflate(R.layout.item_poll_option, optionsContainer, false);
            
            EditText optionEditText = optionView.findViewById(R.id.optionEditText);
            ImageView deleteButton = optionView.findViewById(R.id.deleteButton);

            // 设置选项文本
            optionEditText.setText(options.get(i));
            
            // 设置提示文字
            optionEditText.setHint(getString(R.string.option_hint, i + 1));

            // 文本变化监听
            optionEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    options.set(index, s.toString());
                }
            });

            // 删除按钮
            if (options.size() <= MIN_OPTIONS) {
                deleteButton.setVisibility(View.GONE);
            } else {
                deleteButton.setVisibility(View.VISIBLE);
                deleteButton.setOnClickListener(v -> {
                    options.remove(index);
                    refreshOptionsView();
                });
            }

            optionsContainer.addView(optionView);
        }

        // 更新添加按钮状态
        addOptionButton.setEnabled(options.size() < MAX_OPTIONS);
        addOptionButton.setAlpha(options.size() >= MAX_OPTIONS ? 0.5f : 1.0f);
    }

    /**
     * 显示投票类型选择对话框
     */
    private void showPollTypeDialog() {
        String[] items = {getString(R.string.single_choice), getString(R.string.multiple_choice_simple)};
        new AlertDialog.Builder(this)
            .setTitle(R.string.select_poll_type)
            .setItems(items, (dialog, which) -> {
                type = which + 1; // 1=单选, 2=多选
                if (type == 1) {
                    maxSelect = 1;
                } else {
                    maxSelect = options.size(); // 多选默认最多选所有选项
                }
                updatePollTypeText();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void updatePollTypeText() {
        if (type == 1) {
            pollTypeText.setText(R.string.single_choice);
        } else {
            pollTypeText.setText(getString(R.string.multiple_choice, maxSelect));
        }
    }

    /**
     * 显示截止时间选择器
     */
    private void showEndTimePicker() {
        Calendar calendar = Calendar.getInstance();
        
        // 如果已设置截止时间，使用已设置的时间
        if (endTime > 0) {
            calendar.setTimeInMillis(endTime);
        } else {
            // 默认设置为24小时后
            calendar.add(Calendar.HOUR, 24);
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, 
            (view, year, month, dayOfMonth) -> {
                Calendar selectedCalendar = Calendar.getInstance();
                selectedCalendar.set(year, month, dayOfMonth);
                
                // 显示时间选择器
                new TimePickerDialog(this,
                    (timeView, hourOfDay, minute) -> {
                        selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedCalendar.set(Calendar.MINUTE, minute);
                        selectedCalendar.set(Calendar.SECOND, 0);
                        selectedCalendar.set(Calendar.MILLISECOND, 0);
                        
                        // 验证时间必须大于当前时间
                        if (selectedCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
                            Toast.makeText(this, R.string.end_time_must_be_future, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        endTime = selectedCalendar.getTimeInMillis();
                        updateEndTimeText();
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );

        // 设置最小日期为当前日期
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        
        // 添加清除截止时间的选项
        datePickerDialog.setButton(DatePickerDialog.BUTTON_NEUTRAL, getString(R.string.clear_end_time), (dialog, which) -> {
            endTime = 0;
            updateEndTimeText();
        });
        
        datePickerDialog.setTitle(R.string.select_end_time);
        datePickerDialog.show();
    }

    private void updateEndTimeText() {
        if (endTime > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
            endTimeText.setText(sdf.format(new Date(endTime)));
        } else {
            endTimeText.setText(R.string.no_end_time);
        }
    }

    /**
     * 显示可见性选择对话框
     */
    private void showVisibilityDialog() {
        String[] items = {getString(R.string.group_only), getString(R.string.public_poll)};
        new AlertDialog.Builder(this)
            .setTitle(R.string.select_visibility)
            .setItems(items, (dialog, which) -> {
                visibility = which + 1; // 1=仅群内, 2=公开
                updateVisibilityText();
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void updateVisibilityText() {
        if (visibility == 1) {
            visibilityText.setText(R.string.group_only);
        } else {
            visibilityText.setText(R.string.public_poll);
        }
    }

    /**
     * 创建投票
     */
    private void createPoll() {
        // 验证标题
        String title = titleEditText.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            titleEditText.setError(getString(R.string.poll_title_required));
            titleEditText.requestFocus();
            return;
        }

        // 过滤空选项
        List<String> validOptions = new ArrayList<>();
        for (String option : options) {
            String trimmed = option.trim();
            if (!TextUtils.isEmpty(trimmed)) {
                validOptions.add(trimmed);
            }
        }

        if (validOptions.size() < MIN_OPTIONS) {
            Toast.makeText(this, getString(R.string.poll_options_required, MIN_OPTIONS), Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取描述
        String description = descEditText.getText().toString().trim();

        // 检查服务是否可用
        PollService service = PollServiceProvider.getInstance().getService();
        if (service == null) {
            Toast.makeText(this, R.string.poll_service_not_configured, Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示加载中
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.publishing));
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 调用服务创建投票
        service.createPoll(
            conversation.target,  // groupId
            title,
            description,
            validOptions,
            visibility,
            type,
            maxSelect,
            anonymous,
            endTime,
            showResult,
            new PollService.OnPollCallback<Poll>() {
                @Override
                public void onSuccess(Poll poll) {
                    runOnUiThread(() -> {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        
                        // 根据可见性显示不同的提示
                        if (visibility == 2) {
                            // 公开投票：提示是否转发
                            showForwardDialog(poll);
                        } else {
                            // 群内投票：提示已发送到本群
                            Toast.makeText(CreatePollActivity.this, R.string.poll_sent_to_group, Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }

                @Override
                public void onError(int errorCode, String message) {
                    runOnUiThread(() -> {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(CreatePollActivity.this, 
                            getString(R.string.poll_create_failed, TextUtils.isEmpty(message) ? getString(R.string.network_error) : message), 
                            Toast.LENGTH_SHORT).show();
                    });
                }
            }
        );
    }

    /**
     * 显示转发对话框（公开投票）
     */
    private void showForwardDialog(Poll poll) {
        new AlertDialog.Builder(this)
            .setTitle(R.string.poll_created)
            .setMessage(R.string.forward_public_poll_tip)
            .setPositiveButton(R.string.forward, (dialog, which) -> {
                // 打开转发页面
                forwardPoll(poll);
            })
            .setNegativeButton(R.string.later, (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }

    /**
     * 转发投票
     */
    private void forwardPoll(Poll poll) {
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
        cn.wildfirechat.message.Message message = new cn.wildfirechat.message.Message();
        message.content = content;
        message.conversation = conversation;

        // 跳转到转发页面
        Intent intent = new Intent(this, ForwardActivity.class);
        intent.putExtra("message", message);
        startActivity(intent);
        finish();
    }
}
