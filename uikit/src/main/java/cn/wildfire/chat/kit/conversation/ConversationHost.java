/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.conversation;

/**
 * 会话页（{@link ConversationFragment}）的宿主。
 * <p>
 * 改造前 {@code ConversationFragment} 假设「我独占一个 Activity」：把宿主强转成
 * {@link cn.wildfire.chat.kit.WfcBaseActivity} 直接写标题、读自己的 Intent、调 {@code finish()}。
 * 平板双栏下宿主变成「主界面的右栏」，这些假设全部不成立——标题要写到右栏自己的 toolbar 上而不是
 * 全局标题栏，关闭会话只是清空右栏而不是结束整个主界面，高亮消息 id 也不在宿主的 Intent 里。
 * <p>
 * 本接口把这几件「只有宿主知道该怎么做」的事抽出来。手机端由
 * {@link WfcBaseActivityConversationHost} 实现，逐行搬运自改造前的 Fragment 代码，行为完全一致。
 */
public interface ConversationHost {

    /**
     * 更新会话标题。
     *
     * @param title    标题文本
     * @param subTitle 副标题，可为 null
     * @param silent   会话是否免打扰，为 true 时在标题后追加静音图标
     * @param earpiece 是否听筒播放模式，为 true 时在标题后追加听筒图标
     */
    void setConversationTitle(CharSequence title, CharSequence subTitle, boolean silent, boolean earpiece);

    /**
     * 当前展示的标题文本（不含图标）。用于「对方正在输入」结束后判断是否需要还原标题。
     */
    CharSequence getConversationTitle();

    /**
     * 关闭当前会话。独立 Activity 下等价于 {@code finish()}，双栏下是清空右栏。
     */
    void closeConversation();

    /**
     * 需要高亮定位的消息 id，没有则返回 0。
     */
    long getHighlightMessageId();
}
