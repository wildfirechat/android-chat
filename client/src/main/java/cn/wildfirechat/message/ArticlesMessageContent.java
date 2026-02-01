/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.message;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cn.wildfirechat.message.core.ContentTag;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.message.core.MessagePayload;
import cn.wildfirechat.message.core.PersistFlag;

/**
 * 图文消息内容
 * <p>
 * 用于发送多图文消息，包含一个主文章和多个子文章。
 * 常用于公众号文章、新闻推送等场景，每篇文章包含标题、封面、摘要、链接等信息。
 * </p>
 *
 * @author WildFireChat
 * @since 2022
 */
@ContentTag(type = MessageContentType.ContentType_Articles, flag = PersistFlag.Persist_And_Count)
public class ArticlesMessageContent extends MessageContent {
    /**
     * 主文章
     */
    public Article topArticle;

    /**
     * 子文章列表
     */
    public ArrayList<Article> subArticles;

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.searchableContent = topArticle.title;
        JSONObject object = new JSONObject();
        try {
            object.put("top", topArticle.toJson());
            if (subArticles != null) {
                JSONArray jsonArray = new JSONArray();
                object.put("subArticles", jsonArray);
                for (Article article : subArticles) {
                    jsonArray.put(article.toJson());
                }
            }
            payload.binaryContent = object.toString().getBytes();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        try {
            JSONObject object = new JSONObject(new String(payload.binaryContent));
            JSONObject topObj = object.getJSONObject("top");
            this.topArticle = Article.fromJson(topObj);
            JSONArray jsonArray = object.optJSONArray("subArticles");
            if (jsonArray != null && jsonArray.length() > 0) {
                this.subArticles = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++) {
                    subArticles.add(Article.fromJson(jsonArray.getJSONObject(i)));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String digest(Message message) {
        return this.topArticle.title;
    }

    public List<LinkMessageContent> toLinkMessageContent() {
        List<LinkMessageContent> contents = new ArrayList<>();
        contents.add(this.topArticle.toLinkMessageContent());
        if (this.subArticles != null) {
            for (Article article : subArticles) {
                contents.add(article.toLinkMessageContent());
            }
        }
        return contents;
    }

    /**
     * 文章类
     * <p>
     * 表示单篇文章的信息，包括ID、封面、标题、摘要、URL等。
     * </p>
     */
    public static class Article implements Parcelable {
        /**
         * 文章唯一标识
         */
        public String articleId;

        /**
         * 文章封面图片URL
         */
        public String cover;

        /**
         * 文章标题
         */
        public String title;

        /**
         * 文章摘要
         */
        public String digest;

        /**
         * 文章链接URL
         */
        public String url;

        /**
         * 是否需要阅读回执
         */
        boolean readReport;

        JSONObject toJson() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", articleId);
                obj.put("cover", cover);
                obj.put("title", title);
                obj.put("digest", digest);
                obj.put("url", url);
                obj.put("rr", readReport);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return obj;
        }

        static Article fromJson(JSONObject obj) {
            Article article = new Article();
            article.articleId = obj.optString("id");
            article.cover = obj.optString("cover");
            article.title = obj.optString("title");
            article.digest = obj.optString("digest");
            article.url = obj.optString("url");
            article.readReport = obj.optBoolean("rr");
            return article;
        }

        public LinkMessageContent toLinkMessageContent() {
            LinkMessageContent content = new LinkMessageContent(this.title, this.url);
            content.setContentDigest(this.digest);
            content.setThumbnailUrl(this.cover);
            return content;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.articleId);
            dest.writeString(this.cover);
            dest.writeString(this.title);
            dest.writeString(this.url);
            dest.writeByte(this.readReport ? (byte) 1 : (byte) 0);
        }

        public void readFromParcel(Parcel source) {
            this.articleId = source.readString();
            this.cover = source.readString();
            this.title = source.readString();
            this.url = source.readString();
            this.readReport = source.readByte() != 0;
        }

        public Article() {
        }

        protected Article(Parcel in) {
            this.articleId = in.readString();
            this.cover = in.readString();
            this.title = in.readString();
            this.url = in.readString();
            this.readReport = in.readByte() != 0;
        }

        public static final Creator<Article> CREATOR = new Creator<Article>() {
            @Override
            public Article createFromParcel(Parcel source) {
                return new Article(source);
            }

            @Override
            public Article[] newArray(int size) {
                return new Article[size];
            }
        };
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeParcelable(this.topArticle, flags);
        dest.writeList(this.subArticles);
    }

    public void readFromParcel(Parcel source) {
        this.topArticle = source.readParcelable(Article.class.getClassLoader());
        this.subArticles = new ArrayList<Article>();
        source.readList(this.subArticles, Article.class.getClassLoader());
    }

    public ArticlesMessageContent() {
    }

    protected ArticlesMessageContent(Parcel in) {
        super(in);
        this.topArticle = in.readParcelable(Article.class.getClassLoader());
        this.subArticles = new ArrayList<Article>();
        in.readList(this.subArticles, Article.class.getClassLoader());
    }

    public static final Creator<ArticlesMessageContent> CREATOR = new Creator<ArticlesMessageContent>() {
        @Override
        public ArticlesMessageContent createFromParcel(Parcel source) {
            return new ArticlesMessageContent(source);
        }

        @Override
        public ArticlesMessageContent[] newArray(int size) {
            return new ArticlesMessageContent[size];
        }
    };
}
