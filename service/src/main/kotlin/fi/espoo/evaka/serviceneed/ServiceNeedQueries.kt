// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.serviceneed

import fi.espoo.evaka.absence.ChildServiceNeedInfo
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.EvakaUserId
import fi.espoo.evaka.shared.GroupId
import fi.espoo.evaka.shared.PlacementId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.ServiceNeedOptionId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.shared.domain.NotFound
import java.time.LocalDate

fun Database.Read.getServiceNeedsByChild(childId: ChildId): List<ServiceNeed> {
    return createQuery {
            sql(
                """
SELECT 
    sn.id, sn.placement_id, sn.start_date, sn.end_date, sn.shift_care, sn.updated,
    sno.id as option_id, sno.name_fi as option_name_fi, sno.name_sv as option_name_sv, sno.name_en as option_name_en, sno.updated as option_updated,
    sn.confirmed_by as confirmed_user_id, u.name as confirmed_name, sn.confirmed_at
FROM service_need sn
JOIN service_need_option sno on sno.id = sn.option_id
JOIN placement pl ON pl.id = sn.placement_id
LEFT JOIN evaka_user u on u.id = sn.confirmed_by
WHERE pl.child_id = ${bind(childId)}
"""
            )
        }
        .toList<ServiceNeed>()
}

fun Database.Read.getServiceNeedsByUnit(
    unitId: DaycareId,
    startDate: LocalDate?,
    endDate: LocalDate?
): List<ServiceNeed> {
    return createQuery {
            sql(
                """
SELECT 
    sn.id, sn.placement_id, sn.start_date, sn.end_date, sn.shift_care, sn.updated,
    sno.id as option_id, sno.name_fi as option_name_fi, sno.name_sv as option_name_sv, sno.name_en as option_name_en, sno.updated AS option_updated,
    sn.confirmed_by as confirmed_user_id, u.name as confirmed_name, sn.confirmed_at
FROM service_need sn
JOIN service_need_option sno on sno.id = sn.option_id
JOIN placement pl ON pl.id = sn.placement_id
LEFT JOIN evaka_user u on u.id = sn.confirmed_by
WHERE pl.unit_id = ${bind(unitId)} AND daterange(${bind(startDate)}, ${bind(endDate)}, '[]') && daterange(sn.start_date, sn.end_date, '[]')
"""
            )
        }
        .toList<ServiceNeed>()
}

fun Database.Read.getServiceNeedSummary(childId: ChildId): List<ServiceNeedSummary> {
    return createQuery {
            sql(
                """
SELECT
    sn.start_date,
    sn.end_date,
    sno.id AS option_id,
    sno.name_fi AS option_name_fi,
    sno.name_sv AS option_name_sv,
    sno.name_en AS option_name_en,
    sno.valid_placement_type AS option_valid_placement_type,
    sno.contract_days_per_month,
    u.name AS unit_name
FROM service_need sn
JOIN service_need_option sno ON sno.id = sn.option_id
JOIN placement p ON p.id = sn.placement_id
JOIN daycare u ON u.id = p.unit_id
WHERE p.child_id = ${bind(childId)}
"""
            )
        }
        .toList<ServiceNeedSummary>()
}

