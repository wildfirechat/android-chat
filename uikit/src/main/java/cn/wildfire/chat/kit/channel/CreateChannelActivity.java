/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.channel;

import android.app.Activity;
import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.bean.ImageItem;

import java.util.ArrayList;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfirechat.model.Conversation;

public class CreateChannelActivity extends WfcBaseActivity {
    @Nullable
    ImageView portraitImageView;

    TextInputEditText nameInputEditText;
    TextInputEditText descInputEditText;

    private static final int REQUEST_CODE_PICK_IMAGE = 100;

    private String portraitPath;


    protected void bindEvents() {
        super.bindEvents();
        portraitImageView.setOnClickListener(v -> portraitClick());
    }

    protected void bindViews() {
        super.bindViews();
        portraitImageView = findViewById(R.id.portraitImageView);
        nameInputEditText = findViewById(R.id.channelNameTextInputEditText);
        descInputEditText = findViewById(R.id.channelDescTextInputEditText);
    }

    @Override
    protected int contentLayout() {
        return R.layout.channel_create_fragment;
    }

    void inputChannelName(Editable editable) {
//        if (!TextUtils.isEmpty(editable)) {
//            confirmButton.setEnabled(true);
//        } else {
//            confirmButton.setEnabled(false);
//        }
    }

    void inputChannelDesc(Editable editable) {

    }

    @Override
    protected int menu() {
        return R.menu.channel_create;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.confirm) {
            createChannel();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    void portraitClick() {
        ImagePicker.picker().pick(this, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
            if (images != null && images.size() > 0) {
                portraitPath = images.get(0).path;
                Glide.with(this).load(portraitPath).apply(new RequestOptions().placeholder(R.mipmap.avatar_def).centerCrop()).into(portraitImageView);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    void createChannel() {
        ChannelViewModel channelViewModel = ViewModelProviders.of(this).get(ChannelViewModel.class);
        String channelName = nameInputEditText.getEditableText().toString().trim();
        String desc = descInputEditText.getEditableText().toString().trim();
        if (TextUtils.isEmpty(portraitPath)) {
            Toast.makeText(this, R.string.channel_set_portrait, Toast.LENGTH_SHORT).show();
            return;
        }
        MaterialDialog dialog = new MaterialDialog.Builder(this)
            .content(R.string.channel_create_processing)
            .progress(true, 10)
            .cancelable(false)
            .show();

        channelViewModel.createChannel(null, channelName, portraitPath, desc, null)
            .observe(this, result -> {
                dialog.dismiss();
                if (result.isSuccess()) {
                    Intent intent = ConversationActivity.buildConversationIntent(CreateChannelActivity.this, Conversation.ConversationType.Channel, result.getResult(), 0);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(CreateChannelActivity.this,
                        R.string.channel_create_failed,
                        Toast.LENGTH_SHORT).show();
                }
            });
    }
}
