package gn.departement.sigep.staff.adapters.in.web.dto;

import gn.departement.sigep.staff.domain.Position;

public record PositionResponse(Long id, String title, String code, boolean active) {

    public static PositionResponse from(Position position) {
        return new PositionResponse(position.id(), position.title(), position.code(), position.active());
    }
}
