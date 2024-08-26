// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.absence.AbsenceType
import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.shared.KoskiStudyRightId
import fi.espoo.evaka.shared.data.DateMap
import fi.espoo.evaka.shared.data.DateSet
import fi.espoo.evaka.shared.domain.FiniteDateRange
import fi.espoo.evaka.shared.domain.isWeekend
import fi.espoo.evaka.shared.domain.toFiniteDateRange
import java.time.LocalDate
import java.time.Month
import org.jdbi.v3.core.mapper.Nested
import org.jdbi.v3.json.Json

/**
 * Cached input data version.
 *
 * This number should be incremented to bust the Koski input data cache, for example after making
 * changes to data processing code in this file.
 */
const val KOSKI_DATA_VERSION: Int = 3

data class KoskiData(
    val oppija: Oppija,
    val operation: KoskiOperation,
    val organizationOid: String,
)

enum class KoskiOperation {
    CREATE,
    UPDATE,
    VOID,
}

data class KoskiChildRaw(
    val ssn: String?,
    val ophPersonOid: String?,
    val firstName: String,
    val lastName: String,
) {
    fun toHenkilö(): Henkilö =
        when {
            !ssn.isNullOrBlank() -> UusiHenkilö(ssn, firstName, lastName)
            !ophPersonOid.isNullOrBlank() -> OidHenkilö(ophPersonOid)
            else ->
                throw IllegalStateException(
                    "not enough information available to create Koski Henkilö"
                )
        }
}

data class KoskiUnitRaw(
    val unitLanguage: String,
    val providerType: ProviderType,
    val ophUnitOid: String,
    val ophOrganizerOid: String,
    val approverName: String,
) {
    val approverTitle = "Esiopetusyksikön johtaja"

    fun haeSuoritus(
        type: OpiskeluoikeudenTyyppiKoodi,
        vahvistus: Vahvistus?,
        osasuoritus: Osasuoritus?,
    ) =
        Suoritus(
            when (type) {
                OpiskeluoikeudenTyyppiKoodi.PRESCHOOL ->
                    Koulutusmoduuli(
                        tunniste =
                            KoulutusmoduulinTunniste(KoulutusmoduulinTunnisteKoodi.PRESCHOOL),
                        perusteenDiaarinumero = PerusteenDiaarinumero.PRESCHOOL,
                    )
                OpiskeluoikeudenTyyppiKoodi.PREPARATORY ->
                    Koulutusmoduuli(
                        tunniste =
                            KoulutusmoduulinTunniste(KoulutusmoduulinTunnisteKoodi.PREPARATORY),
                        perusteenDiaarinumero = PerusteenDiaarinumero.PREPARATORY,
                    )
            },
            Toimipiste(ophUnitOid),
            Suorituskieli(unitLanguage.uppercase()),
            SuorituksenTyyppi(
                when (type) {
                    OpiskeluoikeudenTyyppiKoodi.PRESCHOOL -> SuorituksenTyyppiKoodi.PRESCHOOL
                    OpiskeluoikeudenTyyppiKoodi.PREPARATORY -> SuorituksenTyyppiKoodi.PREPARATORY
                }
            ),
            vahvistus,
            osasuoritus?.let { listOf(it) },
        )

    fun haeJärjestämisMuoto(type: OpiskeluoikeudenTyyppiKoodi) =
        when (type) {
            OpiskeluoikeudenTyyppiKoodi.PRESCHOOL ->
                when (providerType) {
                    ProviderType.PURCHASED,
                    ProviderType.EXTERNAL_PURCHASED,
                    ProviderType.PRIVATE -> Järjestämismuoto(JärjestämismuotoKoodi.PURCHASED)
                    ProviderType.PRIVATE_SERVICE_VOUCHER ->
                        Järjestämismuoto(JärjestämismuotoKoodi.PRIVATE_SERVICE_VOUCHER)
                    ProviderType.MUNICIPAL,
                    ProviderType.MUNICIPAL_SCHOOL -> null
                }
            OpiskeluoikeudenTyyppiKoodi.PREPARATORY -> null
        }
}

