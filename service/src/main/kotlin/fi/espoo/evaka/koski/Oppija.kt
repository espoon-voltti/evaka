// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import fi.espoo.evaka.shared.db.DatabaseEnum
import fi.espoo.evaka.shared.domain.FiniteDateRange
import java.time.LocalDate
import java.util.UUID

// https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/opiskeluoikeudentyyppi/latest
data class OpiskeluoikeudenTyyppi(override val koodiarvo: OpiskeluoikeudenTyyppiKoodi) :
    Koodistokoodiviite<OpiskeluoikeudenTyyppiKoodi>("opiskeluoikeudentyyppi")

// https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/suorituksentyyppi/latest
data class SuorituksenTyyppi(override val koodiarvo: SuorituksenTyyppiKoodi) :
    Koodistokoodiviite<SuorituksenTyyppiKoodi>("suorituksentyyppi")

// https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/koulutus/latest
data class KoulutusmoduulinTunniste(override val koodiarvo: KoulutusmoduulinTunnisteKoodi) :
    Koodistokoodiviite<KoulutusmoduulinTunnisteKoodi>("koulutus")

// https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/koskiopiskeluoikeudentila/latest
data class OpiskeluoikeusjaksonTila(override val koodiarvo: OpiskeluoikeusjaksonTilaKoodi) :
    Koodistokoodiviite<OpiskeluoikeusjaksonTilaKoodi>("koskiopiskeluoikeudentila")

// https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/kieli/latest
data class Suorituskieli(override val koodiarvo: String) : Koodistokoodiviite<String>("kieli")

// https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/lahdejarjestelma/latest
data class Lähdejärjestelmä(override val koodiarvo: String) :
    Koodistokoodiviite<String>("lahdejarjestelma")

// https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/vardajarjestamismuoto/latest
data class Järjestämismuoto(override val koodiarvo: JärjestämismuotoKoodi) :
    Koodistokoodiviite<JärjestämismuotoKoodi>("vardajarjestamismuoto")

// https://github.com/Opetushallitus/koski/blob/bb25022e3eff22675246eb73f17a1c718c6f07f9/src/main/scala/fi/oph/koski/schema/Koodiviite.scala#L17
abstract class Koodistokoodiviite<T>(val koodistoUri: String) {
    abstract val koodiarvo: T
}

// https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/vardajarjestamismuoto/latest
enum class JärjestämismuotoKoodi {
    @JsonProperty("JM02") PURCHASED,
    @JsonProperty("JM03") PRIVATE_SERVICE_VOUCHER,
}

// https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/koulutus/latest
enum class KoulutusmoduulinTunnisteKoodi {
    @JsonProperty("001102") // "Päiväkodin esiopetus"
    PRESCHOOL,
    @JsonProperty("999905") PREPARATORY,
}

// https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/koskiopiskeluoikeudentila/latest
enum class OpiskeluoikeusjaksonTilaKoodi {
    @JsonProperty("lasna") PRESENT,
    @JsonProperty("valmistunut") QUALIFIED,
    @JsonProperty("eronnut") RESIGNED,
    @JsonProperty("mitatoity") VOIDED,
    @JsonProperty("valiaikaisestikeskeytynyt") INTERRUPTED,
    @JsonProperty("loma") HOLIDAY,
}

// https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/opiskeluoikeudentyyppi/latest
enum class OpiskeluoikeudenTyyppiKoodi : DatabaseEnum {
    @JsonProperty("esiopetus") PRESCHOOL,
    @JsonProperty("perusopetukseenvalmistavaopetus") PREPARATORY;

    override val sqlType: String = "koski_study_right_type"
}

// https://koski.opintopolku.fi/koski/dokumentaatio/koodisto/suorituksentyyppi/latest
enum class SuorituksenTyyppiKoodi {
    @JsonProperty("esiopetuksensuoritus") PRESCHOOL,
    @JsonProperty("perusopetukseenvalmistavaopetus") PREPARATORY,
    @JsonProperty("perusopetukseenvalmistavanopetuksenoppiaine") PREPARATORY_SUBJECT,
}

