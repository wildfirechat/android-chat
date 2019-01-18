package com.lqr.emoji;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.LruCache;
import android.util.Xml;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * CSDN_LQR
 * emoji表情管理器
 */
public class EmojiManager {

    private static final String EMOT_DIR = "emoji/";

    private static final int CACHE_MAX_SIZE = 1024;
    private static Pattern mPattern;

    private static final List<Entry> mDefaultEntries = new ArrayList<>();
    private static final Map<Integer, Entry> mText2Entry = new HashMap<>();
    private static LruCache<String, Bitmap> mDrawableCache;

    static {
        Context context = LQREmotionKit.getContext();

        load(context, EMOT_DIR + "emoji.xml");

        mPattern = makePattern();

        mDrawableCache = new LruCache<String, Bitmap>(CACHE_MAX_SIZE) {
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                if (oldValue != newValue)
                    oldValue.recycle();
            }
        };
    }

    public static final int getDisplayCount() {
        return mDefaultEntries.size();
    }

    public static final Drawable getDisplayDrawable(Context context, int index) {
        String text = (index >= 0 && index < mDefaultEntries.size() ? mDefaultEntries.get(index).text : null);
        return TextUtils.isEmpty(text) ? null : getDrawable(context, Integer.decode(text));
    }

    public static final String getDisplayText(int index) {
        return index >= 0 && index < mDefaultEntries.size() ? mDefaultEntries.get(index).text : null;
    }

    public static final Drawable getDrawable(Context context, int code) {
        Entry entry = mText2Entry.get(code);
        if (entry == null || TextUtils.isEmpty(entry.text)) {
            return null;
        }

        Bitmap cache = mDrawableCache.get(entry.assetPath);
        if (cache == null) {
            cache = loadAssetBitmap(context, entry.assetPath);
        }
        return new BitmapDrawable(context.getResources(), cache);
    }

    private static Bitmap loadAssetBitmap(Context context, String assetPath) {
        InputStream is = null;
        try {
            Resources resources = context.getResources();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inDensity = DisplayMetrics.DENSITY_HIGH;
            options.inScreenDensity = resources.getDisplayMetrics().densityDpi;
            options.inTargetDensity = resources.getDisplayMetrics().densityDpi;
            is = context.getAssets().open(assetPath);
            Bitmap bitmap = BitmapFactory.decodeStream(is, new Rect(), options);
            if (bitmap != null) {
                mDrawableCache.put(assetPath, bitmap);
            }
            return bitmap;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public static final Pattern getPattern() {
        return mPattern;
    }

    public static boolean contains(int code) {
        return mText2Entry.containsKey(code);
    }

    private static Pattern makePattern() {
        return Pattern.compile(patternOfDefault());
    }

    private static String patternOfDefault() {
        return "\\[[^\\[]{1,10}\\]";
    }

    private static final void load(Context context, String xmlPath) {
        new EntryLoader().load(context, xmlPath);

        //补充最后一页少的表情
        int tmp = mDefaultEntries.size() % EmotionLayout.EMOJI_PER_PAGE;
        if (tmp != 0) {
            int tmp2 = EmotionLayout.EMOJI_PER_PAGE - (mDefaultEntries.size() - (mDefaultEntries.size() / EmotionLayout.EMOJI_PER_PAGE) * EmotionLayout.EMOJI_PER_PAGE);
            for (int i = 0; i < tmp2; i++) {
                mDefaultEntries.add(new Entry("", ""));
            }
        }
    }

    private static class Entry {
        String text;
        String assetPath;

        public Entry(String text, String assetPath) {
            this.text = text;
            this.assetPath = assetPath;
        }
    }

    private static class EntryLoader extends DefaultHandler {
        private String catalog = "";

        void load(Context context, String assetPath) {
            InputStream is = null;
            try {
                is = context.getAssets().open(assetPath);
                Xml.parse(is, Xml.Encoding.UTF_8, this);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SAXException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (localName.equals("Catalog")) {
                catalog = attributes.getValue(uri, "Title");
            } else if (localName.equals("Emoticon")) {
                String tag = attributes.getValue(uri, "Tag");
                String fileName = attributes.getValue(uri, "File");
                Entry entry = new Entry(tag, EMOT_DIR + catalog + "/" + fileName);

                mText2Entry.put(Integer.decode(tag), entry);
                if (catalog.equals("default")) {
                    mDefaultEntries.add(entry);
                }
            }
        }
    }

}
