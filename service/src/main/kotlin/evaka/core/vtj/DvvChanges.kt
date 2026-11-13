// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.vtj

import evaka.core.dvv.Aidinkieli
import evaka.core.dvv.DefaultTietoryhma
import evaka.core.dvv.DvvDate
import evaka.core.dvv.DvvModification
import evaka.core.dvv.HenkilonNimi
import evaka.core.dvv.HenkilotunnusKorjaus
import evaka.core.dvv.HuollettavaSuppea
import evaka.core.dvv.HuoltajaSuppea
import evaka.core.dvv.Huoltosuhde
import evaka.core.dvv.Kotikunta
import evaka.core.dvv.KotimainenOsoite
import evaka.core.dvv.KotimainenPostiosoite
import evaka.core.dvv.Kuolinpaiva
import evaka.core.dvv.Tietoryhma
import evaka.core.dvv.Turvakielto
import evaka.core.dvv.UlkomainenOsoite
import evaka.core.dvv.VakinainenKotimainenAsuinpaikkatunnus
import evaka.core.dvv.startDate
import evaka.core.identity.Ssn
import evaka.core.shared.domain.DateRange
import java.time.LocalDate

/**
 * [newRelatives] are the guardians or dependants that the changes add. [unknown] lists the groups
 * and change types that the rules do not know, as `GROUP:TYPE`.
 */
data class DvvChangesResult(
    val person: StoredVtjPerson,
    val newRelatives: Set<Ssn>,
    val unknown: List<String>,
)

/** Applies the DVV modifications of one person, in the order of DVV, to the stored VTJ data. */
fun applyDvvChanges(
    stored: StoredVtjPerson,
    changes: List<DvvModification>,
    today: LocalDate,
): DvvChangesResult {
    val state = DvvChangeState(stored)
    changes.forEach { change ->
        val (removals, others) =
            change.tietoryhmat.partition {
                it.muutosattribuutti == Muutosattribuutti.KORJATTAVA ||
                    it.muutosattribuutti == Muutosattribuutti.POISTETTU
            }
        // `tietoryhmat` is in arbitrary order, so we apply removals first. KORJATTAVA is considered
        // a removal because it lists previous data that is no longer valid.
        // Source: https://hiekkalaatikko.muutostietopalvelu.cloud.dvv.fi/usage/examples, sections
        // "Vastaussanoman rakenne" and "Muutosattribuutit"
        (removals + others).forEach { state.apply(it) }
    }
    return state.result(today)
}

fun compareStoredVtjPersons(
    predicted: StoredVtjPerson,
    stored: StoredVtjPerson,
): List<VtjFieldDifference> {
    val differences =
        VtjDifferences(
            restricted = predicted.restrictedDetailsEnabled || stored.restrictedDetailsEnabled
        )
    with(differences) {
        text("ssn", predicted.ssn.value, stored.ssn.value)
        text("firstName", predicted.firstName, stored.firstName)
        text("lastName", predicted.lastName, stored.lastName)
        code("language", predicted.language, stored.language)
        date("dateOfDeath", predicted.dateOfDeath, stored.dateOfDeath)
        code(
            "restrictedDetails.enabled",
            predicted.restrictedDetailsEnabled,
            stored.restrictedDetailsEnabled,
        )
        date(
            "restrictedDetails.endDate",
            predicted.restrictedDetailsEndDate,
            stored.restrictedDetailsEndDate,
        )
        text("streetAddress", predicted.streetAddress, stored.streetAddress)
        text("postalCode", predicted.postalCode, stored.postalCode)
        text("postOffice", predicted.postOffice, stored.postOffice)
        text("residenceCode", predicted.residenceCode, stored.residenceCode)
        code(
            "municipalityOfResidence",
            predicted.municipalityOfResidence,
            stored.municipalityOfResidence,
        )
        mailingAddressPeriods(
            predicted.addressPeriods.map { it.toMailingAddressPeriod() },
            stored.addressPeriods.map { it.toMailingAddressPeriod() },
        )
    }
    return differences.list +
        listOfNotNull(
            if (predicted.guardians != null && stored.guardians != null)
                ssnSetDifference("guardians", predicted.guardians, stored.guardians)
            else null,
            if (predicted.dependants != null && stored.dependants != null)
                ssnSetDifference("dependants", predicted.dependants, stored.dependants)
            else null,
        )
}

