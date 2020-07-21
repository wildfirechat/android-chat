package cn.wildfire.chat.kit.conversation.mention;

import java.util.List;

import cn.wildfire.chat.kit.search.SearchActivity;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfirechat.model.GroupInfo;

public class MentionGroupMemberActivity extends SearchActivity {
    private GroupInfo groupInfo;

    @Override
    protected void beforeViews() {
        super.beforeViews();
        groupInfo = getIntent().getParcelableExtra("groupInfo");
    }

    @Override
    protected boolean hideSearchDescView() {
        return true;
    }

    @Override
    protected void afterViews() {
        super.afterViews();
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.mentionGroupMemberContainer, MentionGroupMemberFragment.newInstance(groupInfo))
            .commit();
    }

    @Override
    protected void initSearchModule(List<SearchableModule> modules) {
        modules.add(new GroupMemberSearchModule(groupInfo.target));
    }

    @Override
    protected int contentLayout() {
        return R.layout.group_mention_activity;
    }
}
