/*
 * Copyright (c) 2021 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.app.ptt;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.afollestad.materialdialogs.MaterialDialog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import cn.wildfire.chat.app.AppService;
import cn.wildfire.chat.kit.GlideApp;
import cn.wildfire.chat.kit.net.SimpleCallback;
import cn.wildfire.chat.kit.voip.VoipBaseActivity;
import cn.wildfire.chat.kit.voip.conference.ConferenceParticipantListActivity;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.chat.R;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class PttActivity extends VoipBaseActivity {
    private static final int REQUEST_CODE_ADD_PARTICIPANT = 103;

    @BindView(R.id.membersTextView)
    TextView membersTextView;
    @BindView(R.id.talkingMemberLayout)
    LinearLayout talkingMemberLayout;
    @BindView(R.id.portraitImageView)
    ImageView portraitImageView;
    @BindView(R.id.nameTextView)
    TextView nameTextView;
    @BindView(R.id.talkButton)
    Button talkButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.av_ptt_activity);
        ButterKnife.bind(this);
        updateParticipantCount();
    }

    @OnTouch(R.id.talkButton)
    public boolean talk(View button, MotionEvent event) {
        AVEngineKit.CallSession callSession = AVEngineKit.Instance().getCurrentSession();
        if (callSession == null) {
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            updateTalkButton(true);
            callSession.requestTalk(new AVEngineKit.GeneralCallback() {
                @Override
                public void onSuccess() {
                    String selfUid = ChatManager.Instance().getUserId();
                    if (selfUid.equals(callSession.pttTalkingMember)) {
                        didPttTalking(selfUid);
                    }
                }

                @Override
                public void onFailure(int error_code) {
                    nameTextView.setText("抢麦失败 " + error_code);

                }
            });

        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            updateTalkButton(false);
            callSession.releaseTalk();
            didPttIdle();
        }
        return true;
    }

    @OnClick(R.id.minimizeImageView)
    public void minimize() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null) {
            return;
        }
        String[] items;
        if (!ChatManager.Instance().getUserId().equals(session.getHost())) {
            items = new String[]{"最小化对讲机", "退出对讲机"};
        } else {
            items = new String[]{"最小化对讲机", "退出对讲机", "结束对讲"};
        }

        new MaterialDialog.Builder(this)
            .items(items)
            .itemsCallback(new MaterialDialog.ListCallback() {
                @Override
                public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                    switch (position) {
                        case 0:
                            finishFadeout();
                            break;
                        case 1:
                            session.leavePttChannel();
                            finishFadeout();
                            break;
                        case 2:
                            AppService.Instance().destroyPttChannel(session.getCallId(), new SimpleCallback<Void>() {
                                @Override
                                public void onUiSuccess(Void unused) {
                                    Toast.makeText(PttActivity.this, "销毁对讲频道成功", Toast.LENGTH_SHORT).show();
                                    finish();
                                }

                                @Override
                                public void onUiFailure(int code, String msg) {
                                    Toast.makeText(PttActivity.this, "销毁对讲频道失败 " + code, Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                        default:
                            break;
                    }

                }
            })
            .build()
            .show();
    }

    @OnClick(R.id.membersTextView)
    public void members() {
        isInvitingNewParticipant = true;
        Intent intent = new Intent(this, ConferenceParticipantListActivity.class);
        startActivityForResult(intent, REQUEST_CODE_ADD_PARTICIPANT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onActivityResult(requestCode, resultCode, data);
        }
        if (requestCode == REQUEST_CODE_ADD_PARTICIPANT) {
            isInvitingNewParticipant = false;
        }
    }

    private void updateTalkButton(boolean talking) {
        talkButton.setPressed(talking);
        talkButton.setText(talking ? "松手释放" : "按下说话");
        float scale = talking ? 1.5f : 1.0f;
        talkButton.animate().scaleX(scale).scaleY(scale).setDuration(100).start();
    }

    private void updateParticipantCount() {
        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
        if (session == null) {
            return;
        }
        int count = session.getParticipantIds().size() + 1;
        membersTextView.setText("(" + count + ")");
    }

    @Override
    public void didPttTalking(String userId) {
        postAction(() -> {
            AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
            if (session == null) {
                return;
            }
            talkingMemberLayout.setVisibility(View.VISIBLE);
            UserInfo userInfo = ChatManager.Instance().getUserInfo(userId, false);
            GlideApp.with(this).load(userInfo.portrait).placeholder(R.mipmap.avatar_def).into(portraitImageView);
            nameTextView.setText(userInfo.displayName);
        });
    }

    @Override
    public void didPttIdle() {
        postAction(() -> {
            talkingMemberLayout.setVisibility(View.GONE);
        });
    }

    @Override
    public void didParticipantJoined(String s) {
        updateParticipantCount();
    }

    @Override
    public void didParticipantLeft(String s, AVEngineKit.CallEndReason callEndReason) {
        updateParticipantCount();
    }
}