enum class Muutosattribuutti {
    LISATIETO,
    LISATTY,
    MUUTETTU,
    POISTETTU,
    KORJATTAVA,
    KORJATTU,
}

private data class Period<T>(val start: LocalDate?, val end: LocalDate?, val value: T) {
    fun isValidOn(date: LocalDate) =
        (start == null || start <= date) && (end == null || end >= date)
}

/**
 * Periods of data, for example the mailing addresses of each type or the guardianship of each
 * guardian. A period of the stored data has no start date, and any period of the same key from DVV
 * replaces it.
 */
private class Periods<K, T> {
    private val periods = mutableMapOf<Pair<K, LocalDate?>, Period<T>>()

    fun add(key: K, period: Period<T>) {
        periods[key to period.start] = period
    }

    fun upsert(key: K, period: Period<T>) {
        periods.remove(key to null)
        add(key, period)
    }

    fun remove(key: K, start: LocalDate?) {
        periods.remove(key to start)
        periods.remove(key to null)
    }

    fun replace(key: K, newPeriods: List<Period<T>>) {
        periods.keys.removeIf { it.first == key }
        newPeriods.forEach { add(key, it) }
    }

    fun all(): List<Pair<K, Period<T>>> = periods.map { (key, period) -> key.first to period }

    /** The period with the latest start among the periods valid on [date] */
    fun current(key: K, date: LocalDate): T? =
        all()
            .filter { it.first == key && it.second.isValidOn(date) }
            .maxWithOrNull(compareBy(nullsFirst<LocalDate>()) { it.second.start })
            ?.second
            ?.value
}

/** The text in both languages, or only in the stored language */
private data class LocalizedText(val fi: String, val sv: String?) {
    fun byLanguage(swedish: Boolean): String = sv?.takeIf { swedish && it.isNotEmpty() } ?: fi
}

private class DvvChangeState(private val stored: StoredVtjPerson) {
    private var ssn = stored.ssn
    private var dateOfDeath = stored.dateOfDeath
    private var restricted = stored.restrictedDetailsEnabled
    private var restrictedEndDate = stored.restrictedDetailsEndDate
    private val names =
        Periods<Unit, Pair<String, String>>().apply {
            add(Unit, Period(null, null, stored.firstName to stored.lastName))
        }
    private val languages =
        Periods<Unit, String>().apply { add(Unit, Period(null, null, stored.language)) }
    private val residentialAddresses =
        Periods<Unit, PersonAddress?>().apply {
            add(
                Unit,
                Period(
                    null,
                    null,
                    PersonAddress(stored.streetAddress, stored.postalCode, stored.postOffice),
                ),
            )
        }
    private val residenceCodes =
        Periods<Unit, String?>().apply { add(Unit, Period(null, null, stored.residenceCode)) }
    private val municipalities =
        Periods<Unit, LocalizedText?>().apply {
            add(Unit, Period(null, null, LocalizedText(stored.municipalityOfResidence, null)))
        }
    private val addressPeriods =
        Periods<AddressPeriodType, PersonAddress>().apply {
            stored.addressPeriods.forEach {
                add(
                    it.type,
                    Period(
                        it.validDuring.start,
                        it.validDuring.end,
                        PersonAddress(it.streetAddress, it.postalCode, it.postOffice),
                    ),
                )
            }
        }
    private val guardians = stored.guardians?.let(::relations)
    private val dependants = stored.dependants?.let(::relations)
    private val unknown = mutableListOf<String>()

