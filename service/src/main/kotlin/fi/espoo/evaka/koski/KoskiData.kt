// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.derivePreschoolTerm
import fi.espoo.evaka.shared.Timeline
import fi.espoo.evaka.shared.domain.ClosedPeriod
import fi.espoo.evaka.shared.domain.isWeekend
import fi.espoo.evaka.shared.domain.toClosedPeriod
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json
import java.time.LocalDate
import java.util.UUID

data class KoskiData(
    val oppija: Oppija,
    val operation: KoskiOperation,
    val organizerOid: String
)

enum class KoskiOperation {
    CREATE, UPDATE, VOID
}

data class KoskiChildRaw(
    val ssn: String,
    val firstName: String,
    val lastName: String
) {
    fun toHenkilö() = UusiHenkilö(ssn, firstName, lastName)
}

data class KoskiUnitRaw(
    val daycareLanguage: String,
    val daycareProviderType: ProviderType,
    val ophUnitOid: String,
    val ophOrganizationOid: String,
    val ophOrganizerOid: String
) {
    fun haeSuoritus(type: OpiskeluoikeudenTyyppiKoodi) = if (type == OpiskeluoikeudenTyyppiKoodi.PREPARATORY) Suoritus(
        koulutusmoduuli = Koulutusmoduuli(
            tunniste = KoulutusmoduulinTunniste(KoulutusmoduulinTunnisteKoodi.PREPARATORY),
            perusteenDiaarinumero = PerusteenDiaarinumero.PREPARATORY
        ),
        toimipiste = Toimipiste(ophUnitOid),
        suorituskieli = Suorituskieli(daycareLanguage.toUpperCase()),
        tyyppi = SuorituksenTyyppi(SuorituksenTyyppiKoodi.PREPARATORY)
    )
    else Suoritus(
        koulutusmoduuli = Koulutusmoduuli(
            tunniste = KoulutusmoduulinTunniste(KoulutusmoduulinTunnisteKoodi.PRESCHOOL),
            perusteenDiaarinumero = PerusteenDiaarinumero.PRESCHOOL
        ),
        toimipiste = Toimipiste(ophUnitOid),
        suorituskieli = Suorituskieli(daycareLanguage.toUpperCase()),
        tyyppi = SuorituksenTyyppi(SuorituksenTyyppiKoodi.PRESCHOOL)
    )

    fun haeJärjestämisMuoto() = when (daycareProviderType) {
        ProviderType.PURCHASED -> Järjestämismuoto(JärjestämismuotoKoodi.PURCHASED)
        ProviderType.PRIVATE_SERVICE_VOUCHER -> Järjestämismuoto(JärjestämismuotoKoodi.PRIVATE_SERVICE_VOUCHER)
        ProviderType.MUNICIPAL -> null
        ProviderType.PRIVATE -> null
        ProviderType.MUNICIPAL_SCHOOL -> null
    }
}

data class KoskiVoidedDataRaw(
    @Nested("")
    val child: KoskiChildRaw,
    @Nested("")
    val unit: KoskiUnitRaw,
    val type: OpiskeluoikeudenTyyppiKoodi,
    val voidDate: LocalDate,
    val studyRightId: UUID,
    val studyRightOid: String
) {
    fun toKoskiData(sourceSystem: String) = KoskiData(
        oppija = Oppija(
            henkilö = child.toHenkilö(),
            opiskeluoikeudet = listOf(haeOpiskeluOikeus(sourceSystem))
        ),
        organizerOid = unit.ophOrganizerOid,
        operation = KoskiOperation.VOID
    )

    private fun haeOpiskeluOikeus(sourceSystem: String) = Opiskeluoikeus(
        oid = studyRightOid,
        tila = OpiskeluoikeudenTila(listOf(Opiskeluoikeusjakso.mitätöity(voidDate))),
        suoritukset = listOf(unit.haeSuoritus(type)),
        lähdejärjestelmänId = LähdejärjestelmäId(
            id = studyRightId,
            lähdejärjestelmä = Lähdejärjestelmä(koodiarvo = sourceSystem)
        ),
        tyyppi = OpiskeluoikeudenTyyppi(type),
        lisätiedot = null,
        järjestämismuoto = unit.haeJärjestämisMuoto()
    )
}

data class KoskiPreparatoryAbsence(val date: LocalDate, val type: AbsenceType)

