/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package com.lqr.emoji;

/**
 * Emoji 工具类
 */
public class EmojiUtils {

    // 缩放比例：1.2倍。可以根据视觉感受调整，通常 1.1f - 1.2f 之间效果最好。
    public static final float EMOJI_SCALE_FACTOR = 1.2f;

    /**
     * 判断一个 Unicode 码点是否是 emoji
     *
     * @param codePoint Unicode 码点
     * @return 是否是 emoji
     */
    public static boolean isEmoji(int codePoint) {
        // Emoji 主要分布在以下 Unicode 范围：
        return (codePoint >= 0x1F600 && codePoint <= 0x1F64F) || // Emoticons
            (codePoint >= 0x1F300 && codePoint <= 0x1F5FF) || // Misc Symbols and Pictographs
            (codePoint >= 0x1F680 && codePoint <= 0x1F6FF) || // Transport and Map
            (codePoint >= 0x1F1E0 && codePoint <= 0x1F1FF) || // Flags
            (codePoint >= 0x2600 && codePoint <= 0x26FF) ||   // Misc symbols
            (codePoint >= 0x2700 && codePoint <= 0x27BF) ||   // Dingbats
            (codePoint >= 0xFE00 && codePoint <= 0xFE0F) ||   // Variation Selectors
            (codePoint >= 0x1F900 && codePoint <= 0x1F9FF) || // Supplemental Symbols and Pictographs
            (codePoint >= 0x1FA00 && codePoint <= 0x1FA6F) || // Chess Symbols
            (codePoint >= 0x1FA70 && codePoint <= 0x1FAFF) || // Symbols and Pictographs Extended-A
            (codePoint >= 0x231A && codePoint <= 0x231B) ||   // Watch, Hourglass
            (codePoint >= 0x23E9 && codePoint <= 0x23F3) ||   // Play buttons
            (codePoint >= 0x25FD && codePoint <= 0x25FE) ||   // Squares
            (codePoint >= 0x2614 && codePoint <= 0x2615) ||   // Umbrella, Coffee
            (codePoint >= 0x2648 && codePoint <= 0x2653) ||   // Zodiac signs
            (codePoint >= 0x267F && codePoint <= 0x2693) ||   // Wheelchair, Anchor
            (codePoint >= 0x26A1 && codePoint <= 0x26AA) ||   // Lightning, Circles
            (codePoint >= 0x26BD && codePoint <= 0x26C8) ||   // Sports
            (codePoint >= 0x26CE && codePoint <= 0x26CF) ||   // Ophiuchus
            (codePoint >= 0x26D1 && codePoint <= 0x26D4) ||   // No Entry
            (codePoint >= 0x26E9 && codePoint <= 0x26EA) ||   // Buildings
            (codePoint >= 0x26F0 && codePoint <= 0x26F5) ||   // Mountain, Boat
            (codePoint >= 0x26FA && codePoint <= 0x26FA) ||   // Tent
            (codePoint >= 0x26FD && codePoint <= 0x26FD) ||   // Fuel Pump
            (codePoint >= 0x2702 && codePoint <= 0x2705) ||   // Scissors, Check
            (codePoint >= 0x270A && codePoint <= 0x270B) ||   // Fist, Hand
            (codePoint >= 0x2728 && codePoint <= 0x2728) ||   // Sparkles
            (codePoint >= 0x274C && codePoint <= 0x274C) ||   // Cross Mark
            (codePoint >= 0x274E && codePoint <= 0x274E) ||   // Cross Mark Button
            (codePoint >= 0x2753 && codePoint <= 0x2755) ||   // Question marks
            (codePoint >= 0x2757 && codePoint <= 0x2757) ||   // Exclamation
            (codePoint >= 0x2795 && codePoint <= 0x2797) ||   // Plus/Minus
            (codePoint >= 0x27B0 && codePoint <= 0x27B0) ||   // Loop
            (codePoint >= 0x27BF && codePoint <= 0x27BF) ||   // Loop
            (codePoint >= 0x2934 && codePoint <= 0x2935) ||   // Arrows
            (codePoint >= 0x2B05 && codePoint <= 0x2B07) ||   // Arrows
            (codePoint >= 0x2B1B && codePoint <= 0x2B1C) ||   // Squares
            (codePoint >= 0x2B50 && codePoint <= 0x2B50) ||   // Star
            (codePoint >= 0x2B55 && codePoint <= 0x2B55) ||   // Circle
            (codePoint >= 0x3030 && codePoint <= 0x3030) ||   // Wavy Dash
            (codePoint >= 0x303D && codePoint <= 0x303D) ||   // Part Alternation Mark
            (codePoint >= 0x3297 && codePoint <= 0x3299);     // Japanese symbols
    }
}
