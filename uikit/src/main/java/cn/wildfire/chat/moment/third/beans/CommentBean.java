package cn.wildfire.chat.moment.third.beans;

import android.content.Context;
import android.text.SpannableStringBuilder;

import cn.wildfire.chat.moment.third.Constants;
import cn.wildfire.chat.moment.third.interfaces.OnCommentUserClickListener;
import cn.wildfire.chat.moment.third.utils.SpanUtils;

public class CommentBean {

    private long id;
    private int commentType;

    private String parentUserName;

    private String childUserName;

    private String parentUserId;

    private String childUserId;

    private String commentContent;

    public int getCommentType() {
        return commentType;
    }

    public void setCommentType(int commentType) {
        this.commentType = commentType;
    }

    public String getParentUserName() {
        return parentUserName;
    }

    public void setParentUserName(String parentUserName) {
        this.parentUserName = parentUserName;
    }

    public String getChildUserName() {
        return childUserName;
    }

    public void setChildUserName(String childUserName) {
        this.childUserName = childUserName;
    }

    public String getParentUserId() {
        return parentUserId;
    }

    public void setParentUserId(String parentUserId) {
        this.parentUserId = parentUserId;
    }

    public String getChildUserId() {
        return childUserId;
    }

    public void setChildUserId(String childUserId) {
        this.childUserId = childUserId;
    }

    public String getCommentContent() {
        return commentContent;
    }

    public void setCommentContent(String commentContent) {
        this.commentContent = commentContent;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    /**
     * 富文本内容
     */
    private SpannableStringBuilder commentContentSpan;

    public SpannableStringBuilder getCommentContentSpan() {
        return commentContentSpan;
    }

    public void build(Context context, OnCommentUserClickListener listener) {
        if (commentType == Constants.CommentType.COMMENT_TYPE_SINGLE) {
            commentContentSpan = SpanUtils.makeSingleCommentSpan(context, childUserId, childUserName, commentContent, listener);
        } else {
            commentContentSpan = SpanUtils.makeReplyCommentSpan(context, parentUserId, parentUserName, childUserId, childUserName, commentContent, listener);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CommentBean that = (CommentBean) o;

        return id == that.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
