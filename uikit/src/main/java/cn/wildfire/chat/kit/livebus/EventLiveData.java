/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.livebus;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
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
    public void removeObserver(@NonNull Observer<? super Object> observer) {
        super.removeObserver(observer);
        if (!hasObservers()) {
            LiveDataBus.unregister(mSubject);
        }
    }

    @Override
    public void removeObservers(@NonNull LifecycleOwner owner) {
        super.removeObservers(owner);
        if (!hasObservers()) {
            LiveDataBus.unregister(mSubject);
        }
    }

}
