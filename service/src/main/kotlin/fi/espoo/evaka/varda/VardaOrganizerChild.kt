// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.shared.ChildId
import fi.espoo.evaka.shared.PersonId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.varda.integration.VardaClient
import kotlin.Exception

fun getOrCreateVardaChildByOrganizer(
    db: Database.Connection,
    client: VardaClient,
    evakaPersonId: ChildId,
    organizerOid: String,
    sourceSystem: String,
    municipalOrganizerOid: String
): Long {
    return db.transaction { tx ->
        val isPaosChild = organizerOid != municipalOrganizerOid

        val rowsByChild = getVardaOrganizerChildRows(tx, evakaPersonId)
        if (rowsByChild.isEmpty()) {
            return@transaction createVardaPersonAndChild(
                tx,
                client,
                evakaPersonId,
                municipalOrganizerOid,
                organizerOid,
                isPaosChild,
                sourceSystem
            )
        }

        val vardaPerson = rowsByChild.first()

        val rowsByChildAndOrganizer = rowsByChild.filter { row -> row.organizerOid == organizerOid }
        if (rowsByChildAndOrganizer.isEmpty()) {
            return@transaction createVardaChildWhenPersonExists(
                tx,
                client,
                evakaPersonId,
                vardaPerson.vardaPersonId,
                vardaPerson.vardaPersonOid,
                municipalOrganizerOid,
                organizerOid,
                isPaosChild,
                sourceSystem
            )
        }

        return@transaction rowsByChildAndOrganizer.first().vardaChildId
    }
}

data class VardaChildOrganizerRow(
    val evakaPersonId: ChildId,
    val vardaPersonId: Int,
    val vardaPersonOid: String,
    val vardaChildId: Long,
    val organizerOid: String
)

private fun getVardaOrganizerChildRows(
    tx: Database.Transaction,
    evakaPersonId: ChildId
): List<VardaChildOrganizerRow> {
    val sql =
        """
        SELECT evaka_person_id, varda_person_id, varda_person_oid, varda_child_id, organizer_oid
        FROM varda_organizer_child
        WHERE evaka_person_id = :evakaPersonId
    """
            .trimIndent()

    return tx.createQuery(sql).bind("evakaPersonId", evakaPersonId).toList<VardaChildOrganizerRow>()
}