data class KoskiActiveDataRaw(
    @Nested("")
    val child: KoskiChildRaw,
    @Nested("")
    val unit: KoskiUnitRaw,
    val type: OpiskeluoikeudenTyyppiKoodi,
    val approverName: String,
    val personOid: String?,
    val placementRanges: List<ClosedPeriod> = emptyList(),
    val holidays: List<LocalDate> = emptyList(),
    @Json
    val preparatoryAbsences: List<KoskiPreparatoryAbsence> = emptyList(),
    val developmentalDisability1: List<ClosedPeriod> = emptyList(),
    val developmentalDisability2: List<ClosedPeriod> = emptyList(),
    val extendedCompulsoryEducation: ClosedPeriod? = null,
    val transportBenefit: ClosedPeriod? = null,
    val specialAssistanceDecisionWithGroup: List<ClosedPeriod> = emptyList(),
    val specialAssistanceDecisionWithoutGroup: List<ClosedPeriod> = emptyList(),
    val studyRightId: UUID,
    val studyRightOid: String?
) {
    // Some children are in preschool for 2 years, so they might have multiple placements in different terms
    private val startTerm = derivePreschoolTerm(placementRanges.first().start)
    private val endTerm = derivePreschoolTerm(placementRanges.last().start)

    private val studyRightTimelines = ClosedPeriod(startTerm.start, endTerm.end).let { clampRange ->
        calculateStudyRightTimelines(
            placementRanges = placementRanges.asSequence().mapNotNull { it.intersection(clampRange) },
            holidays = holidays.asSequence().filter { clampRange.includes(it) }.toSet(),
            absences = preparatoryAbsences.asSequence().filter { clampRange.includes(it.date) }
        )
    }

    private val approverTitle = "Esiopetusyksikön johtaja"

    fun toKoskiData(sourceSystem: String, today: LocalDate): KoskiData? {
        // It's possible clamping to preschool term has removed all placements -> no study right can be created
        val placementRange = studyRightTimelines.placement.spanningPeriod() ?: return null

        val isQualified = placementRange.end.let {
            it.isAfter(LocalDate.of(endTerm.end.year, 4, 30)) &&
                it.isBefore(endTerm.end.plusDays(1)) &&
                it.isBefore(today)
        }

        return KoskiData(
            oppija = Oppija(
                henkilö = child.toHenkilö(),
                opiskeluoikeudet = listOf(haeOpiskeluoikeus(sourceSystem, today, isQualified))
            ),
            operation = if (studyRightOid == null) KoskiOperation.CREATE else KoskiOperation.UPDATE,
            organizerOid = unit.ophOrganizerOid
        )
    }

    private fun haeOpiskeluoikeusjaksot(today: LocalDate, isQualified: Boolean): List<Opiskeluoikeusjakso> {
        val placementRange = studyRightTimelines.placement.spanningPeriod() ?: return emptyList()

        val present = studyRightTimelines.present.periods()
            .map { Opiskeluoikeusjakso.läsnä(it.start) }
        val gaps = studyRightTimelines.placement
            .gaps()
            .map { Opiskeluoikeusjakso.väliaikaisestiKeskeytynyt(it.start) }
        val holidays = studyRightTimelines.plannedAbsence.periods()
            .map { Opiskeluoikeusjakso.loma(it.start) }
        val absent = studyRightTimelines.unknownAbsence.periods()
            .map { Opiskeluoikeusjakso.väliaikaisestiKeskeytynyt(it.start) }

        val result = mutableListOf<Opiskeluoikeusjakso>()
        result.addAll((present + gaps + holidays + absent))

        when {
            placementRange.end.isAfter(today) -> {
                // still ongoing
            }
            else -> result.add(
                if (isQualified) Opiskeluoikeusjakso.valmistunut(placementRange.end)
                else Opiskeluoikeusjakso.eronnut(placementRange.end)
            )
        }
        result.sortBy { it.alku }
        return result
    }

    private fun haeSuoritus(isQualified: Boolean) = unit.haeSuoritus(type).let {
        if (type == OpiskeluoikeudenTyyppiKoodi.PREPARATORY) it.copy(
            osasuoritukset = listOf(
                Osasuoritus(
                    koulutusmoduuli = OsasuorituksenKoulutusmoduuli(
                        tunniste = OsasuorituksenTunniste("ai", nimi = LokalisoituTeksti("Suomen kieli")),
                        laajuus = OsasuorituksenLaajuus(
                            arvo = 25,
                            yksikkö = Laajuusyksikkö(koodiarvo = LaajuusyksikköKoodiarvo.VUOSIVIIKKOTUNTI)
                        )
                    ),
                    tyyppi = SuorituksenTyyppi(SuorituksenTyyppiKoodi.PREPARATORY_SUBJECT),
                    arviointi = listOf(
                        Arviointi(
                            arvosana = Arvosana(
                                koodiarvo = ArvosanaKoodiarvo.OSALLISTUNUT
                            ),
                            kuvaus = LokalisoituTeksti(
                                fi = "Keskustelee sujuvasti suomeksi"
                            )
                        )
                    )
                )
            ),
            vahvistus = if (isQualified) haeVahvistus() else null
        ) else it.copy(
            vahvistus = if (isQualified) haeVahvistus() else null
        )
    }

    fun haeOpiskeluoikeus(sourceSystem: String, today: LocalDate, isQualified: Boolean): Opiskeluoikeus {
        return Opiskeluoikeus(
            oid = studyRightOid,
            tila = OpiskeluoikeudenTila(haeOpiskeluoikeusjaksot(today, isQualified)),
            suoritukset = listOf(haeSuoritus(isQualified)),
            lähdejärjestelmänId = LähdejärjestelmäId(
                id = studyRightId,
                lähdejärjestelmä = Lähdejärjestelmä(koodiarvo = sourceSystem)
            ),
            tyyppi = OpiskeluoikeudenTyyppi(type),
            lisätiedot = if (type == OpiskeluoikeudenTyyppiKoodi.PREPARATORY) null else haeLisätiedot(),
            järjestämismuoto = unit.haeJärjestämisMuoto()
        )
    }

    fun haeVahvistus() = Vahvistus(
        päivä = studyRightTimelines.placement.spanningPeriod()!!.end,
        paikkakunta = VahvistusPaikkakunta(koodiarvo = VahvistusPaikkakuntaKoodi.ESPOO),
        myöntäjäOrganisaatio = MyöntäjäOrganisaatio(oid = unit.ophOrganizerOid),
        myöntäjäHenkilöt = listOf(
            MyöntäjäHenkilö(
                nimi = approverName,
                titteli = MyöntäjäHenkilönTitteli(approverTitle),
                organisaatio = MyöntäjäOrganisaatio(unit.ophOrganizationOid)
            )
        )
    )

    fun haeLisätiedot() = Lisätiedot(
        vammainen = developmentalDisability1.map { Aikajakso.from(it) }.takeIf { it.isNotEmpty() },
        vaikeastiVammainen = developmentalDisability2.map { Aikajakso.from(it) }.takeIf { it.isNotEmpty() },
        pidennettyOppivelvollisuus = extendedCompulsoryEducation?.let { Aikajakso.from(it) },
        kuljetusetu = transportBenefit?.let { Aikajakso.from(it) },
        erityisenTuenPäätökset = (
            specialAssistanceDecisionWithGroup.map { ErityisenTuenPäätös.from(it, erityisryhmässä = true) } +
                specialAssistanceDecisionWithoutGroup.map { ErityisenTuenPäätös.from(it, erityisryhmässä = false) }
            ).takeIf { it.isNotEmpty() }
    ).takeIf { it.vammainen != null || it.vaikeastiVammainen != null || it.pidennettyOppivelvollisuus != null || it.kuljetusetu != null || it.erityisenTuenPäätökset != null }
}

