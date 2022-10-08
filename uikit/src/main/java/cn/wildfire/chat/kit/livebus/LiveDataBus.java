/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.livebus;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import java.util.HashMap;
import java.util.Map;

public final class LiveDataBus {

    private static Map<String, EventLiveData> sSubjectMap = new HashMap<>();

    private LiveDataBus() {
    }

    @NonNull
    private static synchronized EventLiveData getLiveData(String subject) {
        EventLiveData liveData = sSubjectMap.get(subject);
        if (liveData == null) {
            liveData = new EventLiveData(subject);
            sSubjectMap.put(subject, liveData);
        }

        return liveData;
    }

    public static void subscribe(String subject, @NonNull LifecycleOwner lifecycle, @NonNull Observer<Object> action) {
        getLiveData(subject).observe(lifecycle, action);
    }

    static void unregister(String subject) {
        sSubjectMap.remove(subject);
        Log.d("LiveDataBus", "remove subject " + subject);
    }

    /**
     * pls refer to {@link androidx.lifecycle.LiveData#postValue(Object)}
     *
     * @param subject
     * @param message
     */
    public static void postValue(String subject, @NonNull Object message) {
        getLiveData(subject).update(message);
    }

    public static void setValue(String subject, @NonNull Object message) {
        getLiveData(subject).update(message);
    }
}