// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.NotFound
import java.time.Instant
import java.time.LocalDate
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

private val VARDA_SERVICE_NEED_CUTOFF_DATE: String = "2019-01-01"
private val VARDA_FINANCIAL_DECICION_CUTOFF_DATE: String = "2019-09-01"

fun Database.Transaction.resetChildResetTimestamp(evakaChildId: ChildId) =
    this.createUpdate {
            sql(
                "UPDATE varda_reset_child SET reset_timestamp = null WHERE evaka_child_id = ${bind(evakaChildId)}"
            )
        }
        .execute()

fun Database.Read.hasVardaServiceNeeds(evakaChildId: ChildId) =
    this.createQuery {
            sql(
                """
                SELECT EXISTS (
                    SELECT evaka_service_need_id FROM varda_service_need
                    WHERE evaka_child_id = ${bind(evakaChildId)}
                )
                """
            )
        }
        .exactlyOne<Boolean>()

fun Database.Transaction.upsertVardaServiceNeed(
    vardaServiceNeed: VardaServiceNeed,
    upsertErrors: List<String> = listOf()
) =
    createUpdate {
            sql(
                """
INSERT INTO varda_service_need (
    evaka_service_need_id, 
    evaka_service_need_updated, 
    evaka_child_id, 
    varda_child_id, 
    varda_decision_id, 
    varda_placement_id, 
    varda_fee_data_ids, 
    update_failed, 
    errors) 
VALUES (
    ${bind(vardaServiceNeed.evakaServiceNeedId)}, 
    ${bind(vardaServiceNeed.evakaServiceNeedUpdated)}, 
    ${bind(vardaServiceNeed.evakaChildId)}, 
    ${bind(vardaServiceNeed.vardaChildId)}, 
    ${bind(vardaServiceNeed.vardaDecisionId)}, 
    ${bind(vardaServiceNeed.vardaPlacementId)}, 
    ${bind(vardaServiceNeed.vardaFeeDataIds)}, 
    ${bind(upsertErrors.isNotEmpty())}, 
    ${bind(upsertErrors)}
) ON CONFLICT (evaka_service_need_id) DO UPDATE 
    SET evaka_service_need_updated = ${bind(vardaServiceNeed.evakaServiceNeedUpdated)}, 
        evaka_child_id = ${bind(vardaServiceNeed.evakaChildId)}, 
        varda_child_id = ${bind(vardaServiceNeed.vardaChildId)},
        varda_decision_id = ${bind(vardaServiceNeed.vardaDecisionId)}, 
        varda_placement_id = ${bind(vardaServiceNeed.vardaPlacementId)}, 
        varda_fee_data_ids = ${bind(vardaServiceNeed.vardaFeeDataIds)},
        update_failed = ${bind(upsertErrors.isNotEmpty())},
        errors = ${bind(upsertErrors)}
"""
            )
        }
        .execute()

fun Database.Transaction.deleteVardaServiceNeedByEvakaServiceNeed(serviceNeedId: ServiceNeedId) =
    createUpdate {
            sql(
                """
                DELETE FROM varda_service_need
                WHERE evaka_service_need_id = ${bind(serviceNeedId)}    
                """
            )
        }
        .execute()

fun Database.Transaction.deleteVardaServiceNeedByVardaChildId(vardaChildId: Long) =
    createUpdate {
            sql(
                """
                DELETE FROM varda_service_need
                WHERE varda_child_id = ${bind(vardaChildId)}
                """
            )
        }
        .execute()

fun Database.Transaction.deleteVardaServiceNeedByEvakaChildId(evakaChildId: ChildId) =
    createUpdate {
            sql(
                """
                DELETE FROM varda_service_need
                WHERE evaka_child_id = ${bind(evakaChildId)}
                """
            )
        }
        .execute()

fun Database.Transaction.deleteVardaOrganizerChildByVardaChildId(vardaChildId: Long) =
    createUpdate {
            sql(
                """
                DELETE FROM varda_organizer_child
                WHERE varda_child_id = ${bind(vardaChildId)}
                """
            )
        }
        .execute()

fun Database.Transaction.markVardaServiceNeedUpdateFailed(
    serviceNeedId: ServiceNeedId,
    errors: List<String>
) =
    createUpdate {
            sql(
                """
                UPDATE varda_service_need
                SET update_failed = true, errors = ${bind(errors)}
                WHERE evaka_service_need_id = ${bind(serviceNeedId)}    
                """
            )
        }
        .execute()

