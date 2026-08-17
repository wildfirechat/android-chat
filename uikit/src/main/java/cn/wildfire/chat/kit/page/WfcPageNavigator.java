/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.page;

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * 「我能把一个页面开在右栏里」。只有平板双栏主界面实现它，手机端没有任何实现类。
 * <p>
 * <strong>为什么需要一个显式接口，而不是继续拦 {@code startActivity}</strong>：
 * androidx fragment 1.5 起，{@code Fragment.startActivity(intent)} 最终调的是
 * {@code ContextCompat.startActivity(Activity, ...)}（{@code FragmentActivity$HostCallbacks}
 * 不再覆写 {@code onStartActivityFromFragment}），到达 Activity 时<strong>发起者已经丢了</strong>；
 * {@code Fragment.startActivityForResult} 更是走 {@code FragmentManager.launchStartActivityForResult}，
 * 到达 Activity 的 requestCode 是 FragmentManager 内部生成的码，<strong>不是调用方写的那个</strong>。
 * <p>
 * 也就是说：靠覆写 Activity 的 startActivity 系列方法，既拿不到「谁发起的」，也拿不到
 * 「要什么结果」。前者决定压栈还是换内容，后者决定选人页能不能开在右栏 —— 两件事都必须
 * 由调用点显式给出，见 {@link WfcPageCompat#startPage} 与 {@link WfcPageCompat#startPageForResult}。
 */
public interface WfcPageNavigator {

    /**
     * 尝试在右栏打开一个页面。
     *
     * @param caller      发起跳转的 Fragment，决定压到哪条栈：它自己在某条右栏栈里就压那条，
     *                    在左栏就换当前 tab 的内容
     * @param requestCode &lt; 0 表示不需要结果；&ge; 0 时该页出栈会把结果投递回 {@code caller}
     *                    的 {@code onActivityResult}
     * @return true 表示已在右栏打开，调用方不要再 startActivity
     */
    boolean openPageInPane(@Nullable Fragment caller, Intent intent, int requestCode);

    /**
     * 尝试在右栏用一个新页面<strong>顶替</strong> {@code caller} 所在的那一页 ——
     * 等价于手机端的 {@code startActivity(下一页)} + {@code finish()}。
     *
     * @return true 表示已顶替，调用方不要再 startActivity / finish；
     * false 表示不在右栏（或发起者那一页是栈底），按原路径处理
     */
    boolean replacePageInPane(@Nullable Fragment caller, Intent intent);
}
