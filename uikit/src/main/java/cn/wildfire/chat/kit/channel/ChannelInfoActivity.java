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
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.chat.R2;
import cn.wildfirechat.model.ChannelInfo;

public class ChannelInfoActivity extends AppCompatActivity {
    @BindView(R2.id.portraitImageView)
    ImageView portraitImageView;
    @BindView(R2.id.channelNameTextView)
    TextView channelTextView;
    @BindView(R2.id.channelDescTextView)
    TextView channelDescTextView;
    @BindView(R2.id.followChannelButton)
    Button followChannelButton;
    @BindView(R2.id.toolbar)
    Toolbar toolbar;

    private boolean isFollowed = false;
    private ChannelViewModel channelViewModel;
    private ChannelInfo channelInfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.channel_info_activity);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        init();
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
        channelViewModel = ViewModelProviders.of(this).get(ChannelViewModel.class);

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
        Glide.with(this).load(channelInfo.portrait).apply(new RequestOptions().placeholder(R.mipmap.ic_group_cheat)).into(portraitImageView);
        channelTextView.setText(channelInfo.name);
        channelDescTextView.setText(TextUtils.isEmpty(channelInfo.desc) ? "频道主什么也没写" : channelInfo.desc);


        UserViewModel userViewModel =ViewModelProviders.of(this).get(UserViewModel.class);
        if (channelInfo.owner.equals(userViewModel.getUserId())) {
            followChannelButton.setVisibility(View.GONE);
            return;
        }

        isFollowed = channelViewModel.isListenedChannel(channelInfo.channelId);
        if (isFollowed) {
            followChannelButton.setText("取消收听");
        } else {
            followChannelButton.setText("收听频道");
        }
    }

    @OnClick(R2.id.followChannelButton)
    void followChannelButtonClick() {
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content(isFollowed ? "正在取消收听" : "正在收听")
                .progress(true, 100)
                .cancelable(false)
                .build();
        dialog.show();
        channelViewModel.listenChannel(channelInfo.channelId, !isFollowed).observe(this, new Observer<OperateResult<Boolean>>() {
            @Override
            public void onChanged(@Nullable OperateResult<Boolean> booleanOperateResult) {
                dialog.dismiss();
                if (booleanOperateResult.isSuccess()) {
                    Toast.makeText(ChannelInfoActivity.this, isFollowed ? "取消收听成功" : "收听成功", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ChannelInfoActivity.this, isFollowed ? "取消收听失败" : "收听失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
