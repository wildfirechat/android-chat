package cn.wildfire.chat.kit.search;

import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.remote.ChatManager;

public class SearchViewModel extends ViewModel {
    private MutableLiveData<SearchResult> resultLiveData = new MutableLiveData<>();

    private Handler workHandler;
    private Handler mainHandler = new Handler();
    private String keyword;

    public SearchViewModel() {
        init();
    }

    public void search(String keyword, List<SearchableModule> searchableModules) {
        if (TextUtils.isEmpty(keyword)) {
            return;
        }
        if (this.keyword != null && this.keyword.equals(keyword)) {
            return;
        }
        this.keyword = keyword;
        workHandler.post(() -> {
            boolean found = false;
            for (SearchableModule module : searchableModules) {
                List result = module.searchInternal(keyword);
                if (keyword.equals(SearchViewModel.this.keyword) && result != null && !result.isEmpty()) {
                    found = true;
                    mainHandler.post(() -> resultLiveData.setValue(new SearchResult(module, result)));
                }
            }

            if (keyword.equals(SearchViewModel.this.keyword) && !found) {
                mainHandler.post(() -> resultLiveData.setValue(null));
            }
            this.keyword = null;
        });
    }


    public LiveData<List<Message>> searchMessage(Conversation conversation, String keyword) {
        MutableLiveData<List<Message>> result = new MutableLiveData<>();
        workHandler.post(() -> {
            List<Message> messages = ChatManager.Instance().searchMessage(conversation, keyword);
            result.postValue(messages);
        });
        return result;
    }

    public MutableLiveData<SearchResult> getResultLiveData() {
        return resultLiveData;
    }

    private void init() {
        if (workHandler == null) {
            HandlerThread thread = new HandlerThread("search");
            thread.start();
            workHandler = new Handler(thread.getLooper());
        }
    }
}
