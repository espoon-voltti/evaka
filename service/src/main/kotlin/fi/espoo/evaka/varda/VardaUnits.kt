// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.varda

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.JsonMappingException
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.DaycareId
import fi.espoo.evaka.shared.db.Database
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.EvakaClock
import fi.espoo.evaka.shared.domain.HelsinkiDateTime
import java.time.LocalDate
import java.util.UUID
import mu.KotlinLogging
import org.jdbi.v3.core.result.UnableToProduceResultException

private val logger = KotlinLogging.logger {}

val unitTypesToUpload =
    listOf(VardaUnitProviderType.MUNICIPAL, VardaUnitProviderType.MUNICIPAL_SCHOOL)

interface VardaUnitClient {
    fun createUnit(unit: VardaUnitRequest): VardaUnitResponse

    fun updateUnit(id: Long, unit: VardaUnitRequest): VardaUnitResponse
}

fun updateUnits(
    dbc: Database.Connection,
    clock: EvakaClock,
    client: VardaUnitClient,
    lahdejarjestelma: String,
    kuntakoodi: String,
    vakajarjestajaUrl: String
) {
    val startTime = clock.now()

    val (states, units) =
        dbc.read {
            val states = it.getVardaUnitStates<VardaUnitRequest>()
            val units = it.getVardaUnits()
            states to units
        }

    val unitsToSend =
        units.mapNotNull { unit ->
            val prevState = states[unit.evakaDaycareId]
            val currState =
                unit.toVardaUnitRequest(
                    lahdejarjestelma = lahdejarjestelma,
                    vakajarjestaja = vakajarjestajaUrl,
                    kuntakoodi = kuntakoodi
                )
            if (prevState?.state != currState) {
                Triple(unit.evakaDaycareId, prevState?.vardaUnitId, currState)
            } else {
                null
            }
        }

    logger.info { "Sending ${unitsToSend.size} new or updated units to Varda" }
    unitsToSend.forEach { (evakaDaycareId, vardaUnitId, request) ->
        try {
            val response =
                if (vardaUnitId == null) {
                    client.createUnit(request)
                } else {
                    client.updateUnit(vardaUnitId, request)
                }
            dbc.transaction {
                setUnitUploaded(
                    tx = it,
                    now = startTime,
                    unitId = evakaDaycareId,
                    vardaUnitId = response.id,
                    ophUnitOid = response.organisaatio_oid,
                    state = request
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to update unit ${request.nimi}: $e", e)
            dbc.transaction {
                setUnitUploadFailed(
                    tx = it,
                    now = startTime,
                    unitId = evakaDaycareId,
                    error = e.localizedMessage
                )
            }
        }
    }
}

data class VardaUnitState<T>(val vardaUnitId: Long?, val state: T?)

inline fun <reified T : Any> Database.Read.getVardaUnitStates(): Map<DaycareId, VardaUnitState<T>> =
    createQuery { sql("SELECT evaka_daycare_id, varda_unit_id, state FROM varda_unit") }
        .toMap {
            val id = column<DaycareId>("evaka_daycare_id")
            val vardaUnitId = column<Long?>("varda_unit_id")
            val state =
                try {
                    jsonColumn<T?>("state")
                } catch (exc: UnableToProduceResultException) {
                    if (exc.cause is JsonMappingException) {
                        null
                    } else {
                        throw exc
                    }
                }
            id to VardaUnitState(vardaUnitId, state)
        }

fun Database.Read.getVardaUnits(): List<VardaUnit> =
    createQuery {
            sql(
                """
                SELECT
                    daycare.id AS evaka_daycare_id,
                    daycare.name,
                    daycare.street_address,
                    daycare.postal_code,
                    daycare.post_office,
                    daycare.mailing_po_box,
                    daycare.mailing_postal_code,
                    daycare.mailing_post_office,
                    daycare.unit_manager_phone,
                    daycare.unit_manager_email,
                    daycare.capacity,
                    daycare.opening_date,
                    daycare.closing_date,
                    daycare.provider_type,
                    daycare.type,
                    daycare.language,
                    daycare.language_emphasis_id
                FROM daycare
                WHERE daycare.upload_to_varda IS TRUE AND daycare.provider_type = ANY(${bind(unitTypesToUpload)})
                """
            )
        }
        .toList()

fun setUnitUploaded(
    tx: Database.Transaction,
    now: HelsinkiDateTime,
    unitId: DaycareId,
    vardaUnitId: Long?,
    ophUnitOid: String?,
    state: Any
) {
    tx.createUpdate {
            sql(
                """
                INSERT INTO varda_unit (evaka_daycare_id, varda_unit_id, state, last_success_at, errored_at, error)
                VALUES (${bind(unitId)}, ${bind(vardaUnitId)}, ${bindJson(state)}, ${bind(now)}, NULL, NULL)
                ON CONFLICT (evaka_daycare_id)
                DO UPDATE SET varda_unit_id = ${bind(vardaUnitId)}, state = ${bindJson(state)}, last_success_at = ${bind(now)}, errored_at = NULL, error = NULL
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
    val evakaDaycareId: DaycareId,
    val name: String,
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
    val mailingPoBox: String?,
    val mailingPostalCode: String?,
    val mailingPostOffice: String?,
    val unitManagerPhone: String,
    val unitManagerEmail: String,
    val capacity: Int,
    val openingDate: LocalDate?,
    val closingDate: LocalDate?,
    val providerType: VardaUnitProviderType,
    val type: List<VardaUnitType>,
    val language: VardaLanguage,
    val languageEmphasisId: UUID?
) {
    fun toVardaUnitRequest(
        lahdejarjestelma: String,
        vakajarjestaja: String,
        kuntakoodi: String,
    ) =
        VardaUnitRequest(
            vakajarjestaja = vakajarjestaja,
            nimi = name,
            kayntiosoite = streetAddress,
            kayntiosoite_postinumero = postalCode,
            kayntiosoite_postitoimipaikka = postOffice,
            postiosoite = mailingPoBox,
            postinumero = mailingPostalCode,
            postitoimipaikka = mailingPostOffice,
            puhelinnumero = unitManagerPhone,
            sahkopostiosoite = unitManagerEmail,
            varhaiskasvatuspaikat = capacity,
            alkamis_pvm = openingDate,
            paattymis_pvm = closingDate,
            kunta_koodi = kuntakoodi,
            lahdejarjestelma = lahdejarjestelma,
            toiminnallinenpainotus_kytkin = false,
            kasvatusopillinen_jarjestelma_koodi = VardaUnitEducationSystem.NONE.vardaCode,
            jarjestamismuoto_koodi = listOfNotNull(providerType.vardaCode),
            toimintamuoto_koodi =
                when {
                    type.contains(VardaUnitType.CENTRE) -> VardaUnitType.CENTRE.vardaCode
                    type.contains(VardaUnitType.FAMILY) -> VardaUnitType.FAMILY.vardaCode
                    type.contains(VardaUnitType.GROUP_FAMILY) ->
                        VardaUnitType.GROUP_FAMILY.vardaCode
                    type.contains(VardaUnitType.PRESCHOOL) -> VardaUnitType.PRESCHOOL.vardaCode
                    else -> null
                },
            toimintakieli_koodi = listOfNotNull(language.vardaCode),
            kielipainotus_kytkin = languageEmphasisId?.let { true } ?: false
        )
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class VardaUnitRequest(
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
    val alkamis_pvm: LocalDate?,
    @JsonInclude(JsonInclude.Include.ALWAYS) val paattymis_pvm: LocalDate?,
    val kunta_koodi: String?,
    val lahdejarjestelma: String?,
    val toiminnallinenpainotus_kytkin: Boolean? = false,
    val kasvatusopillinen_jarjestelma_koodi: String? = VardaUnitEducationSystem.NONE.vardaCode,
    val jarjestamismuoto_koodi: List<String>,
    val toimintamuoto_koodi: String?,
    val toimintakieli_koodi: List<String>,
    val kielipainotus_kytkin: Boolean
)
