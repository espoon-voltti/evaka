package fi.espoo.evaka.varda

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import fi.espoo.evaka.varda.integration.VardaClient
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

class VardaUpdateServiceV2 {
    fun updateAll(
        db: Database.Connection,
        client: VardaClient,
        organizer: String,
//            startingFrom: HelsinkiDateTime
    ) {
        updateOrganizer(db, client, organizer)
        updateUnits(db, client, organizer)

        //       updateAllChildVardaData(db, client, organizer)
    }

    /*
        1. Find out all evaka children with changed Varda related data:
            - new, modified or deleted evaka service need compared to stored service need history (see below)
            - TODO: fee decisions
        2. For each of these children:
            - first check if there is enough info to do the update
                - must have any service need AND
                - must have any fee decision

                (if not, switch to next child)

                then

            - create a varda henkilo and lapsi if missing
            - since given lastUpdated:
                - if existing evaka service need is changed compared to stored service need history timestamp (see below)
                    - delete correspoding varda varhaiskasvatuspaatos from varda and add a new one
                    - store the evaka service need id - service need option - MAX(update) value - varda varhaiskasvatuspaatos id
                - if there is a new evaka service need
                    - create correspoding varda varhaiskasvatuspaatos
                    - create a new varda varhaiskasvatussuhde
                    - store the evaka service need id - service need option - MAX(update) value - varda varhaiskasvatuspaatos id
                - do the same for evaka fee decisions -> varda maksutiedot (TODO more specific design)
     */
//    fun updateAllChildVardaData(db: Database.Connection, client: VardaClient, organizer: String) {
//    }
}

/*
    1. Find out all evaka children with changed Varda related data:
    - new, modified or deleted evaka service need compared to stored service need history (see below)
    - TODO: fee decisions
*/
fun calculateEvakaVsVardaServiceNeedChangesByChild(db: Database.Connection, startingFrom: HelsinkiDateTime): Map<UUID, VardaChildCalculatedServiceNeedChanges> {
    val evakaServiceNeedChangesByChild = db.read { it.getEvakaServiceNeedChanges(startingFrom) }.groupBy { it.evakaChildId }
    return evakaServiceNeedChangesByChild.entries.associate { evakaServiceNeedChangesForChild ->
        val vardaServiceNeedsForChild = db.read { it.getChildVardaServiceNeeds(evakaServiceNeedChangesForChild.key) }

        evakaServiceNeedChangesForChild.key to VardaChildCalculatedServiceNeedChanges(
            childId = evakaServiceNeedChangesForChild.key,
            additions = calculateNewChildServiceNeeds(evakaServiceNeedChangesForChild.value, vardaServiceNeedsForChild),
            updates = calculateUpdatedChildServiceNeeds(evakaServiceNeedChangesForChild.value, vardaServiceNeedsForChild),
            deletes = calculateDeletedChildServiceNeeds(db, evakaServiceNeedChangesForChild.key, vardaServiceNeedsForChild)
        )
    }
}

data class VardaChildCalculatedServiceNeedChanges(
    val childId: UUID,
    val additions: List<UUID>,
    val updates: List<UUID>,
    val deletes: List<UUID>
)

// Find out new varhaiskasvatuspaatos to be added for a child: any new service needs not found from child's full varda history
private fun calculateNewChildServiceNeeds(evakaServiceNeedChangesForChild: List<VardaServiceNeed>, vardaChildServiceNeeds: List<VardaServiceNeed>): List<UUID> {
    return evakaServiceNeedChangesForChild.filter { newServiceNeedChange ->
        vardaChildServiceNeeds.none { vardaChildServiceNeedChange ->
            vardaChildServiceNeedChange.evakaServiceNeedId == newServiceNeedChange.evakaServiceNeedId
        }
    }.map {
        it.evakaServiceNeedId
    }
}

