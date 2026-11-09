package cn.wildfire.chat.moment.third.beans;

import cn.wildfirechat.moment.message.FeedCommentMessageContent;
import cn.wildfirechat.moment.message.FeedMessageContent;

public class MomentMessageBean {

    public static final int MESSAGE_TYPE_FEED = 0;
    public static final int MESSAGE_TYPE_COMMENT = 1;

    private long feedId;
    private long commentId;
    private int messageType;
    private int feedType;
    private int commentType;
    private String userId;
    private String userName;
    private String userPortraitUrl;
    private String content;
    private long timestamp;
    private String[] mediaUrls;

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public int getFeedType() {
        return feedType;
    }

    public void setFeedType(int feedType) {
        this.feedType = feedType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPortraitUrl() {
        return userPortraitUrl;
    }

    public void setUserPortraitUrl(String userPortraitUrl) {
        this.userPortraitUrl = userPortraitUrl;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String[] getMediaUrls() {
        return mediaUrls;
    }

    public void setMediaUrls(String[] mediaUrls) {
        this.mediaUrls = mediaUrls;
    }

    // 消息里面包含了feed所有的信息
    public static MomentMessageBean fromFeedMessage(FeedMessageContent feedMessageContent) {
        MomentMessageBean bean = new MomentMessageBean();
        bean.feedId = feedMessageContent.getFeedId();
        bean.messageType = MESSAGE_TYPE_FEED;
//        private long feedId;
//        private long commentId;
//        private int messageType;
//        private int type;
//        private int feedType;
//        private int commentType;
//        private String userId;
//        private String userName;
//        private String userPortraitUrl;
//        private String content;
//        private long timestamp;
//        private String[] mediaUrls;
        bean.feedType= feedMessageContent.getFeedType();

        return bean;
    }

    // 消息里面只包含评论相关信息，还得通过feedId去拿feed相关信息
    public static MomentMessageBean fromFeedCommentMessage(FeedCommentMessageContent feedCommentMessageContent) {
        MomentMessageBean bean = new MomentMessageBean();
//        bean.type = feedCommentMessageContent.getCommentType();
        bean.commentId = feedCommentMessageContent.getCommentId();
        bean.messageType = MESSAGE_TYPE_COMMENT;

        return bean;
    }
}
