package cn.wildfire.chat.kit.channel;

import android.app.Activity;
import android.content.Intent;
import android.text.Editable;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.textfield.TextInputEditText;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.bean.ImageItem;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.chat.R2;
import cn.wildfirechat.model.Conversation;

public class CreateChannelActivity extends WfcBaseActivity {
    @Nullable
    @BindView(R2.id.portraitImageView)
    ImageView portraitImageView;

    @BindView(R2.id.channelNameTextInputEditText)
    TextInputEditText nameInputEditText;
    @BindView(R2.id.channelDescTextInputEditText)
    TextInputEditText descInputEditText;

    private static final int REQUEST_CODE_PICK_IMAGE = 100;

    private String portraitPath;


    @Override
    protected int contentLayout() {
        return R.layout.channel_create_fragment;
    }

    @OnTextChanged(value = R2.id.channelNameTextInputEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    void inputChannelName(Editable editable) {
//        if (!TextUtils.isEmpty(editable)) {
//            confirmButton.setEnabled(true);
//        } else {
//            confirmButton.setEnabled(false);
//        }
    }

    @OnTextChanged(value = R2.id.channelDescTextInputEditText, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
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

    @OnClick(R2.id.portraitImageView)
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
            Toast.makeText(this, "请设置头像", Toast.LENGTH_SHORT).show();
            return;
        }
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("创建频道中...")
                .progress(true, 10)
                .cancelable(false)
                .show();
        channelViewModel.createChannel(null, channelName, portraitPath, desc, null).observe(this, new Observer<OperateResult<String>>() {
            @Override
            public void onChanged(@Nullable OperateResult<String> result) {
                dialog.dismiss();
                if (result.isSuccess()) {
                    Intent intent = ConversationActivity.buildConversationIntent(CreateChannelActivity.this, Conversation.ConversationType.Channel, result.getResult(), 0);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(CreateChannelActivity.this, "create channel failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
