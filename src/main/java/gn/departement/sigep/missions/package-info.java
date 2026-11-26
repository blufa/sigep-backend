/**
 * Module missions : périodes où un employé est envoyé en mission officielle (déplacement
 * professionnel). Un employé en mission ne doit pas être compté absent par le module attendance
 * pendant cette période (voir {@code MissionUseCases#isOnMission}, consommé par
 * {@code gn.departement.sigep.attendance}).
 */
@org.springframework.modulith.ApplicationModule
package gn.departement.sigep.missions;