private fun createVardaPersonAndChild(
    tx: Database.Transaction,
    client: VardaClient,
    evakaPersonId: ChildId,
    municipalOrganizerOid: String,
    organizerOid: String,
    isPaosChild: Boolean,
    sourceSystem: String
): Long {
    val personPayload = getVardaPersonPayload(tx, evakaPersonId, organizerOid)

    check(!personPayload.ssn.isNullOrBlank() || !personPayload.personOid.isNullOrBlank()) {
        "VardaUpdate: no ssn or oid for person $evakaPersonId"
    }

    val vardaPerson =
        try {
            client.createPerson(personPayload.toVardaPersonRequest())
        } catch (e: Exception) {
            client.getPersonFromVardaBySsnOrOid(
                VardaClient.VardaPersonSearchRequest(
                    henkilotunnus = personPayload.ssn,
                    henkilo_oid =
                        if (personPayload.ssn.isNullOrBlank()) personPayload.personOid else null,
                    etunimet = personPayload.firstName,
                    kutsumanimi = personPayload.nickName.ifEmpty { personPayload.firstName },
                    sukunimi = personPayload.lastName
                )
            )
        } catch (e: Exception) {
            error("VardaUpdate: couldn't create nor fetch Varda person $personPayload")
        }

    return createVardaChildWhenPersonExists(
        tx = tx,
        client = client,
        evakaChildId = evakaPersonId,
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
) {
    fun toVardaChildRequest(evakaChildId: ChildId) =
        VardaChildRequest(
            id = evakaChildId,
            henkilo = personUrl,
            henkilo_oid = personOid,
            vakatoimija_oid = organizerOid,
            oma_organisaatio_oid = null,
            paos_organisaatio_oid = null,
            lahdejarjestelma = sourceSystem
        )
}

data class VardaPaosChildPayload(
    val personUrl: String,
    val personOid: String,
    val organizerOid: String,
    val paosOrganizationOid: String,
    val sourceSystem: String
) {
    fun toVardaChildRequest(evakaChildId: ChildId) =
        VardaChildRequest(
            id = evakaChildId,
            henkilo = personUrl,
            henkilo_oid = personOid,
            vakatoimija_oid = null,
            oma_organisaatio_oid = organizerOid,
            paos_organisaatio_oid = paosOrganizationOid,
            lahdejarjestelma = sourceSystem
        )
}

private fun createVardaChildWhenPersonExists(
    tx: Database.Transaction,
    client: VardaClient,
    evakaChildId: ChildId,
    vardaPersonId: Int,
    vardaPersonOid: String,
    municipalOrganizerOid: String,
    organizerOid: String,
    isPaosChild: Boolean,
    sourceSystem: String
): Long {
    return if (isPaosChild) {
        val vardaChildPayload =
            VardaPaosChildPayload(
                personUrl = client.getPersonUrl(vardaPersonId.toLong()),
                personOid = vardaPersonOid,
                organizerOid = municipalOrganizerOid,
                paosOrganizationOid = organizerOid,
                sourceSystem = sourceSystem
            )

        val vardaChildId =
            try {
                client.createChild(vardaChildPayload.toVardaChildRequest(evakaChildId)).id.toLong()
            } catch (e: Exception) {
                error("VardaUpdate: failed to create PAOS child: ${e.message}")
            }

        insertVardaOrganizerChild(
            tx,
            evakaChildId,
            vardaChildId,
            vardaPersonId,
            vardaPersonOid,
            organizerOid
        )

        vardaChildId
    } else {
        val vardaChildPayload =
            VardaChildPayload(
                personUrl = client.getPersonUrl(vardaPersonId.toLong()),
                personOid = vardaPersonOid,
                organizerOid = municipalOrganizerOid,
                sourceSystem = sourceSystem
            )

        val vardaChildId =
            try {
                client.createChild(vardaChildPayload.toVardaChildRequest(evakaChildId)).id.toLong()
            } catch (e: Exception) {
                error("VardaUpdate: failed to create child: ${e.message}")
            }

        insertVardaOrganizerChild(
            tx,
            evakaChildId,
            vardaChildId,
            vardaPersonId,
            vardaPersonOid,
            organizerOid
        )

        vardaChildId
    }
}

private fun getVardaPersonPayload(
    tx: Database.Transaction,
    evakaChildId: ChildId,
    organizerOid: String
) =
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
        """
                .trimIndent()
        )
        .bind("evakaPersonId", evakaChildId)
        .bind("organizerOid", organizerOid)
        .toList<VardaPerson>()
        .first()

fun insertVardaOrganizerChild(
    tx: Database.Transaction,
    evakaPersonId: ChildId,
    vardaChildId: Long,
    vardaPersonId: Int,
    vardaPersonOid: String,
    organizerOid: String
) {
    // language=SQL
    val sql =
        """
    INSERT INTO varda_organizer_child (evaka_person_id, varda_child_id, varda_person_id, varda_person_oid, organizer_oid)
    VALUES (:evakaPersonId, :vardaChildId, :vardaPersonId, :vardaPersonOid, :organizerOid)
    """
            .trimIndent()

    tx.createUpdate(sql)
        .bind("evakaPersonId", evakaPersonId)
        .bind("vardaChildId", vardaChildId)
        .bind("vardaPersonId", vardaPersonId)
        .bind("vardaPersonOid", vardaPersonOid)
        .bind("organizerOid", organizerOid)
        .execute()
}

data class VardaPerson(
    val id: PersonId,
    val firstName: String,
    val lastName: String,
    val nickName: String,
    val ssn: String? = null,
    val personOid: String? = null
) {
    fun toVardaPersonRequest() =
        VardaPersonRequest(
            id = id,
            etunimet = firstName,
            sukunimi = lastName,
            kutsumanimi = nickName,
            henkilotunnus = ssn,
            henkilo_oid = if (ssn.isNullOrBlank()) personOid else null
        )
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaPersonRequest(
    val id: PersonId,
    val etunimet: String,
    val sukunimi: String,
    val kutsumanimi: String,
    @JsonInclude(JsonInclude.Include.NON_NULL) val henkilotunnus: String? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val henkilo_oid: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaPersonResponse(
    val url: String,
    @JsonProperty("id") val vardaId: Int,
    @JsonProperty("etunimet") val firstName: String,
    @JsonProperty("sukunimi") val lastName: String,
    @JsonProperty("kutsumanimi") val nickName: String,
    @JsonProperty("henkilo_oid") val personOid: String,
    @JsonProperty("syntyma_pvm") val dob: String?,
    @JsonProperty("lapsi") val child: List<String>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaChildRequest(
    val id: ChildId,
    val henkilo: String? = null,
    val henkilo_oid: String? = null,
    val vakatoimija_oid: String?,
    val oma_organisaatio_oid: String?,
    val paos_organisaatio_oid: String?,
    val lahdejarjestelma: String
)

@JsonIgnoreProperties(ignoreUnknown = true) data class VardaChildResponse(val id: Int)
