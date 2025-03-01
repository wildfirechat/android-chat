/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message;

import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;

import org.joda.time.DateTime;

import java.util.List;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.third.utils.TimeConvertUtils;
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
    private OnMessageClickListener onMessageClickListener;

    public CompositeMessageContentAdapter(Message message, OnMessageClickListener onMessageClickListener) {
        this.message = message;
        this.onMessageClickListener = onMessageClickListener;
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
        List<Message> messages = ((CompositeMessageContent) message.content).getMessages();
        if (messages == null || messages.isEmpty()) {
            return 0;
        } else {
            return 1 + messages.size();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return 0;
        } else {
            return 1;
        }
    }

    class MessageContentViewHolder extends RecyclerView.ViewHolder {
        ImageView portraitImageView;
        TextView nameTextView;
        TextView timeTextView;

        // image message
        LinearLayout imageContentLayout;
        ImageView contentImageView;

        // text message and etc.
        LinearLayout textContentLayout;
        TextView contentTextView;

        // file message
        LinearLayout fileContentLayout;
        ImageView fileIconImageView;
        TextView fileNameTextView;
        TextView fileSizeTextView;

        // video message
        LinearLayout videoContentLayout;
        TextView videoDurationTextView;
        ImageView videoThumbnailImageView;

        // composite message
        LinearLayout compositeContentLayout;
        TextView compositeTitleTextView;
        TextView compositeContentTextView;

        private Message message;

        public MessageContentViewHolder(@NonNull View itemView) {
            super(itemView);
            bindViews(itemView);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CompositeMessageContentAdapter.this.onMessageClickListener.onClickMessage(message);
                }
            });
        }

        private void bindViews(View itemView) {
            portraitImageView = itemView.findViewById(R.id.portraitImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            imageContentLayout = itemView.findViewById(R.id.imageContentLayout);
            contentImageView = itemView.findViewById(R.id.contentImageView);
            textContentLayout = itemView.findViewById(R.id.textContentLayout);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            fileContentLayout = itemView.findViewById(R.id.fileContentLayout);
            fileIconImageView = itemView.findViewById(R.id.fileIconImageView);
            fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
            fileSizeTextView = itemView.findViewById(R.id.fileSizeTextView);
            videoContentLayout = itemView.findViewById(R.id.videoContentLayout);
            videoDurationTextView = itemView.findViewById(R.id.videoDurationTextView);
            videoThumbnailImageView = itemView.findViewById(R.id.videoThumbnailImageView);
            compositeContentLayout = itemView.findViewById(R.id.compositeContentLayout);
            compositeTitleTextView = itemView.findViewById(R.id.compositeTitleTextView);
            compositeContentTextView = itemView.findViewById(R.id.compositeContentTextView);
        }

        void bind(Message message, int position) {

            CompositeMessageContent compositeMessageContent = (CompositeMessageContent) message.content;
            Message msg = compositeMessageContent.getMessages().get(position);
            this.message = msg;
            MessageContent content = msg.content;

            UserInfo userInfo = ChatManager.Instance().getUserInfo(msg.sender, false);
            nameTextView.setText(userInfo.displayName);
            timeTextView.setText(TimeUtils.getMsgFormatTime(msg.serverTime));

            if (position == 0) {
                portraitImageView.setVisibility(View.VISIBLE);
                Glide
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
                    Glide
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
                Glide.with(itemView)
                    .load(imageMessageContent.remoteUrl)
                    .error(new BitmapDrawable(imageMessageContent.getThumbnail()))
                    .into(contentImageView);
            } else if (content instanceof VideoMessageContent) {
                textContentLayout.setVisibility(View.GONE);
                fileContentLayout.setVisibility(View.GONE);
                compositeContentLayout.setVisibility(View.GONE);
                imageContentLayout.setVisibility(View.GONE);
                videoContentLayout.setVisibility(View.VISIBLE);
                VideoMessageContent videoMessageContent = (VideoMessageContent) content;
                videoDurationTextView.setText(TimeConvertUtils.formatLongTime(videoMessageContent.getDuration() / 1000));

                Glide.with(itemView)
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
                fileIconImageView.setImageResource(FileUtils.getFileTypeImageResId(fileMessageContent.getName()));
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
        TextView compositeDurationTextView;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            bindViews(itemView);
        }

        private void bindViews(View itemView) {
            compositeDurationTextView = itemView.findViewById(R.id.compositeDurationTextView);
        }

        void bind(Message message) {
            CompositeMessageContent compositeMessageContent = (CompositeMessageContent) message.content;
            List<Message> messages = compositeMessageContent.getMessages();
            DateTime startDate = new DateTime(messages.get(0).serverTime);
            DateTime endDate = startDate;
            if (messages.size() > 1) {
                endDate = new DateTime(messages.get(messages.size() - 1).serverTime);
            }
            String pattern = itemView.getContext().getString(R.string.date_pattern);
            String text = itemView.getContext().getString(R.string.date_range_format,
                startDate.toString(pattern),
                endDate.toString(pattern));
            compositeDurationTextView.setText(text);
        }
    }

    interface OnMessageClickListener {
        void onClickMessage(Message message);
    }

}
