/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.livebus;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

public class EventLiveData extends LiveData<Object> {

    private final String mSubject;

    public EventLiveData(String subject) {
        mSubject = subject;
    }

    public void update(Object object) {
        postValue(object);
    }

    @Override
    public void observeForever(@NonNull Observer<? super Object> observer) {
        super.observeForever(observer);
        if (!hasObservers()) {
            LiveDataBus.unregister(mSubject);
        }
    }

}
