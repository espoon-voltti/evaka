// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.turku.dw

import evaka.core.absence.AbsenceCategory
import evaka.core.absence.AbsenceType
import evaka.core.application.ApplicationOrigin
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.assistance.DaycareAssistanceLevel
import evaka.core.assistance.PreschoolAssistanceLevel
import evaka.core.assistanceaction.AssistanceActionOptionCategory
import evaka.core.daycare.CareType
import evaka.core.daycare.domain.ProviderType
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.VoucherValueDecisionType
import evaka.core.placement.PlacementType
import evaka.core.serviceneed.ShiftCareType
import java.time.LocalDate
import java.util.UUID

data class DwAbsence(
    val lapsenid: UUID,
    val poissaolonpvm: LocalDate,
    val poissaolontyyppi: AbsenceType,
    val poissaolonkategoria: AbsenceCategory,
    val sijoitustyyppi: PlacementType,
)

data class DwApplicationInfo(
    val hakemuksenId: UUID,
    val hakemusLuotu: String,
    val hakemustaPaivitetty: String,
    val tyyppi: ApplicationType,
    val tilanne: ApplicationStatus,
    val alkupera: ApplicationOrigin,
    val siirtohakemus: Boolean,
    val lapsenId: UUID,
    val syntymaaika: LocalDate,
    val yksikot: String,
    val haluttuAloituspaiva: String,
    val yksikkoNimi: String,
    val alueId: UUID,
    val alueNimi: String,
)

data class DwAssistanceAction(
    val pvm: String,
    val lapsenId: UUID,
    val tukitoimi: String?,
    val muuTukitoimi: String?,
    val aloitusPvm: LocalDate,
    val loppuPvm: LocalDate,
    val tuenTyyppi: AssistanceActionOptionCategory?,
)

data class DwAssistanceNeedDecision(
    val aikaleima: String,
    val päätosTuesta: Int,
    val lapsenId: UUID,
    val tuenAlkupvm: LocalDate,
    val tuenLoppupvm: LocalDate,
    val pienennettyRyhmä: Boolean,
    val erityisryhmä: Boolean,
    val pienryhmä: Boolean,
    val ryhmäkohtainenAvustaja: Boolean,
    val lapsikohtainenAvustaja: Boolean,
    val henkilöresurssienLisäys: Boolean,
    val veonAntamaKonsultaatio: Boolean,
    val veonOsaAikainenOpetus: Boolean,
    val veonKokoaikainenOpetus: Boolean,
    val tulkitsemisJaAvustamispalvelut: Boolean,
    val apuvälineet: Boolean,
    val tuenTaso: List<String>,
)

data class DwChildAggregate(
    val pvm: String,
    val lapsenId: UUID,
    val henkilöturvatunnus: String?,
    val syntymäaika: LocalDate,
    val kieli: String?,
    val postiosoite: String,
    val postinumero: String,
    val postitoimipaikka: String,
    val kansalaisuudet: List<String>,
)

data class DwChildReservation(
    val lapsenId: UUID,
    val paivamaara: LocalDate,
    val varausAlkaa: String?,
    val varausPaattyy: String?,
)

data class DwChildAttendance(
    val lapsenId: UUID,
    val paivamaara: LocalDate,
    val toteumaAlkaa: String?,
    val toteumaPaattyy: String?,
    val yksikonId: UUID,
)

data class DwDailyInfo(
    val pvm: String,
    val lapsenId: UUID,
    val henkilöturvatunnus: String?,
    val syntymäaika: LocalDate,
    val kieli: String?,
    val postiosoite: String,
    val postinumero: String,
    val postitoimipaikka: String,
    val kansalaisuudet: List<String>?,
    val sijoitustyyppi: PlacementType,
    val sijoitusyksikköId: UUID,
    val sijoituksenAloitusPvm: LocalDate,
    val sijoituksenLoppuPvm: LocalDate?,
    val yksikönNimi: String,
    val palvelualueId: UUID,
    val palvelualue: String,
    val toimintamuoto: List<CareType>,
    val järjestämistapa: ProviderType,
    val kustannuspaikka: String?,
    val sijoitusryhmäAloitusPvm: LocalDate,
    val sijoitysryhmäLoppuPvm: LocalDate?,
    val sijoitusryhmäId: UUID,
    val sijoitusryhmä: String,
    val varahoitoyksikköId: UUID?,
    val varahoitoyksikköAloitusPvm: LocalDate?,
    val varahoitoyksikköLoppuPvm: LocalDate?,
    val varahoitoyksikkö: String?,
    val varahoitoryhmäId: UUID?,
    val varahoitoryhmä: String?,
    val palveluntarveMerkitty: Boolean?,
    val palveluntarve: String?,
    val palveluntarveId: UUID?,
    val osapäiväinen: Boolean?,
    val osaviikkoinen: Boolean?,
    val palveluntarpeenAloitusPvm: LocalDate?,
    val palveluntarpeenLoppuPvm: LocalDate?,
    val vuorohoito: ShiftCareType?,
    val tuntejaViikossa: Int?,
    val palveluntarvekerroin: Double?,
    val tuentarveVarhaiskasvatuksessa: DaycareAssistanceLevel?,
    val tuentarveVarhaAloitusPvm: LocalDate?,
    val tuentarveVarhaLoppuPvm: LocalDate?,
    val tuentarveEsiopetuksessa: PreschoolAssistanceLevel?,
    val tuentarveEsiopAloitusPvm: LocalDate?,
    val tuentarveEsiopLoppuPvm: LocalDate?,
    val tuentarpeenKerroin: Double?,
    val kerroinAloitusPvm: String?,
    val kerroinLoppuPvm: LocalDate?,
    val lapsenKapasiteetti: Double?,
    val kapasiteettiAloitusPvm: LocalDate?,
    val kapasiteettiLoppuPvm: LocalDate?,
    val poissaolonSyy: String?,
)

