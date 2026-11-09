package cn.wildfire.chat.moment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.contact.pick.PickContactActivity;
import cn.wildfire.chat.kit.page.WfcPageCompat;
import cn.wildfirechat.model.UserInfo;

/**
 * 「谁可以看」：公开 / 私密 / 部分可见 / 不给谁看。入口是发表页的「谁可以看」一栏，
 * 选完点标题栏「确定」把结果回传给 {@link PublishFeedPageFragment}。
 * <p>
 * 逐行搬自 {@link FeedVisibleScopeActivity}，那个类现在只是手机端的壳。
 * {@code setResult + finish} 换成 {@link WfcPageCompat#setPageResult} +
 * {@link WfcPageCompat#finishPage}，两端语义一一对应。
 */
public class FeedVisibleScopePageFragment extends MomentPageFragment {

    private RadioGroup radioGroup;

    private static final int REQUEST_CODE_PICK_TO_BLOCK = 100;
    private static final int REQUEST_CODE_PICK_TO_FETCH = 101;

    private int mode = FeedVisibleScopeActivity.VISIBLE_MODE_ALL;
    private ArrayList<String> uids;
    private ArrayList<UserInfo> users = new ArrayList<>();

    public static FeedVisibleScopePageFragment fromIntent(Intent intent) {
        FeedVisibleScopePageFragment fragment = new FeedVisibleScopePageFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(FeedVisibleScopeActivity.PARAM_UIDS,
            intent.getStringArrayListExtra(FeedVisibleScopeActivity.PARAM_UIDS));
        args.putInt(FeedVisibleScopeActivity.PARAM_VISIBLE_MODE,
            intent.getIntExtra(FeedVisibleScopeActivity.PARAM_VISIBLE_MODE, 0));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int contentLayoutResId() {
        return R.layout.moment_feed_visible_scope_activity;
    }

    @Override
    protected void initView(View view) {
        radioGroup = view.findViewById(R.id.radioGroup);
        view.findViewById(R.id.privateLinearLayout).setOnClickListener(v -> privateScope());
        view.findViewById(R.id.allLinearLayout).setOnClickListener(v -> allScope());
        view.findViewById(R.id.partLinearLayout).setOnClickListener(v -> partScope());
        view.findViewById(R.id.blockLinearLayout).setOnClickListener(v -> blockScope());

        setTitleLeftText("谁可以看");
        Bundle args = getArguments();
        uids = args == null ? null : args.getStringArrayList(FeedVisibleScopeActivity.PARAM_UIDS);
        if (uids == null) {
            uids = new ArrayList<>();
        }
        mode = args == null ? 0 : args.getInt(FeedVisibleScopeActivity.PARAM_VISIBLE_MODE, 0);
        switch (mode) {
            case FeedVisibleScopeActivity.VISIBLE_MODE_ALL:
                radioGroup.check(R.id.allRadioButton);
                break;
            case FeedVisibleScopeActivity.VISIBLE_MODE_PRIVATE:
                radioGroup.check(R.id.privateRadioButton);
                break;
            case FeedVisibleScopeActivity.VISIBLE_MODE_PART:
                radioGroup.check(R.id.partRadioButton);
                break;
            case FeedVisibleScopeActivity.VISIBLE_MODE_BLOCK:
                radioGroup.check(R.id.blockRadioButton);
                break;
            default:
                break;
        }
    }

    @Override
    public void onTitleRightClick() {
        Intent intent = new Intent();
        intent.putExtra(FeedVisibleScopeActivity.RESULT_VISIBLE_MODE, mode);
        intent.putExtra(FeedVisibleScopeActivity.RESULT_USERS, users);
        WfcPageCompat.setPageResult(this, Activity.RESULT_OK, intent);
        WfcPageCompat.finishPage(this);
    }

    void privateScope() {
        radioGroup.check(R.id.privateRadioButton);
        mode = FeedVisibleScopeActivity.VISIBLE_MODE_PRIVATE;
    }

    void allScope() {
        radioGroup.check(R.id.allRadioButton);
        mode = FeedVisibleScopeActivity.VISIBLE_MODE_ALL;
    }

    void partScope() {
        radioGroup.check(R.id.partRadioButton);
        Intent intent;
        if (mode == FeedVisibleScopeActivity.VISIBLE_MODE_PART) {
            intent = PickContactActivity.buildPickIntent(requireContext(), 0, uids, null);
        } else {
            uids.clear();
            intent = PickContactActivity.buildPickIntent(requireContext(), 0, null, null);
        }
        WfcPageCompat.startPageForResult(this, intent, REQUEST_CODE_PICK_TO_FETCH);
        mode = FeedVisibleScopeActivity.VISIBLE_MODE_PART;
    }

    void blockScope() {
        radioGroup.check(R.id.blockRadioButton);
        Intent intent;
        if (mode == FeedVisibleScopeActivity.VISIBLE_MODE_BLOCK) {
            intent = PickContactActivity.buildPickIntent(requireContext(), 0, uids, null);
        } else {
            uids.clear();
            intent = PickContactActivity.buildPickIntent(requireContext(), 0, null, null);
        }
        WfcPageCompat.startPageForResult(this, intent, REQUEST_CODE_PICK_TO_BLOCK);
        mode = FeedVisibleScopeActivity.VISIBLE_MODE_BLOCK;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_CODE_PICK_TO_FETCH || requestCode == REQUEST_CODE_PICK_TO_BLOCK) {
            users = data.getParcelableArrayListExtra(PickContactActivity.RESULT_PICKED_USERS);
        }
        for (UserInfo info : users) {
            uids.add(info.uid);
        }
    }
}
