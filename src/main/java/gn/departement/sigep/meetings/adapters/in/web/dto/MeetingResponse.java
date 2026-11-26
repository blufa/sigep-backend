package gn.departement.sigep.meetings.adapters.in.web.dto;

import gn.departement.sigep.meetings.application.port.in.MeetingUseCases;
import gn.departement.sigep.meetings.domain.Meeting;

import java.time.Instant;
import java.util.List;

public record MeetingResponse(
        Long id,
        String title,
        String description,
        Instant scheduledAt,
        String location,
        List<AttendeeResponse> attendees,
        Instant notifyAt,
        boolean notified,
        Instant createdAt
) {

    public static MeetingResponse from(MeetingUseCases.MeetingView view) {
        List<AttendeeResponse> attendees = view.attendees().stream()
                .map(a -> new AttendeeResponse(a.employeeId(), a.matricule(), a.fullName()))
                .toList();
        return new MeetingResponse(
                view.id(), view.title(), view.description(), view.scheduledAt(), view.location(),
                attendees, view.notifyAt(), view.notified(), view.createdAt());
    }

    /** Juste après la création : pas encore de vue enrichie, le front connaît déjà les invités choisis. */
    public static MeetingResponse from(Meeting meeting) {
        return new MeetingResponse(
                meeting.id(), meeting.title(), meeting.description(), meeting.scheduledAt(), meeting.location(),
                List.of(), meeting.notifyAt(), meeting.notified(), meeting.createdAt());
    }

    public record AttendeeResponse(Long employeeId, String matricule, String fullName) {
    }
}
