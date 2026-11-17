package gn.departement.sigep.attendance.adapters.out.persistence;

import gn.departement.sigep.attendance.domain.Direction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;

import java.time.Instant;

@Entity
@Table(name = "attendance_punches")
@Filter(name = "tenantFilter")
@Getter
@Setter
@NoArgsConstructor
class AttendancePunchJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Column(name = "badge_id")
    private Long badgeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Direction direction;

    @Column(name = "scanned_at", nullable = false)
    private Instant scannedAt;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(name = "photo")
    private byte[] photo;

    @Column(name = "photo_content_type")
    private String photoContentType;
}
