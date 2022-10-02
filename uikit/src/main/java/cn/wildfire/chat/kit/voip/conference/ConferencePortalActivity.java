package cn.wildfire.chat.kit.voip.conference;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.king.zxing.Intents;

import java.util.HashMap;
import java.util.Map;

import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;

public class ConferencePortalActivity extends WfcBaseActivity {

    private static final int REQUEST_CODE_SCAN_QR_CODE = 100;

    @Override
    protected int contentLayout() {
        return R.layout.av_conference_portal_activity;
    }


    @OnClick(R2.id.startConferenceLinearLayout)
    void startConference() {
        Intent intent = new Intent(this, CreateConferenceActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_SCAN_QR_CODE:
                if (resultCode == RESULT_OK) {
                    String result = data.getStringExtra(Intents.Scan.RESULT);
                    onScanPcQrCode(result);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void onScanPcQrCode(String qrcode) {
        String prefix = qrcode.substring(0, qrcode.lastIndexOf('/') + 1);
        String value = qrcode.substring(qrcode.lastIndexOf("/") + 1);
        switch (prefix) {
            case "wfzoom://":
                String[] pairs = value.split("&");
                Map<String, String> queryPairs = new HashMap<>();
                for (String pair : pairs) {
                    int index = pair.indexOf("=");
                    queryPairs.put(pair.substring(0, index), pair.substring(index + 1));
                }
                String conferenceId = queryPairs.get("id");
                String password = queryPairs.get("pwd");
                Intent intent = new Intent(this, ConferenceInfoActivity.class);
                intent.putExtra("conferenceId", conferenceId);
                intent.putExtra("password", password);
                startActivity(intent);
                break;
            default:
                Toast.makeText(this, "qrcode: " + qrcode, Toast.LENGTH_SHORT).show();
                break;
        }
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
        Intent intent = new Intent(this, BookConferenceActivity.class);
        startActivity(intent);
    }
}