// https://github.com/Opetushallitus/koski/blob/d7abc79acca44d2d1265c14be4a631bd8ee91297/src/main/scala/fi/oph/koski/schema/Oppija.scala#L6
data class Oppija(val henkilö: Henkilö, val opiskeluoikeudet: List<Opiskeluoikeus>)

@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION) sealed class Henkilö

// https://github.com/Opetushallitus/koski/blob/1f1d05bb80cbac46cd873419975ed421370b5035/src/main/scala/fi/oph/koski/schema/Henkilo.scala#L56
data class UusiHenkilö(val hetu: String, val etunimet: String, val sukunimi: String) : Henkilö()

// https://github.com/Opetushallitus/koski/blob/1f1d05bb80cbac46cd873419975ed421370b5035/src/main/scala/fi/oph/koski/schema/Henkilo.scala#L65
data class OidHenkilö(val oid: String) : Henkilö()

data class Opiskeluoikeus(
    val tila: OpiskeluoikeudenTila,
    val suoritukset: List<Suoritus>,
    val oid: String?,
    val tyyppi: OpiskeluoikeudenTyyppi,
    val lähdejärjestelmänId: LähdejärjestelmäId,
    val lisätiedot: Lisätiedot?,
    @JsonInclude(JsonInclude.Include.NON_NULL) val järjestämismuoto: Järjestämismuoto?,
)

// https://github.com/Opetushallitus/koski/blob/1f1d05bb80cbac46cd873419975ed421370b5035/src/main/scala/fi/oph/koski/schema/Opiskeluoikeus.scala#L226
data class LähdejärjestelmäId(
    val id: UUID, // actual type is String?
    val lähdejärjestelmä: Lähdejärjestelmä,
)

data class OpiskeluoikeudenTila(val opiskeluoikeusjaksot: List<Opiskeluoikeusjakso>)

data class Opiskeluoikeusjakso(val alku: LocalDate, val tila: OpiskeluoikeusjaksonTila) {
    companion object {
        fun läsnä(alku: LocalDate) =
            Opiskeluoikeusjakso(
                alku = alku,
                tila = OpiskeluoikeusjaksonTila(OpiskeluoikeusjaksonTilaKoodi.PRESENT),
            )

        fun valmistunut(alku: LocalDate) =
            Opiskeluoikeusjakso(
                alku = alku,
                tila = OpiskeluoikeusjaksonTila(OpiskeluoikeusjaksonTilaKoodi.QUALIFIED),
            )

        fun eronnut(alku: LocalDate) =
            Opiskeluoikeusjakso(
                alku = alku,
                tila = OpiskeluoikeusjaksonTila(OpiskeluoikeusjaksonTilaKoodi.RESIGNED),
            )

        fun mitätöity(alku: LocalDate) =
            Opiskeluoikeusjakso(
                alku = alku,
                tila = OpiskeluoikeusjaksonTila(OpiskeluoikeusjaksonTilaKoodi.VOIDED),
            )

        fun väliaikaisestiKeskeytynyt(alku: LocalDate) =
            Opiskeluoikeusjakso(
                alku = alku,
                tila = OpiskeluoikeusjaksonTila(OpiskeluoikeusjaksonTilaKoodi.INTERRUPTED),
            )

        fun loma(alku: LocalDate) =
            Opiskeluoikeusjakso(
                alku = alku,
                tila = OpiskeluoikeusjaksonTila(OpiskeluoikeusjaksonTilaKoodi.HOLIDAY),
            )
    }
}

data class Koulutusmoduuli(
    val perusteenDiaarinumero: PerusteenDiaarinumero,
    val tunniste: KoulutusmoduulinTunniste,
)

data class Toimipiste(val oid: String)

data class Suoritus(
    val koulutusmoduuli: Koulutusmoduuli,
    val toimipiste: Toimipiste,
    val suorituskieli: Suorituskieli,
    val tyyppi: SuorituksenTyyppi,
    val vahvistus: Vahvistus? = null,
    val osasuoritukset: List<Osasuoritus>? = null,
)

data class Osasuoritus(
    val koulutusmoduuli: OsasuorituksenKoulutusmoduuli,
    val arviointi: List<Arviointi>,
    val tyyppi: SuorituksenTyyppi,
    val vahvistus: Vahvistus? = null,
)

