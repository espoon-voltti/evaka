// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.varda.integration.VardaClient
import mu.KotlinLogging
import org.jdbi.v3.core.kotlin.mapTo
import java.util.UUID

private val logger = KotlinLogging.logger {}

val unitTypesToUpload = listOf(VardaUnitProviderType.MUNICIPAL, VardaUnitProviderType.MUNICIPAL_SCHOOL)

fun updateUnits(db: Database.Connection, client: VardaClient, organizerName: String) {
    val units = db.read { getNewOrStaleUnits(it, organizerName) }
    logger.info { "Varda: Sending ${units.size} new or updated units" }
    units.forEach { unit ->
        val response =
            if (unit.vardaUnitId == null) client.createUnit(unit)
            else client.updateUnit(unit)

        response?.let { (vardaUnitId, ophUnitOid) ->
            db.transaction { setUnitUploaded(it, unit.copy(vardaUnitId = vardaUnitId, ophUnitOid = ophUnitOid)) }
        }
    }
}

fun getNewOrStaleUnits(tx: Database.Read, organizerName: String): List<VardaUnit> {
    //language=SQL
    val sql =
        """
        SELECT
            daycare.id AS evakaDaycareId,
            varda_unit.varda_unit_id AS vardaUnitId,
            daycare.oph_unit_oid AS ophUnitOid,
            (SELECT url from varda_organizer WHERE organizer = :organizer) AS organizer,
            (SELECT municipality_code from varda_organizer WHERE organizer = :organizer) AS municipalityCode,
            daycare.name AS name,
            daycare.street_address AS address,
            daycare.postal_code AS postalCode,
            daycare.post_office AS postOffice,
            daycare.mailing_po_box AS mailingStreetAddress,
            daycare.mailing_postal_code AS mailingPostalCode,
            daycare.mailing_post_office AS mailingPostOffice,
            unit_manager.phone AS phoneNumber,
            unit_manager.email AS email,
            daycare.capacity AS capacity,
            daycare.provider_type AS unitProviderType,
            daycare.type AS unitType,
            daycare.language AS language,
            daycare.language_emphasis_id AS languageEmphasisId,
            daycare.opening_date AS openingDate,
            daycare.closing_date AS closingDate
        FROM daycare
        LEFT JOIN varda_unit ON varda_unit.evaka_daycare_id = daycare.id
        LEFT JOIN unit_manager ON daycare.unit_manager_id = unit_manager.id
        WHERE daycare.upload_to_varda IS TRUE
            AND (
                GREATEST(daycare.updated, daycare.closing_date) > varda_unit.uploaded_at OR
                daycare.id NOT IN (SELECT evaka_daycare_id from varda_unit) OR
                daycare.oph_unit_oid IS NULL
            )
            AND daycare.provider_type IN (<providerTypes>)
        """.trimIndent()

    return tx.createQuery(sql)
        .bind("organizer", organizerName)
        .bindList("providerTypes", unitTypesToUpload)
        .mapTo<VardaUnit>()
        .toList()
}

fun setUnitUploaded(tx: Database.Transaction, vardaUnit: VardaUnit) {
    //language=SQL
    val sql =
        """
    INSERT INTO varda_unit (evaka_daycare_id, varda_unit_id, uploaded_at)
    VALUES (:evakaDaycareId, :vardaUnitId, now())
    ON CONFLICT (evaka_daycare_id)
    DO UPDATE SET varda_unit_id = :vardaUnitId, uploaded_at = now();
        """.trimIndent()

    tx.createUpdate(sql)
        .bind("evakaDaycareId", vardaUnit.evakaDaycareId)
        .bind("vardaUnitId", vardaUnit.vardaUnitId)
        .execute()

    val sql2 = "UPDATE daycare SET oph_unit_oid = :ophUnitOid WHERE daycare.id = :evakaDaycareId;"

    tx.createUpdate(sql2)
        .bind("evakaDaycareId", vardaUnit.evakaDaycareId)
        .bind("ophUnitOid", vardaUnit.ophUnitOid)
        .execute()
}

// https://virkailija.opintopolku.fi/koodisto-service/rest/json/vardajarjestamismuoto/koodi
enum class VardaUnitProviderType(val vardaCode: String) {
    MUNICIPAL("jm01"),
    MUNICIPAL_SCHOOL("jm01"),
    PURCHASED("jm02"),
    PRIVATE_SERVICE_VOUCHER("jm03"),
    PRIVATE("jm04")
}

// https://virkailija.opintopolku.fi/koodisto-service/rest/json/vardatoimintamuoto/koodi
enum class VardaUnitType(val vardaCode: String) {
    CENTRE("tm01"),
    PRESCHOOL("tm01"),
    PREPARATORY_EDUCATION(""),
    FAMILY("tm02"),
    GROUP_FAMILY("tm03")
}

