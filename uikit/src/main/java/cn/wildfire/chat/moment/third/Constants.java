package cn.wildfire.chat.moment.third;

/**
 * @author KCrason
 * @date 2018/4/27
 */
public class Constants {

    public static final String EMOJI_REGEX = "\\[([\u4e00-\u9fa5\\w])+\\]|[\\ud83c\\udc00-\\ud83c\\udfff]|[\\ud83d\\udc00-\\ud83d\\udfff]|[\\u2600-\\u27ff]";

    public static final String URL_REGEX = "(((http|https)://)|((?<!((http|https)://))www\\.))" + ".*?" + "(?=(&nbsp;|[\\u4e00-\\u9fa5]|\\s|　|<br />|$|[<>]))";

    public static final String TOPIC_REGEX = "#[\\p{Print}\\p{InCJKUnifiedIdeographs}&&[^#]]+#";

    public static final String AT_REGEX = "@[\u4e00-\u9fa5a-zA-Z0-9_-·\\.]+[\u200B]";

    public static final String SCHEME_URL = "com.kcrason.url//";

    public static final String SCHEME_EMOJI = "com.kcrason.emoji//";

    public static final String SCHEME_TOPIC = "com.kcrason.topic//";

    public static final String SCHEME_AT = "com.kcrason.at//";

    public static final String BLUE = "#ff0000";


    public final static class FriendCircleType {
        public static final int FRIEND_CIRCLE_TYPE_HEADER = -1;
        public static final int FRIEND_CIRCLE_TYPE_FOOTER = -2;
        //纯文字
        public final static int FRIEND_CIRCLE_TYPE_ONLY_WORD = 0;
        //文字和图片
        public final static int FRIEND_CIRCLE_TYPE_WORD_AND_IMAGES = 1;
        //分享链接
        public final static int FRIEND_CIRCLE_TYPE_WORD_AND_URL = 2;
    }

    public final static class CommentType {
        //单一评论
        public final static int COMMENT_TYPE_SINGLE = 0;
        //回复评论
        public final static int COMMENT_TYPE_REPLY = 1;
    }

}
