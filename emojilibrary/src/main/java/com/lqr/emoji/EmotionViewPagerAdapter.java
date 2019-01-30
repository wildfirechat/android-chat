package com.lqr.emoji;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.RelativeLayout;

import java.util.List;

import androidx.viewpager.widget.PagerAdapter;

import static com.lqr.emoji.EmotionLayout.EMOJI_PER_PAGE;
import static com.lqr.emoji.EmotionLayout.STICKER_PER_PAGE;

/**
 * CSDN_LQR
 * 表情控件的ViewPager适配器(emoji + 贴图)
 */

public class EmotionViewPagerAdapter extends PagerAdapter {

    private int mPageCount = 0;

    private int mEmotionLayoutWidth;
    private int mEmotionLayoutHeight;

    private IEmotionSelectedListener listener;
    EditText mMessageEditText;

    public void attachEditText(EditText messageEditText) {
        mMessageEditText = messageEditText;
    }

    public EmotionViewPagerAdapter(int emotionLayoutWidth, int emotionLayoutHeight, IEmotionSelectedListener listener) {
        mEmotionLayoutWidth = emotionLayoutWidth;
        mEmotionLayoutHeight = emotionLayoutHeight;
        mPageCount = (int) Math.ceil(EmojiManager.getDisplayCount() / (float) EMOJI_PER_PAGE);
        for (int i = 0; i < StickerManager.getInstance().getStickerCategories().size(); i++) {
            mPageCount += StickerManager.getInstance().getStickerCategories().get(i).getStickers().size() / STICKER_PER_PAGE;
        }

        this.listener = listener;
    }

    @Override
    public int getCount() {
        return mPageCount == 0 ? 1 : mPageCount;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        Context context = container.getContext();
        RelativeLayout rl = new RelativeLayout(context);
        rl.setGravity(Gravity.CENTER);

        GridView gridView = new GridView(context);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        gridView.setLayoutParams(params);
        gridView.setGravity(Gravity.CENTER);

        int tabPosition = positionToCategoryTabIndex(position);

        gridView.setTag((tabPosition & 0xFFF) << 12 | (position & 0xFFF)); // category index | pageIndex

        if (tabPosition == 0) {
            gridView.setOnItemClickListener(emojiListener);
            gridView.setAdapter(new EmojiAdapter(context, mEmotionLayoutWidth, mEmotionLayoutHeight, position * EMOJI_PER_PAGE));
            gridView.setNumColumns(EmotionLayout.EMOJI_COLUMNS);
        } else {
            int categoryStickerPageIndex = positionToCategoryPageIndex(position);
            StickerCategory category = StickerManager.getInstance().getCategory(StickerManager.getInstance().getStickerCategories().get(tabPosition - 1).getName());
            gridView.setOnItemClickListener(stickerListener);
            gridView.setAdapter(new StickerAdapter(context, category, mEmotionLayoutWidth, mEmotionLayoutHeight, categoryStickerPageIndex * STICKER_PER_PAGE));
            gridView.setNumColumns(EmotionLayout.STICKER_COLUMNS);
        }

        rl.addView(gridView);
        container.addView(rl);
        return rl;
    }

    /**
     * 根据categoryTabIndex计算page position
     *
     * @param categoryTabIndex
     * @return
     */
    int categoryTabIndexToPagePosition(int categoryTabIndex) {
        int position = (int) Math.ceil(EmojiManager.getDisplayCount() / (float) EMOJI_PER_PAGE);
        if (categoryTabIndex == 0) {
            return 0;
        } else {
            for (int i = 0; i < categoryTabIndex - 1; i++) {
                position += (int) Math.ceil(StickerManager.getInstance().getStickerCategories().get(i).getStickers().size() / (float) EmotionLayout.STICKER_PER_PAGE);
            }
        }
        return position;
    }

