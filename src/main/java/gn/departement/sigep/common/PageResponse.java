package gn.departement.sigep.common;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Enveloppe de pagination stable exposée dans l'API, indépendante de la
 * sérialisation interne de {@code Page<T>} de Spring Data.
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalItems,
        int totalPages
) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    public static <T, R> PageResponse<R> from(Page<T> page, List<R> mappedItems) {
        return new PageResponse<>(
                mappedItems,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
