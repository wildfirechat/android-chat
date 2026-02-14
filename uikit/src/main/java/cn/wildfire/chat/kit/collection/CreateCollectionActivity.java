/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.collection;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.Calendar;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.collection.model.Collection;
import cn.wildfirechat.model.Conversation;

/**
 * 创建接龙Activity
 * <p>
 * 与iOS WFCUCreateCollectionViewController对应
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class CreateCollectionActivity extends WfcBaseActivity {

    public static final String EXTRA_CONVERSATION = "conversation";

    private Conversation conversation;

    private EditText titleEditText;
    private EditText descEditText;
    private EditText templateEditText;
    private RadioGroup expireTypeGroup;
    private RadioButton radioNoExpire;
    private RadioButton radioSetExpire;
    private LinearLayout expirePickerContainer;
    private DatePicker expireDatePicker;
    private TimePicker expireTimePicker;

    private int expireType = 0; // 0=无限期，1=有限期
    private long expireAt = 0;
    private MenuItem doneMenuItem;
    private ProgressDialog progressDialog;

    @Override
    protected int contentLayout() {
        return R.layout.activity_create_collection;
    }

    @Override
    protected int menu() {
        return R.menu.create_collection_menu;
    }

    @Override
    protected void afterViews() {
        conversation = getIntent().getParcelableExtra(EXTRA_CONVERSATION);
        if (conversation == null || conversation.type != Conversation.ConversationType.Group) {
            Toast.makeText(this, R.string.collection_only_for_group, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupListeners();
        updateDoneButtonState();

        // 设置日期选择器的最小日期为当前时间
        Calendar minCalendar = Calendar.getInstance();
        expireDatePicker.setMinDate(minCalendar.getTimeInMillis());

        // 默认设置过期时间为24小时后
        Calendar defaultCalendar = Calendar.getInstance();
        defaultCalendar.add(Calendar.HOUR, 24);
        expireDatePicker.updateDate(
            defaultCalendar.get(Calendar.YEAR),
            defaultCalendar.get(Calendar.MONTH),
            defaultCalendar.get(Calendar.DAY_OF_MONTH)
        );
        expireTimePicker.setHour(defaultCalendar.get(Calendar.HOUR_OF_DAY));
        expireTimePicker.setMinute(defaultCalendar.get(Calendar.MINUTE));
    }

    @Override
    protected void afterMenus(Menu menu) {
        doneMenuItem = menu.findItem(R.id.menu_done);
        updateDoneButtonState();
    }

    private void initViews() {
        titleEditText = findViewById(R.id.titleEditText);
        descEditText = findViewById(R.id.descEditText);
        templateEditText = findViewById(R.id.templateEditText);
        expireTypeGroup = findViewById(R.id.expireTypeGroup);
        radioNoExpire = findViewById(R.id.radioNoExpire);
        radioSetExpire = findViewById(R.id.radioSetExpire);
        expirePickerContainer = findViewById(R.id.expirePickerContainer);
        expireDatePicker = findViewById(R.id.expireDatePicker);
        expireTimePicker = findViewById(R.id.expireTimePicker);
    }

    private void setupListeners() {
        // 标题输入监听，用于更新完成按钮状态
        titleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateDoneButtonState();
            }
        });

        // 过期类型选择监听
        expireTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radioNoExpire) {
                expireType = 0;
                expirePickerContainer.setVisibility(View.GONE);
            } else if (checkedId == R.id.radioSetExpire) {
                expireType = 1;
                expirePickerContainer.setVisibility(View.VISIBLE);
            }
        });

        // 点击背景收起键盘
        findViewById(R.id.toolbar).setOnClickListener(v -> hideInputMethod());
    }

    private void updateDoneButtonState() {
        boolean hasTitle = !TextUtils.isEmpty(titleEditText.getText().toString().trim());
        if (doneMenuItem != null) {
            doneMenuItem.setEnabled(hasTitle);
        }
    }

    private void createCollection() {
        String title = titleEditText.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            titleEditText.setError(getString(R.string.collection_title_hint));
            return;
        }

        String desc = descEditText.getText().toString().trim();
        String template = templateEditText.getText().toString().trim();

        // 计算过期时间
        expireAt = 0;
        if (expireType == 1) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, expireDatePicker.getYear());
            calendar.set(Calendar.MONTH, expireDatePicker.getMonth());
            calendar.set(Calendar.DAY_OF_MONTH, expireDatePicker.getDayOfMonth());
            calendar.set(Calendar.HOUR_OF_DAY, expireTimePicker.getHour());
            calendar.set(Calendar.MINUTE, expireTimePicker.getMinute());
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            expireAt = calendar.getTimeInMillis();

            // 验证过期时间必须大于当前时间
            if (expireAt <= System.currentTimeMillis()) {
                Toast.makeText(this, R.string.expire_time_invalid, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 获取服务实现
        CollectionService service = CollectionServiceProvider.getInstance().getService();
        if (service == null) {
            Toast.makeText(this, R.string.collection_service_not_configured, Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示加载中
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.collection_creating));
        progressDialog.setCancelable(false);
        progressDialog.show();

        service.createCollection(conversation.target, title, desc, template, expireType, expireAt, 0,
            new CollectionService.CreateCollectionCallback() {
                @Override
                public void onSuccess(Collection collection) {
                    // 创建成功，后端会自动发送接龙消息，客户端不需要主动发送
                    runOnUiThread(() -> {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(CreateCollectionActivity.this, R.string.collection_create_success, Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onError(int errorCode, String message) {
                    runOnUiThread(() -> {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(CreateCollectionActivity.this,
                            getString(R.string.collection_create_failed) + ": " + message,
                            Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_done) {
            createCollection();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
