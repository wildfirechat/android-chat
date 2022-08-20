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

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcWebViewActivity;
import cn.wildfire.chat.kit.annotation.EnableContextMenu;
import cn.wildfire.chat.kit.annotation.MessageContentType;
import cn.wildfire.chat.kit.conversation.ConversationFragment;
import cn.wildfire.chat.kit.conversation.message.model.UiMessage;
import cn.wildfirechat.message.ArticlesMessageContent;

@MessageContentType(ArticlesMessageContent.class)
@EnableContextMenu
public class ArticlesMessageContentViewHolder extends ContextableNotificationMessageContentViewHolder{
    @BindView(R2.id.singleArticleContainerLinearLayout)
    LinearLayout singleArticleContainerLinearLayout;
    @BindView(R2.id.singleCoverImageView)
    ImageView singleCoverImageView;
    @BindView(R2.id.singleTitleTextView)
    TextView singleTitleTextView;

    @BindView(R2.id.topArticleContainerLinearLayout)
    RelativeLayout topArticleContainerRelativeLayout;
    @BindView(R2.id.topCoverImageView)
    ImageView topCoverImageView;
    @BindView(R2.id.topTitleTextView)
    TextView topTitleTextView;

    @BindView(R2.id.subArticlesContainerLinearLayout)
    LinearLayout subArticlesContainerRelativeLayout;


    private ArticlesMessageContent content;

    public ArticlesMessageContentViewHolder(ConversationFragment fragment, RecyclerView.Adapter adapter, View itemView) {
        super(fragment, adapter, itemView);
    }

    @Override
    public void onBind(UiMessage message, int position) {
        super.onBind(message, position);
        content = (ArticlesMessageContent) message.message.content;
        if (content.subArticles != null && content.subArticles.size() > 0) {
            topArticleContainerRelativeLayout.setVisibility(View.VISIBLE);
            singleArticleContainerLinearLayout.setVisibility(View.GONE);

            GlideApp.with(fragment).load(content.topArticle.cover).into(topCoverImageView);
            topTitleTextView.setText(content.topArticle.title);
        } else {
            topArticleContainerRelativeLayout.setVisibility(View.GONE);
            singleArticleContainerLinearLayout.setVisibility(View.VISIBLE);

            GlideApp.with(fragment).load(content.topArticle.cover).into(singleCoverImageView);
            singleTitleTextView.setText(content.topArticle.title);
        }

        for (int i = 0; i < content.subArticles.size(); i++) {
            ArticlesMessageContent.Article article = content.subArticles.get(i);
            addSubArticle(article, i == content.subArticles.size() - 1);
        }
    }

    private void addSubArticle(ArticlesMessageContent.Article article, boolean isLast) {
        View view = LayoutInflater.from(fragment.getContext()).inflate(R.layout.conversation_item_article, null, false);
        TextView titleTextView = view.findViewById(R.id.titleTextView);
        titleTextView.setText(article.title);
        ImageView coverImageView = view.findViewById(R.id.coverImageView);
        GlideApp.with(fragment).load(article.cover).into(coverImageView);
        view.setOnClickListener(v -> openArticle(article));

        subArticlesContainerRelativeLayout.addView(view);
        if (!isLast) {
            View divider = new View(fragment.getContext());
            divider.setBackgroundResource(R.color.line);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
            subArticlesContainerRelativeLayout.addView(divider, params);
        }
    }

    @OnClick({R2.id.singleArticleContainerLinearLayout, R2.id.topArticleContainerLinearLayout})
    public void onClick(View view) {
        openArticle(content.topArticle);
    }

    private void openArticle(ArticlesMessageContent.Article article) {
        WfcWebViewActivity.loadUrl(fragment.getContext(), article.title, article.url);
    }

}
