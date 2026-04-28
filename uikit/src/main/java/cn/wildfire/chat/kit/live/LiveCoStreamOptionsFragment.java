/*
 * Copyright (c) 2024 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.live;

import android.Manifest;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import cn.wildfire.chat.kit.R;
import cn.wildfirechat.message.LiveStreamingStartMessageContent;
import cn.wildfirechat.uikit.permission.PermissionKit;

/**
 * 观众发起连麦的选项 BottomSheet
 * <p>两个选项：视频连麦 / 音频连麦。选择后检查权限，然后向主播发送连麦申请。</p>
 */
public class LiveCoStreamOptionsFragment extends BottomSheetDialogFragment {

    private static final String ARG_CALL_ID    = "callId";
    private static final String ARG_PIN        = "pin";
    private static final String ARG_HOST       = "host";
    private static final String ARG_TITLE      = "title";

    private String callId;
    private String pin;
    private String host;
    private String title;

    public static LiveCoStreamOptionsFragment newInstance(LiveStreamingStartMessageContent content) {
        LiveCoStreamOptionsFragment f = new LiveCoStreamOptionsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CALL_ID, content.getCallId());
        args.putString(ARG_PIN, content.getPin());
        args.putString(ARG_HOST, content.getHost());
        args.putString(ARG_TITLE, content.getTitle());
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            callId = getArguments().getString(ARG_CALL_ID);
            pin    = getArguments().getString(ARG_PIN);
            host   = getArguments().getString(ARG_HOST);
            title  = getArguments().getString(ARG_TITLE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_live_co_stream_options, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.videoCoStreamOption).setOnClickListener(v -> onCoStreamSelected(false));
        view.findViewById(R.id.audioCoStreamOption).setOnClickListener(v -> onCoStreamSelected(true));
    }

    private void onCoStreamSelected(boolean audioOnly) {
        String[] perms = audioOnly
            ? new String[]{Manifest.permission.RECORD_AUDIO}
            : new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

        PermissionKit.PermissionReqTuple[] tuples =
            PermissionKit.buildRequestPermissionTuples(requireActivity(), perms);
        PermissionKit.checkThenRequestPermission(requireActivity(),
            getChildFragmentManager(), tuples, allGranted -> {
                if (Boolean.TRUE.equals(allGranted)) {
                    LiveStreamingKit.getInstance().requestCoStream(host, callId, pin, host, title);
                    dismiss();
                }
            });
    }
}
