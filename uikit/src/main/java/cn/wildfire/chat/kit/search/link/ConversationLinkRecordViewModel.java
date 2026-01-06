package cn.wildfire.chat.kit.search.link;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.message.LinkMessageContent;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.model.Conversation;

public class ConversationLinkRecordViewModel extends ViewModel {
    private Conversation conversation;
    private MutableLiveData<List<LinkItem>> linkItemsLiveData = new MutableLiveData<>();

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    LiveData<List<LinkItem>> loadLinkMessages() {
        new Thread(() -> {
            try {
                List<Integer> contentTypes = new ArrayList<>();
                contentTypes.add(MessageContentType.ContentType_Link);

                List<Message> messages = ChatManager.Instance().searchMessageByTypesAndTimes(
                        conversation,
                        null,
                        contentTypes,
                        0,
                        System.currentTimeMillis(),
                        true,
                        1000,
                        0,
                        null
                );

                if (messages == null || messages.isEmpty()) {
                    linkItemsLiveData.postValue(new ArrayList<>());
                    return;
                }

                List<LinkItem> linkItems = new ArrayList<>();
                for (Message message : messages) {
                    LinkMessageContent linkContent = (LinkMessageContent) message.content;
                    linkItems.add(new LinkItem(message, linkContent));
                }

                Collections.sort(linkItems, new Comparator<LinkItem>() {
                    @Override
                    public int compare(LinkItem l1, LinkItem l2) {
                        return Long.compare(l2.timestamp, l1.timestamp);
                    }
                });

                linkItemsLiveData.postValue(linkItems);
            } catch (Exception e) {
                e.printStackTrace();
                linkItemsLiveData.postValue(new ArrayList<>());
            }
        }).start();

        return linkItemsLiveData;
    }

    public static class LinkItem {
        public Message message;
        public String title;
        public String contentDigest;
        public String url;
        public String thumbnailUrl;
        public long timestamp;
        public Conversation conversation;

        public LinkItem(Message message, LinkMessageContent linkContent) {
            this.message = message;
            this.title = linkContent.getTitle();
            this.contentDigest = linkContent.getContentDigest();
            this.url = linkContent.getUrl();
            this.thumbnailUrl = linkContent.getThumbnailUrl();
            this.timestamp = message.serverTime;
            this.conversation = message.conversation;
        }
    }
}