    fun apply(group: Tietoryhma) {
        val type = group.muutosattribuutti
        if (type == Muutosattribuutti.LISATIETO) {
            // LISATIETO must be ignored
            // Source: https://hiekkalaatikko.muutostietopalvelu.cloud.dvv.fi/usage/examples,
            // section "Muutosattribuutit"
            return
        }
        when (group) {
            is HenkilotunnusKorjaus -> {
                if (type != Muutosattribuutti.KORJATTAVA) {
                    group.aktiivinenHenkilotunnus?.let(Ssn::parse)?.let { ssn = it }
                }
            }
            is HenkilonNimi ->
                applyPeriod(names, type, group, { it.alkupv }, { null }) {
                    it.etunimi to it.sukunimi
                }
            is Aidinkieli ->
                applyPeriod(languages, type, group, { it.alkupv }, { it.loppupv }) { it.kielikoodi }
            is Kuolinpaiva ->
                when (type) {
                    Muutosattribuutti.KORJATTAVA -> {}
                    Muutosattribuutti.POISTETTU ->
                        dateOfDeath =
                            group.voimassaolevatTiedot
                                ?.filterIsInstance<Kuolinpaiva>()
                                ?.firstOrNull { it.kuollut == true }
                                ?.kuolinpv
                                ?.asLocalDate()
                    else ->
                        dateOfDeath =
                            if (group.kuollut == true) group.kuolinpv?.asLocalDate() else null
                }
            is Turvakielto ->
                when (type) {
                    Muutosattribuutti.KORJATTAVA -> {}
                    Muutosattribuutti.POISTETTU -> {
                        val remaining =
                            group.voimassaolevatTiedot
                                ?.filterIsInstance<Turvakielto>()
                                ?.firstOrNull()
                        restricted = remaining?.turvakieltoAktiivinen == true
                        restrictedEndDate = remaining?.turvaLoppuPv?.asLocalDate()
                    }
                    else -> {
                        restricted = group.turvakieltoAktiivinen
                        restrictedEndDate = group.turvaLoppuPv?.asLocalDate()
                    }
                }
            is KotimainenOsoite ->
                if (group.tietoryhma == VAKINAINEN_KOTIMAINEN_OSOITE) {
                    applyPeriod(residentialAddresses, type, group, { it.alkupv }, { it.loppupv }) {
                        it.toPersonAddress()
                    }
                } else {
                    applyAddressPeriod(
                        AddressPeriodType.TEMPORARY,
                        type,
                        group,
                        censored = group.turvakiellonAlaisetKentat != null,
                    ) {
                        it.toTemporaryAddressPeriod()
                    }
                }
            is KotimainenPostiosoite ->
                if (group.tietoryhma == KOTIMAINEN_POSTIOSOITE) {
                    applyAddressPeriod(
                        AddressPeriodType.MAILING,
                        type,
                        group,
                        censored = group.turvakiellonAlaisetKentat != null,
                    ) {
                        it.toMailingAddressPeriod()
                    }
                }
            is Kotikunta ->
                applyPeriod(municipalities, type, group, { it.kuntaanMuuttopv }, { null }) {
                    it.kuntakoodi?.let(::municipalityByCode)?.let { name ->
                        LocalizedText(name.fi, name.sv)
                    }
                }
            is VakinainenKotimainenAsuinpaikkatunnus ->
                applyPeriod(residenceCodes, type, group, { it.alkupv }, { it.loppupv }) {
                    it.asuinpaikantunnus
                }
            is HuoltajaSuppea -> guardians?.let { applyCustody(it, type, group) }
            is HuollettavaSuppea -> dependants?.let { applyCustody(it, type, group) }
            is UlkomainenOsoite -> {}
            is DefaultTietoryhma -> unknown += "${group.tietoryhma}:$type"
        }
    }

    fun result(today: LocalDate): DvvChangesResult {
        val language = languages.current(Unit, today) ?: ""
        val swedish = language == "sv"
        val name = names.current(Unit, today)
        val residentialAddress = residentialAddresses.current(Unit, today).takeUnless { restricted }
        val newGuardians = guardians?.let { currentRelations(it, today) }
        val newDependants = dependants?.let { currentRelations(it, today) }
        return DvvChangesResult(
            person =
                StoredVtjPerson(
                    ssn = ssn,
                    firstName = name?.first ?: "",
                    lastName = name?.second ?: "",
                    language = language,
                    dateOfDeath = dateOfDeath,
                    restrictedDetailsEnabled = restricted,
                    restrictedDetailsEndDate = restrictedEndDate.takeIf { restricted },
                    streetAddress = residentialAddress?.streetAddressByLanguage(swedish) ?: "",
                    postalCode = residentialAddress?.postalCode ?: "",
                    postOffice = residentialAddress?.postOfficeByLanguage(swedish) ?: "",
                    residenceCode = residenceCodes.current(Unit, today) ?: "",
                    municipalityOfResidence =
                        municipalities
                            .current(Unit, today)
                            ?.takeUnless { restricted }
                            ?.byLanguage(swedish) ?: "",
                    addressPeriods =
                        if (restricted) emptySet()
                        else
                            addressPeriods
                                .all()
                                .filter { (_, period) -> period.end == null || period.end >= today }
                                .mapNotNull { (type, period) ->
                                    period.start?.let { start ->
                                        AddressPeriodRow(
                                            type = type,
                                            validDuring = DateRange(start, period.end),
                                            streetAddress =
                                                period.value.streetAddressByLanguage(swedish),
                                            postalCode = period.value.postalCode ?: "",
                                            postOffice = period.value.postOfficeByLanguage(swedish),
                                        )
                                    }
                                }
                                .toSet(),
                    guardians = newGuardians,
                    dependants = newDependants,
                    blockedRelatives = stored.blockedRelatives,
                ),
            newRelatives =
                (newGuardians.orEmpty() + newDependants.orEmpty()) -
                    (stored.guardians.orEmpty() + stored.dependants.orEmpty()),
            unknown = unknown,
        )
    }

