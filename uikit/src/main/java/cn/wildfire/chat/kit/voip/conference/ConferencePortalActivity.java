package cn.wildfire.chat.kit.voip.conference;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;

public class ConferencePortalActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.av_conference_portal_activity;
    }


    @OnClick(R2.id.startConferenceLinearLayout)
    void startConference() {
        Intent intent = new Intent(this, CreateConferenceActivity.class);
        startActivity(intent);
    }

    @OnClick(R2.id.joinConferenceLinearLayout)
    void joinConference() {
        View view = LayoutInflater.from(this).inflate(R.layout.av_conference_join_dialog, null);
        new MaterialDialog.Builder(this)
            .customView(view, false)
            .cancelable(false)
            .negativeText("取消")
            .positiveText("确认")
            .onPositive(new MaterialDialog.SingleButtonCallback() {
                @Override
                public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                    EditText callIdEditText = view.findViewById(R.id.callIdEditText);
                    EditText passwordEditText = view.findViewById(R.id.passwordEditText);
                    Intent intent = new Intent(ConferencePortalActivity.this, ConferenceInfoActivity.class);
                    intent.putExtra("conferenceId", callIdEditText.getText().toString());
                    intent.putExtra("password", passwordEditText.getText().toString());
                    startActivity(intent);
                }
            })
            .build()
            .show();
    }

    @OnClick(R2.id.bookConferenceLinearLayout)
    void orderConference() {
        Intent intent = new Intent(this, OrderConferenceActivity.class);
        startActivity(intent);
    }
}