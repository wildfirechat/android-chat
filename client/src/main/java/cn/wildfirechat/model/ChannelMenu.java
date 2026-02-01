/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 频道菜单类
 * <p>
 * 用于表示频道的菜单项信息。
 * 支持多级菜单，包含菜单ID、类型、名称、URL、媒体ID等属性。
 * 常用于频道功能的扩展和自定义。
 * </p>
 *
 * @author WildFireChat
 * @since 2022
 */
public class ChannelMenu implements Parcelable {
    /**
     * 菜单ID
     */
    public String menuId;

    /**
     * 菜单类型
     */
    public String type;

    /**
     * 菜单名称
     */
    public String name;

    /**
     * 菜单键值
     */
    public String key;

    /**
     * 菜单链接URL
     */
    public String url;

    /**
     * 媒体ID
     */
    public String mediaId;

    /**
     * 文章ID
     */
    public String articleId;

    /**
     * 应用ID
     */
    public String appId;

    /**
     * 应用页面
     */
    public String appPage;

    /**
     * 附加信息
     */
    public String extra;

    /**
     * 子菜单列表
     */
    public List<ChannelMenu> subMenus;

    public JSONObject toJsonObj() {
        JSONObject obj = new JSONObject();
        try {
            obj.putOpt("menuId", this.menuId);
            obj.putOpt("type", this.type);
            obj.putOpt("name", this.name);
            obj.putOpt("key", this.key);
            obj.putOpt("url", this.url);
            obj.putOpt("mediaId", this.mediaId);
            obj.putOpt("articleId", this.articleId);
            obj.putOpt("appId", this.appId);
            obj.putOpt("appPage", this.appPage);
            obj.putOpt("extra", this.extra);
            if (this.subMenus != null && this.subMenus.size() > 0) {
                JSONArray arr = new JSONArray();
                for (ChannelMenu sm : this.subMenus) {
                    arr.put(sm.toJsonObj());
                }
                obj.put("subMenus", arr);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.menuId);
        dest.writeString(this.type);
        dest.writeString(this.name);
        dest.writeString(this.key);
        dest.writeString(this.url);
        dest.writeString(this.mediaId);
        dest.writeString(this.articleId);
        dest.writeString(this.appId);
        dest.writeString(this.appPage);
        dest.writeString(this.extra);
        dest.writeList(this.subMenus);
    }

    public void readFromParcel(Parcel source) {
        this.menuId = source.readString();
        this.type = source.readString();
        this.name = source.readString();
        this.key = source.readString();
        this.url = source.readString();
        this.mediaId = source.readString();
        this.articleId = source.readString();
        this.appId = source.readString();
        this.appPage = source.readString();
        this.extra = source.readString();
        this.subMenus = new ArrayList<ChannelMenu>();
        source.readList(this.subMenus, ChannelMenu.class.getClassLoader());
    }

    public ChannelMenu() {
    }

    protected ChannelMenu(Parcel in) {
        this.menuId = in.readString();
        this.type = in.readString();
        this.name = in.readString();
        this.key = in.readString();
        this.url = in.readString();
        this.mediaId = in.readString();
        this.articleId = in.readString();
        this.appId = in.readString();
        this.appPage = in.readString();
        this.extra = in.readString();
        this.subMenus = new ArrayList<ChannelMenu>();
        in.readList(this.subMenus, ChannelMenu.class.getClassLoader());
    }

    public static final Parcelable.Creator<ChannelMenu> CREATOR = new Parcelable.Creator<ChannelMenu>() {
        @Override
        public ChannelMenu createFromParcel(Parcel source) {
            return new ChannelMenu(source);
        }

        @Override
        public ChannelMenu[] newArray(int size) {
            return new ChannelMenu[size];
        }
    };
}
