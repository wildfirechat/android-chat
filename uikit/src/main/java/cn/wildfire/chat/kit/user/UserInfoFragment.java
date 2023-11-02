/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.user;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

import com.afollestad.materialdialogs.MaterialDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.bean.ImageItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.Config;
import cn.wildfire.chat.kit.R;
import cn.wildfire.chat.kit.WfcIntent;
import cn.wildfire.chat.kit.WfcScheme;
import cn.wildfire.chat.kit.WfcUIKit;
import cn.wildfire.chat.kit.common.OperateResult;
import cn.wildfire.chat.kit.contact.ContactViewModel;
import cn.wildfire.chat.kit.contact.OrganizationServiceViewModel;
import cn.wildfire.chat.kit.contact.newfriend.InviteFriendActivity;
import cn.wildfire.chat.kit.conversation.ConversationActivity;
import cn.wildfire.chat.kit.group.GroupMemberMessageHistoryActivity;
import cn.wildfire.chat.kit.mm.MMPreviewActivity;
import cn.wildfire.chat.kit.organization.OrganizationMemberListActivity;
import cn.wildfire.chat.kit.organization.model.EmployeeEx;
import cn.wildfire.chat.kit.organization.model.Organization;
import cn.wildfire.chat.kit.organization.model.OrganizationRelationship;
import cn.wildfire.chat.kit.qrcode.QRCodeActivity;
import cn.wildfire.chat.kit.third.utils.ImageUtils;
import cn.wildfire.chat.kit.third.utils.UIUtils;
import cn.wildfire.chat.kit.widget.OptionItemView;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.UserInfo;
import cn.wildfirechat.remote.ChatManager;

public class UserInfoFragment extends Fragment {
    ImageView portraitImageView;

    TextView titleTextView;
    TextView displayNameTextView;
    TextView groupAliasTextView;
    TextView accountTextView;

    View chatButton;
    View voipChatButton;
    Button inviteButton;
    OptionItemView aliasOptionItemView;


    OptionItemView orgOptionItemView;
    OptionItemView messagesOptionItemView;

    OptionItemView qrCodeOptionItemView;

    View momentButton;

    TextView favContactTextView;

    private UserInfo userInfo;
    private String groupId;
    private UserViewModel userViewModel;
    private OrganizationServiceViewModel organizationServiceViewModel;
    private ContactViewModel contactViewModel;

    private List<Organization> organizations;

    public static UserInfoFragment newInstance(UserInfo userInfo, String groupId) {
        UserInfoFragment fragment = new UserInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable("userInfo", userInfo);
        if (!TextUtils.isEmpty(groupId)) {
            args.putString("groupId", groupId);
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        assert args != null;
        userInfo = args.getParcelable("userInfo");
        groupId = args.getString("groupId");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.user_info_fragment, container, false);
        bindViews(view);
        bindEvents(view);
        init();
        return view;
    }

    private void bindEvents(View view) {
        view.findViewById(R.id.chatButton).setOnClickListener(_v -> chat());
        view.findViewById(R.id.momentButton).setOnClickListener(_v -> moment());
        view.findViewById(R.id.voipChatButton).setOnClickListener(_v -> voipChat());
        view.findViewById(R.id.aliasOptionItemView).setOnClickListener(_v -> alias());
        view.findViewById(R.id.messagesOptionItemView).setOnClickListener(_v -> showUserMessages());
        view.findViewById(R.id.portraitImageView).setOnClickListener(_v -> portrait());
        view.findViewById(R.id.orgOptionItemView).setOnClickListener(_v -> showOrg());
        view.findViewById(R.id.inviteButton).setOnClickListener(_v -> invite());
        view.findViewById(R.id.qrCodeOptionItemView).setOnClickListener(_v -> showMyQRCode());
    }

    private void bindViews(View view) {
        portraitImageView = view.findViewById(R.id.portraitImageView);
        titleTextView = view.findViewById(R.id.titleTextView);
        displayNameTextView = view.findViewById(R.id.displayNameTextView);
        groupAliasTextView = view.findViewById(R.id.groupAliasTextView);
        accountTextView = view.findViewById(R.id.accountTextView);
        chatButton = view.findViewById(R.id.chatButton);
        voipChatButton = view.findViewById(R.id.voipChatButton);
        inviteButton = view.findViewById(R.id.inviteButton);
        aliasOptionItemView = view.findViewById(R.id.aliasOptionItemView);
        orgOptionItemView = view.findViewById(R.id.orgOptionItemView);
        messagesOptionItemView = view.findViewById(R.id.messagesOptionItemView);
        qrCodeOptionItemView = view.findViewById(R.id.qrCodeOptionItemView);
        momentButton = view.findViewById(R.id.momentButton);
        favContactTextView = view.findViewById(R.id.favContactTextView);
    }

