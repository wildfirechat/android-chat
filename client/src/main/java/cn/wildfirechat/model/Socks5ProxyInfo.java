/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * Socks5代理信息类
 * <p>
 * 用于表示Socks5代理服务器的连接信息。
 * 包含主机、IP、端口、用户名和密码等信息。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class Socks5ProxyInfo implements Parcelable, Comparable<Socks5ProxyInfo> {
    /**
     * 代理主机名
     */
    public String host;

    /**
     * 代理IP地址
     */
    public String ip;

    /**
     * 代理端口
     */
    public int port;

    /**
     * 代理用户名
     */
    public String username;

    /**
     * 代理密码
     */
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
