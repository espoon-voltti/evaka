// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.placement.PlacementType
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.varda.integration.VardaClient
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

private val vardaMinDate = LocalDate.of(2019, 1, 1)
private val vardaPlacementTypes = listOf(PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME, PlacementType.PRESCHOOL_DAYCARE)

fun getVardaMinDate(): LocalDate = vardaMinDate

fun updateChildren(
    h: Handle,
    client: VardaClient
) {
    createPersons(h, client)
    createChildren(h, client)
}

private fun createPersons(
    h: Handle,
    client: VardaClient
) {
    val vardaPersons = getPersonsToUpload(h)
    vardaPersons.forEach {
        val response = client.createPerson(it)
        if (response != null) {
            insertPerson(response, it.id, h)
        }
    }
}

private fun createChildren(h: Handle, client: VardaClient) {
    val vardaChildren = getChildrenToUpload(client.getPersonUrl, client.getOrganizerUrl, h)
    vardaChildren.forEach { vardaChild ->
        client.createChild(vardaChild)
            ?.let { vardaChildResponse -> updateChild(vardaChildResponse, vardaChild.id, h) }
    }
}

private fun getPersonsToUpload(h: Handle): List<VardaPersonRequest> {
    //language=SQL
    val sql =
        """
        SELECT DISTINCT person.id,
                        person.first_name,
                        person.last_name,
                        person.social_security_number         AS ssn,
                        split_part(person.first_name, ' ', 1) AS nick_name
        FROM person
            INNER JOIN placement ON placement.child_id = person.id
            LEFT JOIN varda_child ON person.id = varda_child.person_id
            INNER JOIN varda_unit ON placement.unit_id = varda_unit.evaka_daycare_id
            INNER JOIN daycare ON varda_unit.evaka_daycare_id = daycare.id
        WHERE placement.type IN (<placementTypes>)
            AND varda_child.id IS NULL
            AND placement.end_date >= :minDate
            AND person.social_security_number <> ''
            AND varda_unit.evaka_daycare_id IS NOT NULL 
            AND daycare.upload_to_varda = true
        """.trimIndent()
    return h.createQuery(sql)
        .bindList("placementTypes", vardaPlacementTypes)
        .bind("minDate", vardaMinDate)
        .mapTo<VardaPersonRequest>()
        .list()
}

private fun insertPerson(vardaPersonResponse: VardaPersonResponse, personId: UUID, h: Handle) {
    //language=SQL
    val sql =
        """
        INSERT INTO varda_child (person_id, varda_person_id, varda_person_oid, modified_at, uploaded_at)
        VALUES (:personId, :vardaId, :personOid, now(), now())
        """.trimIndent()

    h.createUpdate(sql)
        .bind("personId", personId)
        .bind("vardaId", vardaPersonResponse.vardaId)
        .bind("personOid", vardaPersonResponse.personOid)
        .execute()
}

private fun getChildrenToUpload(
    getPersonUrl: (Long) -> String,
    getOrganizerUrl: (Long) -> String,
    h: Handle
): List<VardaChildRequest> {
    //language=SQL
    val sql =
        """
        SELECT varda_child.id, varda_person_id, varda_organizer.varda_organizer_id
            FROM varda_child
            LEFT JOIN LATERAL (SELECT varda_organizer_id FROM varda_organizer WHERE organizer = 'Espoo') varda_organizer
                ON TRUE
            WHERE varda_child.varda_child_id IS NULL;
        """.trimIndent()

    return h.createQuery(sql)
        .map(toVardaChildRequest(getPersonUrl, getOrganizerUrl))
        .list()
}

private fun toVardaChildRequest(
    getPersonUrl: (Long) -> String,
    getOrganizerUrl: (Long) -> String
): (ResultSet, StatementContext) -> VardaChildRequest =
    { rs, _ ->
        VardaChildRequest(
            id = rs.getUUID("id"),
            personUrl = getPersonUrl(rs.getLong("varda_person_id")),
            organizerUrl = getOrganizerUrl(rs.getLong("varda_organizer_id"))
        )
    }

private fun updateChild(vardaChildResponse: VardaChildResponse, id: UUID, h: Handle) {
    //language=SQL
    val sql =
        """
        UPDATE varda_child SET varda_child_id = :vardaChildId, uploaded_at = now() WHERE id = :id
        """.trimIndent()

    h.createUpdate(sql)
        .bind("vardaChildId", vardaChildResponse.vardaId)
        .bind("id", id)
        .execute()
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaPersonRequest(
    val id: UUID,
    @JsonProperty("etunimet")
    val firstName: String,
    @JsonProperty("sukunimi")
    val lastName: String,
    @JsonProperty("kutsumanimi")
    val nickName: String,
    @JsonProperty("henkilotunnus")
    val ssn: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaPersonResponse(
    val url: String,
    @JsonProperty("id")
    val vardaId: Int,
    @JsonProperty("etunimet")
    val firstName: String,
    @JsonProperty("sukunimi")
    val lastName: String,
    @JsonProperty("kutsumanimi")
    val nickName: String,
    @JsonProperty("henkilo_oid")
    val personOid: String,
    @JsonProperty("syntyma_pvm")
    val dob: String?,
    @JsonProperty("lapsi")
    val child: List<String>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaChildRequest(
    val id: UUID,
    @JsonProperty("henkilo")
    val personUrl: String,
    @JsonProperty("vakatoimija")
    val organizerUrl: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaChildResponse(
    @JsonProperty("id")
    val vardaId: Int
)
