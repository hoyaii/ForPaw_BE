package com.hong.forapw.core.utils;

import org.springframework.data.domain.Page;

public class PaginationUtils {

    private PaginationUtils() {
    }

    public static boolean isLastPage(Page<?> page) {
        return !page.hasNext();
    }
}

