/*
 * Copyright (c) 2025 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.net;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import cn.wildfirechat.remote.ChatManager;
import okhttp3.Response;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;

public class SimpleEventSourceListener extends EventSourceListener {
    public SimpleEventSourceListener() {
        super();
    }

    @Override
    public void onClosed(@NonNull EventSource eventSource) {
        ChatManager.Instance().getMainHandler().post(() -> {
            onUiClosed(eventSource);
        });
    }

    @Override
    public void onEvent(@NonNull EventSource eventSource, @Nullable String id, @Nullable String type, @NonNull String data) {
        ChatManager.Instance().getMainHandler().post(() -> {
            try {
                onUiEvent(eventSource, id, type, data);
            } catch (Exception e) {
                onUiFailure(eventSource, e, null);
            }
        });
    }

    @Override
    public void onFailure(@NonNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
        ChatManager.Instance().getMainHandler().post(() -> {
            onUiFailure(eventSource, t, response);
        });
    }

    @Override
    public void onOpen(@NonNull EventSource eventSource, @NonNull Response response) {
        super.onOpen(eventSource, response);
    }

    public void onUiEvent(EventSource eventSource, String id, String type, String data) {
        // Handle UI events here
    }

    public void onUiFailure(EventSource eventSource, Throwable t, Response response) {
        // Handle UI failure here
    }

    public void onUiClosed(@NonNull EventSource eventSource) {

    }
}
