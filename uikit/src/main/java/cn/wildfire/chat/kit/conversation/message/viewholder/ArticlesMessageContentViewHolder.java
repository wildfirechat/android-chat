/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.message.viewholder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.ArticlesMessageContent;

@MessageContentType(ArticlesMessageContent.class)
@EnableContextMenu
public class ArticlesMessageContentViewHolder extends ContextableNotificationMessageContentViewHolder {
    LinearLayout singleArticleContainerLinearLayout;
    ImageView singleCoverImageView;
    TextView singleTitleTextView;

    RelativeLayout topArticleContainerRelativeLayout;
    ImageView topCoverImageView;
    TextView topTitleTextView;

    LinearLayout subArticlesContainerRelativeLayout;


    private ArticlesMessageContent content;

    public ArticlesMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
        bindViews(itemView);
        bindEvents(itemView);
    }

    private void bindEvents(View itemView) {
        itemView.findViewById(R.id.singleArticleContainerLinearLayout).setOnClickListener(this::onClick);
        itemView.findViewById(R.id.topArticleContainerLinearLayout).setOnClickListener(this::onClick);
    }

    private void bindViews(View itemView) {
        singleArticleContainerLinearLayout = itemView.findViewById(R.id.singleArticleContainerLinearLayout);
        singleCoverImageView = itemView.findViewById(R.id.singleCoverImageView);
        singleTitleTextView = itemView.findViewById(R.id.singleTitleTextView);
        topArticleContainerRelativeLayout = itemView.findViewById(R.id.topArticleContainerLinearLayout);
        topCoverImageView = itemView.findViewById(R.id.topCoverImageView);
        topTitleTextView = itemView.findViewById(R.id.topTitleTextView);
        subArticlesContainerRelativeLayout = itemView.findViewById(R.id.subArticlesContainerLinearLayout);
    }

    @Override
    public void onBind(UiMessage message, int position) {
        super.onBind(message, position);
        content = (ArticlesMessageContent) message.message.content;
        if (content.subArticles != null && content.subArticles.size() > 0) {
            topArticleContainerRelativeLayout.setVisibility(View.VISIBLE);
            singleArticleContainerLinearLayout.setVisibility(View.GONE);

            Glide.with(fragment).load(content.topArticle.cover).into(topCoverImageView);
            topTitleTextView.setText(content.topArticle.title);
        } else {
            topArticleContainerRelativeLayout.setVisibility(View.GONE);
            singleArticleContainerLinearLayout.setVisibility(View.VISIBLE);

            Glide.with(fragment).load(content.topArticle.cover).into(singleCoverImageView);
            singleTitleTextView.setText(content.topArticle.title);
        }

        if (content.subArticles != null) {

            for (int i = 0; i < content.subArticles.size(); i++) {
                ArticlesMessageContent.Article article = content.subArticles.get(i);
                addSubArticle(article, i == content.subArticles.size() - 1);
            }
        }
    }

    private void addSubArticle(ArticlesMessageContent.Article article, boolean isLast) {
        View view = LayoutInflater.from(fragment.getContext()).inflate(R.layout.conversation_item_article, null, false);
        TextView titleTextView = view.findViewById(R.id.titleTextView);
        titleTextView.setText(article.title);
        ImageView coverImageView = view.findViewById(R.id.coverImageView);
        Glide.with(fragment).load(article.cover).into(coverImageView);
        view.setOnClickListener(v -> openArticle(article));

        subArticlesContainerRelativeLayout.addView(view);
        if (!isLast) {
            View divider = new View(fragment.getContext());
            divider.setBackgroundResource(R.color.line);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            subArticlesContainerRelativeLayout.addView(divider, params);
        }
    }

    public void onClick(View view) {
        openArticle(content.topArticle);
    }

    private void openArticle(ArticlesMessageContent.Article article) {
        WfcWebViewActivity.loadUrl(fragment.getContext(), article.title, article.url);
    }

}
