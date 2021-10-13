/*
 * Copyright (c) 2021 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.ptt;

import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProviders;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.widget.FixedTextInputEditText;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class CreatePttChannelActivity extends WfcBaseActivity {
    @BindView(R.id.pttTitleTextInputEditText)
    FixedTextInputEditText titleEditText;
    @BindView(R.id.pttDescTextInputEditText)
    FixedTextInputEditText descEditText;

    @BindView(R.id.createPttBtn)
    Button createButton;

    private String title;
    private String desc;

    @Override
    protected int contentLayout() {
        return R.layout.av_ptt_create_activity;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        UserViewModel userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        UserInfo userInfo = userViewModel.getUserInfo(ChatManager.Instance().getUserId(), false);
        if (userInfo != null) {
            titleEditText.setText(userInfo.displayName + "的对讲频道");
        } else {
            titleEditText.setText("频道");
        }
        descEditText.setText("欢迎参加");
    }


    @OnTextChanged(value = R.id.pttTitleTextInputEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void pttTitle(Editable editable) {
        this.title = editable.toString();
        if (!TextUtils.isEmpty(title) && !TextUtils.isEmpty(desc)) {
            createButton.setEnabled(true);
        } else {
            createButton.setEnabled(false);
        }
    }

    @OnTextChanged(value = R.id.pttDescTextInputEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void pttDesc(Editable editable) {
        this.desc = editable.toString();
        if (!TextUtils.isEmpty(desc) && !TextUtils.isEmpty(title)) {
            createButton.setEnabled(true);
        } else {
            createButton.setEnabled(false);
        }
    }

    @OnClick(R.id.createPttBtn)
    public void onClickCreateBtn() {
        String title = titleEditText.getText().toString();
        String desc = descEditText.getText().toString();
        PttChannelInfo info = new PttChannelInfo();
        info.setOwner(ChatManager.Instance().getUserId());
        info.setChannelTitle(title);
        info.setPin("1234");
        info.setChannelDesc(desc);
        AppService.Instance().createPttChannel(info, new SimpleCallback<String>() {
            @Override
            public void onUiSuccess(String response) {
                try {
                    JSONObject object = new JSONObject(response);
                    if (object.optInt("code", -1) == 0) {
                        String channelId = object.getString("result");
                        AVEngineKit.CallSession session = AVEngineKit.Instance().joinPttChannel(channelId, false, info.getPin(), info.getOwner(), info.getChannelTitle(), null);
                        if (session != null) {
                            Intent intent = new Intent(CreatePttChannelActivity.this, PttActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(CreatePttChannelActivity.this, "加入对讲失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                    finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(CreatePttChannelActivity.this, "创建对讲频道失败", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onUiFailure(int code, String msg) {
                Toast.makeText(CreatePttChannelActivity.this, "创建对讲频道失败", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
