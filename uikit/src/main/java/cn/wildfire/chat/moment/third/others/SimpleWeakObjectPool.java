package cn.wildfire.chat.moment.third.others;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;

/**
 * 感谢razerdp提供缓存View思路
 * https://github.com/razerdp/FriendCircle
 */
public final class SimpleWeakObjectPool<T> {
    private WeakReference<T>[] objsPool;
    private int size;
    private int curPointer = -1;


    public SimpleWeakObjectPool() {
        this(5);
    }

    public SimpleWeakObjectPool(int size) {
        this.size = size;
        objsPool = (WeakReference<T>[]) Array.newInstance(WeakReference.class, size);
    }

    public synchronized T get() {
        if (curPointer == -1 || curPointer > objsPool.length) return null;
        T obj = objsPool[curPointer].get();
        objsPool[curPointer] = null;
        curPointer--;
        return obj;
    }

    public synchronized boolean put(T t) {
        if (curPointer == -1 || curPointer < objsPool.length - 1) {
            curPointer++;
            objsPool[curPointer] = new WeakReference<T>(t);
            return true;
        }
        return false;
    }

    public void clearPool() {
        for (int i = 0; i < objsPool.length; i++) {
            objsPool[i].clear();
            objsPool[i] = null;
        }
        curPointer = -1;
    }

    public int size() {
        return objsPool == null ? 0 : objsPool.length;
    }
}
