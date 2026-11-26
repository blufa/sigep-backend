package gn.departement.sigep.attendance.application.service;

import gn.departement.sigep.attendance.application.port.in.AttendanceUseCases;
import gn.departement.sigep.attendance.application.port.out.AttendanceRepository;
import gn.departement.sigep.attendance.application.port.out.MonthlyAttendanceReportRenderer;
import gn.departement.sigep.attendance.domain.AttendancePunch;
import gn.departement.sigep.attendance.domain.AttendancePunchPhoto;
import gn.departement.sigep.attendance.domain.Direction;
import gn.departement.sigep.badges.application.port.in.BadgeUseCases;
import gn.departement.sigep.badges.domain.EmployeeBadge;
import gn.departement.sigep.common.PageResponse;
import gn.departement.sigep.common.TenantContext;
import gn.departement.sigep.common.exception.BusinessRuleException;
import gn.departement.sigep.common.exception.NotFoundException;
import gn.departement.sigep.missions.application.port.in.MissionUseCases;
import gn.departement.sigep.platform.application.port.in.TenantSettingsUseCases;
import gn.departement.sigep.platform.domain.Tenant;
import gn.departement.sigep.staff.application.port.out.EmployeeRepository;
import gn.departement.sigep.staff.application.port.out.PositionRepository;
import gn.departement.sigep.staff.domain.Employee;
import gn.departement.sigep.staff.domain.Position;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class AttendanceService implements AttendanceUseCases {

    private static final ZoneId ZONE = ZoneId.of("Africa/Conakry");

    /** Repli si l'administrateur n'a pas encore configuré les horaires de travail du tenant. */
    private static final LocalTime DEFAULT_WORK_START = LocalTime.of(8, 30);

    private final AttendanceRepository attendanceRepository;
    private final BadgeUseCases badgeUseCases;
    private final EmployeeRepository employeeRepository;
    private final PositionRepository positionRepository;
    private final TenantSettingsUseCases tenantSettingsUseCases;
    private final MonthlyAttendanceReportRenderer reportRenderer;
    private final MissionUseCases missionUseCases;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            BadgeUseCases badgeUseCases,
            EmployeeRepository employeeRepository,
            PositionRepository positionRepository,
            TenantSettingsUseCases tenantSettingsUseCases,
            MonthlyAttendanceReportRenderer reportRenderer,
            MissionUseCases missionUseCases) {
        this.attendanceRepository = attendanceRepository;
        this.badgeUseCases = badgeUseCases;
        this.employeeRepository = employeeRepository;
        this.positionRepository = positionRepository;
        this.tenantSettingsUseCases = tenantSettingsUseCases;
        this.reportRenderer = reportRenderer;
        this.missionUseCases = missionUseCases;
    }

    @Override
    public ScanResult scan(String qrToken, String source, byte[] photo, String photoContentType) {
        EmployeeBadge badge = badgeUseCases.findActiveByQrToken(qrToken);
        Employee employee = getActiveEmployeeOrThrow(badge.employeeId());
        return recordPunch(employee, badge.id(), source == null || source.isBlank() ? "QR" : source, photo, photoContentType);
    }

    @Override
    public ScanResult scanManual(Long employeeId, String source) {
        Employee employee = getActiveEmployeeOrThrow(employeeId);
        EmployeeBadge badge = badgeUseCases.getOrIssue(employeeId);
        return recordPunch(employee, badge.id(), source == null || source.isBlank() ? "MANUEL" : source, null, null);
    }

    private Employee getActiveEmployeeOrThrow(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> NotFoundException.of("Employé", employeeId));
        if (!employee.active()) {
            throw new BusinessRuleException("EMPLOYEE_INACTIVE", "Cet employé n'est plus en activité", HttpStatus.FORBIDDEN);
        }
        return employee;
    }

    private ScanResult recordPunch(Employee employee, Long badgeId, String source, byte[] photo, String photoContentType) {
        Instant now = Instant.now();
        Instant[] dayWindow = todayWindow(now);
        Direction direction = attendanceRepository
                .findLastByEmployeeIdAndScannedAtBetween(employee.id(), dayWindow[0], dayWindow[1])
                .map(last -> last.direction() == Direction.ENTREE ? Direction.SORTIE : Direction.ENTREE)
                .orElse(Direction.ENTREE);

        AttendancePunch punch = new AttendancePunch(
                null, TenantContext.get(), employee.id(), badgeId, direction, now, source, false);
        AttendancePunch saved = attendanceRepository.save(punch);
        if (photo != null && photo.length > 0) {
            attendanceRepository.savePunchPhoto(saved.id(), photo, photoContentType);
        }

        return new ScanResult(saved.id(), employee.id(), employee.matricule(), fullName(employee), direction, now, false);
    }

    @Override
    public Optional<AttendancePunchPhoto> getPunchPhoto(Long punchId) {
        return attendanceRepository.findPunchPhoto(punchId);
    }

    @Override
    public void flagPunch(Long punchId, boolean flagged) {
        attendanceRepository.setFlagged(punchId, flagged);
    }

    @Override
    public PageResponse<DailyStatusEntry> dailyStatus(LocalDate day, DailyStatus statusFilter, Pageable pageable) {
        Tenant organization = tenantSettingsUseCases.getCurrent();
        LocalTime workStart = organization.workStartTime() != null ? organization.workStartTime() : DEFAULT_WORK_START;

        Instant dayStart = day.atStartOfDay(ZONE).toInstant();
        Instant dayEnd = day.plusDays(1).atStartOfDay(ZONE).toInstant();
        List<AttendancePunch> dayPunches = attendanceRepository.findAllByScannedAtBetween(dayStart, dayEnd);
        Map<Long, List<AttendancePunch>> punchesByEmployee = dayPunches.stream()
                .collect(java.util.stream.Collectors.groupingBy(AttendancePunch::employeeId));

        List<Long> activeEmployeeIds = employeeRepository.findActiveEmployeeIds();
        List<DailyStatusEntry> all = new ArrayList<>();

        for (Long employeeId : activeEmployeeIds) {
            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee == null) {
                continue;
            }
            List<AttendancePunch> punches = punchesByEmployee.get(employeeId);
            DailyStatus status;
            Instant firstEntree = null;
            Instant lastSortie = null;
            if (punches == null || punches.isEmpty()) {
                // Un employé en mission officielle ce jour-là ne doit pas être compté absent (ni
                // déclencher la notification SMS d'absence, voir AbsenceSmsNotificationScheduler).
                status = missionUseCases.isOnMission(employeeId, day) ? DailyStatus.EN_MISSION : DailyStatus.ABSENT;
            } else {
                firstEntree = punches.stream()
                        .filter(p -> p.direction() == Direction.ENTREE)
                        .map(AttendancePunch::scannedAt)
                        .min(Comparator.naturalOrder())
                        .orElse(null);
                lastSortie = punches.stream()
                        .filter(p -> p.direction() == Direction.SORTIE)
                        .map(AttendancePunch::scannedAt)
                        .max(Comparator.naturalOrder())
                        .orElse(null);
                status = firstEntree != null && firstEntree.atZone(ZONE).toLocalTime().isAfter(workStart)
                        ? DailyStatus.RETARD
                        : DailyStatus.PRESENT;
            }

            if (statusFilter != null && status != statusFilter) {
                continue;
            }

            String positionTitle = employee.positionId() != null
                    ? positionRepository.findById(employee.positionId()).map(Position::title).orElse(null)
                    : null;
            all.add(new DailyStatusEntry(
                    employee.id(), employee.matricule(), fullName(employee), positionTitle,
                    status, firstEntree, lastSortie));
        }

        all.sort(Comparator.comparing(DailyStatusEntry::fullName));

        int start = Math.min((int) pageable.getOffset(), all.size());
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<DailyStatusEntry> pageContent = all.subList(start, end);
        int totalPages = pageable.getPageSize() == 0 ? 0 : (int) Math.ceil(all.size() / (double) pageable.getPageSize());

        return new PageResponse<>(pageContent, pageable.getPageNumber(), pageable.getPageSize(), all.size(), totalPages);
    }

    @Override
    public PageResponse<AttendanceEntry> history(Long employeeId, Instant from, Instant to, Pageable pageable) {
        Page<AttendancePunch> page = attendanceRepository.search(employeeId, from, to, pageable);
        Map<Long, Employee> employeeCache = new HashMap<>();
        List<Long> punchIds = page.getContent().stream().map(AttendancePunch::id).toList();
        Set<Long> idsWithPhoto = new HashSet<>(attendanceRepository.findPunchIdsWithPhoto(punchIds));
        List<AttendanceEntry> entries = page.getContent().stream()
                .map(punch -> toEntry(punch, employeeCache, idsWithPhoto.contains(punch.id())))
                .toList();
        return PageResponse.from(page, entries);
    }

    @Override
    public AttendanceSummary summary() {
        Tenant organization = tenantSettingsUseCases.getCurrent();
        LocalTime workStart = organization.workStartTime() != null ? organization.workStartTime() : DEFAULT_WORK_START;
        LocalTime workEnd = organization.workEndTime();

        Instant[] dayWindow = todayWindow(Instant.now());
        List<AttendancePunch> todayPunches = attendanceRepository.findAllByScannedAtBetween(dayWindow[0], dayWindow[1]);
        Map<Long, List<AttendancePunch>> punchesByEmployee = todayPunches.stream()
                .collect(java.util.stream.Collectors.groupingBy(AttendancePunch::employeeId));

        long presentToday = punchesByEmployee.values().stream()
                .map(punches -> punches.stream().max(Comparator.comparing(AttendancePunch::scannedAt)).orElseThrow())
                .filter(last -> last.direction() == Direction.ENTREE)
                .count();

        List<Long> activeEmployeeIds = employeeRepository.findActiveEmployeeIds();
        long onTimeToday = 0;
        long lateToday = 0;
        long earlyDepartureToday = 0;
        Set<Long> arrivedToday = new HashSet<>();

        for (Long employeeId : activeEmployeeIds) {
            List<AttendancePunch> punches = punchesByEmployee.get(employeeId);
            if (punches == null || punches.isEmpty()) {
                continue;
            }
            arrivedToday.add(employeeId);
            Instant firstEntree = punches.stream()
                    .filter(punch -> punch.direction() == Direction.ENTREE)
                    .map(AttendancePunch::scannedAt)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            if (firstEntree != null) {
                if (firstEntree.atZone(ZONE).toLocalTime().isAfter(workStart)) {
                    lateToday++;
                } else {
                    onTimeToday++;
                }
            }

            // Sortie "définitive" du jour (la dernière sortie, s'il n'y a pas eu de nouvelle entrée
            // après) avant l'heure de descente configurée — signal de départ anticipé, utile au
            // directeur pour repérer un agent qui ne reste pas jusqu'à l'heure réglementaire.
            if (workEnd != null) {
                AttendancePunch last = punches.stream().max(Comparator.comparing(AttendancePunch::scannedAt)).orElse(null);
                if (last != null && last.direction() == Direction.SORTIE
                        && last.scannedAt().atZone(ZONE).toLocalTime().isBefore(workEnd)) {
                    earlyDepartureToday++;
                }
            }
        }

        LocalDate today = Instant.now().atZone(ZONE).toLocalDate();
        long onMissionToday = activeEmployeeIds.stream()
                .filter(id -> !arrivedToday.contains(id) && missionUseCases.isOnMission(id, today))
                .count();
        long absentToday = activeEmployeeIds.size() - arrivedToday.size() - onMissionToday;

        return new AttendanceSummary(
                presentToday, activeEmployeeIds.size(), onTimeToday, lateToday, absentToday, earlyDepartureToday, onMissionToday);
    }

    @Override
    public byte[] generateMonthlyReportPdf(YearMonth month) {
        Tenant organization = tenantSettingsUseCases.getCurrent();
        MonthContext ctx = buildMonthContext(month, organization);

        List<Long> activeEmployeeIds = employeeRepository.findActiveEmployeeIds();
        List<MonthlyAttendanceReportRenderer.MonthlyReportRow> rows = new ArrayList<>();

        for (Long employeeId : activeEmployeeIds) {
            Employee employee = employeeRepository.findById(employeeId).orElse(null);
            if (employee == null) {
                continue;
            }
            rows.add(computeRow(employee, ctx));
        }

        rows.sort(Comparator.comparing(MonthlyAttendanceReportRenderer.MonthlyReportRow::fullName));

        return reportRenderer.render(month, rows, organization.code(), organization.ministryName(), organization.serviceName(),
                tenantSettingsUseCases.getLogo(TenantContext.get()));
    }

    @Override
    public byte[] generateEmployeeMonthlyReportPdf(Long employeeId, YearMonth month) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> NotFoundException.of("Employé", employeeId));
        Tenant organization = tenantSettingsUseCases.getCurrent();
        MonthContext ctx = buildMonthContext(month, organization);
        List<MonthlyAttendanceReportRenderer.MonthlyReportRow> rows = List.of(computeRow(employee, ctx));
        return reportRenderer.render(month, rows, organization.code(), organization.ministryName(), organization.serviceName(),
                tenantSettingsUseCases.getLogo(TenantContext.get()));
    }

    /** Horaires de référence, jours ouvrables du mois (lundi-vendredi, jusqu'à aujourd'hui si le
     * mois est en cours) et pointages du mois groupés par employé — calculés une seule fois, qu'il
     * s'agisse du rapport global (tous les employés) ou du rapport d'un seul employé. */
    private record MonthContext(
            LocalTime workStart,
            LocalTime workEnd,
            List<LocalDate> workingDays,
            Map<Long, List<AttendancePunch>> punchesByEmployee
    ) {
    }

    private MonthContext buildMonthContext(YearMonth month, Tenant organization) {
        LocalTime workStart = organization.workStartTime() != null ? organization.workStartTime() : DEFAULT_WORK_START;
        LocalTime workEnd = organization.workEndTime();

        LocalDate monthStart = month.atDay(1);
        LocalDate today = Instant.now().atZone(ZONE).toLocalDate();
        LocalDate lastDayToCount = month.atEndOfMonth().isAfter(today) ? today : month.atEndOfMonth();

        Instant rangeStart = monthStart.atStartOfDay(ZONE).toInstant();
        Instant rangeEnd = month.atEndOfMonth().plusDays(1).atStartOfDay(ZONE).toInstant();
        List<AttendancePunch> monthPunches = attendanceRepository.findAllByScannedAtBetween(rangeStart, rangeEnd);
        Map<Long, List<AttendancePunch>> punchesByEmployee = monthPunches.stream()
                .collect(java.util.stream.Collectors.groupingBy(AttendancePunch::employeeId));

        List<LocalDate> workingDays = new ArrayList<>();
        for (LocalDate day = monthStart; !day.isAfter(lastDayToCount); day = day.plusDays(1)) {
            if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                workingDays.add(day);
            }
        }

        return new MonthContext(workStart, workEnd, workingDays, punchesByEmployee);
    }

    private MonthlyAttendanceReportRenderer.MonthlyReportRow computeRow(Employee employee, MonthContext ctx) {
        List<AttendancePunch> punches = ctx.punchesByEmployee().getOrDefault(employee.id(), List.of());
        Map<LocalDate, List<AttendancePunch>> punchesByDay = punches.stream()
                .collect(java.util.stream.Collectors.groupingBy(p -> p.scannedAt().atZone(ZONE).toLocalDate()));

        int presentDays = 0;
        int lateDays = 0;
        int absentDays = 0;
        int missionDays = 0;
        int earlyDepartureDays = 0;
        double totalHours = 0;

        for (LocalDate day : ctx.workingDays()) {
            List<AttendancePunch> dayPunches = punchesByDay.get(day);
            if (dayPunches == null || dayPunches.isEmpty()) {
                if (missionUseCases.isOnMission(employee.id(), day)) {
                    missionDays++;
                } else {
                    absentDays++;
                }
                continue;
            }
            presentDays++;
            Instant firstEntree = dayPunches.stream()
                    .filter(p -> p.direction() == Direction.ENTREE)
                    .map(AttendancePunch::scannedAt)
                    .min(Comparator.naturalOrder())
                    .orElse(null);
            if (firstEntree != null && firstEntree.atZone(ZONE).toLocalTime().isAfter(ctx.workStart())) {
                lateDays++;
            }
            AttendancePunch last = dayPunches.stream().max(Comparator.comparing(AttendancePunch::scannedAt)).orElse(null);
            if (last != null && last.direction() == Direction.SORTIE) {
                if (ctx.workEnd() != null && last.scannedAt().atZone(ZONE).toLocalTime().isBefore(ctx.workEnd())) {
                    earlyDepartureDays++;
                }
                if (firstEntree != null) {
                    totalHours += java.time.Duration.between(firstEntree, last.scannedAt()).toMinutes() / 60.0;
                }
            }
        }

        String positionTitle = employee.positionId() != null
                ? positionRepository.findById(employee.positionId()).map(Position::title).orElse(null)
                : null;

        return new MonthlyAttendanceReportRenderer.MonthlyReportRow(
                employee.matricule(), fullName(employee), employee.hierarchie(), positionTitle,
                ctx.workingDays().size(), presentDays, lateDays, absentDays, missionDays, earlyDepartureDays, totalHours);
    }

    private AttendanceEntry toEntry(AttendancePunch punch, Map<Long, Employee> cache, boolean hasPhoto) {
        Employee employee = cache.computeIfAbsent(punch.employeeId(),
                id -> employeeRepository.findById(id).orElse(null));
        String matricule = employee != null ? employee.matricule() : null;
        String fullName = employee != null ? fullName(employee) : "Employé supprimé";
        return new AttendanceEntry(
                punch.id(), punch.employeeId(), matricule, fullName, punch.direction(), punch.scannedAt(),
                punch.source(), hasPhoto, punch.flagged());
    }

    private String fullName(Employee employee) {
        return employee.firstName() + " " + employee.lastName();
    }

    private Instant[] todayWindow(Instant reference) {
        LocalDate today = reference.atZone(ZONE).toLocalDate();
        Instant start = today.atStartOfDay(ZONE).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(ZONE).toInstant();
        return new Instant[]{start, end};
    }
}
