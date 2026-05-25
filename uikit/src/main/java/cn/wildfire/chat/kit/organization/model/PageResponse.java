/*
 * Copyright (c) 2023 WildFireChat. All rights reserved.
 */

package cn.wildfire.chat.kit.organization.model;

import java.util.List;

public class PageResponse<T> {
    public int totalPages;
    public int totalCount;
    public int currentPage;
    public List<T> contents;
}
