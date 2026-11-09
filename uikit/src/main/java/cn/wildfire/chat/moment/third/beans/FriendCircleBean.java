package cn.wildfire.chat.moment.third.beans;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.mm.MediaEntry;
import cn.wildfire.chat.kit.third.utils.TimeUtils;
import cn.wildfire.chat.moment.third.Constants;
import cn.wildfire.chat.moment.third.utils.SpanUtils;
import cn.wildfire.chat.moment.third.utils.Utils;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.moment.FeedCommentType;
import cn.wildfirechat.moment.FeedContentType;
import cn.wildfirechat.moment.model.Comment;
import cn.wildfirechat.moment.model.Feed;
import cn.wildfirechat.moment.model.FeedEntry;
import cn.wildfirechat.remote.ChatManager;

public class FriendCircleBean {

    private long id;
    private int viewType;

    private String content;

    private List<CommentBean> commentBeans;

    private List<PraiseBean> praiseBeans;

    private List<MediaEntry> mediaEntries;

    private UserBean userBean;

    private OtherInfoBean otherInfoBean;

    public static FriendCircleBean fromFeed(Context context, Feed feed) {
        FriendCircleBean friendCircleBean = new FriendCircleBean();
        friendCircleBean.id = feed.feedId;
        switch (feed.type) {
            case FeedContentType.Content_Text_Type:
                // TODO R.layout.moment_item_recycler_firend_circle_word_and_url;
                friendCircleBean.viewType = Constants.FriendCircleType.FRIEND_CIRCLE_TYPE_ONLY_WORD;
                break;
            case FeedContentType.Content_Image_Type:
                friendCircleBean.viewType = Constants.FriendCircleType.FRIEND_CIRCLE_TYPE_WORD_AND_IMAGES;
                break;
            case FeedContentType.Content_Link_Type:
                friendCircleBean.viewType = Constants.FriendCircleType.FRIEND_CIRCLE_TYPE_WORD_AND_URL;
                break;
            case FeedContentType.Content_Video_Type:
                friendCircleBean.viewType = Constants.FriendCircleType.FRIEND_CIRCLE_TYPE_WORD_AND_IMAGES;
                break;
            default:
                break;
        }

        friendCircleBean.setContent(feed.text);

        UserInfo userInfo = ChatManager.Instance().getUserInfo(feed.sender, false);
        friendCircleBean.userBean = UserBean.fromUserInfo(userInfo);

        // sort?
        if (feed.comments != null && feed.comments.size() > 0) {
            List<CommentBean> commentBeans = new ArrayList<>();
            List<PraiseBean> praiseBeans = new ArrayList<>();
            for (Comment comment : feed.comments) {
                if (comment.type == FeedCommentType.Comment_Comment_Type) {
                    CommentBean commentBean = new CommentBean();
                    if (TextUtils.isEmpty(comment.replyTo)) {
                        commentBean.setCommentType(Constants.CommentType.COMMENT_TYPE_SINGLE);
                        commentBean.setChildUserId(comment.sender);
                        commentBean.setChildUserName(ChatManager.Instance().getUserDisplayName(comment.sender));
                    } else {
                        commentBean.setCommentType(Constants.CommentType.COMMENT_TYPE_REPLY);
                        commentBean.setChildUserId(comment.sender);
                        commentBean.setChildUserName(ChatManager.Instance().getUserDisplayName(comment.sender));
                        commentBean.setParentUserId(comment.replyTo);
                        commentBean.setParentUserName(ChatManager.Instance().getUserDisplayName(comment.replyTo));
                    }
                    commentBean.setId(comment.commentId);
                    commentBean.setCommentContent(comment.text);
                    commentBeans.add(commentBean);
                } else {
                    PraiseBean praiseBean = new PraiseBean();
                    praiseBean.setId(comment.commentId);
                    praiseBean.setPraiseUserId(comment.sender);
                    praiseBean.setPraiseUserName(ChatManager.Instance().getUserDisplayName(comment.sender));
                    praiseBeans.add(praiseBean);
                }
            }
            friendCircleBean.commentBeans = commentBeans;
            friendCircleBean.praiseBeans = praiseBeans;
            friendCircleBean.setPraiseSpan(SpanUtils.makePraiseSpan(context, praiseBeans, null));
        }
        if (feed.medias != null && feed.medias.size() > 0) {
            List<MediaEntry> mediaEntries = new ArrayList<>();
            for (FeedEntry entry : feed.medias) {
                MediaEntry mediaEntry = new MediaEntry();
                if (feed.type == FeedContentType.Content_Image_Type) {
                    mediaEntry.setType(MediaEntry.TYPE_IMAGE);
                } else {
                    mediaEntry.setType(MediaEntry.TYPE_VIDEO);
                }
                mediaEntry.setWidth(entry.mediaWidth);
                mediaEntry.setHeight(entry.mediaHeight);
                mediaEntry.setMediaUrl(entry.mediaUrl);
                mediaEntry.setThumbnailUrl(entry.thumbUrl);
                mediaEntries.add(mediaEntry);
            }
            friendCircleBean.mediaEntries = mediaEntries;
        }
        friendCircleBean.otherInfoBean = new OtherInfoBean();
        friendCircleBean.otherInfoBean.setTime(TimeUtils.getMsgFormatTime(feed.serverTime));

        return friendCircleBean;
    }

