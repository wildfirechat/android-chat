/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import org.joda.time.DateTime;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.mm.MMPreviewActivity;
import cn.wildfire.chat.kit.third.utils.TimeUtils;
import cn.wildfire.chat.kit.utils.FileUtils;
import cn.wildfirechat.message.CompositeMessageContent;
import cn.wildfirechat.message.FileMessageContent;
import cn.wildfirechat.message.ImageMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.MessageContent;
import cn.wildfirechat.message.VideoMessageContent;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class CompositeMessageContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Message message;
    private CompositeMessageContent compositeMessageContent;

    public CompositeMessageContentAdapter(Message message) {
        this.message = message;
        this.compositeMessageContent = (CompositeMessageContent) message.content;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder;
        View itemView;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == 0) {
            itemView = inflater.inflate(R.layout.composite_message_item_header, parent, false);
            holder = new HeaderViewHolder(itemView);
        } else {
            itemView = inflater.inflate(R.layout.composite_message_item, parent, false);
            holder = new MessageContentViewHolder(itemView);
        }
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position == 0) {
            ((HeaderViewHolder) holder).bind(message);
        } else {
            ((MessageContentViewHolder) holder).bind(message, position - 1);
        }
    }


    @Override
    public int getItemCount() {
        return 1 + compositeMessageContent.getMessages().size();
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return 0;
        } else {
            return 1;
        }
    }

    static class MessageContentViewHolder extends RecyclerView.ViewHolder {
        @BindView(R2.id.portraitImageView)
        ImageView portraitImageView;
        @BindView(R2.id.nameTextView)
        TextView nameTextView;
        @BindView(R2.id.timeTextView)
        TextView timeTextView;

        // image message
        @BindView(R2.id.imageContentLayout)
        LinearLayout imageContentLayout;
        @BindView(R2.id.contentImageView)
        ImageView contentImageView;

        // text message and etc.
        @BindView(R2.id.textContentLayout)
        LinearLayout textContentLayout;
        @BindView(R2.id.contentTextView)
        TextView contentTextView;

        // file message
        @BindView(R2.id.fileContentLayout)
        LinearLayout fileContentLayout;
        @BindView(R2.id.fileIconImageView)
        ImageView fileIconImageView;
        @BindView(R2.id.fileNameTextView)
        TextView fileNameTextView;
        @BindView(R2.id.fileSizeTextView)
        TextView fileSizeTextView;

        // video message
        @BindView(R2.id.videoContentLayout)
        LinearLayout videoContentLayout;
        @BindView(R2.id.videoDurationTextView)
        TextView videoDurationTextView;
        @BindView(R2.id.videoThumbnailImageView)
        ImageView videoThumbnailImageView;

        // composite message
        @BindView(R2.id.compositeContentLayout)
        LinearLayout compositeContentLayout;
        @BindView(R2.id.compositeTitleTextView)
        TextView compositeTitleTextView;
        @BindView(R2.id.compositeContentTextView)
        TextView compositeContentTextView;

        @OnClick(R2.id.videoContentLayout)
        void playVideo() {
            VideoMessageContent videoMessageContent = (VideoMessageContent) ((CompositeMessageContent) message.content).getMessages().get(position).content;
            MMPreviewActivity.previewVideo(itemView.getContext(), videoMessageContent);
        }

        private Message message;
        private int position;

        public MessageContentViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bind(Message message, int position) {
            this.message = message;
            this.position = position;

            CompositeMessageContent compositeMessageContent = (CompositeMessageContent) message.content;
            Message msg = compositeMessageContent.getMessages().get(position);
            MessageContent content = msg.content;

            UserInfo userInfo = ChatManager.Instance().getUserInfo(msg.sender, false);
            nameTextView.setText(userInfo.displayName);
            timeTextView.setText(TimeUtils.getMsgFormatTime(msg.serverTime));

            if (position == 0) {
                portraitImageView.setVisibility(View.VISIBLE);
                GlideApp
                    .with(itemView)
                    .load(userInfo.portrait)
                    .transforms(new CenterCrop(), new RoundedCorners(10))
                    .placeholder(R.mipmap.avatar_def)
                    .into(portraitImageView);
            } else {
                Message preMsg = compositeMessageContent.getMessages().get(position - 1);
                if (TextUtils.equals(preMsg.sender, msg.sender)) {
                    portraitImageView.setVisibility(View.INVISIBLE);
                } else {
                    portraitImageView.setVisibility(View.VISIBLE);
                    GlideApp
                        .with(itemView)
                        .load(userInfo.portrait)
                        .transforms(new CenterCrop(), new RoundedCorners(10))
                        .placeholder(R.mipmap.avatar_def)
                        .into(portraitImageView);
                }
            }

            if (content instanceof ImageMessageContent) {
                textContentLayout.setVisibility(View.GONE);
                fileContentLayout.setVisibility(View.GONE);
                compositeContentLayout.setVisibility(View.GONE);
                imageContentLayout.setVisibility(View.VISIBLE);
                videoContentLayout.setVisibility(View.GONE);

                ImageMessageContent imageMessageContent = (ImageMessageContent) content;
                GlideApp.with(itemView)
                    .load(imageMessageContent.remoteUrl)
                    .into(contentImageView);
            } else if (content instanceof VideoMessageContent) {
                textContentLayout.setVisibility(View.GONE);
                fileContentLayout.setVisibility(View.GONE);
                compositeContentLayout.setVisibility(View.GONE);
                imageContentLayout.setVisibility(View.GONE);
                videoContentLayout.setVisibility(View.VISIBLE);
                VideoMessageContent videoMessageContent = (VideoMessageContent) content;
                videoDurationTextView.setText("未知时长");

                GlideApp.with(itemView)
                    .load(videoMessageContent.getThumbnail())
                    .into(videoThumbnailImageView);
            } else if (content instanceof FileMessageContent) {
                textContentLayout.setVisibility(View.GONE);
                fileContentLayout.setVisibility(View.VISIBLE);
                compositeContentLayout.setVisibility(View.GONE);
                imageContentLayout.setVisibility(View.GONE);
                videoContentLayout.setVisibility(View.GONE);

                FileMessageContent fileMessageContent = (FileMessageContent) content;
                fileNameTextView.setText(fileMessageContent.getName());
                fileSizeTextView.setText(FileUtils.getReadableFileSize(fileMessageContent.getSize()));

            } else if (content instanceof CompositeMessageContent) {
                textContentLayout.setVisibility(View.GONE);
                fileContentLayout.setVisibility(View.GONE);
                compositeContentLayout.setVisibility(View.VISIBLE);
                imageContentLayout.setVisibility(View.GONE);
                videoContentLayout.setVisibility(View.GONE);

                CompositeMessageContent cmc = (CompositeMessageContent) content;
                compositeTitleTextView.setText(cmc.getTitle());
                List<Message> messages = cmc.getMessages();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < messages.size() && i < 4; i++) {
                    Message m = messages.get(i);
                    UserInfo u = ChatManager.Instance().getUserInfo(m.sender, false);
                    sb.append(u.displayName + ": " + m.content.digest(m));
                    sb.append("\n");
                }
                compositeContentTextView.setText(sb.toString());


            } else {
                textContentLayout.setVisibility(View.VISIBLE);
                fileContentLayout.setVisibility(View.GONE);
                compositeContentLayout.setVisibility(View.GONE);
                imageContentLayout.setVisibility(View.GONE);
                videoContentLayout.setVisibility(View.GONE);

                contentTextView.setText(content.digest(msg));
            }
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        @BindView(R2.id.compositeDurationTextView)
        TextView compositeDurationTextView;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bind(Message message) {
            CompositeMessageContent compositeMessageContent = (CompositeMessageContent) message.content;
            List<Message> messages = compositeMessageContent.getMessages();
            DateTime startDate = new DateTime(messages.get(0).serverTime);
            DateTime endDate = startDate;
            if (messages.size() > 1) {
                endDate = new DateTime(messages.get(messages.size() - 1).serverTime);
            }
            String pattern = "yyyy年MM月dd日";
            compositeDurationTextView.setText(startDate.toString(pattern) + " 至 " + endDate.toString(pattern));
        }
    }

}
