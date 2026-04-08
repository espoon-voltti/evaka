// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.instance.oulu.dw

import evaka.core.absence.AbsenceCategory
import evaka.core.absence.AbsenceType
import evaka.core.application.ApplicationOrigin
import evaka.core.application.ApplicationStatus
import evaka.core.application.ApplicationType
import evaka.core.assistance.DaycareAssistanceLevel
import evaka.core.assistance.OtherAssistanceMeasureType
import evaka.core.assistance.PreschoolAssistanceLevel
import evaka.core.assistanceaction.AssistanceActionOptionCategory
import evaka.core.daycare.CareType
import evaka.core.daycare.domain.ProviderType
import evaka.core.invoicing.domain.FeeDecisionStatus
import evaka.core.invoicing.domain.FeeDecisionType
import evaka.core.invoicing.domain.VoucherValueDecisionStatus
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
    val haluttuAloituspaiva: String?,
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
    val tuenAlkupvm: String,
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
    val tila: String,
)

data class DwChildReservations(
    val lapsenId: UUID,
    val paivamaara: LocalDate,
    val varausAlkaa: String?,
    val varausPaattyy: String?,
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
    val kansalaisuudet: List<String>,
    val sijoitustyyppi: PlacementType,
    val sijoitusyksikköId: UUID,
    val yksikönNimi: String,
    val palvelualueId: UUID,
    val palvelualue: String,
    val toimintamuoto: List<CareType>,
    val järjestämistapa: ProviderType,
    val kustannuspaikka: String?,
    val sijoitusryhmäId: UUID,
    val sijoitusryhmä: String,
    val varahoitoyksikköId: UUID?,
    val varahoitoyksikkö: String?,
    val varahoitoryhmäId: UUID?,
    val varahoitoryhmä: String?,
    val palveluntarveMerkitty: Boolean?,
    val palveluntarve: String?,
    val palveluntarveId: UUID?,
    val osapäiväinen: Boolean?,
    val osaviikkoinen: Boolean?,
    val vuorohoito: ShiftCareType?,
    val tuntejaViikossa: Int?,
    val tuentarpeenKerroin: Double?,
    val lapsenKapasiteetti: Double?,
    val poissaolonSyy: List<AbsenceType>,
)

data class DwDailyUnitsAndGroupsAttendance(
    val aikaleima: String,
    val pvm: LocalDate?,
    val toimintayksikkö: String,
    val toimintayksikköId: UUID,
    val toimintapäivät: List<Int>,
    val vuorohoitoyksikkö: Boolean?,
    val vuorohoitopäivät: List<Int>?,
    val vuorohoitopyhäpäivinä: Boolean,
    val ryhmä: String,
    val ryhmäId: UUID,
    val toimintayksikönLaskennallinenLapsimäärä: Int?,
    val toimintayksikönLapsimäärä: Int?,
    val henkilökuntaaRyhmässä: Int?,
    val henkilökuntaaLäsnä: Double?,
    val kasvatusvastuullistenLkmYksikössä: Double?,
    val ryhmänLapsimäärä: Int?,
)

data class DwDailyUnitsOccupanciesConfirmed(
    val pvm: LocalDate,
    val toimintayksikköId: UUID,
    val toimintayksikkö: String,
    val kasvattajienLkm: Double?,
    val sijoituksienLkm: Int?,
    val täyttöasteSumma: Double?,
    val täyttöasteProsentteina: Double?,
)

data class DwDailyUnitsOccupanciesRealized(
    val pvm: LocalDate,
    val toimintayksikköId: UUID,
    val toimintayksikkö: String,
    val kasvattajienLkm: Double?,
    val sijoituksienLkm: Int?,
    val käyttöasteSumma: Double?,
    val käyttöasteProsentteina: Double?,
)

data class DwDaycareAssistance(
    val pvm: String,
    val lapsenId: UUID,
    val tuentarveVarhaiskasvatuksessa: DaycareAssistanceLevel,
    val aloitusPvm: String,
    val loppuPvm: LocalDate,
)

