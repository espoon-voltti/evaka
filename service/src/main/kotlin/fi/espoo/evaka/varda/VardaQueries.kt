// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.invoicing.domain.FeeDecisionStatus
import fi.espoo.evaka.invoicing.domain.VoucherValueDecisionStatus
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.ServiceNeedId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.mapColumn
import fi.espoo.evaka.shared.domain.NotFound
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

fun Database.Transaction.resetChildResetTimestamp(evakaChildId: ChildId) = this.createUpdate("UPDATE varda_reset_child SET reset_timestamp = null WHERE evaka_child_id = :id")
    .bind("id", evakaChildId)
    .execute()

fun Database.Read.hasVardaServiceNeeds(evakaChildId: ChildId) =
    this.createQuery(
        """
        SELECT EXISTS (
            SELECT evaka_service_need_id FROM varda_service_need
            WHERE evaka_child_id = :evaka_child_id
        )
        """.trimIndent()
    )
        .bind("evaka_child_id", evakaChildId)
        .mapTo<Boolean>()
        .first()

fun Database.Transaction.upsertVardaServiceNeed(vardaServiceNeed: VardaServiceNeed, upsertErrors: List<String> = listOf()) = createUpdate(
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
    :evakaServiceNeedId, 
    :evakaServiceNeedUpdated, 
    :evakaChildId, 
    :vardaChildId, 
    :vardaDecisionId, 
    :vardaPlacementId, 
    :vardaFeeDataIds, 
    :errorsNotEmpty, 
    :upsertErrors
) ON CONFLICT (evaka_service_need_id) DO UPDATE 
    SET evaka_service_need_updated = :evakaServiceNeedUpdated, 
        evaka_child_id = :evakaChildId, 
        varda_child_id = :vardaChildId,
        varda_decision_id = :vardaDecisionId, 
        varda_placement_id = :vardaPlacementId, 
        varda_fee_data_ids = :vardaFeeDataIds,
        update_failed = :errorsNotEmpty,
        errors = :upsertErrors
"""
).bindKotlin(vardaServiceNeed)
    .bind("upsertErrors", upsertErrors.toTypedArray())
    .bind("errorsNotEmpty", upsertErrors.isNotEmpty())
    .execute()

fun Database.Transaction.deleteVardaServiceNeedByEvakaServiceNeed(serviceNeedId: ServiceNeedId) = createUpdate(
    """
DELETE FROM varda_service_need
WHERE evaka_service_need_id = :serviceNeedId    
        """
).bind("serviceNeedId", serviceNeedId)
    .execute()

fun Database.Transaction.deleteVardaServiceNeedByVardaChildId(vardaChildId: Long) = createUpdate(
    """
DELETE FROM varda_service_need
WHERE varda_child_id = :vardaChildId
        """
).bind("vardaChildId", vardaChildId)
    .execute()

fun Database.Transaction.deleteVardaOrganizerChildByVardaChildId(vardaChildId: Long) = createUpdate(
    """
DELETE FROM varda_organizer_child
WHERE varda_child_id = :vardaChildId
        """
).bind("vardaChildId", vardaChildId)
    .execute()

fun Database.Transaction.markVardaServiceNeedUpdateFailed(serviceNeedId: ServiceNeedId, errors: List<String>) = createUpdate(
    """
UPDATE varda_service_need
SET update_failed = true, errors = :errors
WHERE evaka_service_need_id = :serviceNeedId    
        """
).bind("serviceNeedId", serviceNeedId)
    .bind("errors", errors.toTypedArray())
    .execute()

fun Database.Read.getEvakaServiceNeedChanges(feeDecisionMinDate: LocalDate): List<ChangedChildServiceNeed> =
    createQuery(
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
        p.type = ANY(:vardaPlacementTypes::placement_type[])
        AND d.upload_children_to_varda = true
        AND sno.daycare_hours_per_week >= 1
        AND (vsn.evaka_service_need_updated IS NULL OR sn.updated > vsn.evaka_service_need_updated)
        AND sn.start_date <= current_date
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
            vsn.updated < fd.updated OR vsn.updated < vd.updated
     )  
SELECT DISTINCT
    a.child_id AS evaka_child_id,
    a.service_need_id AS evaka_service_need_id
FROM potential_missing_varda_service_needs a
    LEFT JOIN service_need_fee_decision fd on a.service_need_id = fd.service_need_id
    LEFT JOIN service_need_voucher_decision vd on a.service_need_id = vd.service_need_id
WHERE invoiced_by_municipality = false
   OR a.service_need_end_date < :feeDecisionMinDate
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
        """.trimIndent()
    )
        .bind("vardaPlacementTypes", vardaPlacementTypes)
        .bind("feeDecisionMinDate", feeDecisionMinDate)
        .mapTo<ChangedChildServiceNeed>()
        .list()

