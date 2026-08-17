/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.channel;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.bean.ImageItem;

import java.util.ArrayList;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.conversation.ConversationRouter;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfirechat.model.Conversation;

/**
 * 创建频道页。手机端装在 {@link CreateChannelActivity} 这个空壳里，平板上同一份实现进右栏。
 * <p>
 * 本仓库的 demo App 里没有入口（频道通常由后台创建），这一页是留给以 aar 集成 uikit 的
 * 接入方的；仍然一并接入右栏，免得接入方在平板上拿到一个全屏页。
 */
public class CreateChannelFragment extends Fragment implements WfcPage {

    private static final int REQUEST_CODE_PICK_IMAGE = 100;

    private ImageView portraitImageView;
    private TextInputEditText nameInputEditText;
    private TextInputEditText descInputEditText;

    private String portraitPath;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.channel_create_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        portraitImageView = view.findViewById(R.id.portraitImageView);
        nameInputEditText = view.findViewById(R.id.channelNameTextInputEditText);
        descInputEditText = view.findViewById(R.id.channelDescTextInputEditText);
        portraitImageView.setOnClickListener(v -> portraitClick());
    }

    @Override
    public int pageMenu() {
        return R.menu.channel_create;
    }

    @Override
    public boolean onPageMenuItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            createChannel();
            return true;
        }
        return false;
    }

    private void portraitClick() {
        // 用 WfcPageCompat 发起，才能被 PaneRegistry 接管进右栏；直接 ImagePicker.pick(Fragment,...)
        // 走的是 Fragment 自己的 startActivityForResult，主界面拦不到，只会全屏打开。
        WfcPageCompat.startPageForResult(this, ImagePicker.picker().buildPickIntent(getActivity()), REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
            if (images != null && !images.isEmpty()) {
                portraitPath = images.get(0).path;
                Glide.with(this).load(portraitPath)
                    .apply(new RequestOptions().placeholder(R.mipmap.avatar_def).centerCrop())
                    .into(portraitImageView);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void createChannel() {
        ChannelViewModel channelViewModel = new ViewModelProvider(this).get(ChannelViewModel.class);
        String channelName = nameInputEditText.getEditableText().toString().trim();
        String desc = descInputEditText.getEditableText().toString().trim();
        if (TextUtils.isEmpty(portraitPath)) {
            Toast.makeText(getActivity(), R.string.channel_set_portrait, Toast.LENGTH_SHORT).show();
            return;
        }
        MaterialDialog dialog = new MaterialDialog.Builder(requireContext())
            .content(R.string.channel_create_processing)
            .progress(true, 10)
            .cancelable(false)
            .show();

        channelViewModel.createChannel(null, channelName, portraitPath, desc, null)
            .observe(getViewLifecycleOwner(), result -> {
                dialog.dismiss();
                if (result.isSuccess()) {
                    Intent intent = ConversationActivity.buildConversationIntent(requireContext(),
                        Conversation.ConversationType.Channel, result.getResult(), 0);
                    // 建完就该进这个频道的会话，这张表单没有再回来的必要，让会话页把它顶掉
                    if (!WfcPageCompat.replaceSelfWithPage(this, intent)) {
                        ConversationRouter.open(this, intent);
                        WfcPageCompat.finishPage(this);
                    }
                } else {
                    Toast.makeText(getActivity(), R.string.channel_create_failed, Toast.LENGTH_SHORT).show();
                }
            });
    }
}