    private boolean isExpanded;

    private boolean isShowCheckAll;

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public boolean isShowCheckAll() {
        return isShowCheckAll;
    }

    public void setShowCheckAll(boolean showCheckAll) {
        isShowCheckAll = showCheckAll;
    }

    public boolean isShowComment() {
        return commentBeans != null && commentBeans.size() > 0;
    }

    public boolean isShowPraise() {
        return praiseBeans != null && praiseBeans.size() > 0;
    }

    public OtherInfoBean getOtherInfoBean() {
        return otherInfoBean;
    }

    public void setOtherInfoBean(OtherInfoBean otherInfoBean) {
        this.otherInfoBean = otherInfoBean;
    }

    public void setUserBean(UserBean userBean) {
        this.userBean = userBean;
    }

    public UserBean getUserBean() {
        return userBean;
    }

    public int getViewType() {
        return viewType;
    }

    public void setViewType(int viewType) {
        this.viewType = viewType;
    }

    public String getContent() {
        return content;
    }

    public SpannableStringBuilder getContentSpan() {
        return contentSpan;
    }

    public void setContentSpan(SpannableStringBuilder contentSpan) {
        this.contentSpan = contentSpan;
        this.isShowCheckAll = Utils.calculateShowCheckAllText(contentSpan.toString());
    }

    private SpannableStringBuilder contentSpan;


    public void setContent(String content) {
        this.content = content;
        if (!TextUtils.isEmpty(content)) {
            setContentSpan(new SpannableStringBuilder(content));
        }
    }

    public List<CommentBean> getCommentBeans() {
        return commentBeans;
    }

    public void setCommentBeans(List<CommentBean> commentBeans) {
        this.commentBeans = commentBeans;
    }

    public List<PraiseBean> getPraiseBeans() {
        return praiseBeans;
    }

    public void setPraiseBeans(List<PraiseBean> praiseBeans) {
        this.praiseBeans = praiseBeans;
    }


    public List<MediaEntry> getMediaEntries() {
        return mediaEntries;
    }

    public void setMediaEntries(List<MediaEntry> mediaEntries) {
        this.mediaEntries = mediaEntries;
    }


    public void setPraiseSpan(SpannableStringBuilder praiseSpan) {
        this.praiseSpan = praiseSpan;
    }

    public SpannableStringBuilder getPraiseSpan() {
        return praiseSpan;
    }

    private SpannableStringBuilder praiseSpan;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
