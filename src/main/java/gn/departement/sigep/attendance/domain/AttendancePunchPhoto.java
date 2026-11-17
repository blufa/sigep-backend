package gn.departement.sigep.attendance.domain;

/** Cliché pris par la caméra au moment du scan — preuve visuelle consultable à distance par le
 * directeur, pour dissuader le prêt de badge à un vigile ou un tiers. */
public record AttendancePunchPhoto(byte[] content, String contentType) {
}
