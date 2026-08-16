/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.collection;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Calendar;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.collection.model.Collection;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfirechat.model.Conversation;

/**
 * 创建接龙页。
 * <p>
 * 逐行搬自 {@link CreateCollectionActivity}，那个类现在只是手机端的壳。入口是会话加号面板里的
 * 「接龙」，会话本身就在右栏，不迁的话点开要整屏跳出去再跳回来。
 */
public class CreateCollectionPageFragment extends Fragment implements WfcPage {

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
    private MenuItem doneMenuItem;
    private ProgressDialog progressDialog;

    public static CreateCollectionPageFragment fromIntent(Intent intent) {
        CreateCollectionPageFragment fragment = new CreateCollectionPageFragment();
        Bundle args = new Bundle();
        if (intent != null) {
            args.putParcelable(CreateCollectionActivity.EXTRA_CONVERSATION,
                intent.getParcelableExtra(CreateCollectionActivity.EXTRA_CONVERSATION));
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_create_collection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        conversation = getArguments() == null ? null
            : (Conversation) getArguments().getParcelable(CreateCollectionActivity.EXTRA_CONVERSATION);
        if (conversation == null || conversation.type != Conversation.ConversationType.Group) {
            Toast.makeText(getContext(), R.string.collection_only_for_group, Toast.LENGTH_SHORT).show();
            WfcPageCompat.finishPage(this);
            return;
        }

        initViews(view);
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
    public int pageMenu() {
        return R.menu.create_collection_menu;
    }

    @Override
    public void onPreparePageMenu(Menu menu) {
        doneMenuItem = menu.findItem(R.id.menu_done);
        updateDoneButtonState();
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_done) {
            createCollection();
            return true;
        }
        return false;
    }

    private void initViews(View view) {
        titleEditText = view.findViewById(R.id.titleEditText);
        descEditText = view.findViewById(R.id.descEditText);
        templateEditText = view.findViewById(R.id.templateEditText);
        expireTypeGroup = view.findViewById(R.id.expireTypeGroup);
        radioNoExpire = view.findViewById(R.id.radioNoExpire);
        radioSetExpire = view.findViewById(R.id.radioSetExpire);
        expirePickerContainer = view.findViewById(R.id.expirePickerContainer);
        expireDatePicker = view.findViewById(R.id.expireDatePicker);
        expireTimePicker = view.findViewById(R.id.expireTimePicker);
    }

    private void setupListeners() {
        // 标题输入监听，用于更新完成按钮状态
        titleEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateDoneButtonState();
                WfcPageCompat.invalidatePageMenu(CreateCollectionPageFragment.this);
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

        // 手机端：点击标题栏收起键盘。右栏的标题栏由 PanePageFragment 提供，这里不做
        View toolbar = getActivity() == null ? null : getActivity().findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setOnClickListener(v -> hideInputMethod());
        }
    }

    private void hideInputMethod() {
        if (getActivity() == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
        View focus = getActivity().getCurrentFocus();
        if (imm != null && focus != null) {
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
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
        long expireAt = 0;
        if (expireType == 1) {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, expireDatePicker.getYear());
            calendar.set(Calendar.MONTH, expireDatePicker.getMonth());
            calendar.set(Calendar.DAY_OF_MONTH, expireDatePicker.getDayOfMonth());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                calendar.set(Calendar.HOUR_OF_DAY, expireTimePicker.getHour());
                calendar.set(Calendar.MINUTE, expireTimePicker.getMinute());
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, expireTimePicker.getCurrentHour());
                calendar.set(Calendar.MINUTE, expireTimePicker.getCurrentMinute());
            }
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            expireAt = calendar.getTimeInMillis();

            // 验证过期时间必须大于当前时间
            if (expireAt <= System.currentTimeMillis()) {
                Toast.makeText(getContext(), R.string.expire_time_invalid, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // 获取服务实现
        CollectionService service = CollectionServiceProvider.getInstance().getService();
        if (service == null) {
            Toast.makeText(getContext(), R.string.collection_service_not_configured, Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示加载中
        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage(getString(R.string.collection_creating));
        progressDialog.setCancelable(false);
        progressDialog.show();

        service.createCollection(conversation.target, title, desc, template, expireType, expireAt, 0,
            new CollectionService.CreateCollectionCallback() {
                @Override
                public void onSuccess(Collection collection) {
                    // 创建成功，后端会自动发送接龙消息，客户端不需要主动发送
                    if (getActivity() == null) {
                        return;
                    }
                    getActivity().runOnUiThread(() -> {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(getContext(), R.string.collection_create_success, Toast.LENGTH_SHORT).show();
                        WfcPageCompat.finishPage(CreateCollectionPageFragment.this);
                    });
                }

                @Override
                public void onError(int errorCode, String message) {
                    if (getActivity() == null) {
                        return;
                    }
                    getActivity().runOnUiThread(() -> {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(getContext(),
                            getString(R.string.collection_create_failed) + ": " + message,
                            Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }
}
