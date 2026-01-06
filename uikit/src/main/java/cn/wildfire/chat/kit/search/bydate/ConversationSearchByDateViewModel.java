package cn.wildfire.chat.kit.search.bydate;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetMessageCallback;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;

public class ConversationSearchByDateViewModel extends ViewModel {
    private Conversation conversation;
    private MutableLiveData<List<MonthListAdapter.MonthItem>> monthsLiveData = new MutableLiveData<>();
    private MutableLiveData<MonthData> monthMessageCountLiveData = new MutableLiveData<>();
    private MutableLiveData<Message> firstMessageOfDayLiveData = new MutableLiveData<>();

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    LiveData<List<MonthListAdapter.MonthItem>> loadMonths() {
        new Thread(() -> {
            try {
                // 获取最早的消息时间（1970年时间戳获取最旧消息）
                ChatManager.Instance().getMessagesByTimestamp(
                        conversation,
                        null,
                        0,  // 1970年的时间戳
                        false,  // false表示获取比timestamp晚的消息，即从最早的开始
                        1,  // 只需要1条就知道最早时间
                        null,
                        new GetMessageCallback() {
                            @Override
                            public void onSuccess(List<Message> messages, boolean hasMore) {
                                if (messages == null || messages.isEmpty()) {
                                    generateLast12Months();
                                    return;
                                }

                                long earliestTime = messages.get(0).serverTime;

                                Calendar startCal = Calendar.getInstance();
                                startCal.setTimeInMillis(earliestTime);
                                startCal.set(Calendar.DAY_OF_MONTH, 1);
                                startCal.set(Calendar.HOUR_OF_DAY, 0);
                                startCal.set(Calendar.MINUTE, 0);
                                startCal.set(Calendar.SECOND, 0);
                                startCal.set(Calendar.MILLISECOND, 0);

                                Calendar current = Calendar.getInstance();
                                current.set(Calendar.DAY_OF_MONTH, 1);
                                current.set(Calendar.HOUR_OF_DAY, 0);
                                current.set(Calendar.MINUTE, 0);
                                current.set(Calendar.SECOND, 0);
                                current.set(Calendar.MILLISECOND, 0);

                                android.util.Log.d("CalendarDebug", "Earliest time: " + earliestTime);
                                android.util.Log.d("CalendarDebug", "Start: " + startCal.get(Calendar.YEAR) + "-" + (startCal.get(Calendar.MONTH) + 1));
                                android.util.Log.d("CalendarDebug", "Current: " + current.get(Calendar.YEAR) + "-" + (current.get(Calendar.MONTH) + 1));

                                List<MonthListAdapter.MonthItem> months = new ArrayList<>();
                                Calendar temp = (Calendar) startCal.clone();

                                while (temp.compareTo(current) <= 0) {
                                    int year = temp.get(Calendar.YEAR);
                                    int month = temp.get(Calendar.MONTH) + 1;
                                    months.add(new MonthListAdapter.MonthItem(year, month, new HashMap<>()));
                                    android.util.Log.d("CalendarDebug", "Adding: " + year + "-" + month);
                                    temp.add(Calendar.MONTH, 1);
                                }

                                java.util.Collections.reverse(months);

                                android.util.Log.d("CalendarDebug", "Total months: " + months.size());
                                if (!months.isEmpty()) {
                                    MonthListAdapter.MonthItem first = months.get(0);
                                    android.util.Log.d("CalendarDebug", "First month: " + first.year + "-" + first.month);
                                    MonthListAdapter.MonthItem last = months.get(months.size() - 1);
                                    android.util.Log.d("CalendarDebug", "Last month: " + last.year + "-" + last.month);
                                }

                                monthsLiveData.postValue(months);

                                // 异步加载每个月的消息统计
                                for (int i = 0; i < months.size(); i++) {
                                    final int index = i;
                                    MonthListAdapter.MonthItem item = months.get(index);
                                    loadMonthMessageCount(item.year, item.month, index);
                                }
                            }

                            @Override
                            public void onFail(int errorCode) {
                                // 失败时使用过去12个月
                                generateLast12Months();
                            }
                        }
                );

            } catch (Exception e) {
                e.printStackTrace();
                generateLast12Months();
            }
        }).start();

        return monthsLiveData;
    }

