package gn.departement.sigep.meetings.application.service;

import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.common.TenantContext;
import gn.departement.sigep.common.exception.NotFoundException;
import gn.departement.sigep.communication.application.port.in.SmsUseCases;
import gn.departement.sigep.meetings.application.port.in.MeetingUseCases;
import gn.departement.sigep.meetings.application.port.out.MeetingRepository;
import gn.departement.sigep.meetings.domain.Meeting;
import gn.departement.sigep.staff.application.port.out.EmployeeRepository;
import gn.departement.sigep.staff.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MeetingService implements MeetingUseCases {

    private static final ZoneId ZONE = ZoneId.of("Africa/Conakry");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm");

    private final MeetingRepository repository;
    private final EmployeeRepository employeeRepository;
    private final SmsUseCases smsUseCases;

    public MeetingService(MeetingRepository repository, EmployeeRepository employeeRepository, SmsUseCases smsUseCases) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.smsUseCases = smsUseCases;
    }

    @Override
    public Meeting create(CreateMeetingCommand command) {
        Meeting meeting = new Meeting(
                null, TenantContext.get(), command.title(), command.description(), command.scheduledAt(),
                command.location(), command.attendeeEmployeeIds(), Instant.now());
        Meeting saved = repository.save(meeting);
        notifyAttendees(saved);
        return saved;
    }

    @Override
    public void cancel(Long id) {
        repository.findById(id).orElseThrow(() -> NotFoundException.of("Réunion", id));
        repository.deleteById(id);
    }

    @Override
    public PageResponse<MeetingView> list(Pageable pageable) {
        Page<Meeting> page = repository.findAll(pageable);
        List<MeetingView> views = page.getContent().stream().map(this::toView).toList();
        return PageResponse.from(page, views);
    }

    private void notifyAttendees(Meeting meeting) {
        String when = meeting.scheduledAt().atZone(ZONE).format(DATETIME_FMT);
        String message = "SIGAP : réunion \"" + meeting.title() + "\" le " + when
                + (meeting.location() != null && !meeting.location().isBlank() ? " à " + meeting.location() : "") + ".";
        for (Long employeeId : meeting.attendeeEmployeeIds()) {
            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee != null && employee.phone() != null && !employee.phone().isBlank()) {
                smsUseCases.sendSms(employee.phone(), message);
            }
        }
    }

    private MeetingView toView(Meeting meeting) {
        List<AttendeeView> attendees = meeting.attendeeEmployeeIds().stream()
                .map(employeeRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .map(e -> new AttendeeView(e.id(), e.matricule(), e.firstName() + " " + e.lastName()))
                .toList();
        return new MeetingView(
                meeting.id(), meeting.title(), meeting.description(), meeting.scheduledAt(), meeting.location(),
                attendees, meeting.createdAt());
    }
}
