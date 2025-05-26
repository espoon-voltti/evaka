package fi.espoo.evaka.reports

import fi.espoo.evaka.absence.getAbsences
import fi.espoo.evaka.document.childdocument.ChildBasics
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.reservations.getReservations
import fi.espoo.evaka.serviceneed.ShiftCareType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.Predicate
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import kotlin.collections.groupBy
import org.jdbi.v3.core.mapper.Nested

data class HolidayReportDay(val date: LocalDate, val isHoliday: Boolean)

data class ChildWithName(val id: PersonId, val firstName: String, val lastName: String)

data class HolidayReportRow(
    val date: LocalDate,
    val presentChildren: List<ChildWithName>,
    val assistanceChildren: List<ChildWithName>,
    val presentOccupancyCoefficient: Double,
    val requiredStaff: Int,
    val absentCount: Int,
    val noResponseChildren: List<ChildWithName>,
)

data class ChildServiceNeedOccupancyInfo(
    @Nested("child_") val child: ChildBasics,
    val placementType: PlacementType,
    val coefficientUnder3y: Double,
    val coefficient: Double,
    val validity: FiniteDateRange,
    val shiftCareType: ShiftCareType,
    val groupId: GroupId?,
)

fun Database.Read.getAbsencesForChildrenOverRange(childIds: Set<PersonId>, range: FiniteDateRange) =
    getAbsences(
            Predicate {
                where(
                    "between_start_and_end(${bind(range)}, $it.date) AND $it.child_id = ANY (${bind(childIds)})"
                )
            }
        )
        .groupBy { r -> r.date }

fun Database.Read.getReservationsForChildrenOverRange(
    childIds: Set<PersonId>,
    range: FiniteDateRange,
) =
    getReservations(
            Predicate {
                where(
                    "between_start_and_end(${bind(range)}, $it.date) AND $it.child_id = ANY (${bind(childIds)})"
                )
            }
        )
        .groupBy { Pair(it.date, it.childId) }

fun Database.Read.getServiceNeedOccupancyInfoOverRangeForChildren(
    childIds: Set<PersonId>,
    range: FiniteDateRange,
) =
    if (childIds.isEmpty()) emptyList()
    else
        getServiceNeedOccupancyInfoOverRange(
            range,
            placementPred = Predicate { where("$it.child_id = ANY (${bind(childIds)})") },
            groupsPred = Predicate.alwaysTrue(),
        )

fun Database.Read.getServiceNeedOccupancyInfoOverRangeForGroups(
    groupIds: Set<GroupId>,
    unitId: DaycareId,
    range: FiniteDateRange,
): List<ChildServiceNeedOccupancyInfo> =
    if (groupIds.isEmpty())
        getServiceNeedOccupancyInfoOverRange(
            range,
            placementPred = Predicate { where("$it.unit_id = ${bind(unitId)}") },
            groupsPred = Predicate.alwaysTrue(),
        )
    else
        getServiceNeedOccupancyInfoOverRange(
            range,
            placementPred = Predicate.alwaysTrue(),
            groupsPred = Predicate { where("$it.daycare_group_id = ANY(${bind(groupIds)})") },
        )

