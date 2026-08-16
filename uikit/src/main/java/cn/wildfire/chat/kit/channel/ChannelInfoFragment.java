/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.channel;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfirechat.model.ChannelInfo;

/**
 * 频道详情页：看一眼频道简介，决定订阅或退订。
 * <p>
 * 手机端装在 {@link ChannelInfoActivity} 这个空壳里，平板上同一份实现进右栏。
 * 入口有两个：搜索结果里点一个频道，以及扫描频道二维码。
 */
public class ChannelInfoFragment extends Fragment {

    private ImageView portraitImageView;
    private TextView channelTextView;
    private TextView channelDescTextView;
    private Button followChannelButton;

    private boolean isFollowed = false;
    private ChannelViewModel channelViewModel;
    private ChannelInfo channelInfo;

    /**
     * 入口 intent 里要么直接带着 channelInfo（搜索结果），要么只有一个 channelId（扫码）。
     * 两者都没有就不是一个能显示的页面，返回 null 让调用方放弃。
     */
    @Nullable
    public static ChannelInfoFragment fromIntent(@Nullable Intent intent) {
        if (intent == null) {
            return null;
        }
        ChannelInfo channelInfo = intent.getParcelableExtra("channelInfo");
        String channelId = intent.getStringExtra("channelId");
        if (channelInfo == null && TextUtils.isEmpty(channelId)) {
            return null;
        }
        ChannelInfoFragment fragment = new ChannelInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable("channelInfo", channelInfo);
        args.putString("channelId", channelId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.channel_info_activity, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        portraitImageView = view.findViewById(R.id.portraitImageView);
        channelTextView = view.findViewById(R.id.channelNameTextView);
        channelDescTextView = view.findViewById(R.id.channelDescTextView);
        followChannelButton = view.findViewById(R.id.followChannelButton);
        followChannelButton.setOnClickListener(v -> followChannelButtonClick());

        // 页面自己的 ViewModel，不要用 pageScope：这里没有第二个 Fragment 要跟它共享状态
        channelViewModel = new ViewModelProvider(this).get(ChannelViewModel.class);

        Bundle args = getArguments() == null ? Bundle.EMPTY : getArguments();
        channelInfo = args.getParcelable("channelInfo");
        if (channelInfo == null) {
            String channelId = args.getString("channelId");
            if (!TextUtils.isEmpty(channelId)) {
                channelInfo = channelViewModel.getChannelInfo(channelId, true);
            }
        }
        if (channelInfo == null) {
            WfcPageCompat.finishPage(this);
            return;
        }

        Glide.with(this).load(channelInfo.portrait)
            .apply(new RequestOptions().placeholder(R.mipmap.ic_group_chat))
            .into(portraitImageView);
        channelTextView.setText(channelInfo.name);
        channelDescTextView.setText(TextUtils.isEmpty(channelInfo.desc)
            ? getString(R.string.channel_empty_desc) : channelInfo.desc);

        UserViewModel userViewModel = WfcUIKit.getAppScopeViewModel(UserViewModel.class);
        if (channelInfo.owner.equals(userViewModel.getUserId())) {
            // 自己的频道，没有「订阅」这一说
            followChannelButton.setVisibility(View.GONE);
            return;
        }

        isFollowed = channelViewModel.isListenedChannel(channelInfo.channelId);
        followChannelButton.setText(isFollowed ? R.string.channel_following : R.string.channel_not_following);
    }

    private void followChannelButtonClick() {
        String action = getString(isFollowed ? R.string.channel_following : R.string.channel_not_following);
        MaterialDialog dialog = new MaterialDialog.Builder(requireContext())
            .content(getString(R.string.channel_following_status, action))
            .progress(true, 100)
            .cancelable(false)
            .build();
        dialog.show();
        channelViewModel.listenChannel(channelInfo.channelId, !isFollowed)
            .observe(getViewLifecycleOwner(), booleanOperateResult -> {
                dialog.dismiss();
                if (booleanOperateResult.isSuccess()) {
                    Toast.makeText(getActivity(), getString(R.string.channel_following_success, action),
                        Toast.LENGTH_SHORT).show();
                    WfcPageCompat.finishPage(this);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.channel_following_failed, action),
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
}