fun Database.Read.getEvakaServiceNeedChanges(
    clock: EvakaClock,
    feeDecisionMinDate: LocalDate
): List<ChangedChildServiceNeed> =
    createQuery {
            sql(
                """
WITH potential_missing_varda_service_needs AS (
    SELECT
        sn.id AS service_need_id,
        sn.updated AS service_need_updated,
        sn.end_date AS service_need_end_date,
        p.child_id AS child_id,
        d.invoiced_by_municipality
    FROM service_need sn
    JOIN placement p ON sn.placement_id = p.id
    JOIN service_need_option sno ON sn.option_id = sno.id
    JOIN daycare d ON p.unit_id = d.id
    JOIN varda_reset_child vrc ON vrc.evaka_child_id = p.child_id
    LEFT JOIN varda_service_need vsn ON sn.id = vsn.evaka_service_need_id
    WHERE
        p.type = ANY(${bind(vardaPlacementTypes)}::placement_type[])
        AND d.upload_children_to_varda = true
        AND (sn.end_date IS NULL OR sn.end_date >= '$VARDA_SERVICE_NEED_CUTOFF_DATE')
        AND sno.daycare_hours_per_week >= 1
        AND (vsn.evaka_service_need_updated IS NULL OR sn.updated > vsn.evaka_service_need_updated)
        AND sn.start_date <= ${bind(clock.today())}
        AND vrc.reset_timestamp IS NOT NULL
), service_need_fee_decision AS (
    SELECT
        sn.id service_need_id,
        fdc.fee_decision_id,
        fdc.updated
    FROM service_need sn
        JOIN placement pl ON sn.placement_id = pl.id
        JOIN fee_decision_child fdc ON fdc.child_id = pl.child_id
        JOIN fee_decision fd ON fdc.fee_decision_id = fd.id
    WHERE daterange(sn.start_date, sn.end_date, '[]') && fd.valid_during
        AND fd.status = 'SENT'
        AND daterange('$VARDA_FINANCIAL_DECICION_CUTOFF_DATE', null) && fd.valid_during
    ),
    service_need_voucher_decision AS (
        SELECT
            sn.id service_need_id,
            vvd.id voucher_decision_id,
            vvd.updated
        FROM service_need sn
            JOIN placement pl ON sn.placement_id = pl.id
            JOIN voucher_value_decision vvd ON vvd.child_id = pl.child_id
        WHERE daterange(sn.start_date, sn.end_date, '[]') && daterange(vvd.valid_from, vvd.valid_to, '[]')
            AND vvd.status = 'SENT'
            AND daterange(vvd.valid_from, vvd.valid_to, '[]') && daterange('$VARDA_FINANCIAL_DECICION_CUTOFF_DATE', NULL)
     ),
    existing_varda_service_needs_with_changed_fee_data AS (
        SELECT
            vsn.evaka_service_need_id,
            p.child_id AS evaka_child_id,
            GREATEST(fd.updated, vd.updated) AS evaka_service_need_updated
        FROM 
            service_need sn 
            JOIN placement p ON sn.placement_id = p.id
            JOIN varda_service_need vsn ON vsn.evaka_service_need_id = sn.id
            LEFT JOIN service_need_fee_decision fd ON fd.service_need_id = vsn.evaka_service_need_id
            LEFT JOIN service_need_voucher_decision vd ON vd.service_need_id = vsn.evaka_service_need_id
        WHERE
            (vsn.updated < fd.updated OR vsn.updated < vd.updated)
            AND (sn.end_date IS NULL OR sn.end_date >= '$VARDA_SERVICE_NEED_CUTOFF_DATE')
     )  
SELECT DISTINCT
    a.child_id AS evaka_child_id,
    a.service_need_id AS evaka_service_need_id
FROM potential_missing_varda_service_needs a
    LEFT JOIN service_need_fee_decision fd on a.service_need_id = fd.service_need_id
    LEFT JOIN service_need_voucher_decision vd on a.service_need_id = vd.service_need_id
WHERE invoiced_by_municipality = false
   OR a.service_need_end_date < ${bind(feeDecisionMinDate)}
   OR fd.fee_decision_id IS NOT NULL
   OR vd.voucher_decision_id IS NOT NULL
UNION
SELECT DISTINCT
    a.evaka_child_id,
    a.evaka_service_need_id
FROM existing_varda_service_needs_with_changed_fee_data a
UNION 
SELECT
    vsn.evaka_child_id,
    vsn.evaka_service_need_id
FROM varda_service_need vsn
WHERE update_failed = true       
"""
            )
        }
        .toList<ChangedChildServiceNeed>()

