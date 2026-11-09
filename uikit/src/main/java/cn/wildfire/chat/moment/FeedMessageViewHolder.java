package cn.wildfire.chat.moment;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.third.utils.TimeUtils;
import cn.wildfire.chat.moment.third.interfaces.OnFeedUserClickListener;
import cn.wildfire.chat.moment.third.span.VerticalImageSpan;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.moment.FeedCommentType;
import cn.wildfirechat.moment.FeedContentType;
import cn.wildfirechat.moment.MomentClient;
import cn.wildfirechat.moment.message.FeedCommentMessageContent;
import cn.wildfirechat.moment.message.FeedMessageContent;
import cn.wildfirechat.moment.model.Feed;
import cn.wildfirechat.remote.ChatManager;

public class FeedMessageViewHolder extends RecyclerView.ViewHolder {
    private ImageView portraitImageView;
    private TextView nameTextViw;
    private TextView contentTextView;
    private TextView timeTextView;
    private ImageView descImageView;
    private TextView descTextView;

    private OnFeedUserClickListener onFeedUserClickListener;

    String userId;

    public FeedMessageViewHolder(@NonNull View itemView) {
        super(itemView);
        portraitImageView = itemView.findViewById(R.id.messageSenderPortraitImageView);
        nameTextViw = itemView.findViewById(R.id.messageSenderNameTextView);
        contentTextView = itemView.findViewById(R.id.messageContentTextView);
        timeTextView = itemView.findViewById(R.id.messageTimeTextView);

        descImageView = itemView.findViewById(R.id.feedDescImageView);
        descTextView = itemView.findViewById(R.id.feedDescTextView);

        portraitImageView.setOnClickListener(v -> {
            if (onFeedUserClickListener != null) {
                onFeedUserClickListener.onFeedUserClick("");
            }
        });
    }

    public void setOnFeedUserClickListener(OnFeedUserClickListener onFeedUserClickListener) {
        this.onFeedUserClickListener = onFeedUserClickListener;
    }

    public void onBind(Message message) {
        MessageContent messageContent = message.content;
        String sender = "";
        long feedId = 0;
        if (messageContent instanceof FeedMessageContent) {
            FeedMessageContent feedMessageContent = (FeedMessageContent) messageContent;
            sender = feedMessageContent.getSender();
            contentTextView.setText("提到了我");
            feedId = feedMessageContent.getFeedId();
        } else if (messageContent instanceof FeedCommentMessageContent) {
            FeedCommentMessageContent feedCommentMessageContent = (FeedCommentMessageContent) messageContent;
            sender = feedCommentMessageContent.getSender();
            feedId = feedCommentMessageContent.getFeedId();
            if (feedCommentMessageContent.getCommentType() == FeedCommentType.Comment_Thumbup_Type) {
                SpannableStringBuilder builder = new SpannableStringBuilder();
                builder.append("赞");
                builder.setSpan(new VerticalImageSpan(ContextCompat.getDrawable(itemView.getContext(), R.drawable.heart_drawable_blue)),
                    0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                contentTextView.setText(builder);
            } else {
                contentTextView.setText(feedCommentMessageContent.getText());
            }
        }

        MomentClient.getInstance().getFeed(feedId, new MomentClient.GetFeedCallback() {
            @Override
            public void onSuccess(Feed feed) {
                if (feed.type == FeedContentType.Content_Text_Type) {
                    descTextView.setVisibility(View.VISIBLE);
                    descTextView.setText(feed.text == null ? "" : feed.text);
                } else {
                    if (feed.medias != null && !feed.medias.isEmpty()) {
                        Glide.with(descImageView).load(feed.medias.get(0).mediaUrl).into(descImageView);
                    }
                    descTextView.setText("");
                }

            }

            @Override
            public void onFailure(int errorCode) {

            }
        });
//        descTextView.setText(feedMessageContent.getText() == null ? "" : feedMessageContent.getText());

        UserInfo userInfo = ChatManager.Instance().getUserInfo(sender, false);
        userId = userInfo.uid;

        nameTextViw.setText(userInfo.displayName);
        Glide.with(itemView).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(portraitImageView);
        timeTextView.setText(TimeUtils.getMsgFormatTime(message.serverTime));
    }

    // TODO
    private void bindFeed() {

    }
}
