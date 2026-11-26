package gn.departement.sigep.meetings.adapters.in.web;

import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.meetings.adapters.in.web.dto.MeetingRequest;
import gn.departement.sigep.meetings.adapters.in.web.dto.MeetingResponse;
import gn.departement.sigep.meetings.adapters.in.web.dto.UpdateMeetingRequest;
import gn.departement.sigep.meetings.application.port.in.MeetingUseCases;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meetings")
@PreAuthorize("hasRole('ADMIN')")
public class MeetingController {

    private final MeetingUseCases useCases;

    public MeetingController(MeetingUseCases useCases) {
        this.useCases = useCases;
    }

    @GetMapping
    public PageResponse<MeetingResponse> list(Pageable pageable) {
        PageResponse<MeetingUseCases.MeetingView> page = useCases.list(pageable);
        return new PageResponse<>(
                page.items().stream().map(MeetingResponse::from).toList(),
                page.page(), page.size(), page.totalItems(), page.totalPages());
    }

    @PostMapping
    public MeetingResponse create(@Valid @RequestBody MeetingRequest request) {
        return MeetingResponse.from(useCases.create(new MeetingUseCases.CreateMeetingCommand(
                request.title(), request.description(), request.scheduledAt(), request.location(),
                request.attendeeEmployeeIds(), request.notifyAt())));
    }

    @PutMapping("/{id}")
    public MeetingResponse update(@PathVariable Long id, @Valid @RequestBody UpdateMeetingRequest request) {
        return MeetingResponse.from(useCases.update(id, new MeetingUseCases.UpdateMeetingCommand(
                request.title(), request.description(), request.scheduledAt(), request.location(),
                request.attendeeEmployeeIds())));
    }

    @DeleteMapping("/{id}")
    public void cancel(@PathVariable Long id) {
        useCases.cancel(id);
    }
}
