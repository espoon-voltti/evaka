// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.derivePreschoolTerm
import fi.espoo.evaka.shared.Timeline
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.isWeekend
import fi.espoo.evaka.shared.domain.toFiniteDateRange
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json
import java.time.LocalDate
import java.util.UUID

data class KoskiData(
    val oppija: Oppija,
    val operation: KoskiOperation,
    val organizationOid: String
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
        ProviderType.PRIVATE -> Järjestämismuoto(JärjestämismuotoKoodi.PURCHASED)
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
        organizationOid = unit.ophOrganizationOid,
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
        järjestämismuoto = if (type == OpiskeluoikeudenTyyppiKoodi.PREPARATORY) null else unit.haeJärjestämisMuoto()
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
    val placementRanges: List<FiniteDateRange> = emptyList(),
    val holidays: List<LocalDate> = emptyList(),
    @Json
    val preparatoryAbsences: List<KoskiPreparatoryAbsence> = emptyList(),
    val developmentalDisability1: List<FiniteDateRange> = emptyList(),
    val developmentalDisability2: List<FiniteDateRange> = emptyList(),
    val extendedCompulsoryEducation: FiniteDateRange? = null,
    val transportBenefit: FiniteDateRange? = null,
    val specialAssistanceDecisionWithGroup: List<FiniteDateRange> = emptyList(),
    val specialAssistanceDecisionWithoutGroup: List<FiniteDateRange> = emptyList(),
    val studyRightId: UUID,
    val studyRightOid: String?
) {
    // Some children are in preschool for 2 years, so they might have multiple placements in different terms
    private val startTerm = derivePreschoolTerm(placementRanges.first().start)
    private val endTerm = derivePreschoolTerm(placementRanges.last().start)

    private val studyRightTimelines = FiniteDateRange(startTerm.start, endTerm.end).let { clampRange ->
        calculateStudyRightTimelines(
            placementRanges = placementRanges.asSequence().mapNotNull { it.intersection(clampRange) },
            holidays = holidays.asSequence().filter { clampRange.includes(it) }.toSet(),
            absences = preparatoryAbsences.asSequence().filter { clampRange.includes(it.date) }
        )
    }

    private val approverTitle = "Esiopetusyksikön johtaja"

    fun toKoskiData(sourceSystem: String, today: LocalDate): KoskiData? {
        // It's possible clamping to preschool term has removed all placements -> no study right can be created
        val placementSpan = studyRightTimelines.placement.spanningRange() ?: return null

        val isQualifiedByDate = placementSpan.end.let {
            it.isAfter(LocalDate.of(endTerm.end.year, 4, 30)) && it.isBefore(today)
        }
        val qualifiedDate = placementSpan.end.takeIf {
            when (type) {
                OpiskeluoikeudenTyyppiKoodi.PRESCHOOL -> isQualifiedByDate
                OpiskeluoikeudenTyyppiKoodi.PREPARATORY -> {
                    // We intentionally only include here absence ranges longer than one week
                    // So, it doesn't matter even if the child is randomly absent for 31 or more individual days
                    // if they don't form long enough continuous absence ranges
                    val totalAbsences = studyRightTimelines.plannedAbsence.addAll(studyRightTimelines.unknownAbsence)
                        .ranges().map { it.durationInDays() }.sum()
                    isQualifiedByDate && totalAbsences <= 30
                }
            }
        }

        return KoskiData(
            oppija = Oppija(
                henkilö = child.toHenkilö(),
                opiskeluoikeudet = listOf(haeOpiskeluoikeus(sourceSystem, today, qualifiedDate))
            ),
            operation = if (studyRightOid == null) KoskiOperation.CREATE else KoskiOperation.UPDATE,
            organizationOid = unit.ophOrganizationOid
        )
    }

    private fun haeOpiskeluoikeusjaksot(today: LocalDate, qualifiedDate: LocalDate?): List<Opiskeluoikeusjakso> {
        val placementSpan = studyRightTimelines.placement.spanningRange() ?: return emptyList()

        val present = studyRightTimelines.present.ranges()
            .map { Opiskeluoikeusjakso.läsnä(it.start) }
        val gaps = studyRightTimelines.placement
            .gaps()
            .map { Opiskeluoikeusjakso.väliaikaisestiKeskeytynyt(it.start) }
        val holidays = studyRightTimelines.plannedAbsence.ranges()
            .map { Opiskeluoikeusjakso.loma(it.start) }
        val absent = studyRightTimelines.unknownAbsence.ranges()
            .map { Opiskeluoikeusjakso.väliaikaisestiKeskeytynyt(it.start) }

        val result = mutableListOf<Opiskeluoikeusjakso>()
        result.addAll((present + gaps + holidays + absent))

        when {
            placementSpan.end.isAfter(today) -> {
                // still ongoing
            }
            else -> result.add(
                if (qualifiedDate != null) Opiskeluoikeusjakso.valmistunut(qualifiedDate)
                else Opiskeluoikeusjakso.eronnut(placementSpan.end)
            )
        }
        result.sortBy { it.alku }
        return result
    }

    private fun haeSuoritus(qualifiedDate: LocalDate?) = unit.haeSuoritus(type).let {
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
                                fi = "Suorittanut perusopetukseen valmistavan opetuksen esiopetuksen yhteydessä"
                            )
                        )
                    )
                )
            ),
            vahvistus = if (qualifiedDate != null) haeVahvistus(qualifiedDate) else null
        ) else it.copy(
            vahvistus = if (qualifiedDate != null) haeVahvistus(qualifiedDate) else null
        )
    }

    fun haeOpiskeluoikeus(sourceSystem: String, today: LocalDate, qualifiedDate: LocalDate?): Opiskeluoikeus {
        return Opiskeluoikeus(
            oid = studyRightOid,
            tila = OpiskeluoikeudenTila(haeOpiskeluoikeusjaksot(today, qualifiedDate)),
            suoritukset = listOf(haeSuoritus(qualifiedDate)),
            lähdejärjestelmänId = LähdejärjestelmäId(
                id = studyRightId,
                lähdejärjestelmä = Lähdejärjestelmä(koodiarvo = sourceSystem)
            ),
            tyyppi = OpiskeluoikeudenTyyppi(type),
            lisätiedot = if (type == OpiskeluoikeudenTyyppiKoodi.PREPARATORY) null else haeLisätiedot(),
            järjestämismuoto = if (type == OpiskeluoikeudenTyyppiKoodi.PREPARATORY) null else unit.haeJärjestämisMuoto()
        )
    }

    fun haeVahvistus(qualifiedDate: LocalDate) = Vahvistus(
        päivä = qualifiedDate,
        paikkakunta = VahvistusPaikkakunta(koodiarvo = VahvistusPaikkakuntaKoodi.ESPOO),
        myöntäjäOrganisaatio = MyöntäjäOrganisaatio(oid = unit.ophOrganizationOid),
        myöntäjäHenkilöt = listOf(
            MyöntäjäHenkilö(
                nimi = approverName,
                titteli = MyöntäjäHenkilönTitteli(approverTitle),
                organisaatio = MyöntäjäOrganisaatio(unit.ophOrganizerOid)
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
    placementRanges: Sequence<FiniteDateRange>,
    holidays: Set<LocalDate>,
    absences: Sequence<KoskiPreparatoryAbsence>
): StudyRightTimelines {
    val placement = Timeline().addAll(placementRanges)
    val plannedAbsence = Timeline().addAll(
        Timeline()
            .addAll(
                absences.filter { it.type == AbsenceType.PLANNED_ABSENCE || it.type == AbsenceType.OTHER_ABSENCE }
                    .map { it.date.toFiniteDateRange() }
            )
            .fillWeekendAndHolidayGaps(holidays)
            .intersection(placement)
            .ranges().filter { it.durationInDays() > 7 }
    )
    val unknownAbsence = Timeline().addAll(
        Timeline()
            .addAll(absences.filter { it.type == AbsenceType.UNKNOWN_ABSENCE }.map { it.date.toFiniteDateRange() })
            .fillWeekendAndHolidayGaps(holidays)
            .intersection(placement)
            .ranges().filter { it.durationInDays() > 7 }
    )

    return StudyRightTimelines(
        placement = placement,
        present = placement.removeAll(plannedAbsence).removeAll(unknownAbsence),
        plannedAbsence = plannedAbsence,
        unknownAbsence = unknownAbsence
    )
}
