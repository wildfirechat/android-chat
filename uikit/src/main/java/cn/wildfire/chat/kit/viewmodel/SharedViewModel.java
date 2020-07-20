package cn.wildfire.chat.kit.viewmodel;

import java.util.concurrent.atomic.AtomicInteger;

import androidx.lifecycle.ViewModel;

public abstract class SharedViewModel extends ViewModel {

    private AtomicInteger mRefCounter;

    private Runnable mOnShareCleared;

    protected SharedViewModel() {
        // do nothing
    }

    protected SharedViewModel(Runnable onShareCleared) {
        this();
        mRefCounter = new AtomicInteger(0);
        mOnShareCleared = onShareCleared;
    }

    /**
     * 正常调用流程
     * <p>
     * 1. owner被destroy，并且不重建时
     * <p>
     * 2.
     */
    @Override
    protected final void onCleared() {
        decRefCount();
    }

    /**
     * 所有owner都被destroy之后调用
     */
    protected abstract void onShareCleared();

    protected final int incRefCount() {
        return mRefCounter.incrementAndGet();
    }

    private final int decRefCount() {
        int counter = mRefCounter.decrementAndGet();
        if (counter == 0) {
            if (mOnShareCleared != null) {
                mOnShareCleared.run();
            }
            onShareCleared();
        } else if (counter < 0) {
            mRefCounter.set(0);
            counter = 0;
        }
        return counter;
    }
}
