// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.absence.AbsenceCategory
import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.occupancy.familyUnitPlacementCoefficient
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.AttendanceReservationId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ChildImageId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.HolidayQuestionnaireId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.TimeRange
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import org.jdbi.v3.json.Json

fun Database.Transaction.deleteAbsencesCreatedFromQuestionnaire(
    questionnaireId: HolidayQuestionnaireId,
    childIds: Set<ChildId>
) {
    @Suppress("DEPRECATION")
    this.createUpdate(
            "DELETE FROM absence WHERE child_id = ANY(:childIds) AND questionnaire_id = :questionnaireId"
        )
        .bind("childIds", childIds)
        .bind("questionnaireId", questionnaireId)
        .execute()
}

fun Database.Transaction.clearOldReservations(
    reservations: List<Pair<ChildId, LocalDate>>
): List<AttendanceReservationId> {
    val batch =
        prepareBatch(
            "DELETE FROM attendance_reservation WHERE child_id = :childId AND date = :date RETURNING id"
        )

    reservations.forEach { (childId, date) ->
        batch.bind("childId", childId).bind("date", date).add()
    }

    return batch.executeAndReturn().toList<AttendanceReservationId>()
}

fun Database.Transaction.clearReservationsForRangeExceptInHolidayPeriod(
    childId: ChildId,
    range: DateRange
): Int {
    @Suppress("DEPRECATION")
    return this.createUpdate(
            """
            DELETE FROM attendance_reservation
            WHERE child_id = :childId
            AND between_start_and_end(:range, date)
            AND NOT EXISTS (SELECT 1 FROM holiday_period hp WHERE period @> date)
            """
        )
        .bind("childId", childId)
        .bind("range", range)
        .execute()
}

fun Database.Transaction.deleteAllCitizenReservationsInRange(range: FiniteDateRange) {
    @Suppress("DEPRECATION")
    this.createUpdate(
            "DELETE FROM attendance_reservation WHERE created_by IN (SELECT id FROM evaka_user WHERE type = 'CITIZEN') AND between_start_and_end(:range, date)"
        )
        .bind("range", range)
        .execute()
}

fun Database.Transaction.deleteReservationsFromHolidayPeriodDates(
    deletions: List<Pair<ChildId, LocalDate>>
): List<AttendanceReservationId> {
    val batch =
        prepareBatch(
            """
        DELETE FROM attendance_reservation
        WHERE child_id = :childId
        AND date = :date
        AND EXISTS (SELECT 1 FROM holiday_period WHERE period @> date)
        RETURNING id
    """
        )
    deletions.forEach { batch.bind("childId", it.first).bind("date", it.second).add() }
    return batch.executeAndReturn().toList<AttendanceReservationId>()
}

data class ReservationInsert(val childId: ChildId, val date: LocalDate, val range: TimeRange?)

fun Database.Transaction.insertValidReservations(
    userId: EvakaUserId,
    reservations: List<ReservationInsert>
): List<AttendanceReservationId> {
    return reservations.mapNotNull {
        @Suppress("DEPRECATION")
        createQuery(
                """
        INSERT INTO attendance_reservation (child_id, date, start_time, end_time, created_by)
        SELECT :childId, :date, :start, :end, :userId
        FROM realized_placement_all(:date) rp
        JOIN daycare d ON d.id = rp.unit_id AND 'RESERVATIONS' = ANY(d.enabled_pilot_features)
        LEFT JOIN service_need sn ON sn.placement_id = rp.placement_id AND daterange(sn.start_date, sn.end_date, '[]') @> :date
        WHERE 
            rp.child_id = :childId AND
            (sn.shift_care = 'INTERMITTENT' OR (
                extract(isodow FROM :date) = ANY(d.operation_days) AND
                (d.round_the_clock OR NOT EXISTS(SELECT 1 FROM holiday h WHERE h.date = :date))
            )) AND
            NOT EXISTS(SELECT 1 FROM absence ab WHERE ab.child_id = :childId AND ab.date = :date)
        ON CONFLICT DO NOTHING
        RETURNING id
        """
            )
            .bind("userId", userId)
            .bind("childId", it.childId)
            .bind("date", it.date)
            .run {
                if (it.range == null) {
                    bind<LocalTime?>("start", null).bind<LocalTime?>("end", null)
                } else {
                    bind("start", it.range.start).bind("end", it.range.end)
                }
            }
            .exactlyOneOrNull<AttendanceReservationId>()
    }
}

