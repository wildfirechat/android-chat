package cn.wildfire.chat.kit.viewmodel;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class ShareViewModelFactory extends ViewModelProvider.NewInstanceFactory {

    private static ShareViewModelFactory instance;

    public synchronized static ViewModelProvider.NewInstanceFactory instanceFactory() {
        if (instance == null) {
            instance = new ShareViewModelFactory();
        }
        return instance;
    }

    private final Map<Class<? extends ViewModel>, ViewModel> mShareCache = new HashMap<>();

    @NonNull
    @Override
    public <T extends ViewModel> T create(final @NonNull Class<T> modelClass) {
        if (SharedViewModel.class.isAssignableFrom(modelClass)) {
            SharedViewModel shareVM = null;

            if (mShareCache.containsKey(modelClass)) {
                shareVM = (SharedViewModel) mShareCache.get(modelClass);
            } else {
                try {
                    shareVM = (SharedViewModel) modelClass.getConstructor(Runnable.class).newInstance(new Runnable() {
                        @Override
                        public void run() {
                            mShareCache.remove(modelClass);
                        }
                    });
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException("Cannot create an instance of " + modelClass, e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("Cannot create an instance of " + modelClass, e);
                } catch (InstantiationException e) {
                    throw new RuntimeException("Cannot create an instance of " + modelClass, e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException("Cannot create an instance of " + modelClass, e);
                }
                mShareCache.put(modelClass, shareVM);
            }

            shareVM.incRefCount();
            return (T) shareVM;
        }
        return super.create(modelClass);
    }
}