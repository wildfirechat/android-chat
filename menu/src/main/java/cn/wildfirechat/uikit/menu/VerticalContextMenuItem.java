/*
 * Copyright (c) 2025 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.uikit.menu;

import android.view.View;

public class VerticalContextMenuItem {

    private String item;
    private int itemResId = View.NO_ID;


    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public int getItemResId() {
        return itemResId;
    }

    public void setItemResId(int itemResId) {
        this.itemResId = itemResId;
    }
}