data class DwFeeDecision(
    val aikaleima: String,
    val maksupäätöksenNumero: Long?,
    val maksupäätösId: UUID,
    val alkupvm: String,
    val loppupvm: LocalDate,
    val huojennustyyppi: FeeDecisionType,
    val perhekoko: Int,
    val kokonaismaksu: Int,
    val tila: FeeDecisionStatus,
    val lapsiId: UUID,
    val lapsikohtainenMaksu: Int,
    val toimintamuoto: PlacementType,
    val palvelualue: String,
    val palvelualueId: UUID,
    val toimipaikka: String,
    val toimipaikkaId: UUID,
    val kustannuspaikka: String?,
)

data class DwOtherAssistanceMeasure(
    val pvm: String,
    val lapsenId: UUID,
    val muuToimi: OtherAssistanceMeasureType,
    val aloitusPvm: String,
    val loppuPvm: LocalDate,
)

data class DwPlacement(
    val aikaleima: String,
    val lapsenId: UUID,
    val toimintayksikkö: String,
    val toimintayksikköId: UUID,
    val sijoituksenAlkupvm: LocalDate,
    val sijoituksenLoppupvm: LocalDate,
    val sijoitustyyppi: PlacementType,
    val ryhmä: String,
    val ryhmäId: UUID,
    val ryhmänAlkupvm: LocalDate,
    val ryhmänLoppupvm: LocalDate?,
    val palveluntarve: String?,
    val palveluntarveId: UUID?,
    val palveluntarpeenAlkupvm: LocalDate?,
    val palveluntarpeenLoppupvm: LocalDate?,
)

data class DwPreschoolAssistance(
    val pvm: String,
    val lapsenId: UUID,
    val tuentarveEsiopetuksessa: PreschoolAssistanceLevel,
    val aloitusPvm: String,
    val loppuPvm: LocalDate,
)

data class DwUnitAndGroup(
    val aikaleima: String,
    val toimintayksikkö: String,
    val toimintayksikköId: UUID,
    val toimintayksikönAlkupvm: LocalDate?,
    val toimintayksikönLoppupvm: LocalDate?,
    val toimintamuoto: List<CareType>,
    val järjestämistapa: ProviderType,
    val katuosoite: String,
    val postinumero: String,
    val postitoimipaikka: String,
    val toimintayksikönLaskennallinenLapsimäärä: Int?,
    val palvelualue: String,
    val palvelualueId: UUID,
    val dwKustannuspaikka: String?,
    val ryhmä: String,
    val ryhmäId: UUID,
    val ryhmänAlkupvm: LocalDate,
    val ryhmänLoppupvm: LocalDate?,
)

data class DwVoucherValueDecision(
    val aikaleima: String,
    val arvopäätöksenNumero: Long?,
    val alkupvm: LocalDate,
    val loppupvm: LocalDate,
    val huojennustyyppi: VoucherValueDecisionType,
    val perhekoko: Int,
    val palvelusetelinArvo: Int,
    val omavastuuosuus: Int,
    val lapsenId: UUID,
    val toimintamuoto: PlacementType?,
    val tila: VoucherValueDecisionStatus,
    val palvelualue: String,
    val palvelualueId: UUID,
    val toimipaikka: String,
    val toimipaikkaId: UUID,
)

data class FabricAbsence(
    val poimintaAika: String,
    val poissaolonId: UUID,
    val muokkausaika: String,
    val lapsenid: UUID,
    val poissaolonpvm: LocalDate,
    val poissaolontyyppi: AbsenceType,
    val poissaolonkategoria: AbsenceCategory,
    val sijoitustyyppi: PlacementType,
)

data class FabricApplicationInfo(
    val poimintaAika: String,
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
    val haluttuAloituspaiva: String?,
    val yksikkoNimi: String,
    val alueId: UUID,
    val alueNimi: String,
)

