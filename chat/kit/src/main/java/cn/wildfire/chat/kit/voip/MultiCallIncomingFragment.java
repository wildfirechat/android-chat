package cn.wildfire.chat.kit.voip;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import butterknife.BindView;
import butterknife.OnClick;
import cn.wildfirechat.chat.R;

public class MultiCallIncomingFragment extends Fragment {

    @BindView(R.id.invitorImageView)
    ImageView invitorImageView;
    @BindView(R.id.invitorTextView)
    TextView invitorTextView;
    @BindView(R.id.participantGridView)
    GridView participantGridView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.av_multi_incoming, container, false);
        return view;
    }

    @OnClick(R.id.hangupImageView)
    void hangup() {

    }

    @OnClick(R.id.acceptImageView)
    void accept() {

    }
}
