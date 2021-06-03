package fi.espoo.evaka.varda

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.varda.integration.VardaClient
import fi.espoo.evaka.varda.integration.convertToVardaRequest
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

private val logger = KotlinLogging.logger {}

fun getOrCreateVardaChildByOrganizer(
    db: Database.Connection,
    client: VardaClient,
    evakaPersonId: UUID,
    organizerOid: String,
    sourceSystem: String
): Long {
    return db.transaction { tx ->
        val municipalOrganizerOid = getMunicipalOrganizerOid(tx)
        val isPaosChild = organizerOid != municipalOrganizerOid

        val rowsByChild = getVardaOrganizationChildRows(tx, evakaPersonId)
        if (rowsByChild.isEmpty()) {
            return@transaction createVardaPersonAndChild(tx, client, evakaPersonId, municipalOrganizerOid, organizerOid, isPaosChild, sourceSystem)
        }

        val vardaPersonOid = rowsByChild.first().vardaPersonOid

        val rowsByChildAndOrganizer = rowsByChild.filter { row -> row.organizerOid == organizerOid }
        if (rowsByChildAndOrganizer.isEmpty()) {
            return@transaction createVardaChildWhenPersonExists(tx, client, evakaPersonId, vardaPersonOid, municipalOrganizerOid, organizerOid, isPaosChild, sourceSystem)
        }

        return@transaction rowsByChildAndOrganizer.first().vardaChildId
    }
}

data class VardaChildOrganizerRow(
    val evakaPersonId: UUID,
    val vardaPersonOid: String,
    val vardaChildId: Long,
    val organizerOid: String
)

private fun getVardaOrganizationChildRows(
    tx: Database.Transaction,
    evakaPersonId: UUID,
): List<VardaChildOrganizerRow> {
    val sql = """
        SELECT evaka_person_id, varda_person_oid, varda_child_id, organizer_oid
        FROM varda_organizer_child
        WHERE evaka_person_id = :evakaPersonId
    """.trimIndent()

    return tx.createQuery(sql)
        .bind("evakaPersonId", evakaPersonId)
        .mapTo<VardaChildOrganizerRow>()
        .toList()
}

private fun getMunicipalOrganizerOid(tx: Database.Transaction): String {
    return tx.createQuery("SELECT varda_organizer_oid FROM varda_organizer LIMIT 1") // one row table
        .mapTo<String>()
        .toList()
        .first()
}

private fun createVardaPersonAndChild(
    tx: Database.Transaction,
    client: VardaClient,
    evakaPersonId: UUID,
    municipalOrganizerOid: String,
    organizerOid: String,
    isPaosChild: Boolean,
    sourceSystem: String
): Long {
    val personPayload = getVardaPersonPayload(tx, evakaPersonId, organizerOid)

    val vardaPersonOid = client.createPerson(personPayload)?.personOid ?: error("Couldn't create Varda person (nor child)")

    return createVardaChildWhenPersonExists(
        tx,
        client,
        evakaPersonId,
        vardaPersonOid,
        municipalOrganizerOid,
        organizerOid,
        isPaosChild,
        sourceSystem
    )
}

data class VardaChildPayload(
    val personOid: String,
    val organizerOid: String,
    val sourceSystem: String
)

data class VardaPaosChildPayload(
    val personOid: String,
    val organizerOid: String,
    val paosOrganizationOid: String,
    val sourceSystem: String
)

private fun createVardaChildWhenPersonExists(
    tx: Database.Transaction,
    client: VardaClient,
    evakaPersonId: UUID,
    vardaPersonOid: String,
    municipalOrganizerOid: String,
    organizerOid: String,
    isPaosChild: Boolean,
    sourceSystem: String
): Long {
    return if (isPaosChild) {
        val vardaChildPayload = VardaPaosChildPayload(
            personOid = vardaPersonOid,
            organizerOid = municipalOrganizerOid,
            paosOrganizationOid = organizerOid,
            sourceSystem = sourceSystem
        )

        val vardaChildId = client.createChild(convertToVardaRequest(evakaPersonId, vardaChildPayload))?.vardaId?.toLong()
            ?: error("Couldn't create Varda PAOS child for $vardaChildPayload")

        insertVardaOrganizerChild(
            tx,
            evakaPersonId,
            vardaChildId,
            vardaPersonOid,
            organizerOid
        )

        vardaChildId
    } else {
        val vardaChildPayload = VardaChildPayload(
            personOid = vardaPersonOid,
            organizerOid = organizerOid,
            sourceSystem = sourceSystem
        )

        val vardaChildId = client.createChild(convertToVardaRequest(evakaPersonId, vardaChildPayload))?.vardaId?.toLong()
            ?: error("Couldn't create Varda child for $vardaChildPayload")

        insertVardaOrganizerChild(
            tx,
            evakaPersonId,
            vardaChildId,
            vardaPersonOid,
            organizerOid
        )

        vardaChildId
    }
}

private fun getVardaPersonPayload(tx: Database.Transaction, evakaPersonId: UUID, organizerOid: String) =
    tx.createQuery(
        """
            SELECT 
                p.id,
                p.first_name,
                p.last_name,
                p.social_security_number         AS ssn,
                split_part(p.first_name, ' ', 1) AS nick_name,
                :organizerOid
            FROM person p
            WHERE id = :evakaPersonId
        """.trimIndent()
    )
        .bind("evakaPersonId", evakaPersonId)
        .bind("organizerOid", organizerOid)
        .mapTo<VardaPersonRequest>()
        .toList()
        .first()

fun insertVardaOrganizerChild(
    tx: Database.Transaction,
    evakaPersonId: UUID,
    vardaChildId: Long,
    vardaPersonOid: String,
    organizerOid: String
) {
    //language=SQL
    val sql = """
    INSERT INTO varda_organizer_child (evaka_person_id, varda_child_id, varda_person_oid, organizer_oid)
    VALUES (:evakaPersonId, :vardaChildId, :vardaPersonOid, :organizerOid)
    """.trimIndent()

    tx.createUpdate(sql)
        .bind("evakaPersonId", evakaPersonId)
        .bind("vardaChildId", vardaChildId)
        .bind("vardaPersonOid", vardaPersonOid)
        .bind("organizerOid", organizerOid)
        .execute()
}
