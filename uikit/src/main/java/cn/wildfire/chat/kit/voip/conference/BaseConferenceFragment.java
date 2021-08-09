package cn.wildfire.chat.kit.voip.conference;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.List;

import cn.wildfire.chat.kit.voip.conference.message.ConferenceChangeModeContent;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.OnReceiveMessageListener;

public abstract class BaseConferenceFragment extends Fragment implements OnReceiveMessageListener {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ChatManager.Instance().addOnReceiveMessageListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ChatManager.Instance().removeOnReceiveMessageListener(this);
    }

    public void changeMode(String conferenceId, boolean audience) {
        if (AVEngineKit.Instance().getCurrentSession() != null
            && AVEngineKit.Instance().getCurrentSession().isConference()
            && AVEngineKit.Instance().getCurrentSession().getCallId().equals(conferenceId))
            AVEngineKit.Instance().getCurrentSession().switchAudience(audience);
    }

    @Override
    public void onReceiveMessage(List<Message> messages, boolean hasMore) {
        if (AVEngineKit.Instance().getCurrentSession() != null
            && AVEngineKit.Instance().getCurrentSession().isConference()) {
            for (Message msg : messages) {
                if (msg.content instanceof ConferenceChangeModeContent) {
                    ConferenceChangeModeContent content = (ConferenceChangeModeContent) msg.content;
                    onChangeModeRequest(content.getCallId(), content.isAudience());
                }
            }
        }
    }

    public void onChangeModeRequest(String conferenceId, boolean audience) {
    }
}