fun Database.Read.getChildVardaServiceNeeds(evakaChildId: ChildId): List<VardaServiceNeed> =
    createQuery {
            sql(
                """
SELECT
    evaka_child_id,
    evaka_service_need_id,
    evaka_service_need_updated,
    varda_child_id,
    varda_decision_id,
    varda_placement_id,
    varda_fee_data_ids,
    update_failed,
    errors
FROM varda_service_need
WHERE evaka_child_id = ${bind(evakaChildId)}
"""
            )
        }
        .toList<VardaServiceNeed>()

fun Database.Read.getVardaServiceNeedByEvakaServiceNeedId(
    eVakaServiceNeedId: ServiceNeedId
): VardaServiceNeed? =
    createQuery {
            sql(
                """
SELECT
    evaka_child_id,
    evaka_service_need_id,
    evaka_service_need_updated,
    varda_child_id,
    varda_decision_id,
    varda_placement_id,
    varda_fee_data_ids,
    update_failed,
    errors
FROM varda_service_need
WHERE evaka_service_need_id = ${bind(eVakaServiceNeedId)}
"""
            )
        }
        .exactlyOneOrNull<VardaServiceNeed>()

fun Database.Read.getServiceNeedFeeData(
    serviceNeedId: ServiceNeedId,
    feeDecisionStatus: FeeDecisionStatus = FeeDecisionStatus.SENT,
    voucherValueDecisionStatus: VoucherValueDecisionStatus = VoucherValueDecisionStatus.SENT
): List<FeeDataByServiceNeed> {
    return getServiceNeedFeeDataQuery(serviceNeedId, feeDecisionStatus, voucherValueDecisionStatus)
}

private fun Database.Read.getServiceNeedFeeDataQuery(
    serviceNeedId: ServiceNeedId,
    feeDecisionStatus: FeeDecisionStatus,
    voucherValueDecisionStatus: VoucherValueDecisionStatus
): List<FeeDataByServiceNeed> =
    createQuery {
            sql(
                """
WITH child_fees AS (
    SELECT
        fd.id AS fee_decision_id,
        fdc.child_id,
        fd.valid_during
    FROM fee_decision fd
        JOIN fee_decision_child fdc ON fd.id = fdc.fee_decision_id
    WHERE fd.status = ${bind(feeDecisionStatus)}
        AND daterange('$VARDA_FINANCIAL_DECICION_CUTOFF_DATE', null) && fd.valid_during
), service_need_fees AS (
    SELECT
        sn.id AS service_need_id,
        p.child_id AS child_id,
        array_agg(child_fees.fee_decision_id) AS fee_decision_ids
    FROM service_need sn
        JOIN placement p ON p.id = sn.placement_id
        JOIN daycare d ON d.id = p.unit_id
        JOIN child_fees ON p.child_id = child_fees.child_id
            AND child_fees.valid_during && daterange(sn.start_date, sn.end_date, '[]')
            AND sn.end_date >= '$VARDA_FINANCIAL_DECICION_CUTOFF_DATE'
    WHERE d.upload_children_to_varda = true
        AND d.invoiced_by_municipality = true
    GROUP BY service_need_id, p.child_id
), service_need_vouchers AS (
    SELECT
        sn.id AS service_need_id,
        p.child_id AS child_id,
        array_agg(vvd.id) AS voucher_value_decision_ids
    FROM service_need sn JOIN placement p ON p.id = sn.placement_id
        JOIN daycare d ON d.id = p.unit_id  
        JOIN voucher_value_decision vvd ON p.child_id = vvd.child_id
            AND daterange(vvd.valid_from, vvd.valid_to, '[]') && daterange(sn.start_date, sn.end_date, '[]')
            AND daterange('$VARDA_FINANCIAL_DECICION_CUTOFF_DATE', null) && daterange(vvd.valid_from, vvd.valid_to, '[]')
            AND sn.end_date >= '$VARDA_FINANCIAL_DECICION_CUTOFF_DATE'
    WHERE d.upload_children_to_varda = true
        AND vvd.status = ${bind(voucherValueDecisionStatus)}
    GROUP BY service_need_id, p.child_id
)
SELECT
    COALESCE(service_need_fees.service_need_id, service_need_vouchers.service_need_id) AS service_need_id,
    COALESCE(service_need_fees.child_id, service_need_vouchers.child_id) AS evaka_child_id,
    service_need_fees.fee_decision_ids,
    service_need_vouchers.voucher_value_decision_ids
FROM service_need_fees 
    FULL OUTER JOIN service_need_vouchers ON 
        service_need_fees.service_need_id = service_need_vouchers.service_need_id
WHERE COALESCE(service_need_fees.service_need_id, service_need_vouchers.service_need_id) = ${bind(serviceNeedId)}
"""
            )
        }
        .toList<FeeDataByServiceNeed>()