fun Database.Read.getReservations(where: Predicate): List<ReservationRow> =
    createQuery {
            sql(
                """
SELECT
  ar.date,
  ar.child_id,
  ar.start_time,
  ar.end_time,
  eu.type <> 'CITIZEN' as staff_created
FROM attendance_reservation ar
JOIN evaka_user eu ON ar.created_by = eu.id
WHERE ${predicate(where.forTable("ar"))}
"""
            )
        }
        .toList {
            ReservationRow(
                column("date"),
                column("child_id"),
                Reservation.of(column("start_time"), column("end_time")),
                column("staff_created")
            )
        }

fun Database.Read.getUnitReservations(
    unitId: DaycareId,
    date: LocalDate
): Map<ChildId, List<ReservationResponse>> =
    getReservations(
            Predicate {
                where(
                    """
$it.date = ${bind(date)} AND
$it.child_id IN (
    SELECT child_id FROM placement WHERE unit_id = ${bind(unitId)} AND ${bind(date)} BETWEEN start_date AND end_date
    UNION
    SELECT child_id FROM backup_care WHERE unit_id = ${bind(unitId)} AND ${bind(date)} BETWEEN start_date AND end_date
)
"""
                )
            }
        )
        .groupBy { it.childId }
        .mapValues { (_, value) -> value.map { ReservationResponse.from(it) } }

fun Database.Read.getChildAttendanceReservationStartDatesByRange(
    childId: ChildId,
    range: DateRange
): List<LocalDate> {
    @Suppress("DEPRECATION")
    return createQuery(
            """
        SELECT date
        FROM attendance_reservation
        WHERE between_start_and_end(:range, date)
        AND child_id = :childId
        AND (start_time IS NULL OR start_time != '00:00'::time)  -- filter out overnight reservations
        """
        )
        .bind("range", range)
        .bind("childId", childId)
        .toList<LocalDate>()
}

data class ChildReservationDateRow(val childId: ChildId, val date: LocalDate)

fun Database.Read.getReservationDatesForChildrenInRange(
    childIds: Set<ChildId>,
    range: FiniteDateRange
): Map<ChildId, Set<LocalDate>> {
    @Suppress("DEPRECATION")
    return createQuery(
            """
        SELECT child_id, date
        FROM attendance_reservation
        WHERE between_start_and_end(:range, date)
        AND child_id = ANY (:childIds)
        """
        )
        .bind("range", range)
        .bind("childIds", childIds)
        .toList<ChildReservationDateRow>()
        .groupBy({ it.childId }, { it.date })
        .mapValues { (_, value) -> value.toSet() }
}

fun Database.Read.getReservationsForChildInRange(
    childId: ChildId,
    range: FiniteDateRange
): Map<LocalDate, List<ReservationResponse>> =
    getReservations(
            Predicate {
                where(
                    """
between_start_and_end(${bind(range)}, ar.date)
AND ar.child_id = ${bind(childId)}
"""
                )
            }
        )
        .groupBy { it.date }
        .mapValues { (_, value) -> value.map { ReservationResponse.from(it) } }

fun Database.Read.getReservationsCitizen(
    today: LocalDate,
    guardianId: PersonId,
    range: FiniteDateRange
): List<ReservationRow> =
    getReservations(
        Predicate {
            where(
                """
between_start_and_end(${bind(range)}, $it.date) AND
$it.child_id = ANY (
    SELECT child_id FROM guardian WHERE guardian_id = ${bind(guardianId)}
    UNION
    SELECT child_id FROM foster_parent WHERE parent_id = ${bind(guardianId)} AND valid_during @> ${bind(today)}
)
"""
            )
        }
    )

data class ReservationChildRow(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val duplicateOf: PersonId?,
    val imageId: ChildImageId?,
)