fun Database.Read.getServiceNeedOccupancyInfoOverRange(
    period: FiniteDateRange,
    placementPred: Predicate,
    groupsPred: Predicate,
): List<ChildServiceNeedOccupancyInfo> =
    createQuery {
            sql(
                """
SELECT p.id                                                          AS child_id,
       p.first_name                                                  AS child_first_name,
       p.last_name                                                   AS child_last_name,
       p.date_of_birth                                               AS child_date_of_birth,
       pl.type                                                       AS placement_type,
       coalesce(sno.realized_occupancy_coefficient_under_3y,
                default_sno.realized_occupancy_coefficient_under_3y) AS coefficient_under_3y,
       coalesce(sno.realized_occupancy_coefficient,
                default_sno.realized_occupancy_coefficient)          AS coefficient,
       CASE
           WHEN (sn.start_date IS NOT NULL)
               THEN
               CASE
                   WHEN (dgp.start_date IS NOT NULL)
                       THEN daterange(sn.start_date, sn.end_date, '[]') *
                            daterange(dgp.start_date, dgp.end_date, '[]')
                   ELSE daterange(sn.start_date, sn.end_date, '[]')
                   END
           ELSE
               CASE
                   WHEN (dgp.start_date IS NOT NULL)
                       THEN daterange(pl.start_date, pl.end_date, '[]') *
                            daterange(dgp.start_date, dgp.end_date, '[]')
                   ELSE daterange(pl.start_date, pl.end_date, '[]')
                   END
           END                                                       AS validity,
       coalesce(sn.shift_care, 'NONE')                               AS shift_care_type,
       dgp.daycare_group_id                                          AS group_id
FROM placement pl
         JOIN person p ON pl.child_id = p.id
         LEFT JOIN service_need sn
                   ON sn.placement_id = pl.id
                       AND daterange(sn.start_date, sn.end_date, '[]') && ${bind(period)}
         LEFT JOIN service_need_option sno ON sn.option_id = sno.id
         LEFT JOIN service_need_option default_sno
                   ON pl.type = default_sno.valid_placement_type
                       AND default_sno.default_option
         LEFT JOIN daycare_group_placement dgp
                   ON pl.id = dgp.daycare_placement_id
                       AND daterange(dgp.start_date, dgp.end_date, '[]') && ${bind(period)}
                       AND (sn.start_date IS NULL OR daterange(dgp.start_date, dgp.end_date, '[]') && daterange(sn.start_date, sn.end_date, '[]'))
WHERE ${predicate(placementPred.forTable("pl"))}
  AND ${predicate(groupsPred.forTable("dgp"))}
  AND daterange(pl.start_date, pl.end_date, '[]') && ${bind(period)}
        """
            )
        }
        .toList<ChildServiceNeedOccupancyInfo>()

data class AssistanceRange(val childId: PersonId, val validDuring: FiniteDateRange)

data class BackupPlacementRange(
    val childId: PersonId,
    val validDuring: FiniteDateRange,
    val groupId: GroupId?,
)

fun Database.Read.getAssistanceRanges(
    childIds: Set<PersonId>,
    period: FiniteDateRange,
): List<AssistanceRange> =
    if (childIds.isEmpty()) emptyList()
    else
        createQuery {
                sql(
                    """
SELECT d.child_id, d.valid_during * ${bind(period)} as valid_during
FROM daycare_assistance d
WHERE child_id = ANY(${bind(childIds)}) AND d.valid_during && ${bind(period)}

UNION ALL

SELECT p.child_id, p.valid_during * ${bind(period)} as valid_during
FROM preschool_assistance p
WHERE child_id = ANY(${bind(childIds)}) AND p.valid_during && ${bind(period)}
"""
                )
            }
            .toList<AssistanceRange>()

fun Database.Read.getIncomingBackupCaresOverPeriodForGroupsInUnit(
    groupIds: Set<GroupId>,
    unitId: DaycareId,
    period: FiniteDateRange,
): List<BackupPlacementRange> {
    val where =
        if (groupIds.isEmpty()) Predicate.alwaysTrue()
        else Predicate { where("$it.group_id = ANY(${bind(groupIds)})") }

    return createQuery {
            sql(
                """
SELECT bc.child_id,
       daterange(bc.start_date, bc.end_date, '[]') * ${bind(period)} as valid_during,
       bc.group_id
FROM backup_care bc
WHERE ${predicate(where.forTable("bc"))}
AND bc.unit_id = ${bind(unitId)}
AND daterange(bc.start_date, bc.end_date, '[]') && ${bind(period)}
"""
            )
        }
        .toList<BackupPlacementRange>()
}
