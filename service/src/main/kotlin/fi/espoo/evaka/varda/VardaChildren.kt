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

// Add units to the list when rolling into production
// next care areas: Espoon keskukset, Leppävaarat.
private val unitsToUpload = listOf(
    "Eestinmalmin päiväkoti",
    "Kepelin päiväkoti",
    "Kalajärvi-Niipperi perhepäivähoito",
    "Kantokasken esiopetus",
    "Niipperin esiopetus",
    "Tiistilän esiopetus",
    "Tiistilänraitin päiväkoti",
    "Avokkaan päiväkoti",
    "Aamunkoiton päiväkoti",
    "Ellipsin päiväkoti",
    "Friisilän päiväkoti",
    "Kaarnapurren päiväkoti",
    "Kala-Maijan päiväkoti",
    "Kalamiehen päiväkoti",
    "Kultakalat",
    "Kuutamon päiväkoti",
    "Lehtikalat",
    "Lystimäen päiväkoti",
    "Maakirjan päiväkoti",
    "Matinkylän päiväkoti",
    "Matinniityn päiväkoti",
    "Nokkalanpuiston päiväkoti",
    "Nuottakunnan päiväkoti",
    "Nuottaniemen päiväkoti",
    "Olarin päiväkoti",
    "Opinmäen päiväkoti",
    "Planeetan päiväkoti",
    "Päivänsäteet",
    "Suurpellon päiväkoti",
    "Ryhmäperhepäiväkoti Karhunkolo",
    "Ryhmäperhepäiväkoti Kuunsäteet",
    "Ryhmäperhepäiväkoti Pikku-Helena",
    "Ryhmäperhepäiväkoti Pikku-Mirja",
    "Matinkylä-Olari perhepäivähoito",
    "Siriuksen päiväkoti",
    "Päivänkehrän kielikylpyesiopetus",
    "Päivystys 2 (Svebi/Fågelsången)",
    "Päivystys 1 (Svebi/Mattbergets)",
    "Bergans daghem",
    "Fågelsångens daghem",
    "Askbackens daghem",
    "Bemböle förskola",
    "Kungsgårds daghem och förskola",
    "Lagstads daghem och förskola",
    "Fiskargrändens gruppfamiljedaghem",
    "Meteorgatans gruppfamiljedaghem",
    "Portängens gruppfamiljedaghem",
    "Alberga familjedagvård",
    "Boställs daghem och förskola",
    "Karamalmens daghem och förskola",
    "Niperts daghem",
    "Rödskogs förskola",
    "Smedsby daghem och förskola",
    "Finno daghem och förskola",
    "Klippans daghem",
    "Skepparbackens daghem",
    "Storängens daghem och förskola",
    "Distby daghem",
    "Kvis daghem",
    "Mattbergets daghem och förskola",
    "Portängens daghem",
    "Westends daghem",
    "Vindängens daghem och förskola",
    "Esbo centrum -Norra Esbo familjedagvård",
    "Ivisnäs daghem",
    "Kongsbergs daghem",
    "Daghemmet Knatte-Hörnan",
    "Daghemmet Aftonstjärnan",
    "Daghemmet Sockan",
    "Daghemmet Mymlan",
    "Olars daghem",
    "Ryhmäperhepäiväkoti Tuohiset",
    "Silkkiniityn päiväkoti, kielikylpy",
    "Silkkiniityn ryhmäperhepäiväkoti",
    "Haukilahden päiväkoti",
    "Jousenkaaren päiväkoti",
    "Kivimiehen päiväkoti",
    "Koivumankkaan päiväkoti",
    "Laajalahden päiväkoti",
    "Laakakiven päiväkoti",
    "Maarinniityn päiväkoti",
    "Mankkaan päiväkoti",
    "Niittykummun päiväkoti",
    "Otaniemen päiväkoti",
    "Pohjois-Tapiolan päiväkoti",
    "Purolan päiväkoti",
    "Seilimäen päiväkoti",
    "Silkkiniityn päiväkoti",
    "Taavinkylän lastentalo",
    "Tapiolan päiväkoti",
    "Tontunmäen päiväkoti",
    "Toppelundin päiväkoti",
    "Tuohimäen päiväkoti",
    "Westendinpuiston päiväkoti",
    "Taavintupa",
    "Oravanpesä",
    "Mankkaanpuron koulu, esiopetuksen liittyvä varhaiskasvatus",
    "Ryhmäperhepäiväkoti Kalevala",
    "Ryhmäperhepäiväkoti Kardemumma",
    "Ryhmäperhepäiväkoti Oktaavi",
    "Westendinpuiston esiopetus",
    "Espoon kielikylpypäiväkoti",
    "Niittymaan päiväkoti",
    "Päiväkoti Pupuna",
    "Päiväkoti Aurinkorinne",
    "Servin-Maijan päiväkoti",
    "Taavinkylän koulun esiopetus",
    "Tapiola perhepäivähoito",
    "Aallonhuipun päiväkoti",
    "Eestinkallion koulu, esiopetuksen varhaiskasvatus",
    "Eestinmetsän päiväkoti",
    "Espoonlahden päiväkoti",
    "Iivisniemen koulu, esiopetuksen varhaiskasvatus",
    "Iivisniemen päiväkoti",
    "Järvitorpan päiväkoti",
    "Kaitaanniityn päiväkoti",
    "Kaitaa - Soukka perhepäivähoito",
    "Kanta-Espoonlahti perhepäivähoito",
    "Kantokasken esiopetus",
    "Kaskipihan päiväkoti",
    "Kastevuoren päiväkoti",
    "Kipparin päiväkoti",
    "Kurkihirren päiväkoti",
    "Latokasken päiväkoti",
    "Laurinlahden päiväkoti",
    "Lehtikasken päiväkoti",
    "Mainingin esiopetus",
    "Mainingin päiväkoti",
    "Martinkallion esiopetus",
    "Martinmäen päiväkoti",
    "Merenkulkijan päiväkoti",
    "Nöykkiönlaakson koulu, Esiopetus",
    "Nöykkiönlaakson koulun esiopetus",
    "Nöykkiön päiväkoti",
    "Nöykkiö perhepäivähoito",
    "Ohrakasken päiväkoti",
    "Paapuurin päiväkoti",
    "Pisan päiväkoti",
    "Ryhmäperhepäiväkoti Aitola",
    "Ryhmäperhepäiväkoti Lehtikuusi",
    "Ryhmäperhepäiväkoti Pihlaja",
    "Ryhmäperhepäiväkoti Rullavuori",
    "Ryhmäperhepäiväkoti Vaahtera",
    "Saapasaukion päiväkoti",
    "Saunalahden koulu, esiopetuksen varhaiskasvatus",
    "Saunalahden päiväkoti",
    "Saunarannan päiväkoti",
    "Soukan koulu, esiopetuksen varhaiskasvatus",
    "Soukankujan päiväkoti",
    "Suomenojan päiväkoti",
    "Tillinmäen päiväkoti",
    "Yläkartanon päiväkoti"
)
private val vardaMinDate = LocalDate.of(2019, 1, 1)
private val vardaPlacementTypes = listOf(PlacementType.DAYCARE, PlacementType.DAYCARE_PART_TIME, PlacementType.PRESCHOOL_DAYCARE)

