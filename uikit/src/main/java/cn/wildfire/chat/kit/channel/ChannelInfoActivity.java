/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.channel;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.ChannelInfo;

public class ChannelInfoActivity extends AppCompatActivity {
    ImageView portraitImageView;
    TextView channelTextView;
    TextView channelDescTextView;
    Button followChannelButton;
    Toolbar toolbar;

    private boolean isFollowed = false;
    private ChannelViewModel channelViewModel;
    private ChannelInfo channelInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.channel_info_activity);
        bindViews();
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        init();
    }

    private void bindViews() {
        portraitImageView = findViewById(R.id.portraitImageView);
        channelTextView = findViewById(R.id.channelNameTextView);
        channelDescTextView = findViewById(R.id.channelDescTextView);
        followChannelButton = findViewById(R.id.followChannelButton);
        toolbar = findViewById(R.id.toolbar);
        followChannelButton.setOnClickListener(v -> followChannelButtonClick());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void init() {
        Intent intent = getIntent();
        channelInfo = intent.getParcelableExtra("channelInfo");
        channelViewModel =new ViewModelProvider(this).get(ChannelViewModel.class);

        if (channelInfo == null) {
            String channelId = intent.getStringExtra("channelId");
            if (!TextUtils.isEmpty(channelId)) {
                channelInfo = channelViewModel.getChannelInfo(channelId, true);
            }
        }
        if (channelInfo == null) {
            finish();
            return;
        }

        // FIXME: 2018/12/24 只有应用launcher icon应当反倒mipmap下面，其他还是应当放到drawable下面
        Glide.with(this).load(channelInfo.portrait).apply(new RequestOptions().placeholder(R.mipmap.ic_group_chat)).into(portraitImageView);
        channelTextView.setText(channelInfo.name);
        channelDescTextView.setText(TextUtils.isEmpty(channelInfo.desc) ?
            getString(R.string.channel_empty_desc) : channelInfo.desc);


        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        if (channelInfo.owner.equals(userViewModel.getUserId())) {
            followChannelButton.setVisibility(View.GONE);
            return;
        }

        isFollowed = channelViewModel.isListenedChannel(channelInfo.channelId);
        if (isFollowed) {
            followChannelButton.setText(R.string.channel_following);
        } else {
            followChannelButton.setText(R.string.channel_not_following);
        }
    }

    void followChannelButtonClick() {
        String action = isFollowed ? getString(R.string.channel_following) : getString(R.string.channel_not_following);
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content(getString(R.string.channel_following_status, action))
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();
        channelViewModel.listenChannel(channelInfo.channelId, !isFollowed).observe(this, booleanOperateResult -> {
            dialog.dismiss();
            if (booleanOperateResult.isSuccess()) {
                Toast.makeText(ChannelInfoActivity.this,
                    getString(R.string.channel_following_success, action),
                    Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(ChannelInfoActivity.this,
                    getString(R.string.channel_following_failed, action),
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
}