fun Database.Read.getChildVardaServiceNeeds(evakaChildId: ChildId): List<VardaServiceNeed> =
    createQuery(
        """
SELECT *
FROM varda_service_need
WHERE evaka_child_id = :evakaChildId
"""
    )
        .bind("evakaChildId", evakaChildId)
        .mapTo<VardaServiceNeed>()
        .list()

fun Database.Read.getVardaServiceNeedByEvakaServiceNeedId(eVakaServiceNeedId: ServiceNeedId): VardaServiceNeed? =
    createQuery(
        """
SELECT *
FROM varda_service_need
WHERE evaka_service_need_id = :eVakaServiceNeedId
"""
    )
        .bind("eVakaServiceNeedId", eVakaServiceNeedId)
        .mapTo<VardaServiceNeed>()
        .firstOrNull()

fun Database.Read.getServiceNeedFeeData(serviceNeedId: ServiceNeedId, feeDecisionStatus: FeeDecisionStatus = FeeDecisionStatus.SENT, voucherValueDecisionStatus: VoucherValueDecisionStatus = VoucherValueDecisionStatus.SENT): List<FeeDataByServiceNeed> {
    return getServiceNeedFeeDataQuery(serviceNeedId, feeDecisionStatus, voucherValueDecisionStatus)
}

private fun Database.Read.getServiceNeedFeeDataQuery(serviceNeedId: ServiceNeedId, feeDecisionStatus: FeeDecisionStatus, voucherValueDecisionStatus: VoucherValueDecisionStatus): List<FeeDataByServiceNeed> =
    createQuery(
        """
WITH child_fees AS (
    SELECT
        fd.id AS fee_decision_id,
        fdc.child_id,
        fd.valid_during
    FROM fee_decision fd
        JOIN fee_decision_child fdc ON fd.id = fdc.fee_decision_id
    WHERE fd.status = :feeDecisionStatus
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
    WHERE d.upload_children_to_varda = true
        AND d.invoiced_by_municipality = true
        AND vvd.status = :voucherValueDecisionStatus
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
WHERE COALESCE(service_need_fees.service_need_id, service_need_vouchers.service_need_id) = :serviceNeedId
        """
    )
        .bind("serviceNeedId", serviceNeedId)
        .bind("feeDecisionStatus", feeDecisionStatus)
        .bind("voucherValueDecisionStatus", voucherValueDecisionStatus)
        .mapTo<FeeDataByServiceNeed>()
        .list()

fun Database.Read.getEvakaServiceNeedInfoForVarda(id: ServiceNeedId): EvakaServiceNeedInfoForVarda {
    // language=sql
    val sql = """
        SELECT
            sn.id,
            p.child_id AS child_id,
            sn.start_date,
            sn.end_date,
            LEAST(COALESCE(application_match.sentdate, application_match.created::date), sn.start_date) AS application_date,            
            COALESCE((af.document ->> 'urgent') :: BOOLEAN, false) AS urgent,
            sno.daycare_hours_per_week AS hours_per_week,
            CASE 
                WHEN sno.valid_placement_type = ANY(:vardaTemporaryPlacementTypes::placement_type[]) THEN true
                ELSE false
            END AS temporary,
            NOT(sno.part_week) AS daily,
            sn.shift_care,
            d.provider_type,
            d.oph_organizer_oid,
            d.oph_unit_oid,
            sn.updated AS service_need_updated
        FROM service_need sn
        JOIN service_need_option sno on sn.option_id = sno.id
        JOIN placement p ON p.id = sn.placement_id
        JOIN daycare d ON p.unit_id = d.id
        LEFT JOIN LATERAL (
            SELECT a.id, a.sentdate, a.created
            FROM application a
            WHERE child_id = p.child_id
              AND a.status IN ('ACTIVE')
              AND EXISTS (
                    SELECT 1
                    FROM placement_plan pp
                    WHERE pp.unit_id = p.unit_id AND pp.application_id = a.id
                      AND daterange(pp.start_date, pp.end_date, '[]') && daterange(sn.start_date, sn.end_date, '[]')
                      AND unit_confirmation_status = 'ACCEPTED'
                )
            ORDER BY a.sentdate, a.id
            LIMIT 1
            ) application_match ON true
        LEFT JOIN application_form af ON af.application_id = application_match.id AND af.latest IS TRUE        
        WHERE sn.id = :id
    """.trimIndent()

    return createQuery(sql)
        .bind("id", id)
        .bind("vardaTemporaryPlacementTypes", vardaTemporaryPlacementTypes)
        .mapTo<EvakaServiceNeedInfoForVarda>()
        .firstOrNull() ?: throw NotFound("Service need $id not found")
}

