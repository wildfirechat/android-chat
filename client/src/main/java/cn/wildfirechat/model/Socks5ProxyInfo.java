/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Created by heavyrainlee on 14/12/2017.
 */

public class Socks5ProxyInfo implements Parcelable, Comparable<Socks5ProxyInfo> {
    public String host;
    public String ip;
    public int port;
    public String username;
    public String password;

    public Socks5ProxyInfo() {
    }

    public Socks5ProxyInfo(String host, String ip, int port) {
        this.host = host;
        this.ip = ip;
        this.port = port;
    }

    public Socks5ProxyInfo(String host, String ip, int port, String username, String password) {
        this.host = host;
        this.ip = ip;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @Override
    public int compareTo(@NonNull Socks5ProxyInfo userInfo) {
        return username.compareTo(userInfo.username);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.host);
        dest.writeString(this.ip);
        dest.writeString(this.username);
        dest.writeString(this.password);
        dest.writeInt(this.port);
    }

    protected Socks5ProxyInfo(Parcel in) {
        this.host = in.readString();
        this.ip = in.readString();
        this.username = in.readString();
        this.password = in.readString();
        this.port = in.readInt();
    }

    public static final Creator<Socks5ProxyInfo> CREATOR = new Creator<Socks5ProxyInfo>() {
        @Override
        public Socks5ProxyInfo createFromParcel(Parcel source) {
            return new Socks5ProxyInfo(source);
        }

        @Override
        public Socks5ProxyInfo[] newArray(int size) {
            return new Socks5ProxyInfo[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Socks5ProxyInfo socks5ProxyInfo = (Socks5ProxyInfo) o;
        return port == socks5ProxyInfo.port && Objects.equals(host, socks5ProxyInfo.host) && Objects.equals(ip, socks5ProxyInfo.ip) && Objects.equals(username, socks5ProxyInfo.username) && Objects.equals(password, socks5ProxyInfo.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, ip, username, password, port);
    }
}