fun getVardaMinDate(): LocalDate = vardaMinDate

fun updateChildren(
    h: Handle,
    client: VardaClient,
    unitFilter: Boolean = true
) {
    createPersons(h, client, unitFilter)
    createChildren(h, client)
}

private fun createPersons(
    h: Handle,
    client: VardaClient,
    unitFilter: Boolean
) {
    val units = if (unitFilter) unitsToUpload else emptyList()
    val vardaPersons = getPersonsToUpload(h, units)
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

private fun getPersonsToUpload(h: Handle, units: List<String> = emptyList()): List<VardaPersonRequest> {
    val unitQuery = if (units.isNotEmpty()) "AND daycare.name IN (<units>)" else ""
    // TODO: join to daycare can be removed after varda is in production fully
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
            LEFT JOIN daycare ON varda_unit.evaka_daycare_id = daycare.id
        WHERE placement.type IN (<placementTypes>)
            AND varda_child.id IS NULL
            AND placement.end_date >= :minDate
            AND person.social_security_number <> ''
            AND varda_unit.evaka_daycare_id IS NOT NULL 
            $unitQuery
        """.trimIndent()

    return h.createQuery(sql)
        .bindList("placementTypes", vardaPlacementTypes)
        .bind("minDate", vardaMinDate)
        .let { query -> if (units.isNotEmpty()) query.bindList("units", units) else query }
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