fun Database.Read.getServiceNeed(id: ServiceNeedId): ServiceNeed {
    return createQuery {
            sql(
                """
SELECT 
    sn.id, sn.placement_id, sn.start_date, sn.end_date, sn.shift_care, sn.updated,
    sno.id as option_id, sno.name_fi as option_name_fi, sno.name_sv as option_name_sv, sno.name_en as option_name_en, sno.updated AS option_updated,
    sn.confirmed_by as confirmed_user_id, u.name as confirmed_name, sn.confirmed_at
FROM service_need sn
JOIN service_need_option sno on sn.option_id = sno.id
LEFT JOIN evaka_user u on u.id = sn.confirmed_by
WHERE sn.id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull<ServiceNeed>() ?: throw NotFound("Service need $id not found")
}

fun Database.Read.getServiceNeedChildRange(id: ServiceNeedId): ServiceNeedChildRange {
    return createQuery {
            sql(
                """
SELECT p.child_id, daterange(sn.start_date, sn.end_date, '[]')
FROM service_need sn
JOIN placement p on sn.placement_id = p.id
WHERE sn.id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull<ServiceNeedChildRange>() ?: throw NotFound("Service need $id not found")
}

fun Database.Transaction.insertServiceNeed(
    placementId: PlacementId,
    startDate: LocalDate,
    endDate: LocalDate,
    optionId: ServiceNeedOptionId,
    shiftCare: ShiftCareType,
    confirmedBy: EvakaUserId?,
    confirmedAt: HelsinkiDateTime?
): ServiceNeedId {
    return createQuery {
            sql(
                """
INSERT INTO service_need (placement_id, start_date, end_date, option_id, shift_care, confirmed_by, confirmed_at) 
VALUES (${bind(placementId)}, ${bind(startDate)}, ${bind(endDate)}, ${bind(optionId)}, ${bind(shiftCare)}, ${bind(confirmedBy)}, ${bind(confirmedAt)})
RETURNING id;
"""
            )
        }
        .exactlyOne<ServiceNeedId>()
}

fun Database.Transaction.updateServiceNeed(
    id: ServiceNeedId,
    startDate: LocalDate,
    endDate: LocalDate,
    optionId: ServiceNeedOptionId,
    shiftCare: ShiftCareType,
    confirmedBy: EvakaUserId?,
    confirmedAt: HelsinkiDateTime?
) {
    createUpdate {
            sql(
                """
UPDATE service_need
SET start_date = ${bind(startDate)}, end_date = ${bind(endDate)}, option_id = ${bind(optionId)}, shift_care = ${bind(shiftCare)}, confirmed_by = ${bind(confirmedBy)}, confirmed_at = ${bind(confirmedAt)}
WHERE id = ${bind(id)}
"""
            )
        }
        .execute()
}

fun Database.Transaction.deleteServiceNeed(id: ServiceNeedId) {
    createUpdate { sql("DELETE FROM service_need WHERE id = ${bind(id)}") }.execute()
}

fun Database.Read.getOverlappingServiceNeeds(
    placementId: PlacementId,
    startDate: LocalDate,
    endDate: LocalDate,
    excluding: ServiceNeedId?
): List<ServiceNeed> {
    return createQuery {
            sql(
                """
SELECT 
    sn.id, sn.placement_id, sn.start_date, sn.end_date, sn.shift_care, sn.updated,
    sno.id as option_id, sno.name_fi as option_name_fi, sno.name_sv as option_name_sv, sno.name_en as option_name_en, sno.updated as option_updated,
    sn.confirmed_by as confirmed_user_id, u.name as confirmed_name, sn.confirmed_at
FROM service_need sn
JOIN service_need_option sno on sn.option_id = sno.id
LEFT JOIN evaka_user u on u.id = sn.confirmed_by
WHERE placement_id = ${bind(placementId)} AND daterange(sn.start_date, sn.end_date, '[]') && daterange(${bind(startDate)}, ${bind(endDate)}, '[]')
"""
            )
        }
        .toList<ServiceNeed>()
        .filter { it.id != excluding }
}

fun Database.Read.getServiceNeedOptions(): List<ServiceNeedOption> {
    return createQuery {
            sql(
                """
SELECT
    id,
    name_fi,
    name_sv,
    name_en,
    valid_placement_type,
    default_option,
    fee_coefficient,
    occupancy_coefficient,
    occupancy_coefficient_under_3y,
    realized_occupancy_coefficient,
    realized_occupancy_coefficient_under_3y,
    daycare_hours_per_week,
    contract_days_per_month,
    daycare_hours_per_month,
    part_day,
    part_week,
    fee_description_fi,
    fee_description_sv,
    voucher_value_description_fi,
    voucher_value_description_sv,
    active,
    updated
FROM service_need_option
ORDER BY display_order, part_week, daycare_hours_per_week DESC, part_day, name_fi
        """
            )
        }
        .toList<ServiceNeedOption>()
}

fun Database.Read.findServiceNeedOptionById(id: ServiceNeedOptionId): ServiceNeedOption? {
    return createQuery {
            sql(
                """
SELECT
    id,
    name_fi,
    name_sv,
    name_en,
    valid_placement_type,
    default_option,
    fee_coefficient,
    occupancy_coefficient,
    occupancy_coefficient_under_3y,
    realized_occupancy_coefficient,
    realized_occupancy_coefficient_under_3y,
    daycare_hours_per_week,
    contract_days_per_month,
    daycare_hours_per_month,
    part_day,
    part_week,
    fee_description_fi,
    fee_description_sv,
    voucher_value_description_fi,
    voucher_value_description_sv,
    active,
    updated
FROM service_need_option
WHERE id = ${bind(id)}
        """
            )
        }
        .exactlyOneOrNull<ServiceNeedOption>()
}

fun Database.Read.getServiceNeedOptionPublicInfos(
    placementTypes: List<PlacementType>
): List<ServiceNeedOptionPublicInfo> {
    return createQuery {
            sql(
                """
SELECT
    id,
    name_fi, name_sv, name_en,
    valid_placement_type
FROM service_need_option
WHERE default_option IS FALSE AND show_for_citizen IS TRUE AND valid_placement_type = ANY(${bind(placementTypes)}::placement_type[])
ORDER BY display_order
"""
            )
        }
        .toList<ServiceNeedOptionPublicInfo>()
}

fun Database.Read.getChildServiceNeedInfos(
    unitId: DaycareId,
    childIds: Set<ChildId>,
    dateRange: FiniteDateRange
): List<ChildServiceNeedInfo> {
    return createQuery {
            sql(
                """
    SELECT p.child_id,
           sno.contract_days_per_month IS NOT NULL     AS has_contract_days,
           sno.daycare_hours_per_month,
           sno.name_fi                                 AS option_name,
           daterange(sn.start_date, sn.end_date, '[]') AS valid_during,
           sn.shift_care
    FROM placement p
    JOIN service_need sn ON sn.placement_id = p.id
    JOIN service_need_option sno ON sn.option_id = sno.id
    WHERE p.child_id = ANY(${bind(childIds)}) AND p.unit_id = ${bind(unitId)} AND daterange(sn.start_date, sn.end_date, '[]') && ${bind(dateRange)}
    
    UNION ALL
    
    SELECT bc.child_id,
           sno.contract_days_per_month IS NOT NULL     AS has_contract_days,
           sno.daycare_hours_per_month,
           sno.name_fi                                 AS option_name,
           daterange(sn.start_date, sn.end_date, '[]') AS valid_during,
           sn.shift_care
    FROM backup_care bc
    JOIN placement p ON bc.child_id = p.child_id 
        AND daterange(bc.start_date, bc.end_date, '[]') && daterange(p.start_date, p.end_date, '[]')
    JOIN service_need sn ON sn.placement_id = p.id
    JOIN service_need_option sno ON sn.option_id = sno.id
    WHERE bc.child_id = ANY(${bind(childIds)}) AND bc.unit_id = ${bind(unitId)} 
        AND (daterange(p.start_date, p.end_date, '[]') 
        * daterange(bc.start_date, bc.end_date, '[]') 
        * daterange(sn.start_date, sn.end_date, '[]')) && ${bind(dateRange)}
"""
            )
        }
        .toList<ChildServiceNeedInfo>()
}

fun Database.Read.getActualServiceNeedInfosByRangeAndGroup(
    groupId: GroupId,
    range: FiniteDateRange
): List<ChildServiceNeedInfo> {
    return createQuery {
            sql(
                """
SELECT
    p.child_id,
    sno.contract_days_per_month IS NOT NULL AS has_contract_days,
    sno.daycare_hours_per_month,
    sno.name_fi AS option_name,
    daterange(sn.start_date, sn.end_date, '[]') AS valid_during,
    sn.shift_care
FROM daycare_group_placement AS gp
JOIN placement p ON gp.daycare_placement_id = p.id AND daterange(p.start_date, p.end_date, '[]') && daterange(gp.start_date, gp.end_date, '[]')
JOIN service_need sn ON sn.placement_id = p.id
JOIN service_need_option sno ON sn.option_id = sno.id
WHERE
    daterange(p.start_date, p.end_date, '[]') * daterange(gp.start_date, gp.end_date, '[]') && ${bind(range)} AND
    daterange(sn.start_date, sn.end_date, '[]') * daterange(gp.start_date, gp.end_date, '[]') && ${bind(range)} AND
    gp.daycare_group_id = ${bind(groupId)}

UNION ALL

SELECT
    bc.child_id,
    sno.contract_days_per_month IS NOT NULL AS has_contract_days,
    sno.daycare_hours_per_month,
    sno.name_fi AS option_name,
    daterange(sn.start_date, sn.end_date, '[]') AS valid_during,
    sn.shift_care
FROM backup_care bc
JOIN placement p ON bc.child_id = p.child_id AND daterange(bc.start_date, bc.end_date, '[]') && daterange(p.start_date, p.end_date, '[]')
JOIN service_need sn ON sn.placement_id = p.id
JOIN service_need_option sno ON sn.option_id = sno.id
WHERE
    daterange(p.start_date, p.end_date, '[]') * daterange(bc.start_date, bc.end_date, '[]') && ${bind(range)} AND
    daterange(bc.start_date, bc.end_date, '[]') * daterange(sn.start_date, sn.end_date, '[]') && ${bind(range)} AND
    group_id = ${bind(groupId)}

ORDER BY child_id, valid_during
"""
            )
        }
        .toList<ChildServiceNeedInfo>()
}

fun Database.Read.getServiceNeedOptionFees(from: LocalDate): List<ServiceNeedOptionFee> {
    return createQuery {
            sql(
                """
SELECT
    service_need_option_id,
    validity,
    base_fee,
    sibling_discount_2,
    sibling_fee_2,
    sibling_discount_2_plus,
    sibling_fee_2_plus
FROM service_need_option_fee
WHERE validity && daterange(${bind(from)}, null, '[]')
"""
            )
        }
        .toList<ServiceNeedOptionFee>()
}

fun Database.Read.getServiceNeedOptionFees(): List<ServiceNeedOptionFee> {
    return createQuery {
            sql(
                """
SELECT
    service_need_option_id,
    validity,
    base_fee,
    sibling_discount_2,
    sibling_fee_2,
    sibling_discount_2_plus,
    sibling_fee_2_plus
FROM service_need_option_fee
        """
            )
        }
        .toList<ServiceNeedOptionFee>()
}