    private fun <G : Tietoryhma, T> applyPeriod(
        periods: Periods<Unit, T>,
        type: Muutosattribuutti?,
        group: G,
        start: (G) -> DvvDate?,
        end: (G) -> DvvDate?,
        value: (G) -> T,
    ) {
        fun period(g: G) = Period(start(g)?.asLocalDate(), end(g)?.asLocalDate(), value(g))
        when (type) {
            Muutosattribuutti.KORJATTAVA -> periods.remove(Unit, start(group)?.asLocalDate())

            // `POISTETTU` has `voimassaolevatTiedot`, it remains after the modification
            Muutosattribuutti.POISTETTU -> {
                val remaining = group.voimassaolevatTiedot
                if (remaining == null) {
                    periods.remove(Unit, start(group)?.asLocalDate())
                } else {
                    periods.replace(
                        Unit,
                        remaining
                            .filter { it.tietoryhma == group.tietoryhma }
                            .map { @Suppress("UNCHECKED_CAST") period(it as G) },
                    )
                }
            }
            else -> periods.upsert(Unit, period(group))
        }
    }

    private fun <G : Tietoryhma> applyAddressPeriod(
        type: AddressPeriodType,
        changeType: Muutosattribuutti?,
        group: G,
        censored: Boolean,
        period: (G) -> MailingAddressPeriod?,
    ) {
        fun toPeriod(g: G) =
            period(g)?.let { Period(it.validDuring.start, it.validDuring.end, it.address) }
        val start = group.startDate()
        when {
            // eVaka does not keep an address that restricted details hide
            censored -> addressPeriods.replace(type, emptyList())
            changeType == Muutosattribuutti.KORJATTAVA -> addressPeriods.remove(type, start)
            changeType == Muutosattribuutti.POISTETTU -> {
                val remaining = group.voimassaolevatTiedot
                if (remaining == null) addressPeriods.remove(type, start)
                else
                    addressPeriods.replace(
                        type,
                        remaining
                            .filter { it.tietoryhma == group.tietoryhma }
                            .mapNotNull { @Suppress("UNCHECKED_CAST") toPeriod(it as G) },
                    )
            }
            else -> toPeriod(group)?.let { addressPeriods.upsert(type, it) }
        }
    }

    private fun applyCustody(
        relations: Periods<Ssn, Boolean>,
        type: Muutosattribuutti?,
        group: Huoltosuhde,
    ) {
        val relative = group.relative.henkilotunnus?.let(Ssn::parse) ?: return
        val start = group.huoltosuhteenAlkupv?.asLocalDate()
        when (type) {
            Muutosattribuutti.KORJATTAVA,
            Muutosattribuutti.POISTETTU -> relations.remove(relative, start)
            else ->
                relations.upsert(
                    relative,
                    Period(
                        start,
                        group.huoltosuhteenLoppupv?.asLocalDate(),
                        group.huoltajanLaji in dvvGuardianTypes,
                    ),
                )
        }
    }

    private fun currentRelations(relations: Periods<Ssn, Boolean>, today: LocalDate): Set<Ssn> =
        relations
            .all()
            .filter { (_, period) -> period.value && period.isValidOn(today) }
            .map { it.first }
            .toSet() - stored.blockedRelatives
}

private fun relations(ssns: Set<Ssn>) =
    Periods<Ssn, Boolean>().apply { ssns.forEach { add(it, Period(null, null, true)) } }

private fun AddressPeriodRow.toMailingAddressPeriod() =
    MailingAddressPeriod(type, validDuring, PersonAddress(streetAddress, postalCode, postOffice))
