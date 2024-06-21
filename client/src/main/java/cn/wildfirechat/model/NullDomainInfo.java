/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

public class NullDomainInfo extends DomainInfo {
    public NullDomainInfo(String domainId) {
        this.domainId = domainId;
        this.name = domainId;
    }
}
