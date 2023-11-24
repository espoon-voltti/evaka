// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.reservations

import fi.espoo.evaka.dailyservicetimes.DailyServiceTimesType
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.occupancy.familyUnitPlacementCoefficient
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.AbsenceId
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
import fi.espoo.evaka.shared.domain.BadRequest
import fi.espoo.evaka.shared.domain.DateRange
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.TimeRange
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import org.jdbi.v3.json.Json

data class AbsenceInsert(
    val childId: ChildId,
    val date: LocalDate,
    val absenceType: AbsenceType,
    val questionnaireId: HolidayQuestionnaireId? = null
)

fun Database.Transaction.insertAbsences(
    userId: EvakaUserId,
    absenceInserts: List<AbsenceInsert>
): List<AbsenceId> {
    val batch =
        prepareBatch(
            """
        INSERT INTO absence (child_id, date, category, absence_type, modified_by, questionnaire_id)
        SELECT
            :childId,
            :date,
            category,
            :absenceType,
            :userId,
            :questionnaireId
        FROM (
            SELECT unnest(absence_categories(type)) AS category
            FROM placement
            WHERE child_id = :childId AND :date BETWEEN start_date AND end_date
        ) care_type
        ON CONFLICT DO NOTHING
        RETURNING id
        """
                .trimIndent()
        )

    absenceInserts.forEach { (childId, date, absenceType, questionnaireId) ->
        batch
            .bind("childId", childId)
            .bind("date", date)
            .bind("absenceType", absenceType)
            .bind("userId", userId)
            .bind("questionnaireId", questionnaireId)
            .add()
    }

    return batch.executeAndReturn().toList<AbsenceId>()
}

fun Database.Transaction.deleteAbsencesCreatedFromQuestionnaire(
    questionnaireId: HolidayQuestionnaireId,
    childIds: Set<ChildId>
) {
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

private data class ReservationRow(
    val childId: ChildId,
    val startTime: LocalTime?,
    val endTime: LocalTime?
)

fun Database.Read.getUnitReservations(
    unitId: DaycareId,
    date: LocalDate
): Map<ChildId, List<Reservation>> =
    createQuery(
            """
    WITH placed_children AS (
        SELECT child_id FROM placement WHERE unit_id = :unitId AND :date BETWEEN start_date AND end_date
        UNION
        SELECT child_id FROM backup_care WHERE unit_id = :unitId AND :date BETWEEN start_date AND end_date
    )
    SELECT
        pc.child_id,
        ar.start_time,
        ar.end_time
    FROM placed_children pc
    JOIN attendance_reservation ar ON ar.child_id = pc.child_id 
    WHERE ar.date = :date
    """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("date", date)
        .mapTo<ReservationRow>()
        .useIterable { rows ->
            rows
                .map { it.childId to Reservation.fromLocalTimes(it.startTime, it.endTime) }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, value) -> value.sorted() }
        }

fun Database.Read.getChildAttendanceReservationStartDatesByRange(
    childId: ChildId,
    range: DateRange
): List<LocalDate> {
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
): Map<LocalDate, List<Reservation>> {
    data class ReservationWithDate(val date: LocalDate, val reservation: Reservation)
    return createQuery(
            """
        SELECT date, start_time, end_time
        FROM attendance_reservation
        WHERE between_start_and_end(:range, date)
        AND child_id = :childId
        """
        )
        .bind("range", range)
        .bind("childId", childId)
        .toList {
            ReservationWithDate(
                column("date"),
                Reservation.fromLocalTimes(column("start_time"), column("end_time"))
            )
        }
        .groupBy({ it.date }, { it.reservation })
}

data class DailyReservationData(val date: LocalDate, @Json val children: List<ChildDailyData>)

@Json
data class ChildDailyData(
    val childId: ChildId,
    val absence: AbsenceType?,
    val absenceEditable: Boolean,
    val reservations: List<Reservation>,
    val attendances: List<OpenTimeRange>
)

