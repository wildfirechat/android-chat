/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget;

import android.text.InputFilter;
import android.text.Spanned;

/**
 * This filter will constrain edits not to make the length of the text
 * greater than the specified length.
 */
public class LengthFilter implements InputFilter {
    private final int mMax;
    private OnMaxTextLengthExceedListener mMaxTextLengthExceedListener;

    public LengthFilter(int max, OnMaxTextLengthExceedListener listener) {
        mMax = max;
        mMaxTextLengthExceedListener = listener;
    }

    public CharSequence filter(CharSequence source, int start, int end, Spanned dest,
                               int dstart, int dend) {
        int keep = mMax - (dest.length() - (dend - dstart));
        if (keep <= 0) {
            if (mMaxTextLengthExceedListener != null) {
               mMaxTextLengthExceedListener.onMaxTextLengthExceed(mMax);
            }
            return "";
        } else if (keep >= end - start) {
            return null; // keep original
        } else {
            keep += start;
            if (Character.isHighSurrogate(source.charAt(keep - 1))) {
                --keep;
                if (keep == start) {
                    return "";
                }
            }
            return source.subSequence(start, keep);
        }
    }

    /**
     * @return the maximum length enforced by this input filter
     */
    public int getMax() {
        return mMax;
    }

    public interface OnMaxTextLengthExceedListener {
        void onMaxTextLengthExceed(int maxTextLength);
    }
}
