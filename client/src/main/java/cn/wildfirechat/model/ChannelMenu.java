/*
 * Copyright (c) 2022 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChannelMenu implements Parcelable {
    public String type;
    public String name;
    public String key;
    public String url;
    public String mediaId;
    public String articleId;
    public String appId;
    public String appPage;
    public String extra;
    public List<ChannelMenu> subMenus;

    public JSONObject toJson() {
        JSONObject object = new JSONObject();
        // TODO

        return object;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
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
