/*
 * Copyright (c) 2020 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.model;

/**
 * 空领域信息类
 * <p>
 * 当领域（租户）信息不存在时返回的空对象实现。
 * 使用Null Object模式，避免上层代码不断的做空值检查。
 * </p>
 *
 * @author WildFireChat
 * @since 2020
 */
public class NullDomainInfo extends DomainInfo {
    public NullDomainInfo(String domainId) {
        this.domainId = domainId;
        this.name = domainId;
    }
}
