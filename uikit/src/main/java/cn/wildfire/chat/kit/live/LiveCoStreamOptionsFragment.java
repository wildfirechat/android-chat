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
import cn.wildfire.chat.kit.live.model.LiveInfo;
import cn.wildfirechat.uikit.permission.PermissionKit;

/**
 * 观众发起连麦的选项 BottomSheet
 * <p>两个选项：视频连麦 / 音频连麦。选择后检查权限，然后向主播发送连麦申请。</p>
 */
public class LiveCoStreamOptionsFragment extends BottomSheetDialogFragment {

    private static final String ARG_LIVE_INFO = "liveInfo";
    private LiveInfo liveInfo;

    public static LiveCoStreamOptionsFragment newInstance(LiveInfo info) {
        LiveCoStreamOptionsFragment f = new LiveCoStreamOptionsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_LIVE_INFO, info);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            liveInfo = getArguments().getParcelable(ARG_LIVE_INFO);
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
        View cancelBtn = view.findViewById(R.id.cancelOption);
        if (cancelBtn != null) {
            cancelBtn.setOnClickListener(v -> dismiss());
        }
    }

    private void onCoStreamSelected(boolean audioOnlyCoStream) {
        String[] perms = audioOnlyCoStream
                ? new String[]{Manifest.permission.RECORD_AUDIO}
                : new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

        PermissionKit.PermissionReqTuple[] tuples =
                PermissionKit.buildRequestPermissionTuples(requireActivity(), perms);
        PermissionKit.checkThenRequestPermission(requireActivity(),
                getChildFragmentManager(), tuples, allGranted -> {
                    if (Boolean.TRUE.equals(allGranted)) {
                        LiveStreamingKit.getInstance().requestCoStream(liveInfo.getHost(), liveInfo.getLiveId(), liveInfo.isAudioOnly(), liveInfo.getPin(), liveInfo.getTitle(), audioOnlyCoStream);
                        dismiss();
                    }
                });
    }
}