data class KoskiVoidedDataRaw(
    @Nested("") val child: KoskiChildRaw,
    @Nested("") val unit: KoskiUnitRaw,
    val type: OpiskeluoikeudenTyyppiKoodi,
    val voidDate: LocalDate,
    val studyRightId: KoskiStudyRightId,
    val studyRightOid: String,
) {
    fun toKoskiData(sourceSystem: String, ophOrganizationOid: String) =
        KoskiData(
            oppija =
                Oppija(
                    henkilö = child.toHenkilö(),
                    opiskeluoikeudet = listOf(haeOpiskeluOikeus(sourceSystem)),
                ),
            organizationOid = ophOrganizationOid,
            operation = KoskiOperation.VOID,
        )

    private fun haeOpiskeluOikeus(sourceSystem: String) =
        Opiskeluoikeus(
            oid = studyRightOid,
            tila = OpiskeluoikeudenTila(listOf(Opiskeluoikeusjakso.mitätöity(voidDate))),
            suoritukset = listOf(unit.haeSuoritus(type, vahvistus = null, osasuoritus = null)),
            lähdejärjestelmänId =
                LähdejärjestelmäId(
                    id = studyRightId.raw,
                    lähdejärjestelmä = Lähdejärjestelmä(koodiarvo = sourceSystem),
                ),
            tyyppi = OpiskeluoikeudenTyyppi(type),
            lisätiedot = null,
            järjestämismuoto = unit.haeJärjestämisMuoto(type),
        )
}

sealed class KoskiActiveDataRaw(val type: OpiskeluoikeudenTyyppiKoodi) {
    abstract val child: KoskiChildRaw
    abstract val unit: KoskiUnitRaw
    abstract val studyRightId: KoskiStudyRightId
    abstract val studyRightOid: String?
    abstract val placements: DateSet

    protected abstract fun haeLisätiedot(): Lisätiedot?

    protected abstract fun haeSuoritus(vahvistus: Vahvistus?): Suoritus

    protected abstract fun getTermination(lastPlacementEnd: LocalDate): StudyRightTermination

    protected abstract fun getHolidayDates(): DateSet

    protected abstract fun getInterruptedDates(): DateSet

    fun toKoskiData(
        sourceSystem: String,
        ophOrganizationOid: String,
        ophMunicipalityCode: String,
        today: LocalDate,
    ): KoskiData? {
        val placementSpan = placements.spanningRange() ?: return null

        val termination =
            if (today.isAfter(placementSpan.end)) {
                getTermination(placementSpan.end)
            } else {
                null
            }

        return KoskiData(
            oppija =
                Oppija(
                    henkilö = child.toHenkilö(),
                    opiskeluoikeudet =
                        listOf(
                            haeOpiskeluoikeus(
                                sourceSystem,
                                ophOrganizationOid,
                                ophMunicipalityCode,
                                termination,
                            )
                        ),
                ),
            operation = if (studyRightOid == null) KoskiOperation.CREATE else KoskiOperation.UPDATE,
            organizationOid = ophOrganizationOid,
        )
    }

    fun haeOpiskeluoikeus(
        sourceSystem: String,
        ophOrganizationOid: String,
        ophMunicipalityCode: String,
        termination: StudyRightTermination?,
    ): Opiskeluoikeus {
        val vahvistus =
            when (termination) {
                is StudyRightTermination.Qualified ->
                    Vahvistus(
                        päivä = termination.date,
                        paikkakunta = VahvistusPaikkakunta(koodiarvo = ophMunicipalityCode),
                        myöntäjäOrganisaatio = MyöntäjäOrganisaatio(oid = ophOrganizationOid),
                        myöntäjäHenkilöt =
                            listOf(
                                MyöntäjäHenkilö(
                                    nimi = unit.approverName,
                                    titteli = MyöntäjäHenkilönTitteli(unit.approverTitle),
                                    organisaatio = MyöntäjäOrganisaatio(unit.ophOrganizerOid),
                                )
                            ),
                    )
                else -> null
            }
        return Opiskeluoikeus(
            oid = studyRightOid,
            tila = OpiskeluoikeudenTila(haeOpiskeluoikeusjaksot(termination)),
            suoritukset = listOf(haeSuoritus(vahvistus)),
            lähdejärjestelmänId =
                LähdejärjestelmäId(
                    id = studyRightId.raw,
                    lähdejärjestelmä = Lähdejärjestelmä(koodiarvo = sourceSystem),
                ),
            tyyppi = OpiskeluoikeudenTyyppi(type),
            lisätiedot = haeLisätiedot(),
            järjestämismuoto = unit.haeJärjestämisMuoto(type),
        )
    }

    private fun haeOpiskeluoikeusjaksot(
        termination: StudyRightTermination?
    ): List<Opiskeluoikeusjakso> =
        DateMap.of(placements.ranges(), OpiskeluoikeusjaksonTilaKoodi.PRESENT)
            .set(placements.gaps(), OpiskeluoikeusjaksonTilaKoodi.INTERRUPTED)
            .set(getHolidayDates().ranges(), OpiskeluoikeusjaksonTilaKoodi.HOLIDAY)
            .set(getInterruptedDates().ranges(), OpiskeluoikeusjaksonTilaKoodi.INTERRUPTED)
            .let {
                when (termination) {
                    is StudyRightTermination.Qualified ->
                        it.set(
                            FiniteDateRange(termination.date, LocalDate.MAX),
                            OpiskeluoikeusjaksonTilaKoodi.QUALIFIED,
                        )
                    is StudyRightTermination.Resigned ->
                        it.set(
                            FiniteDateRange(termination.date, LocalDate.MAX),
                            OpiskeluoikeusjaksonTilaKoodi.RESIGNED,
                        )
                    null -> it // still ongoing
                }
            }
            .entries()
            .map { (range, state) ->
                Opiskeluoikeusjakso(alku = range.start, OpiskeluoikeusjaksonTila(state))
            }
            .toList()
}