//  https://virkailija.opintopolku.fi/koodisto-service/rest/json/kieli/koodi
enum class VardaLanguage(val vardaCode: String) {
    fi("FI"),
    sv("SV"),
    en("EN")
}

// https://virkailija.opintopolku.fi/koodisto-service/rest/json/vardakasvatusopillinenjarjestelma/koodi
enum class VardaUnitEducationSystem(val vardaCode: String) {
    STEINER("kj01"),
    MONTESSORI("kj02"),
    FREINET("kj03"),
    REGGIO_EMILIA("kj04"),
    FREIRE("kj05"),
    NONE("kj98"),
    OTHER("kj99")
}

data class VardaUnitResponse(
    @JsonProperty("id")
    var vardaUnitId: Long,
    @JsonProperty("organisaatio_oid")
    val ophUnitOid: String
)

@JsonPropertyOrder(
    "id", "organisaatio_oid", "vakajarjestaja", "kayntiosoite", "postiosoite", "nimi",
    "kayntiosoite_postinumero", "kayntiosoite_postitoimipaikka", "postinumero", "postitoimipaikka", "kunta_koodi",
    "puhelinnumero", "sahkopostiosoite", "kasvatusopillinen_jarjestelma_koodi",
    "toimintamuoto_koodi", "asiointikieli_koodi", "jarjestamismuoto_koodi", "varhaiskasvatuspaikat",
    "toiminnallinenpainotus_kytkin", "kielipainotus_kytkin", "alkamis_pvm", "paattymis_pvm"
)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
data class VardaUnit(
    @JsonProperty("id")
    var vardaUnitId: Long?,
    @JsonProperty("organisaatio_oid")
    val ophUnitOid: String?,
    @JsonIgnore
    val evakaDaycareId: UUID?,
    @JsonProperty("vakajarjestaja")
    var organizer: String?,
    @JsonProperty("nimi")
    val name: String?,
    @JsonProperty("kayntiosoite")
    val address: String?,
    @JsonProperty("kayntiosoite_postinumero")
    val postalCode: String?,
    @JsonProperty("kayntiosoite_postitoimipaikka")
    val postOffice: String?,
    @JsonProperty("postiosoite")
    val mailingStreetAddress: String?,
    @JsonProperty("postinumero")
    val mailingPostalCode: String?,
    @JsonProperty("postitoimipaikka")
    val mailingPostOffice: String?,
    @JsonProperty("puhelinnumero")
    val phoneNumber: String?,
    @JsonProperty("sahkopostiosoite")
    val email: String?,
    @JsonProperty("varhaiskasvatuspaikat")
    val capacity: Int,
    @JsonIgnore
    val unitProviderType: VardaUnitProviderType,
    @JsonIgnore
    val unitType: List<VardaUnitType>,
    @JsonIgnore
    val language: VardaLanguage,
    @JsonIgnore
    val languageEmphasisId: UUID?,
    @JsonProperty("alkamis_pvm")
    val openingDate: String?,
    @JsonProperty("paattymis_pvm")
    val closingDate: String?,
    @JsonProperty("kunta_koodi")
    val municipalityCode: String
) {
    // Espoo always false
    @JsonProperty("toiminnallinenpainotus_kytkin")
    fun getUnitEmphasisSwitch(): Boolean {
        return false
    }

    // Espoo always kj98
    @JsonProperty("kasvatusopillinen_jarjestelma_koodi")
    fun getEducationSystemCode(): String {
        return VardaUnitEducationSystem.NONE.vardaCode
    }

    // Varda needs a list of codes but evaka uses only one code
    @JsonProperty("jarjestamismuoto_koodi")
    fun getUnitProviderTypeAsList(): List<String> {
        return listOfNotNull(unitProviderType.vardaCode)
    }

    // Varda needs a single code and we use some types that are not in varda
    // Before we send units to Varda we filter out units that have "" as a unit type
    @JsonProperty("toimintamuoto_koodi")
    fun getUnitTypeAsString(): String? {
        return when {
            unitType.contains(VardaUnitType.CENTRE) -> {
                VardaUnitType.CENTRE.vardaCode
            }
            unitType.contains(VardaUnitType.FAMILY) -> {
                VardaUnitType.FAMILY.vardaCode
            }
            unitType.contains(VardaUnitType.GROUP_FAMILY) -> {
                VardaUnitType.GROUP_FAMILY.vardaCode
            }
            unitType.contains(VardaUnitType.PRESCHOOL) -> {
                VardaUnitType.PRESCHOOL.vardaCode
            }
            else -> null
        }
    }

    // Varda want's a list of languages
    @JsonProperty("asiointikieli_koodi")
    fun getLanguageAsList(): List<String> {
        return listOfNotNull(language.vardaCode)
    }

    // Varda needs true or false / Evaka has UUID or null
    @JsonProperty("kielipainotus_kytkin")
    fun getLanguageEmphasisSwitch(): Boolean {
        if (languageEmphasisId == null) {
            return false
        }
        return true
    }
}
