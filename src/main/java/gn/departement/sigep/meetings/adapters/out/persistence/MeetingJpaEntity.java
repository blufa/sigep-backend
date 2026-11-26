package gn.departement.sigep.meetings.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "meetings")
@Filter(name = "tenantFilter")
@Getter
@Setter
@NoArgsConstructor
class MeetingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(nullable = false)
    private String title;

    private String description;

    @Column(name = "scheduled_at", nullable = false)
    private Instant scheduledAt;

    private String location;

    @Column(name = "notify_at")
    private Instant notifyAt;

    @Column(nullable = false)
    private Boolean notified = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @jakarta.persistence.CollectionTable(name = "meeting_attendees", joinColumns = @jakarta.persistence.JoinColumn(name = "meeting_id"))
    @Column(name = "employee_id")
    private List<Long> attendeeEmployeeIds = new ArrayList<>();
}
