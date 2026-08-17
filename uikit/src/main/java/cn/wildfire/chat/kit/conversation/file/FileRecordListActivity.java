package cn.wildfire.chat.kit.conversation.file;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcBaseActivity;

/**
 * 「文件」入口页在手机端的外壳，页面本体见 {@link FileRecordListFragment}。
 */
public class FileRecordListActivity extends WfcBaseActivity {

    @Override
    protected int contentLayout() {
        return R.layout.fragment_container_activity;
    }

    @Override
    protected void afterViews() {
        if (!isDarkTheme()) {
            setTitleBackgroundResource(R.color.white, false);
        }
        getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.containerFrameLayout, new FileRecordListFragment())
            .commit();
    }
}