    private void generateLast12Months() {
        android.util.Log.d("CalendarDebug", "Primary method failed, trying alternative: getMessages");

        new Thread(() -> {
            try {
                List<Message> messages = ChatManager.Instance().getMessages(conversation, 0, true, 1, null);
                if (messages != null && !messages.isEmpty()) {
                    long earliestTime = messages.get(0).serverTime;
                    Calendar startCal = Calendar.getInstance();
                    startCal.setTimeInMillis(earliestTime);
                    startCal.set(Calendar.DAY_OF_MONTH, 1);

                    Calendar current = Calendar.getInstance();
                    current.set(Calendar.DAY_OF_MONTH, 1);

                    List<MonthListAdapter.MonthItem> months = new ArrayList<>();
                    Calendar temp = (Calendar) startCal.clone();

                    while (temp.compareTo(current) <= 0) {
                        int year = temp.get(Calendar.YEAR);
                        int month = temp.get(Calendar.MONTH) + 1;
                        months.add(new MonthListAdapter.MonthItem(year, month, new HashMap<>()));
                        temp.add(Calendar.MONTH, 1);
                    }

                    java.util.Collections.reverse(months);

                    android.util.Log.d("CalendarDebug", "Alternative method - Total months: " + months.size());
                    if (!months.isEmpty()) {
                        MonthListAdapter.MonthItem first = months.get(0);
                        android.util.Log.d("CalendarDebug", "Alternative method - First month: " + first.year + "-" + first.month);
                    }

                    monthsLiveData.postValue(months);

                    for (int i = 0; i < months.size(); i++) {
                        final int index = i;
                        MonthListAdapter.MonthItem item = months.get(index);
                        loadMonthMessageCount(item.year, item.month, index);
                    }
                } else {
                    android.util.Log.d("CalendarDebug", "Alternative method also failed, using current month only");
                    List<MonthListAdapter.MonthItem> months = new ArrayList<>();
                    Calendar current = Calendar.getInstance();
                    int year = current.get(Calendar.YEAR);
                    int month = current.get(Calendar.MONTH) + 1;
                    months.add(new MonthListAdapter.MonthItem(year, month, new HashMap<>()));

                    monthsLiveData.postValue(months);
                    loadMonthMessageCount(year, month, 0);
                }
            } catch (Exception e) {
                e.printStackTrace();
                android.util.Log.d("CalendarDebug", "All methods failed, using current month only");
                List<MonthListAdapter.MonthItem> months = new ArrayList<>();
                Calendar current = Calendar.getInstance();
                int year = current.get(Calendar.YEAR);
                int month = current.get(Calendar.MONTH) + 1;
                months.add(new MonthListAdapter.MonthItem(year, month, new HashMap<>()));

                monthsLiveData.postValue(months);
                loadMonthMessageCount(year, month, 0);
            }
        }).start();
    }

    private void loadMonthMessageCount(int year, int month, int index) {
        new Thread(() -> {
            try {
                Calendar start = Calendar.getInstance();
                start.set(year, month - 1, 1, 0, 0, 0);
                start.set(Calendar.MILLISECOND, 0);

                Calendar end = Calendar.getInstance();
                end.set(year, month, 1, 0, 0, 0);
                end.set(Calendar.MILLISECOND, 0);

                List<Message> messages = ChatManager.Instance().searchMessageByTypesAndTimes(
                        conversation,
                        null,
                        null,
                        start.getTimeInMillis(),
                        end.getTimeInMillis(),
                        true,
                        1000,
                        0,
                        null
                );

                Map<Integer, Integer> dayMessageCount = new HashMap<>();
                for (Message message : messages) {
                    Calendar msgCal = Calendar.getInstance();
                    msgCal.setTimeInMillis(message.serverTime);
                    int day = msgCal.get(Calendar.DAY_OF_MONTH);
                    dayMessageCount.put(day, dayMessageCount.getOrDefault(day, 0) + 1);
                }

                monthMessageCountLiveData.postValue(new MonthData(index, dayMessageCount));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static class MonthData {
        public int index;
        public Map<Integer, Integer> dayMessageCount;

        public MonthData(int index, Map<Integer, Integer> dayMessageCount) {
            this.index = index;
            this.dayMessageCount = dayMessageCount;
        }
    }

    LiveData<MonthData> getMonthMessageCountLiveData() {
        return monthMessageCountLiveData;
    }

    LiveData<Message> getFirstMessageOfDay(int year, int month, int day) {
        new Thread(() -> {
            try {
                Calendar start = Calendar.getInstance();
                start.set(year, month - 1, day, 0, 0, 0);
                start.set(Calendar.MILLISECOND, 0);

                Calendar end = Calendar.getInstance();
                end.set(year, month - 1, day, 23, 59, 59);
                end.set(Calendar.MILLISECOND, 999);

                List<Message> messages = ChatManager.Instance().searchMessageByTypesAndTimes(
                        conversation,
                        null,
                        null,
                        start.getTimeInMillis(),
                        end.getTimeInMillis(),
                        true,
                        1,
                        0,
                        null
                );

                if (!messages.isEmpty()) {
                    firstMessageOfDayLiveData.postValue(messages.get(0));
                } else {
                    firstMessageOfDayLiveData.postValue(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
                firstMessageOfDayLiveData.postValue(null);
            }
        }).start();

        return firstMessageOfDayLiveData;
    }
}
