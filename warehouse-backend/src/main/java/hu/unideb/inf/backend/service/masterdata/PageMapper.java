package hu.unideb.inf.backend.service.masterdata;

import hu.unideb.inf.core.dto.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Objects;

public final class PageMapper {

    private PageMapper() {
    }

    public static <T> PageResponse<T> fromPage(Page<T> page) {
        Objects.requireNonNull(page, "page must not be null");
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public static <T> PageResponse<T> paginate(List<T> items, Pageable pageable) {
        Objects.requireNonNull(items, "items must not be null");
        if (pageable == null || pageable.isUnpaged()) {
            int size = items.size();
            int totalPages = items.isEmpty() ? 0 : 1;
            return new PageResponse<>(List.copyOf(items), 0, size, size, totalPages, true, true);
        }

        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        long total = items.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int start = (int) Math.min(pageable.getOffset(), total);
        int end = (int) Math.min((long) start + size, total);
        List<T> slice = List.copyOf(items.subList(start, end));
        boolean first = page == 0;
        boolean last = totalPages == 0 || page >= totalPages - 1;
        return new PageResponse<>(slice, page, size, total, totalPages, first, last);
    }
}
