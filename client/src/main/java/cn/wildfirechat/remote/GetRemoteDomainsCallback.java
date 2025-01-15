/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.remote;

import java.util.List;

import cn.wildfirechat.message.Message;
import cn.wildfirechat.model.DomainInfo;

public interface GetRemoteDomainsCallback {
    void onSuccess(List<DomainInfo> domainInfos);

    void onFail(int errorCode);
}
