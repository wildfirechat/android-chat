package cn.wildfire.chat.kit.conversation.forward;

import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnTextChanged;
import cn.wildfire.chat.kit.WfcBaseActivity;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.group.GroupViewModel;
import cn.wildfire.chat.kit.search.OnResultItemClickListener;
import cn.wildfire.chat.kit.search.SearchFragment;
import cn.wildfire.chat.kit.search.SearchableModule;
import cn.wildfire.chat.kit.search.module.ContactSearchModule;
import cn.wildfire.chat.kit.search.module.GroupSearchViewModule;
import cn.wildfire.chat.kit.user.UserViewModel;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.R2;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.TextMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.model.GroupSearchResult;
import cn.wildfirechat.model.UserInfo;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;

public class ForwardActivity extends WfcBaseActivity {
    private SearchFragment searchFragment;
    private List<SearchableModule> searchableModules;
    private Message message;
    private ForwardViewModel forwardViewModel;
    private UserViewModel userViewModel;
    private GroupViewModel groupViewModel;

    @BindView(R2.id.searchEditText)
    EditText editText;


    @Override
    protected void afterViews() {
        message = getIntent().getParcelableExtra("message");
        getSupportFragmentManager()
                .beginTransaction()
                .add(R.id.containerFrameLayout, new ForwardFragment())
                .commit();
        forwardViewModel = ViewModelProviders.of(this).get(ForwardViewModel.class);
        userViewModel =ViewModelProviders.of(this).get(UserViewModel.class);
        groupViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
        initSearch();
    }

    private void initSearch() {
        searchableModules = new ArrayList<>();
        SearchableModule module = new ContactSearchModule();
        module.setOnResultItemListener(new OnResultItemClickListener<UserInfo>() {
            @Override
            public void onResultItemClick(Fragment fragment, View itemView, View view, UserInfo userInfo) {
                forward(userInfo);
            }
        });
        searchableModules.add(module);

        module = new GroupSearchViewModule();
        module.setOnResultItemListener(new OnResultItemClickListener<GroupSearchResult>() {
            @Override
            public void onResultItemClick(Fragment fragment, View itemView, View view, GroupSearchResult gr) {
                forward(gr.groupInfo);
            }
        });
        searchableModules.add(module);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        editText.setText("");
        editText.clearFocus();
    }

    @OnTextChanged(value = R2.id.searchEditText, callback = AFTER_TEXT_CHANGED)
    void search(Editable editable) {
        String keyword = editable.toString().trim();
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (!TextUtils.isEmpty(keyword)) {
            if (fragmentManager.findFragmentByTag("search") == null) {
                searchFragment = new SearchFragment();
                fragmentManager.beginTransaction()
                        .add(R.id.containerFrameLayout, searchFragment, "search")
                        .addToBackStack("search-back")
                        .commit();
            }
            new Handler().post(() -> {
                searchFragment.search(keyword, searchableModules);
            });
        } else {
            getSupportFragmentManager().popBackStackImmediate();
        }
    }

    public void forward(UserInfo targetUser) {
        Conversation conversation = new Conversation(Conversation.ConversationType.Single, targetUser.uid);
        conversation.line = 0;
        forward(targetUser.displayName, targetUser.portrait, conversation);
    }

    public void forward(GroupInfo targetGroup) {
        Conversation conversation = new Conversation(Conversation.ConversationType.Group, targetGroup.target);
        conversation.line = 0;
        forward(targetGroup.name, targetGroup.portrait, conversation);
    }

    public void forward(Conversation conversation) {
        switch (conversation.type) {
            case Single:
                UserInfo userInfo = userViewModel.getUserInfo(conversation.target, false);
                forward(userInfo);
                break;
            case Group:
                GroupInfo groupInfo = groupViewModel.getGroupInfo(conversation.target, false);
                forward(groupInfo);
                break;
            default:
                break;
        }

    }

    private void forward(String targetName, String targetPortrait, Conversation targetConversation) {
        ForwardPromptView view = new ForwardPromptView(this);
        view.bind(targetName, targetPortrait, message);
        MaterialDialog dialog = new MaterialDialog.Builder(this)
                .customView(view, false)
                .negativeText("取消")
                .positiveText("发送")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        Message extraMsg = null;
                        if (!TextUtils.isEmpty(view.getEditText())) {
                            TextMessageContent content = new TextMessageContent(view.getEditText());
                            extraMsg = new Message();
                            extraMsg.content = content;
                        }
                        forwardViewModel.forward(targetConversation, message, extraMsg)
                                .observe(ForwardActivity.this, new Observer<OperateResult<Integer>>() {
                                    @Override
                                    public void onChanged(@Nullable OperateResult<Integer> integerOperateResult) {
                                        if (integerOperateResult.isSuccess()) {
                                            Toast.makeText(ForwardActivity.this, "转发成功", Toast.LENGTH_SHORT).show();
                                            finish();
                                        } else {
                                            Toast.makeText(ForwardActivity.this, "转发失败" + integerOperateResult.getErrorCode(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });

                    }
                })
                .build();
        dialog.show();
    }

    @Override
    protected int contentLayout() {
        return R.layout.forward_activity;
    }
}