fun Database.Transaction.setVardaResetChildResetTimestamp(evakaChildId: UUID, resetTimestamp: Instant) = createUpdate(
    """
UPDATE varda_reset_child SET reset_timestamp = :resetTimestamp
WHERE evaka_child_id = :evakaChildId
        """
).bind("evakaChildId", evakaChildId)
    .bind("resetTimestamp", resetTimestamp)
    .execute()

fun Database.Read.serviceNeedIsInvoicedByMunicipality(serviceNeedId: ServiceNeedId): Boolean = createQuery(
    """
            SELECT true
            FROM service_need sn 
                LEFT JOIN placement p ON sn.placement_id = p.id
                LEFT JOIN daycare d ON d.id = p.unit_id
            WHERE
                sn.id = :serviceNeedId 
                AND d.invoiced_by_municipality = true
    """.trimIndent()
)
    .bind("serviceNeedId", serviceNeedId)
    .mapTo<Boolean>()
    .list().isNotEmpty()

fun Database.Read.getServiceNeedsForVardaByChild(
    childId: UUID
): List<ServiceNeedId> {
    // language=SQL
    val sql =
        """
        SELECT sn.id
        FROM service_need sn
        JOIN placement pl ON pl.id = sn.placement_id
        JOIN daycare d ON d.id = pl.unit_id
        JOIN service_need_option sno ON sn.option_id = sno.id
        WHERE pl.child_id = :childId
        AND pl.type = ANY(:vardaPlacementTypes::placement_type[])
        AND d.upload_children_to_varda = true
        AND sno.daycare_hours_per_week >= 1
        AND sn.start_date <= current_date
        """.trimIndent()

    return createQuery(sql)
        .bind("childId", childId)
        .bind("vardaPlacementTypes", vardaPlacementTypes)
        .mapTo<ServiceNeedId>()
        .toList()
}

fun Database.Read.getSuccessfullyVardaResetEvakaChildIds(): List<ChildId> =
    createQuery("SELECT evaka_child_id FROM varda_reset_child WHERE reset_timestamp IS NOT NULL")
        .mapTo<ChildId>()
        .list()

fun Database.Transaction.setToBeReset(childIds: List<ChildId>) =
    createUpdate(
        """
        UPDATE varda_reset_child
        SET reset_timestamp = null
        WHERE evaka_child_id = ANY(:childIds)
        """.trimIndent()
    )
        .bind("childIds", childIds.toTypedArray())
        .execute()

fun Database.Read.getVardaChildToEvakaChild(): Map<Long, ChildId?> =
    createQuery(
        """
        SELECT varda_child_id, evaka_person_id evaka_child_id
        FROM varda_organizer_child
        WHERE varda_child_id IS NOT NULL
        """.trimIndent()
    )
        .mapTo<VardaChildIdPair>()
        .associate { it.vardaChildId to it.evakaChildId }

fun Database.Transaction.getVardaChildrenToReset(limit: Int, addNewChildren: Boolean): List<UUID> {
    // We aim to include children by daycare units, capping each batch with <limit>
    val updateCount = if (!addNewChildren) 0 else createUpdate(
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
            limit :limit
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
        """.trimIndent()
    )
        .bind("limit", limit)
        .execute()

    if (updateCount > 0) logger.info("VardaUpdate: added $updateCount new children to be reset")

    return createQuery("SELECT evaka_child_id FROM varda_reset_child WHERE reset_timestamp IS NULL LIMIT :limit")
        .bind("limit", limit)
        .mapTo<UUID>()
        .list()
}

fun Database.Read.calculateDeletedChildServiceNeeds(): Map<ChildId, List<ServiceNeedId>> =
    this.createQuery(
        """
SELECT evaka_child_id AS child_id, array_agg(evaka_service_need_id::uuid) AS service_need_ids
FROM varda_service_need
WHERE evaka_service_need_id NOT IN (
    SELECT id FROM service_need
) OR evaka_service_need_id NOT IN (
SELECT
    sn.id AS service_need_id
FROM service_need sn
JOIN placement p ON sn.placement_id = p.id
JOIN service_need_option sno ON sn.option_id = sno.id
JOIN daycare d ON p.unit_id = d.id
WHERE
  p.type = ANY(:vardaPlacementTypes::placement_type[])
  AND d.upload_children_to_varda = true
  AND sno.daycare_hours_per_week >= 1
  AND sn.start_date <= current_date
)
GROUP BY evaka_child_id
        """.trimIndent()
    )
        .bind("vardaPlacementTypes", vardaPlacementTypes)
        .map { row -> row.mapColumn<ChildId>("child_id") to row.mapColumn<Array<ServiceNeedId>>("service_need_ids").toList() }
        .toMap()
