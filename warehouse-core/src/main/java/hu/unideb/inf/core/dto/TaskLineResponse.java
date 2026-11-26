package hu.unideb.inf.core.dto;

public record TaskLineResponse(
        Long id,
        Long inventoryId,
        Long lotId,
        String lotNumber,
        Long fromShelfId,
        String fromShelfCode,
        Integer fromTierLevel,
        Long toShelfId,
        String toShelfCode,
        Integer toTierLevel,
        int requestedQuantity,
        int completedQuantity
) {
}
