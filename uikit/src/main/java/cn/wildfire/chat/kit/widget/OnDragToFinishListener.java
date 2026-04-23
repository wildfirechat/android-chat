/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.widget;

import android.graphics.RectF;

public interface OnDragToFinishListener {
    void onDragToFinish();

    /**
     * Called when drag-to-dismiss is triggered, providing the child's current
     * screen rect so the caller can run a return animation.
     * Falls back to {@link #onDragToFinish()} by default.
     */
    default void onDragToFinishWithCurrentViewRect(RectF currentRect) {
        onDragToFinish();
    }

    void onDragOffset(float offset, float maxOffset);
}
