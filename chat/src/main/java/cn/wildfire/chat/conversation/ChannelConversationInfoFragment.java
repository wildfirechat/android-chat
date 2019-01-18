package cn.wildfire.chat.conversation;

import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.kyleduo.switchbutton.SwitchButton;
import com.lqr.optionitemview.OptionItemView;
import cn.wildfirechat.chat.R;

import java.util.Arrays;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.wildfire.chat.channel.ChannelViewModel;
import cn.wildfire.chat.conversationlist.ConversationListViewModel;
import cn.wildfire.chat.conversationlist.ConversationListViewModelFactory;
import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.ConversationInfo;

public class ChannelConversationInfoFragment extends Fragment implements CompoundButton.OnCheckedChangeListener {

    // common
    @Bind(R.id.portraitImageView)
    ImageView portraitImageView;
    @Bind(R.id.stickTopSwitchButton)
    SwitchButton stickTopSwitchButton;
    @Bind(R.id.silentSwitchButton)
    SwitchButton silentSwitchButton;

    @Bind(R.id.channelNameOptionItemView)
    OptionItemView channelNameOptionItemView;
    @Bind(R.id.channelDescOptionItemView)
    OptionItemView channelDescOptionItemView;

    private ConversationInfo conversationInfo;
    private ConversationViewModel conversationViewModel;
    private ChannelViewModel channelViewModel;

    public static ChannelConversationInfoFragment newInstance(ConversationInfo conversationInfo) {
        ChannelConversationInfoFragment fragment = new ChannelConversationInfoFragment();
        Bundle args = new Bundle();
        args.putParcelable("conversationInfo", conversationInfo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        assert args != null;
        conversationInfo = args.getParcelable("conversationInfo");
        assert conversationInfo != null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.conversation_info_channel_fragment, container, false);
        ButterKnife.bind(this, view);
        init();
        return view;
    }

    private void init() {
        conversationViewModel = ViewModelProviders.of(this, new ConversationViewModelFactory(conversationInfo.conversation)).get(ConversationViewModel.class);
        channelViewModel = ViewModelProviders.of(this).get(ChannelViewModel.class);
        ChannelInfo channelInfo = channelViewModel.getChannelInfo(conversationInfo.conversation.target, true);

        if (channelInfo != null) {
            initChannel(channelInfo);
        }

        channelViewModel.channelInfoLiveData().observe(this, channelInfos -> {
            if (channelInfos != null) {
                for (ChannelInfo info : channelInfos) {
                    if (conversationInfo.conversation.target.equals(info.channelId)) {
                        initChannel(info);
                    }
                }
            }

        });

        stickTopSwitchButton.setChecked(conversationInfo.isTop);
        silentSwitchButton.setChecked(conversationInfo.isSilent);
        stickTopSwitchButton.setOnCheckedChangeListener(this);
        silentSwitchButton.setOnCheckedChangeListener(this);
    }

    private void initChannel(ChannelInfo channelInfo) {
        channelDescOptionItemView.setRightText(channelInfo.name);
        channelDescOptionItemView.setRightText(channelInfo.desc);
        Glide.with(this).load(channelInfo.portrait).into(portraitImageView);
    }

    @OnClick(R.id.clearMessagesOptionItemView)
    void clearMessage() {
        conversationViewModel.clearConversationMessage(conversationInfo.conversation);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    private void stickTop(boolean top) {
        ConversationListViewModel conversationListViewModel = ViewModelProviders
                .of(this, new ConversationListViewModelFactory(Arrays.asList(Conversation.ConversationType.Single, Conversation.ConversationType.Group, Conversation.ConversationType.Channel), Arrays.asList(0)))
                .get(ConversationListViewModel.class);
        conversationListViewModel.setConversationTop(conversationInfo, top);
    }

    private void silent(boolean silent) {
        conversationViewModel.setConversationSilent(conversationInfo.conversation, silent);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.stickTopSwitchButton:
                stickTop(isChecked);
                break;
            case R.id.silentSwitchButton:
                silent(isChecked);
                break;
            default:
                break;
        }

    }
}
