package cn.wildfire.chat.kit.conversation;

import android.content.Intent;
import android.widget.Toast;

import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickConversationTargetActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.R;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.UserInfo;

public class CreateConversationActivity extends PickConversationTargetActivity {
    private GroupViewModel groupViewModel;

    @Override
    protected void afterViews() {
        super.afterViews();
        groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onContactPicked(List<UIUserInfo> newlyCheckedUserInfos) {
        List<String> initialCheckedIds = pickUserViewModel.getInitialCheckedIds();
        List<UserInfo> userInfos = null;
        if (initialCheckedIds != null && !initialCheckedIds.isEmpty()) {
            UserViewModel userViewModel = new ViewModelProvider(this).get(UserViewModel.class);
            userInfos = userViewModel.getUserInfos(initialCheckedIds);
        }
        userInfos = userInfos == null ? new ArrayList<>() : userInfos;

        for (UIUserInfo uiUserinfo : newlyCheckedUserInfos) {
            userInfos.add(uiUserinfo.getUserInfo());
        }

        if (userInfos.size() == 1) {

            Intent intent = new Intent(this, ConversationActivity.class);
            Conversation conversation = new Conversation(Conversation.ConversationType.Single, userInfos.get(0).uid);
            intent.putExtra("conversation", conversation);
            startActivity(intent);
            finish();
        } else {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                .content("创建中...")
                .progress(true, 100)
                .build();
            dialog.show();

            groupViewModel.createGroup(this, userInfos, null, Arrays.asList(0)).observe(this, result -> {
                dialog.dismiss();
                if (result.isSuccess()) {
                    Toast.makeText(this, getString(R.string.create_group_success), Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(CreateConversationActivity.this, ConversationActivity.class);
                    Conversation conversation = new Conversation(Conversation.ConversationType.Group, result.getResult(), 0);
                    intent.putExtra("conversation", conversation);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, getString(R.string.create_group_fail), Toast.LENGTH_SHORT).show();
                }
                finish();
            });
        }

    }

    @Override
    public void onGroupPicked(List<GroupInfo> groupInfos) {
        Intent intent = new Intent(this, ConversationActivity.class);
        Conversation conversation = new Conversation(Conversation.ConversationType.Group, groupInfos.get(0).target);
        intent.putExtra("conversation", conversation);
        startActivity(intent);
        finish();
    }
}