data class KoskiActivePreschoolDataRaw(
    @Nested("") override val child: KoskiChildRaw,
    @Nested("") override val unit: KoskiUnitRaw,
    override val studyRightId: KoskiStudyRightId,
    override val studyRightOid: String?,
    override val placements: DateSet,
    val lastOfChild: Boolean,
    val specialSupport: DateSet,
    val specialSupportWithDecisionLevel1: DateSet,
    val specialSupportWithDecisionLevel2: DateSet,
    val transportBenefit: DateSet,
) : KoskiActiveDataRaw(OpiskeluoikeudenTyyppiKoodi.PRESCHOOL) {
    override fun getHolidayDates(): DateSet = DateSet.of()

    override fun getInterruptedDates(): DateSet = DateSet.of()

    override fun haeSuoritus(vahvistus: Vahvistus?): Suoritus =
        unit.haeSuoritus(type, vahvistus = vahvistus, osasuoritus = null)

    override fun getTermination(lastPlacementEnd: LocalDate): StudyRightTermination {
        val isQualified =
            lastOfChild &&
                when (lastPlacementEnd.month) {
                    Month.MAY,
                    Month.JUNE -> true
                    else -> false
                }
        return if (isQualified) {
            StudyRightTermination.Qualified(lastPlacementEnd)
        } else {
            StudyRightTermination.Resigned(lastPlacementEnd)
        }
    }

    override fun haeLisätiedot(): Lisätiedot? {
        // The spanning range of all placements (= gaps are *not* considered) is used to trim
        // all the date ranges, because some of them might extend beyond the first/last placements.
        // Example: child changes from unit A to unit B, while having one long ECE date range.
        // When sending the study right for unit A, we need to send the first "half" of the ECE
        // range, and when sending for unit B, the second "half".
        val placementSpan = placements.spanningRange() ?: return null

        val level1 = specialSupportWithDecisionLevel1.intersection(listOf(placementSpan))
        val level2 = specialSupportWithDecisionLevel2.intersection(listOf(placementSpan))
        val specialSupportWithEce = level1.addAll(level2)
        val allSpecialSupport =
            specialSupport.intersection(listOf(placementSpan)).addAll(specialSupportWithEce)

        // Koski only accepts one range
        val longestEce = specialSupportWithEce.ranges().maxByOrNull { it.durationInDays() }
        // Koski only accepts one range
        val longestTransportBenefit =
            transportBenefit
                .ranges()
                .mapNotNull { it.intersection(placementSpan) }
                .maxByOrNull { it.durationInDays() }

        return Lisätiedot(
                vammainen =
                    level1.ranges().map { Aikajakso.from(it) }.toList().takeIf { it.isNotEmpty() },
                vaikeastiVammainen =
                    level2.ranges().map { Aikajakso.from(it) }.toList().takeIf { it.isNotEmpty() },
                pidennettyOppivelvollisuus = longestEce?.let { Aikajakso.from(it) },
                kuljetusetu = longestTransportBenefit?.let { Aikajakso.from(it) },
                erityisenTuenPäätökset =
                    (allSpecialSupport
                        .ranges()
                        .map { ErityisenTuenPäätös.from(it) }
                        .toList()
                        .takeIf { it.isNotEmpty() }),
            )
            .takeIf {
                it.vammainen != null ||
                    it.vaikeastiVammainen != null ||
                    it.pidennettyOppivelvollisuus != null ||
                    it.kuljetusetu != null ||
                    it.erityisenTuenPäätökset != null
            }
    }
}

