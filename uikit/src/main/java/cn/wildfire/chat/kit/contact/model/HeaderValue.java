/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.contact.model;

public class HeaderValue {
    Object value;
    boolean boolValue;

    public HeaderValue() {
    }

    public HeaderValue(Object value, boolean boolValue) {
        this.value = value;
        this.boolValue = boolValue;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public boolean isBoolValue() {
        return boolValue;
    }

    public void setBoolValue(boolean boolValue) {
        this.boolValue = boolValue;
    }
}