fun Database.Read.getReservationChildren(
    guardianId: PersonId,
    today: LocalDate
): List<ReservationChildRow> {
    @Suppress("DEPRECATION")
    return createQuery(
            """
WITH children AS (
    SELECT child_id FROM guardian WHERE guardian_id = :guardianId
    UNION
    SELECT child_id FROM foster_parent WHERE parent_id = :guardianId AND valid_during @> :today
)
SELECT
    p.id,
    p.first_name,
    p.last_name,
    p.preferred_name,
    p.duplicate_of,
    ci.id AS image_id
FROM person p
LEFT JOIN child_images ci ON ci.child_id = p.id
WHERE p.id = ANY (SELECT child_id FROM children)
ORDER BY p.date_of_birth, p.duplicate_of
        """
        )
        .bind("guardianId", guardianId)
        .bind("today", today)
        .toList<ReservationChildRow>()
}

data class ReservationPlacement(
    val childId: ChildId,
    val range: FiniteDateRange,
    val type: PlacementType,
    val operationTimes: List<TimeRange?>,
    val dailyPreschoolTime: TimeRange?,
    val dailyPreparatoryTime: TimeRange?,
    val serviceNeeds: List<ReservationServiceNeed>
)

data class ReservationServiceNeed(
    val range: FiniteDateRange,
    val shiftCareType: ShiftCareType,
    val daycareHoursPerMonth: Int?
)

data class ReservationPlacementRow(
    val childId: ChildId,
    val placementId: PlacementId,
    val range: FiniteDateRange,
    val type: PlacementType,
    val operationTimes: List<TimeRange?>,
    val dailyPreschoolTime: TimeRange?,
    val dailyPreparatoryTime: TimeRange?,
    val shiftCareType: ShiftCareType?,
    val daycareHoursPerMonth: Int?,
    val serviceNeedRange: FiniteDateRange?,
)

fun Database.Read.getReservationPlacements(
    childIds: Set<ChildId>,
    range: DateRange
): Map<ChildId, List<ReservationPlacement>> =
    createQuery {
            sql(
                """
SELECT
    pl.child_id,
    pl.id AS placement_id,
    daterange(pl.start_date, pl.end_date, '[]') AS range,
    pl.type,
    u.operation_times,
    u.daily_preschool_time,
    u.daily_preparatory_time,
    sn.shift_care AS shift_care_type,
    sno.daycare_hours_per_month,
    CASE WHEN sn IS NOT NULL THEN daterange(sn.start_date, sn.end_date, '[]') END AS service_need_range
FROM placement pl
JOIN daycare u ON pl.unit_id = u.id
LEFT JOIN service_need sn ON sn.placement_id = pl.id
LEFT JOIN service_need_option sno ON sno.id = sn.option_id
WHERE
    pl.child_id = ANY (${bind(childIds)}) AND
    daterange(pl.start_date, pl.end_date, '[]') && ${bind(range)} AND
    'RESERVATIONS' = ANY(u.enabled_pilot_features)
"""
            )
        }
        .toList<ReservationPlacementRow>()
        .groupBy { it.placementId }
        .map { (_, rows) ->
            ReservationPlacement(
                childId = rows[0].childId,
                range = rows[0].range,
                type = rows[0].type,
                operationTimes = rows[0].operationTimes,
                dailyPreschoolTime = rows[0].dailyPreschoolTime,
                dailyPreparatoryTime = rows[0].dailyPreparatoryTime,
                serviceNeeds =
                    rows
                        .mapNotNull {
                            if (it.serviceNeedRange == null || it.shiftCareType == null) null
                            else
                                ReservationServiceNeed(
                                    range = it.serviceNeedRange,
                                    shiftCareType = it.shiftCareType,
                                    daycareHoursPerMonth = it.daycareHoursPerMonth
                                )
                        }
                        .sortedBy { it.range.start }
                        .toList()
            )
        }
        .groupBy { it.childId }
        .mapValues { (_, value) -> value.sortedBy { it.range.start } }

data class ReservationBackupPlacement(
    val childId: ChildId,
    val range: FiniteDateRange,
    val operationTimes: List<TimeRange>
)

fun Database.Read.getReservationBackupPlacements(
    childIds: Set<ChildId>,
    range: FiniteDateRange
): Map<ChildId, List<ReservationBackupPlacement>> {
    @Suppress("DEPRECATION")
    return createQuery(
            """
SELECT
    bc.child_id,
    daterange(bc.start_date, bc.end_date, '[]') * :range AS range,
    u.operation_times
FROM backup_care bc
JOIN daycare u ON bc.unit_id = u.id

WHERE
    bc.child_id = ANY (:childIds) AND
    daterange(bc.start_date, bc.end_date, '[]') && :range AND
    'RESERVATIONS' = ANY(u.enabled_pilot_features)
"""
        )
        .bind("childIds", childIds)
        .bind("range", range)
        .toList<ReservationBackupPlacement>()
        .groupBy { it.childId }
}

