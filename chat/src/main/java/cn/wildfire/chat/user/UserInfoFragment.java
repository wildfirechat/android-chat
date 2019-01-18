package cn.wildfire.chat.user;

import android.app.Activity;
import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.bean.ImageItem;
import com.lqr.optionitemview.OptionItemView;
import cn.wildfirechat.chat.R;
import cn.wildfire.chat.app.MyApp;
import cn.wildfire.chat.third.utils.UIUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.contact.ContactViewModel;
import cn.wildfire.chat.contact.newfriend.InviteFriendActivity;
import cn.wildfire.chat.conversation.ConversationActivity;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ModifyMyInfoEntry;
import cn.wildfirechat.model.ModifyMyInfoType;
import cn.wildfirechat.model.UserInfo;

public class UserInfoFragment extends Fragment {
    @Bind(R.id.portraitImageView)
    ImageView portraitImageView;
    @Bind(R.id.nameTextView)
    TextView nameTextView;
    @Bind(R.id.accountTextView)
    TextView accountTextView;
    @Bind(R.id.nickyName)
    TextView nickyNameTextView;
    @Bind(R.id.chatButton)
    Button chatButton;
    @Bind(R.id.voipChatButton)
    Button voipChatButton;
    @Bind(R.id.inviteButton)
    Button inviteButton;
    @Bind(R.id.aliasOptionItemView)
    OptionItemView aliasOptionItemView;

    private UserInfo userInfo;
    private UserViewModel userViewModel;
    private ContactViewModel contactViewModel;

    public static UserInfoFragment newInstance(UserInfo userInfo) {
        UserInfoFragment fragment = new UserInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable("userInfo", userInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        assert args != null;
        userInfo = args.getParcelable("userInfo");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.user_info_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        contactViewModel = ViewModelProviders.of(this).get(ContactViewModel.class);
        String selfUid = userViewModel.getUserId();
        if (selfUid.equals(userInfo.uid)) {
            // self
            chatButton.setVisibility(View.GONE);
            voipChatButton.setVisibility(View.GONE);
            inviteButton.setVisibility(View.GONE);
        } else if (contactViewModel.isFriend(userInfo.uid)) {
            // friend
            chatButton.setVisibility(View.VISIBLE);
            voipChatButton.setVisibility(View.VISIBLE);
            inviteButton.setVisibility(View.GONE);
        } else {
            // stranger
            chatButton.setVisibility(View.GONE);
            voipChatButton.setVisibility(View.GONE);
            inviteButton.setVisibility(View.VISIBLE);
            aliasOptionItemView.setVisibility(View.GONE);
        }

        setUserInfo(userInfo);
        userViewModel.userInfoLiveData().observe(this, userInfos -> {
            for (UserInfo info : userInfos) {
                if (userInfo.uid.equals(info.uid)) {
                    userInfo = info;
                    setUserInfo(info);
                    break;
                }
            }
        });
        userViewModel.getUserInfo(userInfo.uid, true);
    }

    private void setUserInfo(UserInfo userInfo) {
        Glide.with(this).load(userInfo.portrait).apply(new RequestOptions().placeholder(R.mipmap.avatar_def).centerCrop()).into(portraitImageView);
        nameTextView.setText(userInfo.name);
        nickyNameTextView.setText(userInfo.displayName);
        accountTextView.setText(UIUtils.getString(R.string.my_chat_account, userInfo.uid));
    }

    @OnClick(R.id.chatButton)
    void chat() {
        Intent intent = new Intent(getActivity(), ConversationActivity.class);
        Conversation conversation = new Conversation(Conversation.ConversationType.Single, userInfo.uid, 0);
        intent.putExtra("conversation", conversation);
        startActivity(intent);
        getActivity().finish();
    }

    @OnClick(R.id.voipChatButton)
    void voipChat() {
        MyApp.onCall(getActivity(), userInfo.uid, true, false);
    }

    @OnClick(R.id.aliasOptionItemView)
    void alias() {
        String selfUid = userViewModel.getUserId();
        if (selfUid.equals(userInfo.uid)) {
            Intent intent = new Intent(getActivity(), ChangeMyNameActivity.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(getActivity(), SetAliasActivity.class);
            intent.putExtra("userId", userInfo.uid);
            startActivity(intent);
        }
    }

    private static final int REQUEST_CODE_PICK_IMAGE = 100;

    @OnClick(R.id.portraitImageView)
    void portrait() {
        ImagePicker.picker().pick(this, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
            String imagePath = images.get(0).path;
            List<ModifyMyInfoEntry> entries = new ArrayList<>();
            entries.add(new ModifyMyInfoEntry(ModifyMyInfoType.Modify_Portrait, imagePath));
            userViewModel.modifyMyInfo(entries).observe(this, booleanOperateResult -> {
                if (booleanOperateResult.isSuccess()) {
                    Toast.makeText(getActivity(), "修改成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "修改失败", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @OnClick(R.id.inviteButton)
    void invite() {
        Intent intent = new Intent(getActivity(), InviteFriendActivity.class);
        intent.putExtra("userInfo", userInfo);
        startActivity(intent);
        getActivity().finish();
    }
}
