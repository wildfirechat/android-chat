package cn.wildfire.chat.kit.conversation.forward;

import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.contact.model.UIUserInfo;
import cn.wildfire.chat.kit.contact.pick.PickConversationTargetActivity;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfirechat.model.GroupInfo;

public class PickConversationTargetToForwardActivity extends PickConversationTargetActivity {
    // TODO 多选，单选
    // 先支持单选
    private boolean singleMode = true;

    @Override
    protected void onContactPicked(List<UIUserInfo> initialCheckedUserInfos, List<UIUserInfo> newlyCheckedUserInfos) {
        if (singleMode && newlyCheckedUserInfos.size() > 1) {
            // 先创建群组
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .content("创建中...")
                    .progress(true, 100)
                    .build();
            dialog.show();
            GroupViewModel groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
            groupViewModel.createGroup(this, newlyCheckedUserInfos)
                    .observe(this, new Observer<OperateResult<String>>() {
                        @Override
                        public void onChanged(@Nullable OperateResult<String> result) {
                            dialog.dismiss();
                            if (result.isSuccess()) {
                                GroupInfo groupInfo = groupViewModel.getGroupInfo(result.getResult(), false);
                                Intent intent = new Intent();
                                intent.putExtra("groupInfo", groupInfo);
                                setResult(RESULT_OK, intent);
                                finish();
                            } else {
                                Toast.makeText(PickConversationTargetToForwardActivity.this, "create group error", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        } else {
            Intent intent = new Intent();
            intent.putExtra("userInfo", newlyCheckedUserInfos.get(0).getUserInfo());
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    // TODO 多选
    @Override
    public void onGroupPicked(List<GroupInfo> groupInfos) {
        Intent intent = new Intent();
        intent.putExtra("groupInfo", groupInfos.get(0));
        setResult(RESULT_OK, intent);
        finish();
    }
}