/** Automatic `PLANNED_ABSENCE` logic is enabled for contract days and hour based service needs */
fun Database.Read.getPlannedAbsenceEnabledRanges(
    childIds: Set<PersonId>,
    range: FiniteDateRange,
    enabledForHourBasedServiceNeeds: Boolean
): Map<ChildId, DateSet> {
    val enabledForHourBasedServiceNeedsPredicate =
        if (enabledForHourBasedServiceNeeds) {
            Predicate {
                where(
                    "COALESCE(sno.daycare_hours_per_month, sno_default.daycare_hours_per_month) IS NOT NULL"
                )
            }
        } else {
            Predicate.alwaysFalse()
        }

    return createQuery {
            sql(
                """
            SELECT
                pl.child_id,
                range_agg(daterange(sn.start_date, sn.end_date, '[]') * ${bind(range)}) AS enabled_ranges
            FROM placement pl
            JOIN service_need sn ON sn.placement_id = pl.id
            JOIN service_need_option sno ON sno.id = sn.option_id
            LEFT JOIN service_need_option sno_default ON sno_default.valid_placement_type = pl.type AND sno_default.default_option
            WHERE
                pl.child_id = ANY(${bind(childIds)}) AND
                daterange(pl.start_date, pl.end_date, '[]') && ${bind(range)} AND
                daterange(sn.start_date, sn.end_date, '[]') && ${bind(range)} AND (
                    COALESCE(sno.contract_days_per_month, sno_default.contract_days_per_month) IS NOT NULL OR
                    ${predicate(enabledForHourBasedServiceNeedsPredicate.forTable(""))}
                )
            GROUP BY child_id
            """
            )
        }
        .toMap { columnPair("child_id", "enabled_ranges") }
}

data class DailyChildReservationInfoRow(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val dateOfBirth: LocalDate,
    @Json val reservations: List<ConfirmedDayReservationInfo>,
    @Json val absences: List<ConfirmedDayAbsenceInfo>,
    val groupId: GroupId?,
    val absenceType: AbsenceType?,
    val backupUnitId: DaycareId?,
    val placementType: PlacementType
)

data class ConfirmedDayAbsenceInfo(val category: AbsenceCategory)

data class ConfirmedDayReservationInfo(
    val start: LocalTime?,
    val end: LocalTime?,
    val staffCreated: Boolean
)

