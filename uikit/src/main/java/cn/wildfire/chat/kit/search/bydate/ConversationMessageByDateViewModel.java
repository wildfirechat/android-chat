package cn.wildfire.chat.kit.search.bydate;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;
import cn.wildfirechat.remote.GetMessageCallback;

public class ConversationMessageByDateViewModel extends ViewModel {
    private Conversation conversation;
    private final MutableLiveData<List<MonthListAdapter.MonthItem>> monthsLiveData = new MutableLiveData<>();

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    LiveData<List<MonthListAdapter.MonthItem>> loadMonths() {
        new Thread(() -> {
            try {
                ChatManager.Instance().getMessagesByTimestamp(
                    conversation,
                    null,
                    1000,
                    false,
                    1,
                    null,
                    new GetMessageCallback() {
                        @Override
                        public void onSuccess(List<Message> messages, boolean hasMore) {
                            if (messages == null || messages.isEmpty()) {
                                monthsLiveData.postValue(null);
                                return;
                            }

                            long earliestTime = messages.get(0).serverTime;
                            Map<String, Integer> messageCountByDay = ChatManager.Instance().getMessageCountByDay(
                                conversation,
                                null,
                                earliestTime / 1000,
                                System.currentTimeMillis() / 1000
                            );

                            List<MonthListAdapter.MonthItem> months = buildMonthsWithMessageCount(earliestTime, messageCountByDay);
                            monthsLiveData.postValue(months);
                        }

                        @Override
                        public void onFail(int errorCode) {
                            monthsLiveData.postValue(null);
                        }
                    }
                );
            } catch (Exception e) {
                e.printStackTrace();
                monthsLiveData.postValue(null);
            }
        }).start();

        return monthsLiveData;
    }

    private List<MonthListAdapter.MonthItem> buildMonthsWithMessageCount(long earliestTime, Map<String, Integer> messageCountByDay) {
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

        List<MonthListAdapter.MonthItem> months = new ArrayList<>();
        Calendar temp = (Calendar) startCal.clone();

        while (temp.compareTo(current) <= 0) {
            int year = temp.get(Calendar.YEAR);
            int month = temp.get(Calendar.MONTH) + 1;
            HashMap<Integer, Integer> dayMessageCount = new HashMap<>();
            Calendar dayCal = (Calendar) temp.clone();

            int maxDay = temp.getActualMaximum(Calendar.DAY_OF_MONTH);
            for (int day = 1; day <= maxDay; day++) {
                dayCal.set(Calendar.DAY_OF_MONTH, day);
                String dateKey = String.format("%d-%02d-%02d", year, month, day);
                Integer count = messageCountByDay.get(dateKey);
                if (count != null && count > 0) {
                    dayMessageCount.put(day, count);
                }
            }

            months.add(new MonthListAdapter.MonthItem(year, month, dayMessageCount));
            temp.add(Calendar.MONTH, 1);
        }

        Collections.reverse(months);
        return months;
    }

    LiveData<Message> getFirstMessageOfDay(int year, int month, int day) {
        MutableLiveData<Message> firstMessageOfDayLiveData = new MutableLiveData<>();
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

                firstMessageOfDayLiveData.postValue(messages.isEmpty() ? null : messages.get(0));
            } catch (Exception e) {
                firstMessageOfDayLiveData.postValue(null);
            }
        }).start();

        return firstMessageOfDayLiveData;
    }

}
