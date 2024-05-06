// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.annotation.JsonInclude
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.util.UUID
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

val unitTypesToUpload =
    listOf(VardaUnitProviderType.MUNICIPAL, VardaUnitProviderType.MUNICIPAL_SCHOOL)

interface VardaUnitClient {
    fun createUnit(unit: VardaUnitRequest): VardaUnitResponse

    fun updateUnit(id: Long, unit: VardaUnitRequest): VardaUnitResponse
}

fun updateUnits(
    db: Database.Connection,
    clock: EvakaClock,
    client: VardaUnitClient,
    lahdejarjestelma: String,
    kuntakoodi: String,
    vakajarjestajaUrl: String
) {
    val startTime = clock.now()
    val units = db.read { getNewOrStaleUnits(it) }

    logger.info { "Sending ${units.size} new or updated units to Varda" }
    units.forEach { unit ->
        try {
            val request =
                unit.toVardaUnitRequest(
                    lahdejarjestelma = lahdejarjestelma,
                    vakajarjestaja = vakajarjestajaUrl,
                    kuntakoodi = kuntakoodi
                )
            val response =
                if (unit.vardaUnitId == null) {
                    client.createUnit(request)
                } else {
                    client.updateUnit(unit.vardaUnitId, request)
                }
            db.transaction {
                setUnitUploaded(
                    tx = it,
                    now = startTime,
                    unitId = unit.evakaDaycareId,
                    vardaUnitId = response.id,
                    ophUnitOid = response.organisaatio_oid
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to update unit ${unit.name}: $e", e)
            db.transaction {
                setUnitUploadFailed(
                    tx = it,
                    now = startTime,
                    unitId = unit.evakaDaycareId,
                    error = e.localizedMessage
                )
            }
        }
    }
}

fun getNewOrStaleUnits(
    tx: Database.Read,
): List<VardaUnit> {
    return tx.createQuery {
            sql(
                """
                SELECT
                    daycare.id AS evakaDaycareId,
                    varda_unit.varda_unit_id AS vardaUnitId,
                    daycare.oph_unit_oid AS ophUnitOid,
                    daycare.name AS name,
                    daycare.street_address AS address,
                    daycare.postal_code AS postalCode,
                    daycare.post_office AS postOffice,
                    daycare.mailing_po_box AS mailingStreetAddress,
                    daycare.mailing_postal_code AS mailingPostalCode,
                    daycare.mailing_post_office AS mailingPostOffice,
                    daycare.unit_manager_phone AS phoneNumber,
                    daycare.unit_manager_email AS email,
                    daycare.capacity AS capacity,
                    daycare.provider_type AS unitProviderType,
                    daycare.type AS unitType,
                    daycare.language AS language,
                    daycare.language_emphasis_id AS languageEmphasisId,
                    daycare.opening_date AS openingDate,
                    daycare.closing_date AS closingDate
                FROM daycare
                LEFT JOIN varda_unit ON varda_unit.evaka_daycare_id = daycare.id
                WHERE daycare.upload_to_varda IS TRUE
                    AND (
                        varda_unit.last_success_at IS NULL OR
                        daycare.updated > varda_unit.last_success_at OR
                        daycare.oph_unit_oid IS NULL
                    )
                    AND daycare.provider_type = ANY(${bind(unitTypesToUpload)})
                """
            )
        }
        .toList<VardaUnit>()
}

fun setUnitUploaded(
    tx: Database.Transaction,
    now: HelsinkiDateTime,
    unitId: DaycareId,
    vardaUnitId: Long?,
    ophUnitOid: String?
) {
    tx.createUpdate {
            sql(
                """
                INSERT INTO varda_unit (evaka_daycare_id, varda_unit_id, last_success_at, errored_at, error)
                VALUES (${bind(unitId)}, ${bind(vardaUnitId)}, ${bind(now)}, NULL, NULL)
                ON CONFLICT (evaka_daycare_id)
                DO UPDATE SET varda_unit_id = ${bind(vardaUnitId)}, last_success_at = ${bind(now)}, errored_at = NULL, error = NULL
                """
            )
        }
        .execute()

    tx.createUpdate {
            sql(
                "UPDATE daycare SET oph_unit_oid = ${bind(ophUnitOid)} WHERE daycare.id = ${bind(unitId)}"
            )
        }
        .execute()
}

fun setUnitUploadFailed(
    tx: Database.Transaction,
    now: HelsinkiDateTime,
    unitId: DaycareId,
    error: String
) {
    tx.createUpdate {
            sql(
                """
                INSERT INTO varda_unit (evaka_daycare_id, varda_unit_id, last_success_at, errored_at, error)
                VALUES (${bind(unitId)}, NULL, NULL, ${bind(now)}, ${bind(error)})
                ON CONFLICT (evaka_daycare_id)
                DO UPDATE SET errored_at = ${bind(now)}, error = ${bind(error)}
                """
            )
        }
        .execute()
}

// https://virkailija.opintopolku.fi/koodisto-service/rest/json/vardajarjestamismuoto/koodi
enum class VardaUnitProviderType(val vardaCode: String) : DatabaseEnum {
    MUNICIPAL("jm01"),
    MUNICIPAL_SCHOOL("jm01"),
    PURCHASED("jm02"),
    EXTERNAL_PURCHASED("jm02"),
    PRIVATE_SERVICE_VOUCHER("jm03"),
    PRIVATE("jm04");

    override val sqlType: String = "unit_provider_type"

    companion object {
        fun fromEvakaProviderType(providerType: ProviderType) =
            when (providerType) {
                ProviderType.MUNICIPAL -> MUNICIPAL
                ProviderType.MUNICIPAL_SCHOOL -> MUNICIPAL_SCHOOL
                ProviderType.PURCHASED -> PURCHASED
                ProviderType.EXTERNAL_PURCHASED -> EXTERNAL_PURCHASED
                ProviderType.PRIVATE_SERVICE_VOUCHER -> PRIVATE_SERVICE_VOUCHER
                ProviderType.PRIVATE -> PRIVATE
            }
    }
}

// https://virkailija.opintopolku.fi/koodisto-service/rest/json/vardatoimintamuoto/koodi
enum class VardaUnitType(val vardaCode: String) {
    CENTRE("tm01"),
    PRESCHOOL("tm01"),
    PREPARATORY_EDUCATION(""),
    FAMILY("tm02"),
    GROUP_FAMILY("tm03"),
    CLUB("")
}

//  https://virkailija.opintopolku.fi/koodisto-service/rest/json/kieli/koodi
enum class VardaLanguage(val vardaCode: String) {
    FI("FI"),
    SV("SV"),
    EN("EN")
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

data class VardaUnitResponse(val id: Long, val organisaatio_oid: String)

data class VardaUnit(
    val vardaUnitId: Long?,
    val ophUnitOid: String?,
    val name: String?,
    val address: String?,
    val postalCode: String?,
    val postOffice: String?,
    val mailingStreetAddress: String?,
    val mailingPostalCode: String?,
    val mailingPostOffice: String?,
    val phoneNumber: String?,
    val email: String?,
    val capacity: Int,
    val openingDate: String?,
    val closingDate: String?,
    val evakaDaycareId: DaycareId,
    val unitProviderType: VardaUnitProviderType,
    val unitType: List<VardaUnitType>,
    val language: VardaLanguage,
    val languageEmphasisId: UUID?
) {
    fun toVardaUnitRequest(
        lahdejarjestelma: String,
        vakajarjestaja: String,
        kuntakoodi: String,
    ) =
        VardaUnitRequest(
            id = vardaUnitId,
            organisaatio_oid = ophUnitOid,
            vakajarjestaja = vakajarjestaja,
            nimi = name,
            kayntiosoite = address,
            kayntiosoite_postinumero = postalCode,
            kayntiosoite_postitoimipaikka = postOffice,
            postiosoite = mailingStreetAddress,
            postinumero = mailingPostalCode,
            postitoimipaikka = mailingPostOffice,
            puhelinnumero = phoneNumber,
            sahkopostiosoite = email,
            varhaiskasvatuspaikat = capacity,
            alkamis_pvm = openingDate,
            paattymis_pvm = closingDate,
            kunta_koodi = kuntakoodi,
            lahdejarjestelma = lahdejarjestelma,
            toiminnallinenpainotus_kytkin = false,
            kasvatusopillinen_jarjestelma_koodi = VardaUnitEducationSystem.NONE.vardaCode,
            jarjestamismuoto_koodi = listOfNotNull(unitProviderType.vardaCode),
            toimintamuoto_koodi =
                when {
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
                },
            toimintakieli_koodi = listOfNotNull(language.vardaCode),
            kielipainotus_kytkin = languageEmphasisId?.let { true } ?: false
        )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class VardaUnitRequest(
    val id: Long?,
    val organisaatio_oid: String?,
    val vakajarjestaja: String?,
    val nimi: String?,
    val kayntiosoite: String?,
    val kayntiosoite_postinumero: String?,
    val kayntiosoite_postitoimipaikka: String?,
    val postiosoite: String?,
    val postinumero: String?,
    val postitoimipaikka: String?,
    val puhelinnumero: String?,
    val sahkopostiosoite: String?,
    val varhaiskasvatuspaikat: Int,
    val alkamis_pvm: String?,
    @JsonInclude(JsonInclude.Include.ALWAYS) val paattymis_pvm: String?,
    val kunta_koodi: String?,
    val lahdejarjestelma: String?,
    val toiminnallinenpainotus_kytkin: Boolean? = false,
    val kasvatusopillinen_jarjestelma_koodi: String? = VardaUnitEducationSystem.NONE.vardaCode,
    val jarjestamismuoto_koodi: List<String>,
    val toimintamuoto_koodi: String?,
    val toimintakieli_koodi: List<String>,
    val kielipainotus_kytkin: Boolean
)