fun Database.Read.getChildReservationsOfUnitForDay(
    day: LocalDate,
    unitId: DaycareId
): List<DailyChildReservationInfoRow> {
    @Suppress("DEPRECATION")
    return createQuery(
            """
SELECT pcd.child_id,
       p.date_of_birth,
       p.first_name,
       p.last_name,
       p.preferred_name,
       CASE -- affected group in the examination unit
           WHEN (pcd.unit_id <> :unitId) THEN pcd.placement_group_id
           ELSE pcd.group_id END                                       AS group_id,
       CASE
           WHEN (pcd.unit_id <> pcd.placement_unit_id AND pcd.placement_unit_id = :unitId)
           THEN unit_id END                                            AS backup_unit_id,
        pcd.placement_type,
        -- reservation roll up
        (SELECT coalesce(jsonb_agg(json_build_object(
               'start', s.start_time,
               'end', s.end_time,
               'staffCreated', s.staff_created)), '[]'::jsonb)
        FROM (select ar.start_time, ar.end_time, eu.type = 'EMPLOYEE' as staff_created
                FROM attendance_reservation ar
                JOIN evaka_user eu ON ar.created_by = eu.id
              WHERE ar.child_id = pcd.child_id
                AND ar.date = :examinationDate) s)
           AS reservations,
       -- absence roll up
       (SELECT coalesce(jsonb_agg(json_build_object(
               'category', s.category)), '[]'::jsonb)
        FROM (SELECT ab.category
              FROM absence ab
              JOIN evaka_user eu ON ab.modified_by = eu.id
              WHERE ab.child_id = pcd.child_id
                AND ab.date = :examinationDate) s)
           AS absences
FROM realized_placement_one(:examinationDate) pcd
         JOIN person p ON pcd.child_id = p.id
  -- show placed children and children of both backup directions
WHERE (pcd.unit_id = :unitId OR pcd.placement_unit_id = :unitId)
  -- only show groupless children if they are on back up care in another unit
  AND (pcd.group_id IS NOT NULL OR pcd.unit_id <> :unitId)
            """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("examinationDate", day)
        .toList<DailyChildReservationInfoRow>()
}

data class GroupReservationStatisticsRow(
    val date: LocalDate,
    val groupId: GroupId,
    val calculatedPresent: BigDecimal,
    val absent: Int,
    val present: Int
)

fun Database.Read.getReservationStatisticsForUnit(
    confirmedDays: List<LocalDate>,
    unitId: DaycareId
): Map<LocalDate, List<GroupReservationStatisticsRow>> {
    @Suppress("DEPRECATION")
    return createQuery(
            """
select date,
       count(1) FILTER ( WHERE NOT a.child_in_unit ) AS absent,
       count(1) FILTER ( WHERE a.child_in_unit )     AS present,
       coalesce(sum(a.occupancy_coefficient * a.capacity_factor)
                FILTER ( WHERE a.child_in_unit ), 0) AS calculated_present,
       affected_group_id                             AS group_id
from (SELECT d                                     AS date,
             CASE -- affected group in the examination unit
                 WHEN (rp.placement_unit_id <> rp.unit_id AND rp.placement_unit_id = :unitId) THEN rp.placement_group_id
                 ELSE rp.group_id END              AS affected_group_id,
             CASE -- whether child in examination unit at given date
                 WHEN ( -- absence or backup care
                             absence.categories @> absence_categories(rp.placement_type) OR
                             (rp.placement_unit_id <> rp.unit_id AND rp.placement_unit_id = :unitId)) THEN false
                 WHEN (ct.id IS NOT NULL OR pt.id IS NOT NULL) -- term break
                     THEN false
                 ELSE true
                 END                               AS child_in_unit,
             CASE -- service need occupancy coefficient of child
                 WHEN u.type && array ['FAMILY', 'GROUP_FAMILY']::care_types[]
                     THEN $familyUnitPlacementCoefficient
                 ELSE CASE
                          WHEN (extract('year' from age(d, p.date_of_birth)) < 3)
                              THEN coalesce(
                                  sno.realized_occupancy_coefficient_under_3y,
                                  sno_default.realized_occupancy_coefficient_under_3y)
                          ELSE coalesce(
                                  sno.realized_occupancy_coefficient,
                                  sno_default.realized_occupancy_coefficient)
                     END
                 END                               AS occupancy_coefficient,
             coalesce(af.capacity_factor, 1.00)    AS capacity_factor
      FROM unnest(:confirmedDays) d
               LEFT JOIN realized_placement_one(d) rp
                         ON TRUE
               JOIN LATERAL ( select coalesce(array_agg(ab.category), '{}'::absence_category[]) as categories
                              from absence ab
                              where ab.child_id = rp.child_id
                                and ab.date = d) absence on true
               JOIN daycare u
                    ON u.id = :unitId
               JOIN person p
                    ON rp.child_id = p.id
               LEFT JOIN service_need sn
                         ON rp.placement_id = sn.placement_id AND d BETWEEN sn.start_date AND sn.end_date
               LEFT JOIN service_need_option sno
                         ON sn.option_id = sno.id
               LEFT JOIN service_need_option sno_default
                         ON sno_default.default_option IS TRUE
                             AND sno_default.valid_placement_type = rp.placement_type
               LEFT JOIN assistance_factor af
                         ON rp.child_id = af.child_id
                             AND af.valid_during @> d
               LEFT JOIN club_term ct ON rp.placement_type IN ('CLUB') AND ct.term_breaks @> d
               LEFT JOIN preschool_term pt ON rp.placement_type IN ('PRESCHOOL', 'PREPARATORY') AND pt.term_breaks @> d
      WHERE (rp.unit_id = :unitId OR rp.placement_unit_id = :unitId)) a
WHERE a.affected_group_id IS NOT NULL
GROUP BY a.date, a.affected_group_id
            """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("confirmedDays", confirmedDays)
        .toList<GroupReservationStatisticsRow>()
        .groupBy { it.date }
}
