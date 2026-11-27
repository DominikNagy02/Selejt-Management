package hu.unideb.inf.backend.service.masterdata;

import hu.unideb.inf.core.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageMapperTest {

    @Test
    void mapsSpringDataPageToPageResponse() {
        PageImpl<String> page = new PageImpl<>(List.of("A", "B"), PageRequest.of(0, 2), 5);
        PageResponse<String> response = PageMapper.fromPage(page);

        assertEquals(List.of("A", "B"), response.content());
        assertEquals(0, response.page());
        assertEquals(2, response.size());
        assertEquals(5, response.totalElements());
        assertEquals(3, response.totalPages());
        assertTrue(response.first());
        assertFalse(response.last());
    }

    @Test
    void paginatesListsAcrossUnpagedEmptyMiddleAndLastPages() {
        PageResponse<String> unpagedEmpty = PageMapper.paginate(List.of(), null);
        assertTrue(unpagedEmpty.content().isEmpty());
        assertEquals(0, unpagedEmpty.totalPages());
        assertTrue(unpagedEmpty.first());
        assertTrue(unpagedEmpty.last());

        PageResponse<String> unpaged = PageMapper.paginate(List.of("A", "B"), Pageable.unpaged());
        assertEquals(2, unpaged.content().size());
        assertEquals(1, unpaged.totalPages());

        PageResponse<String> pagedEmpty = PageMapper.paginate(List.of(), PageRequest.of(0, 5));
        assertTrue(pagedEmpty.content().isEmpty());
        assertTrue(pagedEmpty.last());

        PageResponse<String> page0 = PageMapper.paginate(List.of("A", "B", "C"), PageRequest.of(0, 2));
        assertEquals(List.of("A", "B"), page0.content());
        assertTrue(page0.first());
        assertFalse(page0.last());

        PageResponse<String> page1 = PageMapper.paginate(List.of("A", "B", "C"), PageRequest.of(1, 2));
        assertEquals(List.of("C"), page1.content());
        assertFalse(page1.first());
        assertTrue(page1.last());
    }
}