fun Database.Read.getEvakaServiceNeedInfoForVarda(id: ServiceNeedId): EvakaServiceNeedInfoForVarda {
    // The default application date is set to be 15 days before the start because it's the minimum
    // for Varda to not deduce the application as urgent
    return createQuery {
            sql(
                """
SELECT
    sn.id,
    p.child_id AS child_id,
    sn.start_date,
    sn.end_date,
    LEAST(COALESCE(application_match.sentdate, application_match.created::date), sn.start_date - interval '15 days') AS application_date,
    COALESCE((application_match.document ->> 'urgent') :: BOOLEAN, false) AS urgent,
    sno.daycare_hours_per_week AS hours_per_week,
    CASE 
        WHEN sno.valid_placement_type = ANY(${bind(vardaTemporaryPlacementTypes)}::placement_type[]) THEN true
        ELSE false
    END AS temporary,
    NOT(sno.part_week) AS daily,
    sn.shift_care = 'FULL' as shift_care,
    d.provider_type,
    d.oph_organizer_oid,
    d.oph_unit_oid,
    sn.updated AS service_need_updated
FROM service_need sn
JOIN service_need_option sno on sn.option_id = sno.id
JOIN placement p ON p.id = sn.placement_id
JOIN daycare d ON p.unit_id = d.id
LEFT JOIN LATERAL (
    SELECT a.id, a.sentdate, a.created, a.document
    FROM application a
    WHERE child_id = p.child_id
      AND a.status IN ('ACTIVE')
      AND EXISTS (
            SELECT 1
            FROM placement_plan pp
            WHERE pp.unit_id = p.unit_id AND pp.application_id = a.id
              AND daterange(pp.start_date, pp.end_date, '[]') && daterange(sn.start_date, sn.end_date, '[]')
        )
    ORDER BY a.sentdate, a.id
    LIMIT 1
    ) application_match ON true
WHERE sn.id = ${bind(id)}
"""
            )
        }
        .exactlyOneOrNull<EvakaServiceNeedInfoForVarda>()
        ?: throw NotFound("Service need $id not found")
}

fun Database.Transaction.setVardaResetChildResetTimestamp(
    evakaChildId: ChildId,
    resetTimestamp: Instant
) =
    createUpdate {
            sql(
                """
UPDATE varda_reset_child SET reset_timestamp = ${bind(resetTimestamp)}
WHERE evaka_child_id = ${bind(evakaChildId)}
"""
            )
        }
        .execute()

fun Database.Read.serviceNeedIsInvoicedByMunicipality(serviceNeedId: ServiceNeedId): Boolean =
    createQuery {
            sql(
                """
                SELECT true
                FROM service_need sn 
                    LEFT JOIN placement p ON sn.placement_id = p.id
                    LEFT JOIN daycare d ON d.id = p.unit_id
                WHERE
                    sn.id = ${bind(serviceNeedId)} 
                    AND d.invoiced_by_municipality = true
                """
            )
        }
        .toList<Boolean>()
        .isNotEmpty()

fun Database.Read.getServiceNeedsForVardaByChild(
    clock: EvakaClock,
    childId: ChildId
): List<ServiceNeedId> {
    return createQuery {
            sql(
                """
                SELECT sn.id
                FROM service_need sn
                JOIN placement pl ON pl.id = sn.placement_id
                JOIN daycare d ON d.id = pl.unit_id
                JOIN service_need_option sno ON sn.option_id = sno.id
                WHERE pl.child_id = ${bind(childId)}
                AND pl.type = ANY(${bind(vardaPlacementTypes)}::placement_type[])
                AND d.upload_children_to_varda = true
                AND sno.daycare_hours_per_week >= 1
                AND sn.start_date <= ${bind(clock.today())}
                AND (sn.end_date IS NULL OR sn.end_date >= '$VARDA_SERVICE_NEED_CUTOFF_DATE')
                """
            )
        }
        .toList<ServiceNeedId>()
}