data class FabricAssistanceAction(
    val poimintaAika: String,
    val tukitoimenId: UUID,
    val luontiaika: String,
    val muokkausaika: String,
    val lapsenId: UUID,
    val tukitoimi: String?,
    val muuTukitoimi: String?,
    val aloitusPvm: LocalDate,
    val loppuPvm: LocalDate,
    val tuenTyyppi: AssistanceActionOptionCategory?,
)

data class FabricChildReservations(
    val varauksenId: UUID,
    val luontiaika: String,
    val muokkausaika: String,
    val lapsenId: UUID,
    val paivamaara: LocalDate,
    val varausAlkaa: String?,
    val varausPaattyy: String?,
    val toteumaAlkaa: String?,
    val toteumaPaattyy: String?,
    val yksikonId: UUID,
)

data class FabricDailyInfo(
    val poimintaAika: String,
    val lapsenId: UUID,
    val henkilöturvatunnus: String?,
    val syntymäaika: LocalDate,
    val kieli: String?,
    val postiosoite: String,
    val postinumero: String,
    val postitoimipaikka: String,
    val kansalaisuudet: List<String>,
    val sijoitustyyppi: PlacementType,
    val sijoitusyksikköId: UUID,
    val yksikönNimi: String,
    val palvelualueId: UUID,
    val palvelualue: String,
    val toimintamuoto: List<CareType>,
    val järjestämistapa: ProviderType,
    val kustannuspaikka: String?,
    val sijoitusryhmäId: UUID,
    val sijoitusryhmä: String,
    val varahoitoyksikköId: UUID?,
    val varahoitoyksikkö: String?,
    val varahoitoryhmäId: UUID?,
    val varahoitoryhmä: String?,
    val palveluntarveMerkitty: Boolean?,
    val palveluntarve: String?,
    val palveluntarveId: UUID?,
    val osapäiväinen: Boolean?,
    val osaviikkoinen: Boolean?,
    val vuorohoito: ShiftCareType?,
    val tuntejaViikossa: Int?,
    val tuentarpeenKerroin: Double?,
    val lapsenKapasiteetti: Double?,
    val poissaolonSyy: List<AbsenceType>,
)

data class FabricDailyUnitsAndGroupsAttendance(
    val poimintaAika: String,
    val päivämäärä: LocalDate?,
    val toimintayksikkö: String,
    val toimintayksikköId: UUID,
    val toimintapäivät: List<Int>,
    val vuorohoitoyksikkö: Boolean?,
    val vuorohoitopäivät: List<Int>?,
    val vuorohoitopyhäpäivinä: Boolean,
    val ryhmä: String,
    val ryhmäId: UUID,
    val toimintayksikönLaskennallinenLapsimäärä: Int?,
    val toimintayksikönLapsimäärä: Int?,
    val henkilökuntaaRyhmässä: Int?,
    val henkilökuntaaLäsnä: Double?,
    val kasvatusvastuullistenLkmYksikössä: Double?,
    val ryhmänLapsimäärä: Int?,
)

data class FabricDailyUnitsOccupanciesConfirmed(
    val poimintaAika: String,
    val päivämäärä: LocalDate,
    val toimintayksikköId: UUID,
    val toimintayksikkö: String,
    val kasvattajienLkm: Double?,
    val sijoituksienLkm: Int?,
    val täyttöasteSumma: Double?,
    val täyttöasteProsentteina: Double?,
)

data class FabricDailyUnitsOccupanciesRealized(
    val poimintaAika: String,
    val päivämäärä: LocalDate,
    val toimintayksikköId: UUID,
    val toimintayksikkö: String,
    val kasvattajienLkm: Double?,
    val sijoituksienLkm: Int?,
    val käyttöasteSumma: Double?,
    val käyttöasteProsentteina: Double?,
)

data class FabricDaycareAssistance(
    val poimintaAika: String,
    val tuentarveId: UUID,
    val luontiaika: String,
    val muokkausaika: String,
    val lapsenId: UUID,
    val tuentarveVarhaiskasvatuksessa: DaycareAssistanceLevel,
    val aloitusPvm: String,
    val loppuPvm: LocalDate,
)

