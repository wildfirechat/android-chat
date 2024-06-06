/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.model.ChannelInfo;
import cn.wildfirechat.model.DomainInfo;

public interface OnDomainInfoUpdateListener {
    void onDomainInfoUpdate(DomainInfo domainInfo);
}