fun Database.Read.getSuccessfullyVardaResetEvakaChildIds(): List<ChildId> =
    createQuery {
            sql("SELECT evaka_child_id FROM varda_reset_child WHERE reset_timestamp IS NOT NULL")
        }
        .toList<ChildId>()

fun Database.Transaction.setToBeReset(childIds: List<ChildId>) =
    createUpdate {
            sql(
                """
                UPDATE varda_reset_child
                SET reset_timestamp = null
                WHERE evaka_child_id = ANY(${bind(childIds)})
                """
            )
        }
        .execute()

fun Database.Read.getVardaChildToEvakaChild(): Map<Long, ChildId?> =
    createQuery {
            sql(
                """
                SELECT varda_child_id, evaka_person_id evaka_child_id
                FROM varda_organizer_child
                WHERE varda_child_id IS NOT NULL
                """
            )
        }
        .toMap { columnPair("varda_child_id", "evaka_child_id") }

fun Database.Read.getDistinctVardaPersonOidsByEvakaPersonId(id: PersonId) =
    createQuery {
            sql(
                "SELECT DISTINCT varda_person_oid FROM varda_organizer_child WHERE evaka_person_id = ${bind(id)}"
            )
        }
        .toList<String>()

fun Database.Transaction.getVardaChildrenToReset(
    limit: Int,
    addNewChildren: Boolean
): List<ChildId> {
    // We aim to include children by daycare units, capping each batch with <limit>
    val updateCount =
        if (!addNewChildren) {
            0
        } else {
            createUpdate {
                    sql(
                        """
                        with varda_daycare as (
                            select id, name from daycare
                            where upload_children_to_varda = true
                            order by name asc
                        ),
                        child_daycare as (
                            select distinct p.child_id, d.name
                            from placement p
                            join varda_daycare d on d.id = p.unit_id
                            where not exists (
                                select 1 from varda_reset_child where evaka_child_id = p.child_id
                            )
                            order by d.name asc
                            limit ${bind(limit)}
                        ),
                        last_daycare as (
                            select name from child_daycare order by name desc limit 1
                        ),
                        daycare_count as (
                            select count(distinct name) from child_daycare
                        )
                        insert into varda_reset_child(evaka_child_id)
                        select distinct child_id
                        from child_daycare
                        where name not in (select name from last_daycare)
                        or 1 in (select count from daycare_count)
                        """
                    )
                }
                .execute()
        }

    if (updateCount > 0) logger.info("VardaUpdate: added $updateCount new children to be reset")

    return createQuery {
            sql(
                "SELECT evaka_child_id FROM varda_reset_child WHERE reset_timestamp IS NULL LIMIT ${bind(limit)}"
            )
        }
        .toList<ChildId>()
}

fun Database.Read.calculateDeletedChildServiceNeeds(
    clock: EvakaClock
): Map<ChildId, List<ServiceNeedId>> =
    this.createQuery {
            sql(
                """
SELECT evaka_child_id AS child_id, array_agg(evaka_service_need_id::uuid) AS service_need_ids
FROM varda_service_need vsn
WHERE NOT EXISTS (
    SELECT 1
    FROM service_need sn
    WHERE sn.id = vsn.evaka_service_need_id
) OR evaka_service_need_id NOT IN (
SELECT
    sn.id AS service_need_id
FROM service_need sn
JOIN placement p ON sn.placement_id = p.id
JOIN service_need_option sno ON sn.option_id = sno.id
JOIN daycare d ON p.unit_id = d.id
WHERE
  p.type = ANY(${bind(vardaPlacementTypes)}::placement_type[])
  AND d.upload_children_to_varda = true
  AND sno.daycare_hours_per_week >= 1
  AND sn.start_date <= ${bind(clock.today())}
  AND (sn.end_date IS NULL OR sn.end_date >= '$VARDA_SERVICE_NEED_CUTOFF_DATE')
)
GROUP BY evaka_child_id
"""
            )
        }
        .toMap { columnPair("child_id", "service_need_ids") }
