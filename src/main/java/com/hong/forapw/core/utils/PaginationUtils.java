package com.hong.forapw.core.utils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationUtils {

    private PaginationUtils() {
    }

    public static boolean isLastPage(Page<?> page) {
        return !page.hasNext();
    }

    public static Pageable createPageable(int pageNumber, int pageSize, String sortByField, Sort.Direction direction) {
        return PageRequest.of(pageNumber, pageSize, Sort.by(direction, sortByField));
    }
}

