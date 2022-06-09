// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.daycare.service.AbsenceType
import fi.espoo.evaka.shared.KoskiStudyRightId
import fi.espoo.evaka.shared.Timeline
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.isWeekend
import fi.espoo.evaka.shared.domain.toFiniteDateRange
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json
import java.time.LocalDate
import java.time.Month

data class KoskiData(
    val oppija: Oppija,
    val operation: KoskiOperation,
    val organizationOid: String
)

enum class KoskiOperation {
    CREATE, UPDATE, VOID
}

data class KoskiChildRaw(
    val ssn: String?,
    val personOid: String?,
    val firstName: String,
    val lastName: String
) {
    fun toHenkilö(): Henkilö = when {
        ssn != null && ssn.isNotBlank() -> UusiHenkilö(ssn, firstName, lastName)
        personOid != null && personOid.isNotBlank() -> OidHenkilö(personOid)
        else -> throw IllegalStateException("not enough information available to create Koski Henkilö")
    }
}

data class KoskiUnitRaw(
    val daycareLanguage: String,
    val daycareProviderType: ProviderType,
    val ophUnitOid: String,
    val ophOrganizerOid: String
) {
    fun haeSuoritus(type: OpiskeluoikeudenTyyppiKoodi) = if (type == OpiskeluoikeudenTyyppiKoodi.PREPARATORY) Suoritus(
        koulutusmoduuli = Koulutusmoduuli(
            tunniste = KoulutusmoduulinTunniste(KoulutusmoduulinTunnisteKoodi.PREPARATORY),
            perusteenDiaarinumero = PerusteenDiaarinumero.PREPARATORY
        ),
        toimipiste = Toimipiste(ophUnitOid),
        suorituskieli = Suorituskieli(daycareLanguage.uppercase()),
        tyyppi = SuorituksenTyyppi(SuorituksenTyyppiKoodi.PREPARATORY)
    )
    else Suoritus(
        koulutusmoduuli = Koulutusmoduuli(
            tunniste = KoulutusmoduulinTunniste(KoulutusmoduulinTunnisteKoodi.PRESCHOOL),
            perusteenDiaarinumero = PerusteenDiaarinumero.PRESCHOOL
        ),
        toimipiste = Toimipiste(ophUnitOid),
        suorituskieli = Suorituskieli(daycareLanguage.uppercase()),
        tyyppi = SuorituksenTyyppi(SuorituksenTyyppiKoodi.PRESCHOOL)
    )

    fun haeJärjestämisMuoto() = when (daycareProviderType) {
        ProviderType.PURCHASED -> Järjestämismuoto(JärjestämismuotoKoodi.PURCHASED)
        ProviderType.EXTERNAL_PURCHASED -> Järjestämismuoto(JärjestämismuotoKoodi.PURCHASED)
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
    val studyRightId: KoskiStudyRightId,
    val studyRightOid: String
) {
    fun toKoskiData(sourceSystem: String, ophOrganizationOid: String) = KoskiData(
        oppija = Oppija(
            henkilö = child.toHenkilö(),
            opiskeluoikeudet = listOf(haeOpiskeluOikeus(sourceSystem))
        ),
        organizationOid = ophOrganizationOid,
        operation = KoskiOperation.VOID
    )

    private fun haeOpiskeluOikeus(sourceSystem: String) = Opiskeluoikeus(
        oid = studyRightOid,
        tila = OpiskeluoikeudenTila(listOf(Opiskeluoikeusjakso.mitätöity(voidDate))),
        suoritukset = listOf(unit.haeSuoritus(type)),
        lähdejärjestelmänId = LähdejärjestelmäId(
            id = studyRightId.raw,
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
    val lastOfChild: Boolean,
    val placementRanges: List<FiniteDateRange> = emptyList(),
    val holidays: Set<LocalDate> = emptySet(),
    @Json
    val preparatoryAbsences: List<KoskiPreparatoryAbsence> = emptyList(),
    val developmentalDisability1: List<FiniteDateRange> = emptyList(),
    val developmentalDisability2: List<FiniteDateRange> = emptyList(),
    val extendedCompulsoryEducation: FiniteDateRange? = null,
    val transportBenefit: FiniteDateRange? = null,
    val specialAssistanceDecisionWithGroup: List<FiniteDateRange> = emptyList(),
    val specialAssistanceDecisionWithoutGroup: List<FiniteDateRange> = emptyList(),
    val studyRightId: KoskiStudyRightId,
    val studyRightOid: String?
) {
    private val studyRightTimelines = calculateStudyRightTimelines(
        placementRanges = placementRanges.asSequence(),
        holidays = holidays,
        absences = preparatoryAbsences.asSequence(),
    )

    private val approverTitle = "Esiopetusyksikön johtaja"

    fun toKoskiData(sourceSystem: String, ophOrganizationOid: String, today: LocalDate): KoskiData? {
        val placementSpan = studyRightTimelines.placement.spanningRange() ?: return null

        val termination = if (today.isAfter(placementSpan.end)) {
            val canBeQualified = lastOfChild && when (placementSpan.end.month) {
                Month.MAY, Month.JUNE -> true
                else -> false
            }
            val isQualified = when (type) {
                OpiskeluoikeudenTyyppiKoodi.PRESCHOOL -> canBeQualified
                OpiskeluoikeudenTyyppiKoodi.PREPARATORY -> {
                    // We intentionally only include here absence ranges longer than one week
                    // So, it doesn't matter even if the child is randomly absent for 31 or more individual days
                    // if they don't form long enough continuous absence ranges
                    val totalAbsences = studyRightTimelines.plannedAbsence.addAll(studyRightTimelines.unknownAbsence)
                        .ranges().map { it.durationInDays() }.sum()
                    canBeQualified && totalAbsences <= 30
                }
            }
            if (isQualified) StudyRightTermination.Qualified(placementSpan.end)
            else StudyRightTermination.Resigned(placementSpan.end)
        } else null

        return KoskiData(
            oppija = Oppija(
                henkilö = child.toHenkilö(),
                opiskeluoikeudet = listOf(haeOpiskeluoikeus(sourceSystem, ophOrganizationOid, termination))
            ),
            operation = if (studyRightOid == null) KoskiOperation.CREATE else KoskiOperation.UPDATE,
            organizationOid = ophOrganizationOid
        )
    }

    private fun haeOpiskeluoikeusjaksot(termination: StudyRightTermination?): List<Opiskeluoikeusjakso> {
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
        result.addAll(
            // Make sure we don't end up with duplicate states on the termination dates.
            // For example, if we have 1-day placement, `present` will include the termination date
            (present + gaps + holidays + absent).filterNot {
                it.alku == termination?.date
            }
        )
        when (termination) {
            is StudyRightTermination.Qualified -> result.add(Opiskeluoikeusjakso.valmistunut(termination.date))
            is StudyRightTermination.Resigned -> result.add(Opiskeluoikeusjakso.eronnut(termination.date))
            null -> {} // still ongoing
        }
        result.sortBy { it.alku }
        return result
    }

    private fun haeSuoritus(termination: StudyRightTermination?, ophOrganizationOid: String) = unit.haeSuoritus(type).let {
        val vahvistus = when (termination) {
            is StudyRightTermination.Qualified -> haeVahvistus(termination.date, ophOrganizationOid)
            else -> null
        }
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
            vahvistus = vahvistus
        ) else it.copy(vahvistus = vahvistus)
    }

    fun haeOpiskeluoikeus(sourceSystem: String, ophOrganizationOid: String, termination: StudyRightTermination?): Opiskeluoikeus {
        return Opiskeluoikeus(
            oid = studyRightOid,
            tila = OpiskeluoikeudenTila(haeOpiskeluoikeusjaksot(termination)),
            suoritukset = listOf(haeSuoritus(termination, ophOrganizationOid)),
            lähdejärjestelmänId = LähdejärjestelmäId(
                id = studyRightId.raw,
                lähdejärjestelmä = Lähdejärjestelmä(koodiarvo = sourceSystem)
            ),
            tyyppi = OpiskeluoikeudenTyyppi(type),
            lisätiedot = if (type == OpiskeluoikeudenTyyppiKoodi.PREPARATORY) null else haeLisätiedot(),
            järjestämismuoto = if (type == OpiskeluoikeudenTyyppiKoodi.PREPARATORY) null else unit.haeJärjestämisMuoto()
        )
    }

    fun haeVahvistus(qualifiedDate: LocalDate, ophOrganizationOid: String) = Vahvistus(
        päivä = qualifiedDate,
        paikkakunta = VahvistusPaikkakunta(koodiarvo = VahvistusPaikkakuntaKoodi.ESPOO),
        myöntäjäOrganisaatio = MyöntäjäOrganisaatio(oid = ophOrganizationOid),
        myöntäjäHenkilöt = listOf(
            MyöntäjäHenkilö(
                nimi = approverName,
                titteli = MyöntäjäHenkilönTitteli(approverTitle),
                organisaatio = MyöntäjäOrganisaatio(unit.ophOrganizerOid)
            )
        )
    )

    fun haeLisätiedot(): Lisätiedot? {
        // Koski has an extra validation rule for developmental disability and extended compulsory education date ranges.
        // https://github.com/Opetushallitus/koski/blob/2b25eb7e16bb651e1eb09cce636a736e85811ad0/src/main/scala/fi/oph/koski/validation/KoskiValidator.scala#L579
        // https://github.com/Opetushallitus/koski/pull/1860
        // Since we don't have similar restrictions, adjust data to satisfy Koski requirements
        val adjustedEce = extendedCompulsoryEducation?.let { ece ->
            val disabilities = Timeline.of(developmentalDisability1).addAll(developmentalDisability2)
            val overlaps = disabilities.intersection(Timeline.of(ece))
            overlaps.ranges().maxByOrNull { it.durationInDays() }?.let { Aikajakso.from(it) }
        }

        return Lisätiedot(
            vammainen = developmentalDisability1.map { Aikajakso.from(it) }.takeIf { it.isNotEmpty() && adjustedEce != null },
            vaikeastiVammainen = developmentalDisability2.map { Aikajakso.from(it) }.takeIf { it.isNotEmpty() && adjustedEce != null },
            pidennettyOppivelvollisuus = adjustedEce,
            kuljetusetu = transportBenefit?.let { Aikajakso.from(it) },
            erityisenTuenPäätökset = (
                specialAssistanceDecisionWithGroup.map { ErityisenTuenPäätös.from(it, erityisryhmässä = true) } +
                    specialAssistanceDecisionWithoutGroup.map { ErityisenTuenPäätös.from(it, erityisryhmässä = false) }
                ).takeIf { it.isNotEmpty() }
        ).takeIf { it.vammainen != null || it.vaikeastiVammainen != null || it.pidennettyOppivelvollisuus != null || it.kuljetusetu != null || it.erityisenTuenPäätökset != null }
    }
}

sealed class StudyRightTermination {
    abstract val date: LocalDate
    data class Resigned(override val date: LocalDate) : StudyRightTermination()
    data class Qualified(override val date: LocalDate) : StudyRightTermination()
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
