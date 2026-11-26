package gn.departement.sigep.meetings.adapters.out.persistence;

import gn.departement.sigep.meetings.domain.Meeting;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface MeetingMapper {

    Meeting toDomain(MeetingJpaEntity entity);

    MeetingJpaEntity toEntity(Meeting domain);
}
