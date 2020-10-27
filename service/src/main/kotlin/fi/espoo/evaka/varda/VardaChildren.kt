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
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

private val vardaMinDate = LocalDate.of(2019, 1, 1)
private val vardaPlacementTypes = listOf(PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME, PlacementType.PRESCHOOL_DAYCARE)

fun getVardaMinDate(): LocalDate = vardaMinDate

fun updateChildren(
    h: Handle,
    client: VardaClient,
    organizerName: String
) {
    createPersons(h, client)
    createChildren(h, client, organizerName)
}

private fun createPersons(
    h: Handle,
    client: VardaClient
) {
    val vardaPersons = getPersonsToUpload(h)
    vardaPersons.forEach { (oid, payload) ->
        val response = client.createPerson(payload)
        if (response != null) {
            initVardaChild(response, oid, payload.id, h)
        }
    }
}

private fun createChildren(h: Handle, client: VardaClient, organizerName: String) {
    initNewChildRows(h)
    val vardaChildren = getChildrenToUpload(client.getPersonUrl, h, organizerName)
    vardaChildren.forEach { vardaChild ->
        client.createChild(vardaChild)
            ?.let { vardaChildResponse -> updateChild(vardaChildResponse, vardaChild.id, h) }
    }
}

private fun getPersonsToUpload(h: Handle): List<Pair<String, VardaPersonRequest>> {
    fun toVardaPersonRequestWithOrganizerOid(): (ResultSet, StatementContext) -> Pair<String, VardaPersonRequest> =
        { rs, _ ->
            Pair(
                rs.getString("oph_organizer_oid"),
                VardaPersonRequest(
                    id = rs.getUUID("id"),
                    firstName = rs.getString("first_name"),
                    lastName = rs.getString("last_name"),
                    nickName = rs.getString("nick_name"),
                    ssn = rs.getString("ssn")
                )
            )
        }

    //language=SQL
    val sql =
        """
        WITH child_unit AS (
            SELECT unit_id, child_id, row_number() OVER (PARTITION BY child_id ORDER BY child_id, start_date) FROM placement 
            WHERE placement.type IN (<placementTypes>)
            AND end_date >= :minDate
        )
        SELECT DISTINCT person.id,
                        person.first_name,
                        person.last_name,
                        person.social_security_number         AS ssn,
                        split_part(person.first_name, ' ', 1) AS nick_name,
                        daycare.oph_organizer_oid
        FROM child_unit cu
            INNER JOIN person ON cu.child_id = person.id AND cu.row_number = 1
            LEFT JOIN varda_child ON person.id = varda_child.person_id 
            INNER JOIN daycare ON cu.unit_id = daycare.id
        WHERE person.social_security_number <> ''
            AND daycare.upload_to_varda = true
            AND varda_child.id IS NULL
        """.trimIndent()

    return h.createQuery(sql)
        .bindList("placementTypes", vardaPlacementTypes)
        .bind("minDate", vardaMinDate)
        .map(toVardaPersonRequestWithOrganizerOid())
        .list()
}

private fun initVardaChild(vardaPersonResponse: VardaPersonResponse, ophOrganizerOid: String, personId: UUID, h: Handle) {
    //language=SQL
    val sql =
        """
        INSERT INTO varda_child (person_id, varda_person_id, varda_person_oid, modified_at, uploaded_at, oph_organizer_oid)
        VALUES (:personId, :vardaId, :personOid, now(), now(), :ophOrganizerOid)
        """.trimIndent()

    h.createUpdate(sql)
        .bind("personId", personId)
        .bind("ophOrganizerOid", ophOrganizerOid)
        .bind("vardaId", vardaPersonResponse.vardaId)
        .bind("personOid", vardaPersonResponse.personOid)
        .execute()
}

private fun initNewChildRows(h: Handle) {
    val sql =
        """
        WITH child_organizer AS (
            SELECT vc.person_id, vc.varda_person_id, vc.varda_person_oid, d.oph_organizer_oid 
            FROM varda_child vc
            JOIN placement p ON vc.person_id = p.child_id     
            JOIN daycare d ON p.unit_id = d.id
        )
        INSERT INTO varda_child (person_id, varda_person_id, varda_person_oid, oph_organizer_oid) 
        SELECT person_id, varda_person_id, varda_person_oid, oph_organizer_oid FROM child_organizer
        ON CONFLICT ON CONSTRAINT unique_varda_child_organizer DO NOTHING;
        """.trimIndent()

    h.createUpdate(sql)
        .execute()
}

private fun getChildrenToUpload(
    getPersonUrl: (Long) -> String,
    h: Handle,
    organizerName: String
): List<VardaChildRequest> {
    //language=SQL
    val sql =
        """
        SELECT vc.id, vc.varda_person_id, vc.oph_organizer_oid, vo.varda_organizer_oid,
        CASE 
            WHEN oph_organizer_oid = varda_organizer_oid
            THEN false
            ELSE true
        END AS is_paos
        FROM varda_child vc
        JOIN varda_organizer vo ON vo.organizer ilike :organizer
        WHERE varda_child_id IS NULL AND oph_organizer_oid IS NOT NULL
        """.trimIndent()

    return h.createQuery(sql)
        .bind("organizer", organizerName)
        .map(toVardaChildRequest(getPersonUrl))
        .list()
}

private fun toVardaChildRequest(
    getPersonUrl: (Long) -> String
): (ResultSet, StatementContext) -> VardaChildRequest =
    { rs, _ ->
        val isPaos = rs.getBoolean("is_paos")
        VardaChildRequest(
            id = rs.getUUID("id"),
            personUrl = getPersonUrl(rs.getLong("varda_person_id")),
            organizerOid = if (isPaos) null else rs.getString("varda_organizer_oid"),
            ownOrganizationOid = if (isPaos) rs.getString("varda_organizer_oid") else null,
            paosOrganizationOid = if (isPaos) rs.getString("oph_organizer_oid") else null
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
    @JsonProperty("vakatoimija_oid")
    val organizerOid: String?,
    @JsonProperty("oma_organisaatio_oid")
    val ownOrganizationOid: String?,
    @JsonProperty("paos_organisaatio_oid")
    val paosOrganizationOid: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaChildResponse(
    @JsonProperty("id")
    val vardaId: Int
)