/**
 * Fill gaps between periods if those gaps contain only holidays or weekend days
 */
internal fun Timeline.fillWeekendAndHolidayGaps(holidays: Set<LocalDate>) =
    this.addAll(this.gaps().filter { gap -> gap.dates().all { it.isWeekend() || holidays.contains(it) } })

internal data class StudyRightTimelines(
    val placement: Timeline,
    val present: Timeline,
    val plannedAbsence: Timeline,
    val unknownAbsence: Timeline
)

internal fun calculateStudyRightTimelines(
    placementRanges: Sequence<ClosedPeriod>,
    holidays: Set<LocalDate>,
    absences: Sequence<KoskiPreparatoryAbsence>
): StudyRightTimelines {
    val placement = Timeline().addAll(placementRanges)
    val plannedAbsence = Timeline().addAll(
        Timeline()
            .addAll(absences.filter { it.type == AbsenceType.PLANNED_ABSENCE }.map { it.date.toClosedPeriod() })
            .fillWeekendAndHolidayGaps(holidays)
            .intersection(placement)
            .periods().filter { it.durationInDays() > 7 }
    )
    val unknownAbsence = Timeline().addAll(
        Timeline()
            .addAll(absences.filter { it.type == AbsenceType.UNKNOWN_ABSENCE }.map { it.date.toClosedPeriod() })
            .fillWeekendAndHolidayGaps(holidays)
            .intersection(placement)
            .periods().filter { it.durationInDays() > 7 }
    )

    return StudyRightTimelines(
        placement = placement,
        present = placement.removeAll(plannedAbsence).removeAll(unknownAbsence),
        plannedAbsence = plannedAbsence,
        unknownAbsence = unknownAbsence
    )
}
