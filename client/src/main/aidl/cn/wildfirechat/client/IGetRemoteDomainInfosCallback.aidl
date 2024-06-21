// IConnectionStatusChanged.aidl
package cn.wildfirechat.client;

import cn.wildfirechat.model.DomainInfo;

interface IGetRemoteDomainInfosCallback {
    void onSuccess(in List<DomainInfo> domainInfos);
    void onFailure(in int errorCode);
}
