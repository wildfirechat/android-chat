package cn.wildfire.chat.kit.search.media;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.message.core.MessageContentType;
import cn.wildfirechat.model.Conversation;

public class ConversationMediaViewModel extends ViewModel {
    private Conversation conversation;
    private MutableLiveData<List<MediaMonthGroup>> mediaGroupsLiveData = new MutableLiveData<>();

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    LiveData<List<MediaMonthGroup>> loadMediaMessages() {
        new Thread(() -> {
            try {
                List<Integer> contentTypes = new ArrayList<>();
                contentTypes.add(MessageContentType.ContentType_Image);
                contentTypes.add(MessageContentType.ContentType_Video);

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
                    mediaGroupsLiveData.postValue(new ArrayList<>());
                    return;
                }

                Map<String, List<MediaItem>> groupedByMonth = new HashMap<>();

                for (Message message : messages) {
                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(message.serverTime);
                    String key = cal.get(Calendar.YEAR) + "-" + cal.get(Calendar.MONTH);

                    if (!groupedByMonth.containsKey(key)) {
                        groupedByMonth.put(key, new ArrayList<>());
                    }

                    groupedByMonth.get(key).add(new MediaItem(message));
                }

                List<MediaMonthGroup> groups = new ArrayList<>();
                for (Map.Entry<String, List<MediaItem>> entry : groupedByMonth.entrySet()) {
                    String[] parts = entry.getKey().split("-");
                    int year = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);

                    Collections.sort(entry.getValue(), new Comparator<MediaItem>() {
                        @Override
                        public int compare(MediaItem m1, MediaItem m2) {
                            return Long.compare(m2.timestamp, m1.timestamp);
                        }
                    });

                    groups.add(new MediaMonthGroup(year, month + 1, entry.getValue()));
                }

                Collections.sort(groups, new Comparator<MediaMonthGroup>() {
                    @Override
                    public int compare(MediaMonthGroup g1, MediaMonthGroup g2) {
                        if (g1.year != g2.year) {
                            return g2.year - g1.year;
                        }
                        return g2.month - g1.month;
                    }
                });

                mediaGroupsLiveData.postValue(groups);
            } catch (Exception e) {
                e.printStackTrace();
                mediaGroupsLiveData.postValue(new ArrayList<>());
            }
        }).start();

        return mediaGroupsLiveData;
    }

    public static class MediaMonthGroup {
        public int year;
        public int month;
        public List<MediaItem> items;

        public MediaMonthGroup(int year, int month, List<MediaItem> items) {
            this.year = year;
            this.month = month;
            this.items = items;
        }

        public String getTitle() {
            return year + "年" + month + "月";
        }
    }

    public static class MediaItem {
        public Message message;
        public int contentType;
        public String path;
        public String remotePath;
        public android.graphics.Bitmap thumbnail;
        public long duration;
        public long timestamp;
        public Conversation conversation;

        public MediaItem(Message message) {
            this.message = message;
            this.contentType = message.content.getMessageContentType();
            this.timestamp = message.serverTime;
            this.conversation = message.conversation;

            if (contentType == MessageContentType.ContentType_Image) {
                cn.wildfirechat.message.ImageMessageContent imageContent =
                        (cn.wildfirechat.message.ImageMessageContent) message.content;
                this.path = imageContent.localPath;
                this.remotePath = imageContent.remoteUrl;
                this.thumbnail = imageContent.getThumbnail();
            } else if (contentType == MessageContentType.ContentType_Video) {
                cn.wildfirechat.message.VideoMessageContent videoContent =
                        (cn.wildfirechat.message.VideoMessageContent) message.content;
                this.path = videoContent.localPath;
                this.remotePath = videoContent.remoteUrl;
                this.duration = videoContent.getDuration();
                this.thumbnail = videoContent.getThumbnail();
            }
        }

        public boolean isVideo() {
            return contentType == MessageContentType.ContentType_Video;
        }

        public String getImagePath() {
            if (cn.wildfire.chat.kit.utils.FileUtils.isFileExists(path)) {
                return path;
            }
            return remotePath;
        }
    }
}
