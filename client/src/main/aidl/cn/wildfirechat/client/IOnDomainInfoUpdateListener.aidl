// IOnReceiveMessage.aidl
package cn.wildfirechat.client;

// Declare any non-default types here with import statements
import cn.wildfirechat.model.DomainInfo;

interface IOnDomainInfoUpdateListener {
    void onDomainInfoUpdated(in DomainInfo domainInfo);
}