// Find out changed varhaiskasvatuspaatos for a child: any new service need with a different update timestamp in history
private fun calculateUpdatedChildServiceNeeds(evakaServiceNeedChangesForChild: List<VardaServiceNeed>, vardaServiceNeedsForChild: List<VardaServiceNeed>): List<UUID> {
    return evakaServiceNeedChangesForChild.filter { newServiceNeedChange ->
        val match = vardaServiceNeedsForChild.find { it.evakaServiceNeedId == newServiceNeedChange.evakaServiceNeedId }
        match != null && (
            (
                newServiceNeedChange.evakaServiceNeedUpdated != match.evakaServiceNeedUpdated ||
                    newServiceNeedChange.evakaServiceNeedOptionUpdated != match.evakaServiceNeedOptionUpdated
                )
            )
    }.map {
        it.evakaServiceNeedId
    }
}

private fun calculateDeletedChildServiceNeeds(db: Database.Connection, childId: UUID, vardaServiceNeedsForChild: List<VardaServiceNeed>): List<UUID> {
    val allChildEvakaServiceNeedsIds = db.read {
        it.createQuery(
            """
SELECT sn.id 
FROM new_service_need sn LEFT JOIN placement p ON sn.placement_id = p.id
WHERE p.child_id = :childId
"""
        )
            .bind("childId", childId)
            .mapTo<UUID>()
            .list()
    }
    return vardaServiceNeedsForChild.filter { !allChildEvakaServiceNeedsIds.contains(it.evakaServiceNeedId) }.map { it.evakaServiceNeedId }
}

data class VardaServiceNeed(
    val evakaChildId: UUID,
    val evakaServiceNeedId: UUID,
    val evakaServiceNeedOptionId: UUID,
    val evakaServiceNeedUpdated: HelsinkiDateTime,
    val evakaServiceNeedOptionUpdated: HelsinkiDateTime
) {
    val evakaLastUpdated = if (evakaServiceNeedUpdated.isAfter(evakaServiceNeedOptionUpdated)) evakaServiceNeedUpdated else evakaServiceNeedOptionUpdated
}

fun Database.Read.getEvakaServiceNeedChanges(startingFrom: HelsinkiDateTime): List<VardaServiceNeed> =
    createQuery(
        """
SELECT 
    placement.child_id AS evakaChildId,
    sn.id AS evakaServiceNeedId,
    option.id AS evakaServiceNeedOptionId,
    sn.updated AS evakaServiceNeedUpdated,
    option.updated AS evakaServiceNeedOptionUpdated
FROM new_service_need sn
LEFT JOIN service_need_option option ON sn.option_id = option.id
LEFT JOIN placement ON sn.placement_id = placement.id
WHERE (sn.updated >= :startingFrom OR option.updated >= :startingFrom)
"""
    )
        .bind("startingFrom", startingFrom)
        .mapTo<VardaServiceNeed>()
        .list()

fun Database.Read.getChildVardaServiceNeeds(evakaChildId: UUID): List<VardaServiceNeed> =
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
/*
fun Database.Transaction.upsertVardaServiceNeedChange(vardaServiceNeed: VardaServiceNeed) =
    createUpdate(
            """
INSERT INTO service_need_change(service_need_id, service_need_option_id, service_need_updated, service_need_option_updated)
VALUES (:serviceNeedId, :serviceNeedOptionId, :serviceNeedUpdated, :serviceNeedOptionUpdated)
ON CONFLICT (service_need_id) DO UPDATE 
    SET service_need_option_id = :serviceNeedOptionId,
        service_need_updated = :serviceNeedUpdated,
        service_need_option_updated = :serviceNeedOptionUpdated
        """
    )
            .bind("serviceNeedId", vardaServiceNeed.serviceNeedId)
            .bind("serviceNeedOptionId", vardaServiceNeed.serviceNeedOptionId)
            .bind("serviceNeedUpdated", vardaServiceNeed.serviceNeedUpdated)
            .bind("serviceNeedOptionUpdated", vardaServiceNeed.serviceNeedOptionUpdated)
            .execute()
*/
