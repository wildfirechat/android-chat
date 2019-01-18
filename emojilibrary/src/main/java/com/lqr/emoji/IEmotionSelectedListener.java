package com.lqr.emoji;

public interface IEmotionSelectedListener {
    void onEmojiSelected(String key);

    void onStickerSelected(String categoryName, String stickerName, String stickerBitmapPath);
}
