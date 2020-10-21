// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package fi.espoo.evaka.koski

import fi.espoo.evaka.daycare.domain.ProviderType
import fi.espoo.evaka.derivePreschoolTerm
import fi.espoo.evaka.shared.domain.ClosedPeriod
import org.jdbi.v3.core.mapper.Nested
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

data class KoskiActiveDataRaw(
    @Nested("")
    val child: KoskiChildRaw,
    @Nested("")
    val unit: KoskiUnitRaw,
    val type: OpiskeluoikeudenTyyppiKoodi,
    val approverName: String,
    val personOid: String?,
    val placementRanges: List<ClosedPeriod> = emptyList(),
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

    private val studyRightRanges =
        calculateStudyRightRanges(placementRanges.asSequence(), clampRange = ClosedPeriod(startTerm.start, endTerm.end))

    private val approverTitle = "Esiopetusyksikön johtaja"

    fun toKoskiData(sourceSystem: String, today: LocalDate): KoskiData? {
        // It's possible clamping to preschool term has removed all placements -> no study right can be created
        val lastDateRange = studyRightRanges.lastOrNull() ?: return null

        val isQualified = lastDateRange.end.let {
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
        val result = mutableListOf<Opiskeluoikeusjakso>()
        for (range in studyRightRanges.dropLast(1)) {
            result.add(Opiskeluoikeusjakso.läsnä(range.start))
            result.add(Opiskeluoikeusjakso.väliaikaisestiKeskeytynyt(range.end.plusDays(1)))
        }
        val range = studyRightRanges.last()
        result.add(Opiskeluoikeusjakso.läsnä(range.start))
        when {
            range.end.isAfter(today) -> {
                // still ongoing
            }
            else -> result.add(
                if (isQualified) Opiskeluoikeusjakso.valmistunut(range.end)
                else Opiskeluoikeusjakso.eronnut(range.end)
            )
        }
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
        päivä = studyRightRanges.last().end,
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
 * Calculates active date ranges for a Koski study right.
 *
 * Merges immediately adjacent date ranges, and provides optional clamping.
 *
 * `inclusiveRanges`: a sequence of non-overlapping date ranges that are considered *inclusive* (child is present)
 * `clampRange`: an optional clamping range. If available, all returned date ranges are guaranteed to be contained within this clamping range
 */
fun calculateStudyRightRanges(
    inclusiveRanges: Sequence<ClosedPeriod>,
    clampRange: ClosedPeriod? = null
): List<ClosedPeriod> {
    val iter = inclusiveRanges.sortedWith(compareBy({ it.start }, { it.end }))
        .mapNotNull { if (clampRange == null) it else clampRange.intersection(it) }
        .iterator()

    var current: ClosedPeriod? = null
    val result = mutableListOf<ClosedPeriod>()
    while (iter.hasNext()) {
        current = if (current == null) {
            iter.next()
        } else {
            val next = iter.next()
            check(current.end < next.start)
            if (current.end.plusDays(1) == next.start) {
                ClosedPeriod(current.start, next.end)
            } else {
                result.add(current)
                next
            }
        }
    }
    current?.let { result.add(it) }
    return result
}