data class Arviointi(val arvosana: Arvosana, val kuvaus: LokalisoituTeksti)

enum class ArvosanaKoodiarvo {
    @JsonProperty("O") OSALLISTUNUT
}

data class Arvosana(
    val koodiarvo: ArvosanaKoodiarvo,
    val koodistoUri: String = "arviointiasteikkoyleissivistava",
)

data class OsasuorituksenKoulutusmoduuli(
    val tunniste: OsasuorituksenTunniste,
    val laajuus: OsasuorituksenLaajuus,
)

data class OsasuorituksenTunniste(val koodiarvo: String, val nimi: LokalisoituTeksti)

data class OsasuorituksenLaajuus(val arvo: Int, val yksikkö: Laajuusyksikkö)

enum class LaajuusyksikköKoodiarvo {
    @JsonProperty("3") VUOSIVIIKKOTUNTI
}

data class Laajuusyksikkö(
    val koodiarvo: LaajuusyksikköKoodiarvo,
    val koodistoUri: String = "opintojenlaajuusyksikko",
)

data class LokalisoituTeksti(val fi: String)

data class Vahvistus(
    val päivä: LocalDate,
    val paikkakunta: VahvistusPaikkakunta,
    val myöntäjäOrganisaatio: MyöntäjäOrganisaatio,
    val myöntäjäHenkilöt: List<MyöntäjäHenkilö>,
)

data class VahvistusPaikkakunta(val koodiarvo: String, val koodistoUri: String = "kunta")

data class MyöntäjäOrganisaatio(val oid: String)

data class MyöntäjäHenkilö(
    val nimi: String,
    val titteli: MyöntäjäHenkilönTitteli,
    val organisaatio: MyöntäjäOrganisaatio,
)

data class MyöntäjäHenkilönTitteli(val fi: String)

data class Lisätiedot(
    val vammainen: List<Aikajakso>?,
    val vaikeastiVammainen: List<Aikajakso>?,
    val pidennettyOppivelvollisuus: Aikajakso?,
    val kuljetusetu: Aikajakso?,
    val erityisenTuenPäätökset: List<ErityisenTuenPäätös>?,
)

data class ErityisenTuenPäätös(
    val alku: LocalDate,
    val loppu: LocalDate,
    val opiskeleeToimintaAlueittain: Boolean,
) {
    companion object {
        fun from(aikajakso: FiniteDateRange) =
            ErityisenTuenPäätös(
                alku = aikajakso.start,
                loppu = aikajakso.end,
                opiskeleeToimintaAlueittain = false, // not used in Espoo
            )
    }
}

data class Aikajakso(val alku: LocalDate, val loppu: LocalDate?) {
    companion object {
        fun from(aikajakso: FiniteDateRange) = Aikajakso(aikajakso.start, aikajakso.end)
    }
}

enum class PerusteenDiaarinumero {
    // "Esiopetuksen opetussuunnitelman perusteet 2014"
    // https://eperusteet.opintopolku.fi/#/en/esiopetus/419551/tiedot
    @JsonProperty("102/011/2014") PRESCHOOL,

    // https://eperusteet.opintopolku.fi/#/fi/pvalmistava/1541511/tiedot
    @JsonProperty("57/011/2015") PREPARATORY,
}

// https://github.com/Opetushallitus/koski/blob/d7abc79acca44d2d1265c14be4a631bd8ee91297/src/main/scala/fi/oph/koski/oppija/KoskiOppijaFacade.scala#L278
data class HenkilönOpiskeluoikeusVersiot(
    val henkilö: OidHenkilö,
    val opiskeluoikeudet: List<OpiskeluoikeusVersio>,
)

// https://github.com/Opetushallitus/koski/blob/d7abc79acca44d2d1265c14be4a631bd8ee91297/src/main/scala/fi/oph/koski/oppija/KoskiOppijaFacade.scala#L279
data class OpiskeluoikeusVersio(
    val oid: String,
    val versionumero: Int,
    val lähdejärjestelmänId: LähdejärjestelmäId,
)
