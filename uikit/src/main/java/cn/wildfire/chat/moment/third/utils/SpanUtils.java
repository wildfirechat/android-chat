package cn.wildfire.chat.moment.third.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import androidx.core.content.ContextCompat;

import java.util.List;

import cn.wildfire.chat.moment.third.beans.PraiseBean;
import cn.wildfire.chat.moment.third.interfaces.OnCommentUserClickListener;
import cn.wildfire.chat.moment.third.span.CommentUserSpan;
import cn.wildfire.chat.moment.third.span.VerticalImageSpan;
import cn.wildfire.chat.kit.R;

/**
 * @author KCrason
 * @date 2018/5/2
 */
public class SpanUtils {

    public static SpannableStringBuilder makeSingleCommentSpan(Context context, String childUserId, String childUserName, String commentContent, OnCommentUserClickListener listener) {
        String richText = String.format("%s: %s", childUserName, commentContent);
        SpannableStringBuilder builder = new SpannableStringBuilder(richText);
        if (!TextUtils.isEmpty(childUserName)) {
            builder.setSpan(new CommentUserSpan(context, childUserId, childUserName, listener), 0, childUserName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new StyleSpan(Typeface.BOLD), 0, childUserName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }

    public static SpannableStringBuilder makeReplyCommentSpan(Context context, String parentUserId, String parentUserName, String childUserId, String childUserName, String commentContent, OnCommentUserClickListener listener) {
        String richText = String.format("%s回复%s: %s", childUserName, parentUserName, commentContent);
        SpannableStringBuilder builder = new SpannableStringBuilder(richText);
        int childEnd = 0;
        if (!TextUtils.isEmpty(childUserName)) {
            childEnd = childUserName.length();
            builder.setSpan(new CommentUserSpan(context, parentUserId, childUserName, listener), 0, childEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (!TextUtils.isEmpty(parentUserName)) {
            int parentStart = childEnd + 2;
            int parentEnd = parentStart + parentUserName.length();
            builder.setSpan(new CommentUserSpan(context, parentUserId, parentUserName, listener), parentStart, parentEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }

    public static SpannableStringBuilder makePraiseSpan(Context context, List<PraiseBean> praiseBeans, OnCommentUserClickListener listener) {
        if (praiseBeans != null && praiseBeans.size() > 0) {
            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append("  ");
            int praiseSize = praiseBeans.size();
            for (int i = 0; i < praiseSize; i++) {
                PraiseBean praiseBean = praiseBeans.get(i);
                String praiseUserName = praiseBean.getPraiseUserName();
                int start = builder.length();
                int end = start + praiseUserName.length();
                builder.append(praiseUserName);
                if (i != praiseSize - 1) {
                    builder.append(", ");
                }
                builder.setSpan(new CommentUserSpan(context, praiseBean.getPraiseUserId(), praiseUserName, listener), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            builder.setSpan(new VerticalImageSpan(ContextCompat.getDrawable(context, R.drawable.heart_drawable_blue)),
                    0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return builder;
        }
        return null;
    }
}
