/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.forward;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.utils.WfcTextUtils;

public class ForwardPromptView extends LinearLayout {
    @BindView(R2.id.portraitImageView)
    ImageView portraitImageView;
    @BindView(R2.id.nameTextView)
    TextView nameTextView;
    @BindView(R2.id.contentTextView)
    TextView contentTextView;
    @BindView(R2.id.contentImageView)
    ImageView contentImageView;
    @BindView(R2.id.editText)
    EditText editText;

    public ForwardPromptView(Context context) {
        super(context);
        init();
    }

    public ForwardPromptView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ForwardPromptView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ForwardPromptView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        View view = LayoutInflater.from(getContext())
            .inflate(R.layout.forward_prompt_dialog, this, true);
        ButterKnife.bind(this, view);
    }

    public void bind(String targetName, String targetPortrait, String contentText) {
        bind(targetName, targetPortrait, contentText, null);
    }

    public void bind(String targetName, String targetPortrait, Bitmap contentImage) {
        bind(targetName, targetPortrait, null, contentImage);
    }

    private void bind(String targetName, String targetPortrait, String contentText, Bitmap contentImage) {
        nameTextView.setText(targetName);
        Glide.with(getContext()).load(targetPortrait).apply(new RequestOptions().placeholder(R.mipmap.ic_group_cheat).centerCrop()).into(portraitImageView);
        if (!TextUtils.isEmpty(contentText)) {
            contentImageView.setVisibility(GONE);
            contentTextView.setVisibility(VISIBLE);
            contentTextView.setText(WfcTextUtils.htmlToText(contentText));
        } else if (contentImage != null) {
            contentTextView.setVisibility(GONE);
            contentImageView.setVisibility(VISIBLE);
            contentImageView.getLayoutParams().width = UIUtils.dip2Px(contentImage.getWidth());
            contentImageView.getLayoutParams().height = UIUtils.dip2Px(contentImage.getHeight());
            contentImageView.setImageBitmap(contentImage);
        }
        invalidate();
    }

    public String getEditText() {
        return editText.getText().toString().trim();
    }
}
