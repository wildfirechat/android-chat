/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.conversation.mention.Mention;
import cn.wildfire.chat.kit.conversation.mention.MentionSpan;
import cn.wildfirechat.model.QuoteInfo;

public class Draft {
    private String content;
    private int emojiCount;
    private List<Mention> mentions;
    private QuoteInfo quoteInfo;

    public String getContent() {
        return content;
    }

    public List<Mention> getMentions() {
        return mentions;
    }

    public int getEmojiCount() {
        return emojiCount;
    }

    public QuoteInfo getQuoteInfo() {
        return quoteInfo;
    }

    public void setQuoteInfo(QuoteInfo quoteInfo) {
        this.quoteInfo = quoteInfo;
    }

    public static Draft toDraft(Editable content, int emojiCount) {
        return toDraft(content, emojiCount, null);
    }

    public static Draft toDraft(Editable content, int emojiCount, QuoteInfo quoteInfo) {
        Draft draft = new Draft();
        draft.content = content.toString();
        draft.emojiCount = emojiCount;
        draft.quoteInfo = quoteInfo;

        List<Mention> mentions;
        MentionSpan[] mentionSpans = content.getSpans(0, content.length(), MentionSpan.class);
        if (mentionSpans != null && mentionSpans.length > 0) {
            mentions = new ArrayList<>();
            Mention mention;
            for (MentionSpan span : mentionSpans) {
                mention = new Mention(content.getSpanStart(span), content.getSpanEnd(span), content.getSpanFlags(span));
                if (span.isMentionAll()) {
                    mention.setMentionAll(true);
                } else {
                    mention.setUid(span.getUid());
                }

                mentions.add(mention);
            }
            draft.mentions = mentions;
        }
        return draft;
    }

    public static Draft fromDraftJson(String json) {

        if (TextUtils.isEmpty(json)) {
            return null;
        }
        return new Gson().fromJson(json, Draft.class);
    }

    public static String toDraftJson(Editable content, int emojiCount) {
        return toDraftJson(content, emojiCount, null);
    }

    public static String toDraftJson(Editable content, int emojiCount, QuoteInfo quoteInfo) {
        if (TextUtils.isEmpty(content) && quoteInfo == null) {
            return null;
        }
        Draft draft = toDraft(content, emojiCount, quoteInfo);
        return new Gson().toJson(draft);
    }

    public static CharSequence parseDraft(String draftJson) {
        Draft draft = fromDraftJson(draftJson);
        if (draft == null) {
            return "";
        }
        SpannableStringBuilder builder = new SpannableStringBuilder(draft.content);
        if (draft.mentions != null && !draft.mentions.isEmpty()) {
            for (Mention mention : draft.mentions) {
                MentionSpan span;
                if (mention.isMentionAll()) {
                    span = new MentionSpan(true);
                } else {
                    span = new MentionSpan(mention.getUid());
                }
                builder.setSpan(span, mention.getStart(), mention.getEnd(), mention.getFlags());
            }
        }
        return builder;
    }

    @Override
    public String toString() {
        return content;
    }
}
