/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation.file;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.pick.PickContactActivity;
import cn.wildfire.chat.kit.conversation.pick.PickConversationActivity;
import cn.wildfire.chat.kit.page.WfcPage;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfirechat.model.ConversationInfo;
import cn.wildfirechat.model.UserInfo;

/**
 * 「文件」入口页：全部文件 / 我的文件 / 按会话 / 按发送人，逐行搬自
 * {@link FileRecordListActivity}。
 * <p>
 * 后两项要先挑一个会话或一个联系人，因此走
 * {@link WfcPageCompat#startPageForResult}：裸 {@code startActivityForResult} 的 requestCode
 * 会被 FragmentManager 换成内部生成的码，右栏拿到后送不回本页。
 */
public class FileRecordListFragment extends Fragment implements WfcPage {

    private static final int PICK_CONVERSATION_REQUEST = 200;
    private static final int PICK_CONTACT_REQUEST = 201;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.file_record_list_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.allFilesItemView).setOnClickListener(v -> allFiles());
        view.findViewById(R.id.myFilesItemView).setOnClickListener(v -> myFiles());
        view.findViewById(R.id.conversationFilesItemView).setOnClickListener(v -> convFiles());
        view.findViewById(R.id.userFilesItemView).setOnClickListener(v -> userFiles());
    }

    private void allFiles() {
        WfcPageCompat.startPage(this, new Intent(getActivity(), FileRecordActivity.class));
    }

    private void myFiles() {
        Intent intent = new Intent(getActivity(), FileRecordActivity.class);
        intent.putExtra("isMyFiles", true);
        WfcPageCompat.startPage(this, intent);
    }

    private void convFiles() {
        Intent intent = new Intent(getActivity(), PickConversationActivity.class);
        WfcPageCompat.startPageForResult(this, intent, PICK_CONVERSATION_REQUEST);
    }

    private void userFiles() {
        Intent intent = PickContactActivity.buildPickIntent(getActivity(), 1, null, null);
        intent.putExtra("showChannel", false);
        WfcPageCompat.startPageForResult(this, intent, PICK_CONTACT_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode != Activity.RESULT_OK || data == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        if (requestCode == PICK_CONVERSATION_REQUEST) {
            ConversationInfo conversationInfo = data.getParcelableExtra("conversationInfo");
            if (conversationInfo != null) {
                Intent intent = new Intent(getActivity(), FileRecordActivity.class);
                intent.putExtra("conversation", conversationInfo.conversation);
                WfcPageCompat.startPage(this, intent);
            }
        } else if (requestCode == PICK_CONTACT_REQUEST) {
            ArrayList<UserInfo> userInfos = data.getParcelableArrayListExtra(PickContactActivity.RESULT_PICKED_USERS);
            if (userInfos != null && !userInfos.isEmpty()) {
                Intent intent = new Intent(getActivity(), FileRecordActivity.class);
                intent.putExtra("fromUser", userInfos.get(0).uid);
                WfcPageCompat.startPage(this, intent);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
