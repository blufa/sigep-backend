package gn.departement.sigep.badges.application.service;

import gn.departement.sigep.badges.application.port.in.BadgeUseCases;
import gn.departement.sigep.badges.application.port.out.BadgeCardRenderer;
import gn.departement.sigep.badges.application.port.out.BadgeCardView;
import gn.departement.sigep.badges.application.port.out.BadgeRepository;
import gn.departement.sigep.badges.application.port.out.PublicBadgeVerification;
import gn.departement.sigep.badges.domain.EmployeeBadge;
import gn.departement.sigep.common.TenantContext;
import gn.departement.sigep.common.exception.BusinessRuleException;
import gn.departement.sigep.common.exception.NotFoundException;
import gn.departement.sigep.platform.application.port.in.TenantSettingsUseCases;
import gn.departement.sigep.staff.application.port.out.EmployeeRepository;
import gn.departement.sigep.staff.application.port.out.PositionRepository;
import gn.departement.sigep.staff.domain.Employee;
import gn.departement.sigep.staff.domain.EmployeePhoto;
import gn.departement.sigep.staff.domain.Position;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class BadgeService implements BadgeUseCases {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final BadgeRepository badgeRepository;
    private final EmployeeRepository employeeRepository;
    private final PositionRepository positionRepository;
    private final BadgeCardRenderer renderer;
    private final TenantSettingsUseCases tenantSettingsUseCases;

    public BadgeService(
            BadgeRepository badgeRepository,
            EmployeeRepository employeeRepository,
            PositionRepository positionRepository,
            BadgeCardRenderer renderer,
            TenantSettingsUseCases tenantSettingsUseCases) {
        this.badgeRepository = badgeRepository;
        this.employeeRepository = employeeRepository;
        this.positionRepository = positionRepository;
        this.renderer = renderer;
        this.tenantSettingsUseCases = tenantSettingsUseCases;
    }

    @Override
    public EmployeeBadge getOrIssue(Long employeeId) {
        return badgeRepository.findByEmployeeId(employeeId).orElseGet(() -> issue(employeeId));
    }

    private EmployeeBadge issue(Long employeeId) {
        EmployeeBadge badge = new EmployeeBadge(
                null, TenantContext.get(), employeeId, generateToken(), true, Instant.now(), null);
        return badgeRepository.save(badge);
    }

    @Override
    public EmployeeBadge findActiveByQrToken(String qrToken) {
        EmployeeBadge badge = badgeRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new BusinessRuleException("BADGE_UNKNOWN", "Badge inconnu", HttpStatus.NOT_FOUND));
        if (!badge.active()) {
            throw new BusinessRuleException("BADGE_REVOKED", "Ce badge a été révoqué", HttpStatus.FORBIDDEN);
        }
        return badge;
    }

    @Override
    public BadgeCardView getCardView(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> NotFoundException.of("Employé", employeeId));
        EmployeeBadge badge = getOrIssue(employeeId);
        return toCardView(employee, badge);
    }

    @Override
    public byte[] generateCardPdf(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> NotFoundException.of("Employé", employeeId));
        EmployeeBadge badge = getOrIssue(employeeId);
        BadgeCardView view = toCardView(employee, badge);
        markPrinted(badge);
        return renderer.renderSingle(view, employeePhoto(employeeId), currentLogo());
    }

    @Override
    public byte[] generateSheetPdf(List<Long> employeeIds) {
        List<Employee> employees = employeeIds.stream()
                .map(id -> employeeRepository.findById(id).orElseThrow(() -> NotFoundException.of("Employé", id)))
                .toList();

        List<BadgeCardRenderer.CardWithPhoto> cards = employees.stream().map(employee -> {
            EmployeeBadge badge = getOrIssue(employee.id());
            markPrinted(badge);
            return new BadgeCardRenderer.CardWithPhoto(toCardView(employee, badge), employeePhoto(employee.id()));
        }).toList();

        return renderer.renderSheet(cards, currentLogo());
    }

    @Override
    public PublicBadgeVerification verifyPublic(String qrToken) {
        return resolveByToken(qrToken)
                .map(resolved -> new PublicBadgeVerification(
                        true,
                        resolved.employee().matricule(),
                        resolved.employee().firstName() + " " + resolved.employee().lastName(),
                        resolved.positionTitle(),
                        resolved.employee().hierarchie(),
                        employeeRepository.findPhoto(resolved.employee().id()).isPresent()))
                .orElseGet(PublicBadgeVerification::invalid);
    }

    @Override
    public Optional<EmployeePhoto> publicPhoto(String qrToken) {
        return resolveByToken(qrToken).flatMap(resolved -> employeeRepository.findPhoto(resolved.employee().id()));
    }

    @Override
    public PublicBadgeVerification verifyPublicByMatricule(String matricule) {
        return resolveByMatricule(matricule)
                .map(resolved -> new PublicBadgeVerification(
                        true,
                        resolved.employee().matricule(),
                        resolved.employee().firstName() + " " + resolved.employee().lastName(),
                        resolved.positionTitle(),
                        resolved.employee().hierarchie(),
                        employeeRepository.findPhoto(resolved.employee().id()).isPresent()))
                .orElseGet(PublicBadgeVerification::invalid);
    }

    @Override
    public Optional<EmployeePhoto> publicPhotoByMatricule(String matricule) {
        return resolveByMatricule(matricule).flatMap(resolved -> employeeRepository.findPhoto(resolved.employee().id()));
    }

    private record ResolvedBadge(Employee employee, String positionTitle) {
    }

    /** Résolution par jeton QR uniquement — ne doit JAMAIS dépendre du tenant courant (il n'y en a
     * pas pour un appel public non authentifié, voir {@link gn.departement.sigep.common.TenantContext}) ;
     * le jeton aléatoire (24 octets) sert lui-même d'autorisation. */
    private Optional<ResolvedBadge> resolveByToken(String qrToken) {
        return badgeRepository.findByQrToken(qrToken)
                .filter(EmployeeBadge::active)
                .flatMap(badge -> employeeRepository.findById(badge.employeeId()))
                .map(this::toResolvedBadge);
    }

    /** Résolution par matricule (saisie manuelle) — même absence de dépendance au tenant courant que
     * {@link #resolveByToken}, et même exigence de badge actif. */
    private Optional<ResolvedBadge> resolveByMatricule(String matricule) {
        return employeeRepository.findByMatricule(matricule)
                .filter(employee -> badgeRepository.findByEmployeeId(employee.id())
                        .map(EmployeeBadge::active)
                        .orElse(false))
                .map(this::toResolvedBadge);
    }

    private ResolvedBadge toResolvedBadge(Employee employee) {
        String positionTitle = employee.positionId() != null
                ? positionRepository.findById(employee.positionId()).map(Position::title).orElse(null)
                : null;
        return new ResolvedBadge(employee, positionTitle);
    }

    private BadgeCardRenderer.OrganizationLogo currentLogo() {
        var logo = tenantSettingsUseCases.getLogo(TenantContext.get());
        return logo != null
                ? new BadgeCardRenderer.OrganizationLogo(logo.content(), logo.contentType())
                : BadgeCardRenderer.OrganizationLogo.NONE;
    }

    private BadgeCardRenderer.CardPhoto employeePhoto(Long employeeId) {
        var photo = employeeRepository.findPhoto(employeeId).orElse(null);
        return photo != null
                ? new BadgeCardRenderer.CardPhoto(photo.content(), photo.contentType())
                : BadgeCardRenderer.CardPhoto.NONE;
    }

    private BadgeCardView toCardView(Employee employee, EmployeeBadge badge) {
        String positionTitle = null;
        if (employee.positionId() != null) {
            positionTitle = positionRepository.findById(employee.positionId()).map(Position::title).orElse(null);
        }
        var organization = tenantSettingsUseCases.getCurrent();
        return new BadgeCardView(
                employee.matricule(),
                employee.firstName() + " " + employee.lastName(),
                positionTitle,
                employee.hierarchie(),
                badge.qrToken(),
                organization.ministryName(),
                organization.serviceName(),
                employee.city()
        );
    }

    private void markPrinted(EmployeeBadge badge) {
        badgeRepository.save(new EmployeeBadge(
                badge.id(), badge.tenantId(), badge.employeeId(), badge.qrToken(),
                badge.active(), badge.issuedAt(), Instant.now()));
    }

    private String generateToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
