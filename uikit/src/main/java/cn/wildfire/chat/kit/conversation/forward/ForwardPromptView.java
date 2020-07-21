package cn.wildfire.chat.kit.conversation.forward;

import android.content.Context;
import android.graphics.Bitmap;
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
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.utils.WfcTextUtils;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.VideoMessageContent;

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

    public void bind(String targetName, String targetPortrait, Message message) {
        nameTextView.setText(targetName);
        Glide.with(getContext()).load(targetPortrait).apply(new RequestOptions().placeholder(R.mipmap.ic_group_cheat).centerCrop()).into(portraitImageView);
        if (message.content instanceof ImageMessageContent) {
            contentTextView.setVisibility(GONE);
            contentImageView.setVisibility(VISIBLE);
            Bitmap bitmap = ((ImageMessageContent) message.content).getThumbnail();
            if (bitmap != null) {
                contentImageView.getLayoutParams().width = UIUtils.dip2Px(bitmap.getWidth());
                contentImageView.getLayoutParams().height = UIUtils.dip2Px(bitmap.getHeight());
                contentImageView.setImageBitmap(bitmap);
            }
        } else if (message.content instanceof VideoMessageContent) {
            contentTextView.setVisibility(GONE);
            contentImageView.setVisibility(VISIBLE);
            Bitmap bitmap = ((VideoMessageContent) message.content).getThumbnail();
            if (bitmap != null) {
                contentImageView.getLayoutParams().width = UIUtils.dip2Px(bitmap.getWidth());
                contentImageView.getLayoutParams().height = UIUtils.dip2Px(bitmap.getHeight());
                contentImageView.setImageBitmap(bitmap);
            }
        } else {
            contentImageView.setVisibility(GONE);
            contentTextView.setVisibility(VISIBLE);
            contentTextView.setText(WfcTextUtils.htmlToText(message.digest()));
        }
        invalidate();
    }

    public String getEditText() {
        return editText.getText().toString().trim();
    }
}
