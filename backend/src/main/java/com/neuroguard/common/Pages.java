package com.neuroguard.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/** Clamps client paging input the same way everywhere: page >= 0, 1 <= size <= 100. */
public final class Pages {

    public static final int MAX_PAGE_SIZE = 100;

    private Pages() {
    }

    public static PageRequest of(int page, int size, Sort sort) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);
    }
}