    private void init() {
        userViewModel = ViewModelProviders.of(this).get(UserViewModel.class);
        contactViewModel = ViewModelProviders.of(this).get(ContactViewModel.class);
        organizationServiceViewModel = new ViewModelProvider(this).get(OrganizationServiceViewModel.class);
        String selfUid = userViewModel.getUserId();
        if (selfUid.equals(userInfo.uid)) {
            // self
            chatButton.setVisibility(View.GONE);
            voipChatButton.setVisibility(View.GONE);
            inviteButton.setVisibility(View.GONE);
            qrCodeOptionItemView.setVisibility(View.VISIBLE);
            aliasOptionItemView.setVisibility(View.VISIBLE);
        } else if (contactViewModel.isFriend(userInfo.uid)) {
            // friend
            chatButton.setVisibility(View.VISIBLE);
            voipChatButton.setVisibility(View.VISIBLE);
            inviteButton.setVisibility(View.GONE);
        } else {
            // stranger
            momentButton.setVisibility(View.GONE);
            chatButton.setVisibility(View.GONE);
            voipChatButton.setVisibility(View.GONE);
            inviteButton.setVisibility(View.VISIBLE);
            aliasOptionItemView.setVisibility(View.GONE);
        }
        if (userInfo.type == 1) {
            voipChatButton.setVisibility(View.GONE);
        }
        if (userInfo.uid.equals(Config.FILE_TRANSFER_ID)) {
            chatButton.setVisibility(View.VISIBLE);
            inviteButton.setVisibility(View.GONE);
        }

        setUserInfo(userInfo);
        userViewModel.userInfoLiveData().observe(getViewLifecycleOwner(), userInfos -> {
            for (UserInfo info : userInfos) {
                if (userInfo.uid.equals(info.uid)) {
                    userInfo = info;
                    setUserInfo(info);
                    break;
                }
            }
        });
        userViewModel.getUserInfo(userInfo.uid, true);
        favContactTextView.setVisibility(contactViewModel.isFav(userInfo.uid) ? View.VISIBLE : View.GONE);

        if (!WfcUIKit.getWfcUIKit().isSupportMoment()) {
            momentButton.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(groupId)) {
            messagesOptionItemView.setVisibility(View.VISIBLE);
        } else {
            messagesOptionItemView.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(Config.ORG_SERVER_ADDRESS)) {
            loadOrganizationData();
        }
    }

    private void setUserInfo(UserInfo userInfo) {
        userInfo = ChatManager.Instance().getUserInfo(userInfo.uid, groupId, false);
        RequestOptions requestOptions = new RequestOptions()
            .placeholder(R.mipmap.avatar_def)
            .transforms(new CenterCrop(), new RoundedCorners(UIUtils.dip2Px(getContext(), 10)));
        Glide.with(this)
            .load(userInfo.portrait)
            .apply(requestOptions)
            .into(portraitImageView);
        if (!TextUtils.isEmpty(userInfo.friendAlias)) {
            titleTextView.setText(userInfo.friendAlias);
            displayNameTextView.setText("昵称:" + userInfo.displayName);
        } else {
            titleTextView.setText(userInfo.displayName);
            displayNameTextView.setVisibility(View.GONE);
        }
        if (!TextUtils.isEmpty(userInfo.groupAlias)) {
            groupAliasTextView.setText("群昵称:" + userInfo.groupAlias);
            groupAliasTextView.setVisibility(View.VISIBLE);
        } else {
            groupAliasTextView.setVisibility(View.GONE);
        }
        accountTextView.setText("野火ID:" + userInfo.name);
    }

    void chat() {
        Intent intent = new Intent(getActivity(), ConversationActivity.class);
        Conversation conversation = new Conversation(Conversation.ConversationType.Single, userInfo.uid, 0);
        intent.putExtra("conversation", conversation);
        startActivity(intent);
        getActivity().finish();
    }

    void moment() {
        Intent intent = new Intent(WfcIntent.ACTION_MOMENT);
        intent.putExtra("userInfo", userInfo);
        startActivity(intent);
    }

