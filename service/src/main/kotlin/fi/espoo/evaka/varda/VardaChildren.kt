// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.getUUID
import fi.espoo.evaka.varda.integration.VardaClient
import mu.KotlinLogging
import org.jdbi.v3.core.statement.StatementContext
import java.sql.ResultSet
import java.time.LocalDate
import java.util.UUID

private val logger = KotlinLogging.logger {}

fun getVardaMinDate(): LocalDate = vardaMinDate

fun updateChildren(db: Database.Connection, client: VardaClient, organizerName: String) {
    createPersons(db, client)
    createChildren(db, client, organizerName)
}

private fun createPersons(db: Database.Connection, client: VardaClient) {
    val vardaPersons = db.read { getPersonsToUpload(it) }
    logger.info { "Varda: Creating ${vardaPersons.size} new people" }
    vardaPersons.forEach { (oid, payload) ->
        val response = client.createPerson(payload)
        if (response != null) {
            db.transaction { initVardaChild(it, response, oid, payload.id) }
        }
    }
}

private fun createChildren(db: Database.Connection, client: VardaClient, organizerName: String) {
    db.transaction { initNewChildRows(it) }
    val vardaChildren = db.read { getChildrenToUpload(it, client.getPersonUrl, organizerName, client.sourceSystem) }
    logger.info { "Varda: Creating ${vardaChildren.size} new children" }
    vardaChildren.forEach { vardaChild ->
        client.createChild(vardaChild)
            ?.let { vardaChildResponse -> db.transaction { updateChild(it, vardaChildResponse, vardaChild.id) } }
    }
}

private fun getPersonsToUpload(tx: Database.Read): List<Pair<String, VardaPersonRequest>> {
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
            WHERE placement.type = ANY(:placementTypes::placement_type[])
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
            AND (person.first_name <> '' AND person.last_name <> '')
            AND daycare.upload_to_varda = true
            AND varda_child.id IS NULL
        """.trimIndent()

    return tx.createQuery(sql)
        .bind("placementTypes", vardaPlacementTypes)
        .bind("minDate", vardaMinDate)
        .map(toVardaPersonRequestWithOrganizerOid())
        .list()
}

private fun initVardaChild(
    tx: Database.Transaction,
    vardaPersonResponse: VardaPersonResponse,
    ophOrganizerOid: String,
    personId: UUID
) {
    //language=SQL
    val sql =
        """
        INSERT INTO varda_child (person_id, varda_person_id, varda_person_oid, modified_at, uploaded_at, oph_organizer_oid)
        VALUES (:personId, :vardaId, :personOid, now(), now(), :ophOrganizerOid)
        """.trimIndent()

    tx.createUpdate(sql)
        .bind("personId", personId)
        .bind("ophOrganizerOid", ophOrganizerOid)
        .bind("vardaId", vardaPersonResponse.vardaId)
        .bind("personOid", vardaPersonResponse.personOid)
        .execute()
}

private fun initNewChildRows(tx: Database.Transaction) {
    // language=SQL
    val sql =
        """
        WITH child_organizer AS (
            SELECT vc.person_id, vc.varda_person_id, vc.varda_person_oid, d.oph_organizer_oid 
            FROM varda_child vc
            JOIN placement p ON vc.person_id = p.child_id     
            JOIN daycare d ON p.unit_id = d.id AND d.upload_to_varda = true AND d.oph_organizer_oid IS NOT NULL AND char_length(trim(d.oph_organizer_oid)) > 14
        )
        INSERT INTO varda_child (person_id, varda_person_id, varda_person_oid, oph_organizer_oid) 
        SELECT person_id, varda_person_id, varda_person_oid, oph_organizer_oid FROM child_organizer
        ON CONFLICT ON CONSTRAINT unique_varda_child_organizer DO NOTHING;
        """.trimIndent()

    tx.createUpdate(sql)
        .execute()
}

private fun getChildrenToUpload(
    tx: Database.Read,
    getPersonUrl: (Long) -> String,
    organizerName: String,
    sourceSystem: String
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

    return tx.createQuery(sql)
        .bind("organizer", organizerName)
        .map(toVardaChildRequest(getPersonUrl, sourceSystem))
        .list()
}

private fun toVardaChildRequest(
    getPersonUrl: (Long) -> String,
    sourceSystem: String
): (ResultSet, StatementContext) -> VardaChildRequest =
    { rs, _ ->
        val isPaos = rs.getBoolean("is_paos")
        VardaChildRequest(
            id = rs.getUUID("id"),
            personUrl = getPersonUrl(rs.getLong("varda_person_id")),
            organizerOid = if (isPaos) null else rs.getString("varda_organizer_oid"),
            ownOrganizationOid = if (isPaos) rs.getString("varda_organizer_oid") else null,
            paosOrganizationOid = if (isPaos) rs.getString("oph_organizer_oid") else null,
            sourceSystem = sourceSystem
        )
    }

private fun updateChild(tx: Database.Transaction, vardaChildResponse: VardaChildResponse, id: UUID) {
    //language=SQL
    val sql =
        """
        UPDATE varda_child SET varda_child_id = :vardaChildId, uploaded_at = now() WHERE id = :id
        """.trimIndent()

    tx.createUpdate(sql)
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
    val paosOrganizationOid: String?,
    @JsonProperty("lahdejarjestelma")
    val sourceSystem: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class VardaChildResponse(
    @JsonProperty("id")
    val vardaId: Int
)
