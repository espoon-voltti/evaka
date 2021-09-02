// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.varda.integration.VardaClient
import fi.espoo.evaka.varda.integration.convertToVardaChildRequest
import org.jdbi.v3.core.kotlin.mapTo
import java.lang.Exception
import java.util.UUID

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

        val rowsByChild = getVardaOrganizerChildRows(tx, evakaPersonId)
        if (rowsByChild.isEmpty()) {
            return@transaction createVardaPersonAndChild(tx, client, evakaPersonId, municipalOrganizerOid, organizerOid, isPaosChild, sourceSystem)
        }

        val vardaPerson = rowsByChild.first()

        val rowsByChildAndOrganizer = rowsByChild.filter { row -> row.organizerOid == organizerOid }
        if (rowsByChildAndOrganizer.isEmpty()) {
            return@transaction createVardaChildWhenPersonExists(tx, client, evakaPersonId, vardaPerson.vardaPersonId, vardaPerson.vardaPersonOid, municipalOrganizerOid, organizerOid, isPaosChild, sourceSystem)
        }

        return@transaction rowsByChildAndOrganizer.first().vardaChildId
    }
}

data class VardaChildOrganizerRow(
    val evakaPersonId: UUID,
    val vardaPersonId: Int,
    val vardaPersonOid: String,
    val vardaChildId: Long,
    val organizerOid: String
)

private fun getVardaOrganizerChildRows(
    tx: Database.Transaction,
    evakaPersonId: UUID,
): List<VardaChildOrganizerRow> {
    val sql = """
        SELECT evaka_person_id, varda_person_id, varda_person_oid, varda_child_id, organizer_oid
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

    check(!personPayload.ssn.isNullOrBlank() || !personPayload.personOid.isNullOrBlank()) {
        "VardaUpdate: no ssn or oid for person $evakaPersonId"
    }

    val vardaPerson = try {
        client.createPerson(personPayload)
            ?: client.getPersonFromVardaBySsnOrOid(VardaClient.VardaPersonSearchRequest(personPayload.ssn, personPayload.personOid))
    } catch (e: Exception) {
        error("VardaUpdate: couldn't create nor fetch Varda person $personPayload")
    }

    return createVardaChildWhenPersonExists(
        tx = tx,
        client = client,
        evakaPersonId = evakaPersonId,
        vardaPersonId = vardaPerson!!.vardaId,
        vardaPersonOid = vardaPerson.personOid,
        municipalOrganizerOid = municipalOrganizerOid,
        organizerOid = organizerOid,
        isPaosChild = isPaosChild,
        sourceSystem = sourceSystem
    )
}

data class VardaChildPayload(
    val personUrl: String,
    val personOid: String,
    val organizerOid: String,
    val sourceSystem: String
)

data class VardaPaosChildPayload(
    val personUrl: String,
    val personOid: String,
    val organizerOid: String,
    val paosOrganizationOid: String,
    val sourceSystem: String
)

private fun createVardaChildWhenPersonExists(
    tx: Database.Transaction,
    client: VardaClient,
    evakaPersonId: UUID,
    vardaPersonId: Int,
    vardaPersonOid: String,
    municipalOrganizerOid: String,
    organizerOid: String,
    isPaosChild: Boolean,
    sourceSystem: String
): Long {
    return if (isPaosChild) {
        val vardaChildPayload = VardaPaosChildPayload(
            personUrl = client.getPersonUrl(vardaPersonId.toLong()),
            personOid = vardaPersonOid,
            organizerOid = municipalOrganizerOid,
            paosOrganizationOid = organizerOid,
            sourceSystem = sourceSystem
        )

        val vardaChildId = client.createChild(convertToVardaChildRequest(evakaPersonId, vardaChildPayload))?.id?.toLong()
            ?: error("VardaUpdate: Couldn't create Varda PAOS child for $vardaChildPayload")

        insertVardaOrganizerChild(
            tx,
            evakaPersonId,
            vardaChildId,
            vardaPersonId,
            vardaPersonOid,
            organizerOid
        )

        vardaChildId
    } else {
        val vardaChildPayload = VardaChildPayload(
            personUrl = client.getPersonUrl(vardaPersonId.toLong()),
            personOid = vardaPersonOid,
            organizerOid = municipalOrganizerOid,
            sourceSystem = sourceSystem
        )

        val vardaChildId = client.createChild(convertToVardaChildRequest(evakaPersonId, vardaChildPayload))?.id?.toLong()
            ?: error("VardaUpdate: Couldn't create Varda child for $vardaChildPayload")

        insertVardaOrganizerChild(
            tx,
            evakaPersonId,
            vardaChildId,
            vardaPersonId,
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
                p.oph_person_oid                 AS person_oid,
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
    vardaPersonId: Int,
    vardaPersonOid: String,
    organizerOid: String
) {
    //language=SQL
    val sql = """
    INSERT INTO varda_organizer_child (evaka_person_id, varda_child_id, varda_person_id, varda_person_oid, organizer_oid)
    VALUES (:evakaPersonId, :vardaChildId, :vardaPersonId, :vardaPersonOid, :organizerOid)
    """.trimIndent()

    tx.createUpdate(sql)
        .bind("evakaPersonId", evakaPersonId)
        .bind("vardaChildId", vardaChildId)
        .bind("vardaPersonId", vardaPersonId)
        .bind("vardaPersonOid", vardaPersonOid)
        .bind("organizerOid", organizerOid)
        .execute()
}
