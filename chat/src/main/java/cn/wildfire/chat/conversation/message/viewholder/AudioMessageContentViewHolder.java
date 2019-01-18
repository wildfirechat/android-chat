package cn.wildfire.chat.conversation.message.viewholder;

import android.graphics.drawable.AnimationDrawable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import cn.wildfirechat.chat.R;

import butterknife.Bind;
import butterknife.OnClick;
import cn.wildfire.chat.Config;
import cn.wildfire.chat.annotation.EnableContextMenu;
import cn.wildfire.chat.annotation.MessageContentType;
import cn.wildfire.chat.annotation.ReceiveLayoutRes;
import cn.wildfire.chat.annotation.SendLayoutRes;
import cn.wildfire.chat.conversation.message.model.UiMessage;
import cn.wildfire.chat.third.utils.UIUtils;
import cn.wildfirechat.message.SoundMessageContent;
import cn.wildfirechat.message.core.MessageDirection;

@MessageContentType(SoundMessageContent.class)
@SendLayoutRes(resId = R.layout.conversation_item_audio_send)
@ReceiveLayoutRes(resId = R.layout.conversation_item_audio_receive)
@EnableContextMenu
public class AudioMessageContentViewHolder extends MediaMessageContentViewHolder {
    @Bind(R.id.audioImageView)
    ImageView ivAudio;
    @Bind(R.id.durationTextView)
    TextView durationTextView;
    @Bind(R.id.audioContentLayout)
    RelativeLayout contentLayout;

    public AudioMessageContentViewHolder(FragmentActivity context, RecyclerView.Adapter adapter, View itemView) {
        super(context, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message) {
        super.onBind(message);
        SoundMessageContent voiceMessage = (SoundMessageContent) message.message.content;
        int increment = UIUtils.getDisplayWidth() / 2 / Config.DEFAULT_MAX_AUDIO_RECORD_TIME_SECOND * voiceMessage.getDuration();

        durationTextView.setText(voiceMessage.getDuration() + "''");
        ViewGroup.LayoutParams params = contentLayout.getLayoutParams();
        params.width = UIUtils.dip2Px(65) + UIUtils.dip2Px(increment);
        contentLayout.setLayoutParams(params);

        AnimationDrawable animation;
        if (message.isPlaying) {
            animation = (AnimationDrawable) ivAudio.getBackground();
            if (!animation.isRunning()) {
                animation.start();
            }
        } else {
            // TODO 不知道怎么回事，动画开始了，就停不下来, 所以采用这种方式
            ivAudio.setBackground(null);
            if (message.message.direction == MessageDirection.Send) {
                ivAudio.setBackgroundResource(R.drawable.audio_animation_right_list);
            } else {
                ivAudio.setBackgroundResource(R.drawable.audio_animation_left_list);
            }
        }
    }

    @OnClick(R.id.audioContentLayout)
    public void onClick(View view) {
        conversationViewModel.playAudioMessage(message);
    }

}
