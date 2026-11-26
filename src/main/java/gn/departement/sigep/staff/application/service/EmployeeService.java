package gn.departement.sigep.staff.application.service;

import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.common.TenantContext;
import gn.departement.sigep.common.exception.BusinessRuleException;
import gn.departement.sigep.common.exception.NotFoundException;
import gn.departement.sigep.platform.application.port.in.TenantSettingsUseCases;
import gn.departement.sigep.staff.application.port.in.EmployeeUseCases;
import gn.departement.sigep.staff.application.port.out.EmployeeRepository;
import gn.departement.sigep.staff.application.port.out.EmployeeReportRenderer;
import gn.departement.sigep.staff.application.port.out.PositionRepository;
import gn.departement.sigep.staff.domain.Employee;
import gn.departement.sigep.staff.domain.EmployeePhoto;
import gn.departement.sigep.staff.domain.Genre;
import gn.departement.sigep.staff.domain.Position;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmployeeService implements EmployeeUseCases {

    /** Taille cible (carré) de la photo employée après traitement — cohérent sur toutes les photos
     * affichées sur le badge et la fiche, quelle que soit la photo d'origine envoyée. */
    private static final int PHOTO_SIZE = 400;

    private final EmployeeRepository repository;
    private final PositionRepository positionRepository;
    private final TenantSettingsUseCases tenantSettingsUseCases;
    private final EmployeeReportRenderer reportRenderer;

    public EmployeeService(
            EmployeeRepository repository,
            PositionRepository positionRepository,
            TenantSettingsUseCases tenantSettingsUseCases,
            EmployeeReportRenderer reportRenderer) {
        this.repository = repository;
        this.positionRepository = positionRepository;
        this.tenantSettingsUseCases = tenantSettingsUseCases;
        this.reportRenderer = reportRenderer;
    }

    @Override
    public Employee create(CreateEmployeeCommand command) {
        if (repository.existsByMatricule(command.matricule())) {
            throw new BusinessRuleException(
                    "MATRICULE_TAKEN", "Ce matricule d'Etat est déjà enregistré", HttpStatus.CONFLICT);
        }
        Instant now = Instant.now();
        Employee employee = new Employee(
                null,
                TenantContext.get(),
                command.matricule(),
                command.lastName(),
                command.firstName(),
                command.hierarchie(),
                command.genre(),
                command.positionId(),
                command.phone(),
                command.city(),
                true,
                now,
                now
        );
        return repository.save(employee);
    }

    @Override
    public Employee update(Long id, UpdateEmployeeCommand command) {
        Employee existing = get(id);
        if (!command.matricule().equals(existing.matricule()) && repository.existsByMatricule(command.matricule())) {
            throw new BusinessRuleException(
                    "MATRICULE_TAKEN", "Ce matricule d'Etat est déjà enregistré", HttpStatus.CONFLICT);
        }
        Employee updated = new Employee(
                existing.id(),
                existing.tenantId(),
                command.matricule(),
                command.lastName(),
                command.firstName(),
                command.hierarchie(),
                command.genre(),
                command.positionId(),
                command.phone(),
                command.city(),
                command.active(),
                existing.createdAt(),
                Instant.now()
        );
        return repository.save(updated);
    }

    @Override
    public void delete(Long id) {
        get(id);
        try {
            repository.deleteById(id);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new BusinessRuleException("EMPLOYEE_HAS_HISTORY",
                    "Impossible de supprimer cet employé : des pointages, badges, missions ou réunions "
                            + "lui sont liés. Désactivez-le plutôt depuis sa fiche.",
                    HttpStatus.CONFLICT);
        }
    }

    @Override
    public Employee get(Long id) {
        return repository.findById(id).orElseThrow(() -> NotFoundException.of("Employé", id));
    }

    @Override
    public PageResponse<Employee> list(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return PageResponse.from(repository.findAll(pageable));
        }
        return PageResponse.from(repository.search(query.trim(), pageable));
    }

    @Override
    public void uploadPhoto(Long id, byte[] content, String contentType) {
        get(id);
        byte[] processed = flattenToWhiteAndResize(content);
        repository.savePhoto(id, processed != null ? processed : content, processed != null ? "image/jpeg" : contentType);
    }

    /** Aplatit une éventuelle transparence (PNG) sur fond blanc et redimensionne en carré
     * {@value #PHOTO_SIZE}×{@value #PHOTO_SIZE} (ratio préservé, cadré au centre) — pas de détourage
     * de la personne, juste un rendu homogène quelle que soit la photo envoyée. Retourne {@code null}
     * (et laisse l'original tel quel) si le format n'est pas décodable par {@link ImageIO}, pour ne
     * jamais faire échouer un envoi de photo à cause de ce traitement. */
    private byte[] flattenToWhiteAndResize(byte[] content) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(content));
            if (source == null) {
                return null;
            }
            double scale = Math.min((double) PHOTO_SIZE / source.getWidth(), (double) PHOTO_SIZE / source.getHeight());
            int scaledWidth = Math.max(1, (int) Math.round(source.getWidth() * scale));
            int scaledHeight = Math.max(1, (int) Math.round(source.getHeight() * scale));

            BufferedImage result = new BufferedImage(PHOTO_SIZE, PHOTO_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = result.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, PHOTO_SIZE, PHOTO_SIZE);
            g.drawImage(source, (PHOTO_SIZE - scaledWidth) / 2, (PHOTO_SIZE - scaledHeight) / 2, scaledWidth, scaledHeight, null);
            g.dispose();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(result, "jpg", output);
            return output.toByteArray();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    @Override
    public EmployeePhoto getPhoto(Long id) {
        return repository.findPhoto(id).orElse(null);
    }

    @Override
    public byte[] generateReportPdf() {
        Map<Long, String> positionTitleById = new HashMap<>();
        for (Position position : positionRepository.findAll(Pageable.unpaged())) {
            positionTitleById.put(position.id(), position.title());
        }

        var rows = repository.findAll(Pageable.unpaged()).stream()
                .map(employee -> new EmployeeReportRenderer.EmployeeReportRow(
                        employee.matricule(),
                        employee.lastName(),
                        employee.firstName(),
                        employee.hierarchie(),
                        employee.positionId() != null ? positionTitleById.get(employee.positionId()) : null,
                        employee.phone(),
                        genreLabel(employee.genre()),
                        employee.active()))
                .toList();

        var organization = tenantSettingsUseCases.getCurrent();
        var logo = tenantSettingsUseCases.getLogo(TenantContext.get());
        return reportRenderer.render(rows, organization.ministryName(), organization.serviceName(), logo);
    }

    private String genreLabel(Genre genre) {
        if (genre == null) {
            return null;
        }
        return genre == Genre.HOMME ? "Homme" : "Femme";
    }
}
