package cn.wildfirechat;

import android.util.LruCache;

public class AliasStore extends LruCache<String, String> {
    /**
     * @param maxSize for caches that do not override {@link #sizeOf}, this is
     *                the maximum number of entries in the cache. For all other caches,
     *                this is the maximum sum of the sizes of the entries in this cache.
     */
    public AliasStore(int maxSize) {
        super(maxSize);
    }
}
