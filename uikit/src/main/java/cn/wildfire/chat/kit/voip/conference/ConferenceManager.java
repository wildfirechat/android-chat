package cn.wildfire.chat.kit.voip.conference;

import java.util.List;

import cn.wildfire.chat.kit.voip.conference.message.ConferenceChangeModelContent;
import cn.wildfire.chat.kit.voip.conference.message.ConferenceKickoffMemberContent;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.OnReceiveMessageListener;

public class ConferenceManager implements OnReceiveMessageListener {
    public interface ConferenceManagerEventCallback {
        void onChangeModeRequest(String conferenceId, boolean audience);

        void onKickoffRequest(String conferenceId);
    }

    private ConferenceManagerEventCallback callback;
    public static ConferenceManager managerInstance = null;

    public static synchronized ConferenceManager Instance() {
        if (managerInstance == null) {
            managerInstance = new ConferenceManager();
        }
        return managerInstance;
    }

    private ConferenceManager() {
        ChatManager.Instance().addOnReceiveMessageListener(this);
    }

    public void setCallback(ConferenceManagerEventCallback callback) {
        this.callback = callback;
    }

    public void requestChangeModel(String conferenceId, String userId, boolean audience) {
        ConferenceChangeModelContent content = new ConferenceChangeModelContent(conferenceId, audience);
        Conversation conversation = new Conversation(Conversation.ConversationType.Single, userId);
        ChatManager.Instance().sendMessage(conversation, content, null, 0, null);
    }

    public void requestLeave(String conferenceId, String userId) {
        ConferenceKickoffMemberContent content = new ConferenceKickoffMemberContent(conferenceId);
        Conversation conversation = new Conversation(Conversation.ConversationType.Single, userId);
        ChatManager.Instance().sendMessage(conversation, content, null, 0, null);
    }

    public void changeModel(String conferenceId, boolean audience) {
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
                if (msg.content instanceof ConferenceChangeModelContent) {
                    ConferenceChangeModelContent content = (ConferenceChangeModelContent) msg.content;
                    if (callback != null) {
                        callback.onChangeModeRequest(content.getCallId(), content.isAudience());
                    }
                } else if (msg.content instanceof ConferenceKickoffMemberContent) {
                    ConferenceKickoffMemberContent content = (ConferenceKickoffMemberContent) msg.content;
                    if (callback != null) {
                        callback.onKickoffRequest(content.getCallId());
                    }
                }
            }
        }
    }
}