data class DwDailyUnitAndGroupAttendance(
    val aikaleima: String,
    val poimintaAjaltaPvm: LocalDate?,
    val toimintayksikkö: String,
    val toimintayksikköId: UUID,
    val toimintayksikönLapsimäärä: Int?,
    val toimintayksikönLapsimääräEdKuunLopussa: Int?,
    val ryhmä: String,
    val ryhmäId: UUID,
    val henkilökuntaaRyhmässä: Double?,
    val henkilökuntaaLäsnä: Double?,
    val lapsiaLäsnäRyhmässä: Int?,
    val laskennallinenLapsiaLäsnäRyhmässä: Double?,
    val lapsiaLäsnäYksikössä: Int?,
    val ryhmänLapsimäärä: Int?,
    val laskennallinenRyhmänLapsimäärä: Double?,
    val ryhmänLapsimääräEdKuunLopussa: Int?,
)

data class DwDailyUnitOccupancyConfirmed(
    val pvm: LocalDate,
    val toimintayksikköId: UUID,
    val toimintayksikkö: String,
    val kasvattajienLkm: Double?,
    val sijoituksienLkm: Int?,
    val täyttöasteSumma: Double?,
    val täyttöasteProsentteina: Double?,
)

data class DwDailyUnitOccupancyRealized(
    val pvm: LocalDate,
    val toimintayksikköId: UUID,
    val toimintayksikkö: String,
    val kasvattajienLkm: Double?,
    val sijoituksienLkm: Int?,
    val käyttöasteSumma: Double?,
    val käyttöasteProsentteina: Double?,
)

data class DwFeeDecision(
    val aikaleima: String,
    val maksupäätöksenNumero: String?,
    val maksupäätösId: UUID,
    val alkupvm: LocalDate,
    val loppupvm: LocalDate,
    val huojennustyyppi: FeeDecisionType,
    val perhekoko: Int,
    val kokonaismaksu: Int,
    val lapsiId: UUID,
    val lapsikohtainenMaksu: Int,
    val toimintamuoto: PlacementType,
    val palvelualue: String,
    val palvelualueId: UUID,
    val toimipaikka: String,
    val toimipaikkaId: UUID,
    val kustannuspaikka: String?,
)

data class DwUnitAndGroup(
    val aikaleima: String,
    val toimintayksikkö: String,
    val toimintayksikköId: UUID,
    val toimintayksikönAlkupvm: LocalDate?,
    val toimintayksikönLoppupvm: LocalDate?,
    val toimintamuoto: List<CareType>,
    val järjestämistapa: ProviderType,
    val palvelualue: String,
    val palvelualueId: UUID,
    val dwKustannuspaikka: String?,
    val toimintayksikönLapsimäärä: Int,
    val ryhmä: String,
    val ryhmäId: UUID,
    val ryhmänAlkupvm: LocalDate,
    val ryhmänLoppupvm: LocalDate?,
    val ryhmanHenkilokunnanAlkupvm: LocalDate?,
    val ryhmanHenkilokunnanLoppupvm: LocalDate?,
    val henkilökuntaaRyhmässäViim: Double?,
    val koordinaatit: String?,
    val koulunYhteydessa: Boolean,
)

data class DwVoucherValueDecision(
    val aikaleima: String,
    val arvopäätöksenNumero: String?,
    val alkupvm: LocalDate,
    val loppupvm: LocalDate?,
    val huojennustyyppi: VoucherValueDecisionType,
    val perhekoko: Int,
    val palvelusetelinArvo: Int,
    val omavastuuosuus: Int,
    val lapsenId: UUID,
    val toimintamuoto: PlacementType?,
    val palvelualue: String,
    val palvelualueId: UUID,
    val toimipaikka: String,
    val toimipaikkaId: UUID,
)