fun Database.Read.getReservationsCitizen(
    today: LocalDate,
    userId: PersonId,
    range: FiniteDateRange,
): List<DailyReservationData> {
    if (range.durationInDays() > 450) throw BadRequest("Range too long")

    return createQuery(
            """
WITH children AS (
    SELECT child_id FROM guardian WHERE guardian_id = :userId
    UNION
    SELECT child_id FROM foster_parent WHERE parent_id = :userId AND valid_during @> :today
)
SELECT
    t::date AS date,
    coalesce(
        jsonb_agg(
            jsonb_build_object(
                'childId', c.child_id,
                'absence', a.absence_type,
                'absenceEditable', a.absence_editable,
                'reservations', coalesce(ar.reservations, '[]'),
                'attendances', coalesce(ca.attendances, '[]')
            )
        ) FILTER (
            WHERE (a.absence_type IS NOT NULL OR ar.reservations IS NOT NULL OR ca.attendances IS NOT NULL)
              AND EXISTS(
                SELECT 1 FROM placement p
                JOIN daycare d ON p.unit_id = d.id AND 'RESERVATIONS' = ANY(d.enabled_pilot_features)
                WHERE c.child_id = p.child_id AND p.start_date <= t::date AND p.end_date >= t::date
              )
        ),
        '[]'
    ) AS children
FROM generate_series(:start, :end, '1 day') t, children c
LEFT JOIN LATERAL (
    SELECT
        jsonb_agg(
            CASE WHEN ar.start_time IS NULL OR ar.end_time IS NULL THEN
                jsonb_build_object('type', 'NO_TIMES')
            ELSE
                jsonb_build_object('type', 'TIMES', 'startTime', ar.start_time, 'endTime', ar.end_time)
            END
            ORDER BY ar.start_time
        ) AS reservations
    FROM attendance_reservation ar WHERE ar.child_id = c.child_id AND ar.date = t::date
) ar ON true
LEFT JOIN LATERAL (
    SELECT
        jsonb_agg(
            jsonb_build_object(
                'startTime', to_char(ca.start_time, 'HH24:MI'),
                'endTime', to_char(ca.end_time, 'HH24:MI')
            ) ORDER BY ca.start_time ASC
        ) AS attendances
    FROM child_attendance ca WHERE ca.child_id = c.child_id AND ca.date = t::date
) ca ON true
LEFT JOIN LATERAL (
    SELECT
        a.absence_type,
        (a.absence_type <> 'FREE_ABSENCE' AND eu.type = 'CITIZEN') AS absence_editable
    FROM absence a
    JOIN evaka_user eu ON eu.id = a.modified_by
    WHERE
        a.child_id = c.child_id AND
        a.date = t::date
    ORDER BY a.category DESC
    LIMIT 1
) a ON true
GROUP BY date
        """
                .trimIndent()
        )
        .bind("today", today)
        .bind("userId", userId)
        .bind("start", range.start)
        .bind("end", range.end)
        .toList<DailyReservationData>()
}

data class ReservationChild(
    val id: ChildId,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val duplicateOf: PersonId?,
    val imageId: ChildImageId?,
    val upcomingPlacementType: PlacementType?,
)