    /**
     * 根据page position，计算tab index
     *
     * @param position
     * @return
     */
    int positionToCategoryTabIndex(int position) {
        int categoryTabindex = 0;
        int emojiPageCount = (int) Math.ceil(EmojiManager.getDisplayCount() / (float) EMOJI_PER_PAGE);
        if (position >= emojiPageCount) {
            int stickerPageCount = 0;
            for (int i = 0; i < StickerManager.getInstance().getStickerCategories().size(); i++) {
                stickerPageCount += (int) Math.ceil(StickerManager.getInstance().getStickerCategories().get(i).getStickers().size() / (float) EmotionLayout.STICKER_PER_PAGE);
                if (position < emojiPageCount + stickerPageCount) {
                    categoryTabindex = 1 + i;
                    break;
                }
            }
        }
        return categoryTabindex;
    }

    /**
     * 根据page position，计算Category内部的pageIndex
     *
     * @param position
     * @return
     */
    int positionToCategoryPageIndex(int position) {
        int emojiPageCount = (int) Math.ceil(EmojiManager.getDisplayCount() / (float) EMOJI_PER_PAGE);
        int categoryPageIndex = -1;
        if (position < emojiPageCount) {
            categoryPageIndex = position;
        } else {
            int stickerPageCount = 0;
            for (int i = 0; i < StickerManager.getInstance().getStickerCategories().size(); i++) {
                categoryPageIndex = position - emojiPageCount - stickerPageCount;
                stickerPageCount += (int) Math.ceil(StickerManager.getInstance().getStickerCategories().get(i).getStickers().size() / (float) EmotionLayout.STICKER_PER_PAGE);
                if (position < emojiPageCount + stickerPageCount) {
                    break;
                }
            }
        }
        return categoryPageIndex;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    public AdapterView.OnItemClickListener emojiListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            int index = position + (Integer) parent.getTag() * EmotionLayout.EMOJI_PER_PAGE;
            int count = EmojiManager.getDisplayCount();
            if (position == EmotionLayout.EMOJI_PER_PAGE || index >= count) {
                if (listener != null) {
                    listener.onEmojiSelected("/DEL");
                }
                onEmojiSelected("/DEL");
            } else {
                String text = EmojiManager.getDisplayText((int) id);
                if (!TextUtils.isEmpty(text)) {
                    if (listener != null) {
                        listener.onEmojiSelected(text);
                    }
                    onEmojiSelected(text);
                }
            }
        }
    };
    public AdapterView.OnItemClickListener stickerListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int tag = (int) parent.getTag();
            int pagePosition = tag & 0xFFF;
            int categoryIndex = (tag >> 12) & 0xFFF;
            int index = position;
            if (pagePosition > 0) {
                int categoryStartPagePosition = categoryTabIndexToPagePosition(categoryIndex);
                index += (pagePosition - categoryStartPagePosition) * EmotionLayout.STICKER_PER_PAGE;
            }
            StickerCategory category = StickerManager.getInstance().getStickerCategories().get(categoryIndex - 1);
            List<StickerItem> stickers = category.getStickers();

            if (index >= stickers.size()) {
                Log.i("CSDN_LQR", "index " + index + " larger than size " + stickers.size());
                return;
            }

            if (listener != null) {
                StickerItem sticker = stickers.get(index);
                StickerCategory real = StickerManager.getInstance().getCategory(sticker.getCategory());

                if (real == null) {
                    return;
                }

                listener.onStickerSelected(sticker.getCategory(), sticker.getName(), StickerManager.getInstance().getStickerBitmapPath(sticker.getCategory(), sticker.getName()));
            }
        }
    };

    private void onEmojiSelected(String key) {

        if (mMessageEditText == null)
            return;
        Editable editable = mMessageEditText.getText();
        if (key.equals("/DEL")) {
            mMessageEditText.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
        } else {
            int code = Integer.decode(key);
            char[] chars = Character.toChars(code);
            String value = Character.toString(chars[0]);
            for (int i = 1; i < chars.length; i++) {
                value += Character.toString(chars[i]);
            }

            int start = mMessageEditText.getSelectionStart();
            int end = mMessageEditText.getSelectionEnd();
            start = (start < 0 ? 0 : start);
            end = (start < 0 ? 0 : end);
            editable.replace(start, end, value);

            int editEnd = mMessageEditText.getSelectionEnd();
            MoonUtils.replaceEmoticons(LQREmotionKit.getContext(), editable, 0, editable.toString().length());
            mMessageEditText.setSelection(editEnd);
        }
    }
}