    void voipChat() {
        new MaterialDialog.Builder(getActivity())
            .items("音频聊天", "视频聊天")
            .itemsCallback(new MaterialDialog.ListCallback() {
                @Override
                public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                    WfcUIKit.singleCall(getActivity(), userInfo.uid, position == 0);
                }
            })
            .build()
            .show();
    }

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

    void showUserMessages() {
        Intent intent = new Intent(getActivity(), GroupMemberMessageHistoryActivity.class);
        intent.putExtra("groupId", groupId);
        intent.putExtra("groupMemberId", userInfo.uid);
        startActivity(intent);
    }

    private static final int REQUEST_CODE_PICK_IMAGE = 100;

    void portrait() {
        if (!userInfo.uid.equals(userViewModel.getUserId())) {
            if (!TextUtils.isEmpty(userInfo.portrait)) {
                MMPreviewActivity.previewImage(getContext(), userInfo.portrait);
            } else {
                Toast.makeText(getActivity(), "用户未设置头像", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        String[] permissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions = new String[]{
                Manifest.permission.READ_MEDIA_IMAGES,
            };
        } else {
            permissions = new String[]{
                Manifest.permission.READ_EXTERNAL_STORAGE,
            };
        }
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                if (activity.checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(permissions, 100);
                    return;
                }
            }
        }
        updatePortrait();
    }

    void showOrg() {
        if (organizations.size() > 1) {
            String[] names = new String[organizations.size()];
            for (int i = 0; i < organizations.size(); i++) {
                names[i] = organizations.get(i).name;
            }
            new MaterialDialog.Builder(getActivity())
                .items(names)
                .itemsCallback(new MaterialDialog.ListCallback() {
                    @Override
                    public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                        Intent intent = new Intent(getActivity(), OrganizationMemberListActivity.class);
                        intent.putExtra("organizationId", organizations.get(position).id);
                        startActivity(intent);
                    }
                })
                .build()
                .show();
        } else {
            Intent intent = new Intent(getActivity(), OrganizationMemberListActivity.class);
            intent.putExtra("organizationId", organizations.get(0).id);
            startActivity(intent);
        }
    }

    private void updatePortrait() {
        ImagePicker.picker().pick(this, REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
            if (images == null || images.isEmpty()) {
                Toast.makeText(getActivity(), "更新头像失败: 选取文件失败 ", Toast.LENGTH_SHORT).show();
                return;
            }
            File thumbImgFile = ImageUtils.genThumbImgFile(images.get(0).path);
            if (thumbImgFile == null) {
                Toast.makeText(getActivity(), "更新头像失败: 生成缩略图失败", Toast.LENGTH_SHORT).show();
                return;
            }
            String imagePath = thumbImgFile.getAbsolutePath();

            MutableLiveData<OperateResult<Boolean>> result = userViewModel.updateUserPortrait(imagePath);
            result.observe(this, booleanOperateResult -> {
                if (booleanOperateResult.isSuccess()) {
                    Toast.makeText(getActivity(), "更新头像成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getActivity(), "更新头像失败: " + booleanOperateResult.getErrorCode(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    void invite() {
        Intent intent = new Intent(getActivity(), InviteFriendActivity.class);
        intent.putExtra("userInfo", userInfo);
        startActivity(intent);
        getActivity().finish();
    }

    void showMyQRCode() {
        UserInfo userInfo = userViewModel.getUserInfo(userViewModel.getUserId(), false);
        String qrCodeValue = WfcScheme.QR_CODE_PREFIX_USER + userInfo.uid;
        startActivity(QRCodeActivity.buildQRCodeIntent(getActivity(), "二维码", userInfo.portrait, qrCodeValue));
    }

    private void loadOrganizationData() {
        organizationServiceViewModel.getEmployeeEx(userInfo.uid)
            .observe(getViewLifecycleOwner(), new Observer<EmployeeEx>() {
                @Override
                public void onChanged(EmployeeEx employeeEx) {
                    if (employeeEx.relationships != null && !employeeEx.relationships.isEmpty()) {
                        organizations = new ArrayList<>();
                        List<Integer> ids = new ArrayList<>();
                        for (OrganizationRelationship r : employeeEx.relationships) {
                            if (r.bottom) {
                                ids.add(r.organizationId);
                            }
                        }
                        organizationServiceViewModel.getOrganizations(ids)
                            .observe(getViewLifecycleOwner(), new Observer<List<Organization>>() {
                                @Override
                                public void onChanged(List<Organization> orgs) {
                                    StringBuilder desc = new StringBuilder();
                                    for (Organization org : orgs) {
                                        desc.append(org.name);
                                        desc.append("、");
                                    }
                                    organizations.addAll(orgs);

                                    if (!TextUtils.isEmpty(desc)) {
                                        orgOptionItemView.setVisibility(View.VISIBLE);
                                        orgOptionItemView.setDesc(desc.substring(0, desc.length() - 1));
                                    }
                                }
                            });
                    }
                }
            });
    }
}