fun Database.Read.getReservationChildren(
    guardianId: PersonId,
    today: LocalDate
): List<ReservationChild> {
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
    ci.id AS image_id,
    (
        SELECT type FROM placement
        WHERE child_id = p.id AND :today <= end_date
        ORDER BY start_date
        LIMIT 1
    ) AS upcoming_placement_type
FROM person p
LEFT JOIN child_images ci ON ci.child_id = p.id
WHERE p.id = ANY (SELECT child_id FROM children)
ORDER BY p.date_of_birth, p.duplicate_of
        """
        )
        .bind("guardianId", guardianId)
        .bind("today", today)
        .toList<ReservationChild>()
}

data class ReservationPlacement(
    val childId: ChildId,
    val range: FiniteDateRange,
    val type: PlacementType,
    val operationTimes: List<TimeRange?>,
    val serviceNeeds: List<ReservationServiceNeed>
)

data class ReservationServiceNeed(val range: FiniteDateRange, val shiftCareType: ShiftCareType)

data class ReservationPlacementRow(
    val childId: ChildId,
    val placementId: PlacementId,
    val range: FiniteDateRange,
    val type: PlacementType,
    val operationTimes: List<TimeRange?>,
    val serviceNeedRange: FiniteDateRange?,
    val shiftCareType: ShiftCareType?,
)

fun Database.Read.getReservationPlacements(
    childIds: Set<ChildId>,
    range: FiniteDateRange
): Map<ChildId, List<ReservationPlacement>> {
    val sql =
        """
SELECT
    pl.child_id,
    pl.id as placement_id,
    daterange(pl.start_date, pl.end_date, '[]') * :range AS range,
    pl.type,
    u.operation_times,
    sn.shift_care as shift_care_type,
    daterange(sn.start_date, sn.end_date, '[]') * :range AS service_need_range
FROM placement pl
JOIN daycare u ON pl.unit_id = u.id
LEFT JOIN service_need sn ON sn.placement_id = pl.id AND daterange(sn.start_date, sn.end_date, '[]') && :range
WHERE
    pl.child_id = ANY (:childIds) AND
    daterange(pl.start_date, pl.end_date, '[]') && :range AND
    'RESERVATIONS' = ANY(u.enabled_pilot_features)
"""

    return createQuery(sql)
        .bind("childIds", childIds)
        .bind("range", range)
        .toList<ReservationPlacementRow>()
        .groupBy { it.placementId }
        .map { (_, rows) ->
            ReservationPlacement(
                childId = rows[0].childId,
                range = rows[0].range,
                type = rows[0].type,
                operationTimes = rows[0].operationTimes,
                serviceNeeds =
                    rows
                        .mapNotNull {
                            if (it.serviceNeedRange == null || it.shiftCareType == null) null
                            else
                                ReservationServiceNeed(
                                    range = it.serviceNeedRange,
                                    shiftCareType = it.shiftCareType
                                )
                        }
                        .toList()
            )
        }
        .groupBy { it.childId }
}

data class ReservationBackupPlacement(
    val childId: ChildId,
    val range: FiniteDateRange,
    val operationTimes: List<TimeRange>
)

fun Database.Read.getReservationBackupPlacements(
    childIds: Set<ChildId>,
    range: FiniteDateRange
): Map<ChildId, List<ReservationBackupPlacement>> {
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

fun Database.Read.getReservationContractDayRanges(
    childIds: Set<PersonId>,
    range: FiniteDateRange
): Map<ChildId, DateSet> {
    return createQuery(
            """
            SELECT
                pl.child_id,
                range_agg(daterange(sn.start_date, sn.end_date, '[]') * :range) AS contract_days
            FROM placement pl
            JOIN service_need sn ON sn.placement_id = pl.id
            JOIN service_need_option sno ON sno.id = sn.option_id
            LEFT JOIN service_need_option sno_default ON sno_default.valid_placement_type = pl.type AND sno_default.default_option
            WHERE
                pl.child_id = ANY(:childIds) AND
                daterange(pl.start_date, pl.end_date, '[]') && :range AND
                daterange(sn.start_date, sn.end_date, '[]') && :range AND
                COALESCE(sno.contract_days_per_month, sno_default.contract_days_per_month) IS NOT NULL
            GROUP BY child_id
            """
        )
        .bind("childIds", childIds)
        .bind("range", range)
        .toMap { columnPair("child_id", "contract_days") }
}

data class DailyChildReservationInfoRow(
    val childId: ChildId,
    val firstName: String,
    val lastName: String,
    val preferredName: String,
    val dateOfBirth: LocalDate,
    val reservationStartTime: LocalTime?,
    val reservationEndTime: LocalTime?,
    val groupId: GroupId?,
    val absenceType: AbsenceType?,
    val backupUnitId: DaycareId?,
    val dailyServiceTimeType: DailyServiceTimesType?,
    val validityPeriod: DateRange?,
    val regularTimes: TimeRange?,
    val mondayTimes: TimeRange?,
    val tuesdayTimes: TimeRange?,
    val wednesdayTimes: TimeRange?,
    val thursdayTimes: TimeRange?,
    val fridayTimes: TimeRange?,
    val saturdayTimes: TimeRange?,
    val sundayTimes: TimeRange?
)

fun Database.Read.getChildReservationsOfUnitForDay(
    day: LocalDate,
    unitId: DaycareId
): List<DailyChildReservationInfoRow> {
    return createQuery(
            """
with placement_days as
         (SELECT pl.child_id,
                 pl.start_date        as placement_start,
                 pl.end_date          as placement_end,
                 gpl.start_date       as grouping_start,
                 gpl.end_date         as grouping_end,
                 gpl.daycare_group_id as group_id
          FROM placement pl
                   JOIN daycare_group_placement gpl
                             ON pl.id = gpl.daycare_placement_id

          WHERE unit_id = :unitId
            AND daterange(pl.start_date, pl.end_date, '[]') @> :examinationDate
            AND daterange(gpl.start_date, gpl.end_date, '[]') @> :examinationDate
            AND NOT EXISTS (select
                            from backup_care bc
                            where bc.child_id = pl.child_id
                              AND bc.unit_id = pl.unit_id
                              AND daterange(bc.start_date, bc.end_date, '[]') @> :examinationDate)
          UNION
          SELECT bc.child_id,
                 bc.start_date as placement_start,
                 bc.end_date   as placement_end,
                 bc.start_date as grouping_start,
                 bc.end_date   as grouping_end,
                 bc.group_id   as group_id
          FROM backup_care bc
          WHERE unit_id = :unitId
            AND daterange(bc.start_date, bc.end_date, '[]') @> :examinationDate)
SELECT pcd.child_id,
       p.date_of_birth,
       p.first_name,
       p.last_name,
       p.preferred_name,
       pcd.group_id,
       ar.start_time   as reservation_start_time,
       ar.end_time     as reservation_end_time,
       ab.absence_type as absence_type,
       dst.type        as daily_service_time_type,
       dst.validity_period,
       dst.regular_times,
       dst.monday_times,
       dst.tuesday_times,
       dst.wednesday_times,
       dst.thursday_times,
       dst.friday_times,
       dst.saturday_times,
       dst.sunday_times,
       bc.unit_id      as backup_unit_id
FROM placement_days pcd
         JOIN person p ON pcd.child_id = p.id
         JOIN daycare u ON u.id = :unitId
         LEFT JOIN absence ab
                   ON ab.child_id = pcd.child_id
                       AND ab.date = :examinationDate
         LEFT JOIN backup_care bc
                   ON bc.child_id = pcd.child_id
                       AND bc.unit_id <> :unitId
                       AND
                      daterange(bc.start_date, bc.end_date, '[]') @>
                      :examinationDate
         LEFT JOIN attendance_reservation ar
                   ON ar.child_id = pcd.child_id
                       AND ar.date = :examinationDate
         LEFT JOIN daily_service_time dst
                   ON pcd.child_id = dst.child_id
                       AND dst.validity_period @> :examinationDate
            """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("examinationDate", day)
        .toList<DailyChildReservationInfoRow>()
}

data class GroupReservationStatisticsRow(
    val date: LocalDate,
    val groupId: GroupId?,
    val calculatedPresent: BigDecimal,
    val absent: Int,
    val present: Int
)

fun Database.Read.getReservationStatisticsForUnit(
    confirmedDays: List<LocalDate>,
    unitId: DaycareId
): Map<LocalDate, List<GroupReservationStatisticsRow>> {
    return createQuery(
            """
select count(1) filter (where a.absence_status = 'ABSENT')     as absent,
       count(1) filter (where a.absence_status = 'PRESENT') as present,
       coalesce(sum(occupancy_coefficient) filter ( where a.absence_status = 'PRESENT' ), 0)              as calculated_present,
       date,
       group_id
from (with placement_days as
               (SELECT day.date,
                       pl.child_id,
                       pl.start_date        as placement_start,
                       pl.end_date          as placement_end,
                       gpl.start_date       as grouping_start,
                       gpl.end_date         as grouping_end,
                       gpl.daycare_group_id as group_id
                FROM (SELECT t.date
                      FROM unnest(:confirmedDays) AS t(date)) day
                         LEFT JOIN placement pl
                                   ON daterange(pl.start_date, pl.end_date, '[]') @> day.date AND pl.unit_id = :unitId
                         JOIN daycare_group_placement gpl
                                   ON pl.id = gpl.daycare_placement_id AND
                                      daterange(gpl.start_date, gpl.end_date, '[]') @> day.date
                WHERE NOT EXISTS (select
                                  from backup_care bc
                                  where bc.child_id = pl.child_id
                                    AND bc.unit_id = pl.unit_id
                                    AND daterange(bc.start_date, bc.end_date, '[]') @> day.date)
                UNION
                SELECT day.date,
                       bc.child_id,
                       bc.start_date as placement_start,
                       bc.end_date   as placement_end,
                       bc.start_date as grouping_start,
                       bc.end_date   as grouping_end,
                       bc.group_id   as group_id
                FROM (SELECT t.date
                      FROM unnest(:confirmedDays) AS t(date)) day
                         LEFT JOIN backup_care bc ON daterange(bc.start_date, bc.end_date, '[]') @> day.date
                    AND bc.unit_id = :unitId)
      SELECT pcd.date,
             pcd.group_id,
             CASE
                 WHEN p.id IS NULL
                     THEN null
                 ELSE CASE
                          WHEN (ab.absence_type IS NOT NULL OR bc.unit_id IS NOT NULL)
                              THEN 'ABSENT'
                          ELSE 'PRESENT'
                          END
                 END as absence_status,
             CASE
                 WHEN u.type && array ['FAMILY', 'GROUP_FAMILY']::care_types[]
                     THEN $familyUnitPlacementCoefficient
                 ELSE CASE
                          WHEN (extract('year' from age(pcd.date, p.date_of_birth)) < 3)
                              THEN coalesce(
                                  sno.realized_occupancy_coefficient_under_3y,
                                  sno_default.realized_occupancy_coefficient_under_3y)
                          ELSE coalesce(
                                  sno.realized_occupancy_coefficient,
                                  sno_default.realized_occupancy_coefficient)
                     END
                 END *
             coalesce(af.capacity_factor, 1.00)  as occupancy_coefficient
      FROM placement_days pcd
               JOIN daycare u ON u.id = :unitId
               LEFT JOIN absence ab
                         ON ab.child_id = pcd.child_id
                             AND ab.date = pcd.date
               LEFT JOIN backup_care bc
                         ON bc.child_id = pcd.child_id
                             AND bc.unit_id <> :unitId
                             AND
                            daterange(bc.start_date, bc.end_date, '[]') @>
                            pcd.date
               LEFT JOIN placement pl
                         ON pcd.child_id = pl.child_id
                             AND
                            daterange(pl.start_date, pl.end_date, '[]') @>
                            pcd.date
               LEFT JOIN service_need sn
                         ON pl.id = sn.placement_id
               LEFT JOIN service_need_option sno
                         ON sn.option_id = sno.id
               LEFT JOIN service_need_option sno_default
                         ON sno_default.default_option IS TRUE
                             AND
                            sno_default.valid_placement_type =
                            pl.type
               LEFT JOIN person p
                         ON pcd.child_id = p.id
               LEFT JOIN assistance_factor af
                         ON pcd.child_id = af.child_id
                             AND
                            af.valid_during @> pcd.date) a
group by a.date, a.group_id
            """
                .trimIndent()
        )
        .bind("unitId", unitId)
        .bind("confirmedDays", confirmedDays)
        .toList<GroupReservationStatisticsRow>()
        .groupBy { it.date }
}
