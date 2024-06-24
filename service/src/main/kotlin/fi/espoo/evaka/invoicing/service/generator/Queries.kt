// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.invoicing.service.generator

import fi.espoo.evaka.invoicing.controller.ServiceNeedOptionVoucherValueRangeWithId
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.FiniteDateRange

fun Database.Read.getPlacementRangesByChild(childIds: Set<ChildId>) =
    createQuery {
        sql(
            """
                SELECT 
                    child_id, 
                    daterange(pl.start_date, pl.end_date, '[]') as finite_range, 
                    pl.type,
                    pl.unit_id,
                    d.provider_type as unit_provider_type,
                    invoiced_by_municipality as invoiced_unit
                FROM placement pl
                JOIN daycare d ON pl.unit_id = d.id
                WHERE child_id = ANY(${bind(childIds)}) AND NOT pl.type = ANY(${bind(ignoredPlacementTypes)})
            """
        )
    }.toList<PlacementRange>()
        .groupBy { it.childId }

fun Database.Read.getServiceNeedRangesByChild(childIds: Set<ChildId>) =
    createQuery {
        sql(
            """
        SELECT child_id, daterange(sn.start_date, sn.end_date, '[]') as finite_range, sn.option_id
        FROM service_need sn
        JOIN placement p ON sn.placement_id = p.id
        WHERE child_id = ANY(${bind(childIds)})
    """
        )
    }.toList<ServiceNeedRange>()
        .groupBy { it.childId }

fun Database.Read.getVoucherValuesByServiceNeedOption() =
    createQuery {
        sql(
            """
SELECT
    id,
    service_need_option_id,
    validity as range,
    base_value,
    coefficient,
    value,
    base_value_under_3y,
    coefficient_under_3y,
    value_under_3y
FROM service_need_option_voucher_value
        """
        )
    }.toList<ServiceNeedOptionVoucherValueRangeWithId>()
        .groupBy { it.voucherValues.serviceNeedOptionId }

fun Database.Read.getChildRelations(parentIds: Set<PersonId>): Map<PersonId, List<ChildRelation>> {
    if (parentIds.isEmpty()) return emptyMap()

    return createQuery {
        sql(
            """
SELECT 
    fc.head_of_child, 
    daterange(fc.start_date, fc.end_date, '[]') as finite_range,
    p.id as child_id,
    p.date_of_birth as child_date_of_birth,
    p.social_security_number as child_ssn
FROM fridge_child fc
JOIN person p on fc.child_id = p.id
WHERE head_of_child = ANY(${bind(parentIds)}) AND NOT conflict
"""
        )
    }.toList<ChildRelation>()
        .mapNotNull {
            val under18 =
                FiniteDateRange(
                    it.child.dateOfBirth,
                    it.child.dateOfBirth
                        .plusYears(18)
                        .minusDays(1)
                )
            it.range.intersection(under18)?.let { range -> it.copy(finiteRange = range) }
        }.groupBy { it.headOfChild }
}

fun Database.Read.getPartnerRelations(id: PersonId): List<PartnerRelation> =
    createQuery {
        sql(
            """
SELECT 
    fp2.person_id as partnerId,
    daterange(fp2.start_date, fp2.end_date, '[]') as range
FROM fridge_partner fp1
JOIN fridge_partner fp2 ON fp1.partnership_id = fp2.partnership_id AND fp1.indx <> fp2.indx
WHERE fp1.person_id = ${bind(id)} AND NOT fp1.conflict AND NOT fp2.conflict
"""
        )
    }.toList<PartnerRelation>()