data class KoskiActivePreparatoryDataRaw(
    @Nested("") override val child: KoskiChildRaw,
    @Nested("") override val unit: KoskiUnitRaw,
    override val studyRightId: KoskiStudyRightId,
    override val studyRightOid: String?,
    override val placements: DateSet,
    val lastOfChild: Boolean,
    val lastOfType: Boolean,
    val holidays: Set<LocalDate>,
    @Json val absences: Map<AbsenceType, Set<LocalDate>>,
) : KoskiActiveDataRaw(OpiskeluoikeudenTyyppiKoodi.PREPARATORY) {
    private val effectiveAbsences = calculatePreparatoryAbsences(placements, holidays, absences)

    override fun getHolidayDates(): DateSet = effectiveAbsences.plannedAbsence

    override fun getInterruptedDates(): DateSet =
        effectiveAbsences.unknownAbsence + effectiveAbsences.sickLeaveAbsence

    override fun haeSuoritus(vahvistus: Vahvistus?): Suoritus =
        unit.haeSuoritus(
            type,
            vahvistus = vahvistus,
            osasuoritus =
                Osasuoritus(
                    OsasuorituksenKoulutusmoduuli(
                        OsasuorituksenTunniste("ai", nimi = LokalisoituTeksti("Suomen kieli")),
                        OsasuorituksenLaajuus(
                            arvo = 25,
                            yksikkö =
                                Laajuusyksikkö(koodiarvo = LaajuusyksikköKoodiarvo.VUOSIVIIKKOTUNTI),
                        ),
                    ),
                    listOf(
                        Arviointi(
                            arvosana = Arvosana(koodiarvo = ArvosanaKoodiarvo.OSALLISTUNUT),
                            kuvaus =
                                LokalisoituTeksti(
                                    fi =
                                        "Suorittanut perusopetukseen valmistavan opetuksen esiopetuksen yhteydessä"
                                ),
                        )
                    ),
                    SuorituksenTyyppi(SuorituksenTyyppiKoodi.PREPARATORY_SUBJECT),
                ),
        )

    override fun getTermination(lastPlacementEnd: LocalDate): StudyRightTermination {
        val canBeQualified =
            if (lastOfChild)
                when (lastPlacementEnd.month) {
                    Month.MAY,
                    Month.JUNE -> true
                    else -> false
                }
            // changing from PREPARATORY to PRESCHOOL is considered qualified
            else lastOfType

        // We intentionally only include here absence ranges longer than one
        // week, so it doesn't matter even if the child is randomly absent for
        // 31 or more individual days if they don't form long enough continuous
        // absence ranges
        val totalAbsences =
            effectiveAbsences.plannedAbsence
                .addAll(effectiveAbsences.unknownAbsence)
                .ranges()
                .map { it.durationInDays() }
                .sum()
        val isQualified = canBeQualified && totalAbsences <= 30
        return if (isQualified) {
            StudyRightTermination.Qualified(lastPlacementEnd)
        } else {
            StudyRightTermination.Resigned(lastPlacementEnd)
        }
    }

    override fun haeLisätiedot(): Lisätiedot? = null
}

sealed class StudyRightTermination {
    abstract val date: LocalDate

    data class Resigned(override val date: LocalDate) : StudyRightTermination()

    data class Qualified(override val date: LocalDate) : StudyRightTermination()
}

/** Fill gaps between periods if those gaps contain only holidays or weekend days */
internal fun DateSet.fillWeekendAndHolidayGaps(holidays: Set<LocalDate>) =
    this.addAll(
        this.gaps().filter { gap -> gap.dates().all { it.isWeekend() || holidays.contains(it) } }
    )

internal data class PreparatoryAbsences(
    val plannedAbsence: DateSet,
    val sickLeaveAbsence: DateSet,
    val unknownAbsence: DateSet,
)

internal fun calculatePreparatoryAbsences(
    placements: DateSet,
    holidays: Set<LocalDate>,
    absences: Map<AbsenceType, Set<LocalDate>>,
): PreparatoryAbsences {
    val plannedAbsence =
        DateSet.of(
            DateSet.of(
                    (absences[AbsenceType.PLANNED_ABSENCE]?.map { it.toFiniteDateRange() }
                        ?: emptyList())
                )
                .addAll(
                    absences[AbsenceType.OTHER_ABSENCE]?.map { it.toFiniteDateRange() }
                        ?: emptyList()
                )
                .fillWeekendAndHolidayGaps(holidays)
                .intersection(placements)
                .ranges()
                .filter { it.durationInDays() > 7 }
        )
    val sickLeaveAbsence =
        DateSet.of(
            DateSet.of(
                    absences[AbsenceType.SICKLEAVE]?.map { it.toFiniteDateRange() } ?: emptyList()
                )
                .fillWeekendAndHolidayGaps(holidays)
                .intersection(placements)
                .ranges()
                .filter { it.durationInDays() > 7 }
        )
    val unknownAbsence =
        DateSet.of(
            DateSet.of(
                    absences[AbsenceType.UNKNOWN_ABSENCE]?.map { it.toFiniteDateRange() }
                        ?: emptyList()
                )
                .fillWeekendAndHolidayGaps(holidays)
                .intersection(placements)
                .ranges()
                .filter { it.durationInDays() > 7 }
        )

    return PreparatoryAbsences(
        plannedAbsence = plannedAbsence,
        sickLeaveAbsence = sickLeaveAbsence,
        unknownAbsence = unknownAbsence,
    )
}
