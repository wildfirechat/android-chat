package cn.wildfire.chat.kit.conversation.file;

import android.content.Intent;

import butterknife.OnClick;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfire.chat.kit.WfcBaseActivity;

public class FileRecordListActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.activity_file_record_list;
    }

    @Override
    protected void afterViews() {
        if (!isDarkTheme()) {
            setTitleBackgroundResource(R.color.white, false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @OnClick(R2.id.allFilesItemView)
    void allFiles() {
        Intent intent = new Intent(this, FileRecordActivity.class);
        startActivity(intent);
    }

    @OnClick(R2.id.myFilesItemView)
    void myFiles() {
        Intent intent = new Intent(this, FileRecordActivity.class);
        intent.putExtra("isMyFiles", true);
        startActivity(intent);
    }

    @OnClick(R2.id.conversationFilesItemView)
    void convFiles() {
        //Todo Select a conversation first.
//        Intent intent = new Intent(this, FileRecordActivity.class);
//        intent.putExtra("conversation", conversation);
//        startActivity(intent);
    }

    @OnClick(R2.id.userFilesItemView)
    void userFiles() {
        //Todo Select a user first.
//        Intent intent = new Intent(this, FileRecordActivity.class);
//        intent.putExtra("user", user);
//        startActivity(intent);
    }
}