package cn.wildfire.chat.kit.voip;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfirechat.chat.R;

public class MultiCallOutgoingConnectedFragment extends Fragment {
    @BindView(R.id.participantGridLayout)
    GridLayout participantGridLayout;

    @BindView(R.id.durationTextView)
    TextView durationTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_multi_outgoing_connected, container, false);
        return view;
    }

    @OnClick(R.id.minimizeImageView)
    void minimize() {

    }

    @OnClick(R.id.addParticipantImageView)
    void addParticipant() {

    }

    @OnClick(R.id.muteImageView)
    void mute() {

    }

    @OnClick(R.id.speakerImageView)
    void speaker() {

    }

    @OnClick(R.id.videoImageView)
    void video() {

    }

    @OnClick(R.id.hangupImageView)
    void hangup() {

    }
}
