package gn.departement.sigep.badges.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "employee_badges")
@Filter(name = "tenantFilter")
@Getter
@Setter
@NoArgsConstructor
class EmployeeBadgeJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "employee_id", nullable = false, unique = true)
    private Long employeeId;

    @Column(name = "qr_token", nullable = false, unique = true)
    private String qrToken;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "printed_at")
    private Instant printedAt;
}