data class FabricFeeDecision(
    val poimintaAika: String,
    val maksupäätöksenNumero: Long?,
    val maksupäätösId: UUID,
    val luontiaika: String,
    val muokkausaika: String,
    val alkupvm: String,
    val loppupvm: LocalDate,
    val huojennustyyppi: FeeDecisionType,
    val perhekoko: Int,
    val kokonaismaksu: Int,
    val tila: FeeDecisionStatus,
    val lapsiId: UUID,
    val lapsikohtainenMaksu: Int,
    val toimintamuoto: PlacementType,
    val palvelualue: String,
    val palvelualueId: UUID,
    val toimipaikka: String,
    val toimipaikkaId: UUID,
    val kustannuspaikka: String?,
)

data class FabricOtherAssistanceMeasure(
    val poimintaAika: String,
    val muuToimiId: UUID,
    val luontiaika: String,
    val muokkausaika: String,
    val lapsenId: UUID,
    val muuToimi: OtherAssistanceMeasureType,
    val aloitusPvm: String,
    val loppuPvm: LocalDate,
)

data class FabricPlacement(
    val poimintaAika: String,
    val lapsenId: UUID,
    val toimintayksikkö: String,
    val toimintayksikköId: UUID,
    val sijoituksenId: UUID,
    val sijoituksenLuontiaika: String,
    val sijoituksenMuokkausaika: String,
    val sijoituksenAlkupvm: LocalDate,
    val sijoituksenLoppupvm: LocalDate,
    val sijoitustyyppi: PlacementType,
    val ryhmä: String,
    val ryhmäId: UUID,
    val ryhmänLuontiaika: String,
    val ryhmänMuokkausaika: String,
    val ryhmänAlkupvm: LocalDate,
    val ryhmänLoppupvm: LocalDate?,
    val palveluntarve: String?,
    val palveluntarveId: UUID?,
    val palveluntarpeenLuontiaika: String?,
    val palveluntarpeenMuokkausaika: String?,
    val palveluntarpeenAlkupvm: LocalDate?,
    val palveluntarpeenLoppupvm: LocalDate?,
)

data class FabricPreschoolAssistance(
    val poimintaAika: String,
    val tuentarveId: UUID,
    val luontiaika: String,
    val muokkausaika: String,
    val lapsenId: UUID,
    val tuentarveEsiopetuksessa: PreschoolAssistanceLevel,
    val aloitusPvm: String,
    val loppuPvm: LocalDate,
)

data class FabricUnitAndGroup(
    val poimintaAika: String,
    val toimintayksikkö: String,
    val toimintayksikköId: UUID,
    val toimintayksikköLuontiaika: String,
    val toimintayksikköMuokkausaika: String,
    val toimintayksikönAlkupvm: LocalDate?,
    val toimintayksikönLoppupvm: LocalDate?,
    val toimintamuoto: List<CareType>,
    val järjestämistapa: ProviderType,
    val katuosoite: String,
    val postinumero: String,
    val postitoimipaikka: String,
    val toimintayksikönLaskennallinenLapsimäärä: Int?,
    val palvelualue: String,
    val palvelualueId: UUID,
    val dwKustannuspaikka: String?,
    val ryhmä: String,
    val ryhmäId: UUID,
    val ryhmänAlkupvm: LocalDate,
    val ryhmänLoppupvm: LocalDate?,
)

data class FabricVoucherValueDecision(
    val poimintaAika: String,
    val arvopäätöksenNumero: Long?,
    val luontiaika: String,
    val muokkausaika: String,
    val alkupvm: LocalDate,
    val loppupvm: LocalDate,
    val huojennustyyppi: VoucherValueDecisionType,
    val perhekoko: Int,
    val palvelusetelinArvo: Int,
    val omavastuuosuus: Int,
    val lapsenId: UUID,
    val toimintamuoto: PlacementType?,
    val tila: VoucherValueDecisionStatus,
    val palvelualue: String,
    val palvelualueId: UUID,
    val toimipaikka: String,
    val toimipaikkaId: UUID,
)
