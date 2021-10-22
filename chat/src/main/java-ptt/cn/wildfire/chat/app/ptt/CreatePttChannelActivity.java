/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.ptt;

import android.text.Editable;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProviders;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.widget.FixedTextInputEditText;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.ptt.PTTClient;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GeneralCallback2;

public class CreatePttChannelActivity extends WfcBaseActivity {
    @BindView(R.id.pttTitleTextInputEditText)
    FixedTextInputEditText titleEditText;
    @BindView(R.id.pttDescTextInputEditText)
    FixedTextInputEditText descEditText;

    @BindView(R.id.createpttBtn)
    Button createButton;

    private String title;
    private String desc;

    @Override
    protected int contentLayout() {
        return R.layout.ptt_create_activity;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        UserInfo userInfo = userViewModel.getUserInfo(ChatManager.Instance().getUserId(), false);
        if (userInfo != null) {
            titleEditText.setText(userInfo.displayName + "的对讲");
        } else {
            titleEditText.setText("对讲");
        }
        descEditText.setText("欢迎参加");
    }


    @OnTextChanged(value = R.id.pttTitleTextInputEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void pttTitleChannelName(Editable editable) {
        this.title = editable.toString();
        if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(desc)) {
            createButton.setEnabled(true);
        } else {
            createButton.setEnabled(false);
        }
    }

    @OnTextChanged(value = R.id.pttDescTextInputEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void pttDescChannelName(Editable editable) {
        this.desc = editable.toString();
        if (!TextUtils.isEmpty(desc) && !TextUtils.isEmpty(title)) {
            createButton.setEnabled(true);
        } else {
            createButton.setEnabled(false);
        }
    }

    @OnClick(R.id.createpttBtn)
    public void onClickCreateBtn() {
        String title = titleEditText.getText().toString();
        PTTClient.getInstance().createChannel(null, title, null, 0, false, 60, new GeneralCallback2() {
            @Override
            public void onSuccess(String result) {
                Toast.makeText(CreatePttChannelActivity.this, "创建对讲频道成功", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onFail(int errorCode) {
                Toast.makeText(CreatePttChannelActivity.this, "创建对讲频道失败 " + errorCode, Toast.LENGTH_LONG).show();
            }
        });
    }
}
