// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.dvv

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import evaka.core.vtj.Muutosattribuutti
import java.time.LocalDate

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "tietoryhma",
    defaultImpl = DefaultTietoryhma::class,
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(value = HenkilotunnusKorjaus::class, name = "HENKILOTUNNUS_KORJAUS"),
    JsonSubTypes.Type(value = HenkilonNimi::class, name = "HENKILON_NIMI"),
    JsonSubTypes.Type(value = Aidinkieli::class, name = "AIDINKIELI"),
    JsonSubTypes.Type(value = Kuolinpaiva::class, name = "KUOLINPAIVA"),
    JsonSubTypes.Type(value = Turvakielto::class, name = "TURVAKIELTO"),
    JsonSubTypes.Type(value = HuoltajaSuppea::class, name = "HUOLTAJA_SUPPEA"),
    JsonSubTypes.Type(value = HuollettavaSuppea::class, name = "HUOLLETTAVA_SUPPEA"),
    JsonSubTypes.Type(
        value = KotimainenOsoite::class,
        names = ["VAKINAINEN_KOTIMAINEN_OSOITE", "TILAPAINEN_KOTIMAINEN_OSOITE"],
    ),
    JsonSubTypes.Type(
        value = UlkomainenOsoite::class,
        names = ["VAKINAINEN_ULKOMAINEN_OSOITE", "TILAPAINEN_ULKOMAINEN_OSOITE"],
    ),
    JsonSubTypes.Type(
        value = KotimainenPostiosoite::class,
        names = ["KOTIMAINEN_POSTIOSOITE", "KOTIMAINEN_YHTEYSOSOITE"],
    ),
    JsonSubTypes.Type(value = Kotikunta::class, name = "KOTIKUNTA"),
    JsonSubTypes.Type(
        value = VakinainenKotimainenAsuinpaikkatunnus::class,
        name = "VAKINAINEN_KOTIMAINEN_ASUINPAIKKATUNNUS",
    ),
)
sealed interface Tietoryhma {
    val tietoryhma: String
    // Responses from perustietohaku don't have muutosattribuutti
    val muutosattribuutti: Muutosattribuutti?

    /** The data that is valid after the change, in the basic info form of the same group */
    val voimassaolevatTiedot: List<Tietoryhma>?
}

/** A group that eVaka does not read, for example a group outside the VAKA product */
data class DefaultTietoryhma(
    override val tietoryhma: String,
    override val muutosattribuutti: Muutosattribuutti? = null,
    override val voimassaolevatTiedot: List<Tietoryhma>? = null,
    val alkupv: DvvDate? = null,
    val loppupv: DvvDate? = null,
) : Tietoryhma

/**
 * The row of a corrected SSN has this group with the active SSN in [aktiivinenHenkilotunnus]. The
 * row of the active SSN can have it too, with its own SSN.
 */
data class HenkilotunnusKorjaus(
    override val tietoryhma: String,
    override val muutosattribuutti: Muutosattribuutti? = null,
    override val voimassaolevatTiedot: List<Tietoryhma>? = null,
    val aktiivinenHenkilotunnus: String? = null,
    val edellisetHenkilotunnukset: List<String>? = null,
) : Tietoryhma

data class HenkilonNimi(
    override val tietoryhma: String,
    override val muutosattribuutti: Muutosattribuutti? = null,
    override val voimassaolevatTiedot: List<Tietoryhma>? = null,
    val etunimi: String,
    val sukunimi: String,
    val etunimiUTF8: String,
    val sukunimiUTF8: String,
    val alkupv: DvvDate? = null,
) : Tietoryhma

data class Aidinkieli(
    override val tietoryhma: String,
    override val muutosattribuutti: Muutosattribuutti? = null,
    override val voimassaolevatTiedot: List<Tietoryhma>? = null,
    val kielikoodi: String,
    val kielikoodiISO6392: String,
    val nimi: String? = null,
    val alkupv: DvvDate? = null,
    val loppupv: DvvDate? = null,
) : Tietoryhma

data class Kuolinpaiva(
    override val tietoryhma: String,
    override val muutosattribuutti: Muutosattribuutti? = null,
    override val voimassaolevatTiedot: List<Tietoryhma>? = null,
    val kuollut: Boolean,
    val kuolinpv: DvvDate? = null,
) : Tietoryhma

data class Turvakielto(
    override val tietoryhma: String,
    override val muutosattribuutti: Muutosattribuutti? = null,
    override val voimassaolevatTiedot: List<Tietoryhma>? = null,
    val turvakieltoAktiivinen: Boolean,
    val turvaLoppuPv: DvvDate? = null,
) : Tietoryhma

/**
 * [HuoltajaSuppea] or [HuollettavaSuppea]. [relative] is the guardian or the dependant of the
 * person of the row.
 */
sealed interface Huoltosuhde : Tietoryhma {
    val relative: DvvSsn
    val huoltajanLaji: String?
    val huoltosuhteenAlkupv: DvvDate?
    val huoltosuhteenLoppupv: DvvDate?
    val asuminen: String?
    val oikeudet: List<DvvCustodyRight>?
}

data class HuoltajaSuppea(
    override val tietoryhma: String,
    override val muutosattribuutti: Muutosattribuutti? = null,
    override val voimassaolevatTiedot: List<Tietoryhma>? = null,
    val huoltaja: DvvSsn,
    val huoltajanRooli: String,
    override val huoltajanLaji: String? = null,
    override val huoltosuhteenAlkupv: DvvDate? = null,
    override val huoltosuhteenLoppupv: DvvDate? = null,
    override val asuminen: String? = null,
    override val oikeudet: List<DvvCustodyRight>? = null,
) : Huoltosuhde {
    @get:JsonIgnore
    override val relative: DvvSsn
        get() = huoltaja
}

data class HuollettavaSuppea(
    override val tietoryhma: String,
    override val muutosattribuutti: Muutosattribuutti? = null,
    override val voimassaolevatTiedot: List<Tietoryhma>? = null,
    val huollettava: DvvSsn,
    val huoltajanRooli: String,
    override val huoltajanLaji: String? = null,
    override val huoltosuhteenAlkupv: DvvDate? = null,
    override val huoltosuhteenLoppupv: DvvDate? = null,
    override val asuminen: String? = null,
    override val oikeudet: List<DvvCustodyRight>? = null,
) : Huoltosuhde {
    @get:JsonIgnore
    override val relative: DvvSsn
        get() = huollettava
}

/** The person of a guardianship, of whom eVaka reads only the SSN */
data class DvvSsn(val henkilotunnus: String? = null)

data class DvvCustodyRight(
    val oikeus: String? = null,
    val oikeudenAlkupv: DvvDate? = null,
    val oikeudenLoppupv: DvvDate? = null,
)

/** A permanent or temporary domestic address */
data class KotimainenOsoite(
    override val tietoryhma: String,
    override val muutosattribuutti: Muutosattribuutti? = null,
    override val voimassaolevatTiedot: List<Tietoryhma>? = null,
    val turvakiellonAlaisetKentat: List<String>? = null,
    val katunimi: DvvText? = null,
    val katunumero: String? = null,
    val huoneistokirjain: String? = null,
    val huoneistonumero: String? = null,
    val jakokirjain: String? = null,
    val postinumero: String? = null,
    val postitoimipaikka: DvvText? = null,
    val kuntakoodi: String? = null,
    val alkupv: DvvDate? = null,
    val loppupv: DvvDate? = null,
) : Tietoryhma

/** A permanent or temporary foreign address */
data class UlkomainenOsoite(
    override val tietoryhma: String,
    override val muutosattribuutti: Muutosattribuutti? = null,
    override val voimassaolevatTiedot: List<Tietoryhma>? = null,
    val turvakiellonAlaisetKentat: List<String>? = null,
    val lahiosoite: String? = null,
    val paikkakunta: String? = null,
    val valtiokoodi: String? = null,
    val alkupv: DvvDate? = null,
    val loppupv: DvvDate? = null,
) : Tietoryhma

/** A domestic mailing address, or the contact address of a person with restricted details */
data class KotimainenPostiosoite(
    override val tietoryhma: String,
    override val muutosattribuutti: Muutosattribuutti? = null,
    override val voimassaolevatTiedot: List<Tietoryhma>? = null,
    val turvakiellonAlaisetKentat: List<String>? = null,
    val postiosoite: DvvText? = null,
    val postinumero: String? = null,
    val postitoimipaikka: DvvText? = null,
    val alkupv: DvvDate? = null,
    val loppupv: DvvDate? = null,
) : Tietoryhma

data class Kotikunta(
    override val tietoryhma: String,
    override val muutosattribuutti: Muutosattribuutti? = null,
    override val voimassaolevatTiedot: List<Tietoryhma>? = null,
    val turvakiellonAlaisetKentat: List<String>? = null,
    val kuntakoodi: String? = null,
    val kuntaanMuuttopv: DvvDate? = null,
) : Tietoryhma

data class VakinainenKotimainenAsuinpaikkatunnus(
    override val tietoryhma: String,
    override val muutosattribuutti: Muutosattribuutti? = null,
    override val voimassaolevatTiedot: List<Tietoryhma>? = null,
    val turvakiellonAlaisetKentat: List<String>? = null,
    val asuinpaikantunnus: String? = null,
    val alkupv: DvvDate? = null,
    val loppupv: DvvDate? = null,
) : Tietoryhma

/** The start date of the data in the group, for the groups that have one */
fun Tietoryhma.startDate(): LocalDate? =
    when (this) {
        is DefaultTietoryhma -> alkupv
        is HenkilonNimi -> alkupv
        is Aidinkieli -> alkupv
        is Huoltosuhde -> huoltosuhteenAlkupv
        is KotimainenOsoite -> alkupv
        is UlkomainenOsoite -> alkupv
        is KotimainenPostiosoite -> alkupv
        is Kotikunta -> kuntaanMuuttopv
        is VakinainenKotimainenAsuinpaikkatunnus -> alkupv
        is HenkilotunnusKorjaus,
        is Kuolinpaiva,
        is Turvakielto -> null
    }?.asLocalDate()

/** Counts of the guardianship data that eVaka does not use, without personal data */
fun custodyCounts(groups: List<Tietoryhma>): Map<String, Any> {
    val custody = groups.filterIsInstance<Huoltosuhde>()
    return mapOf(
        "byGuardianType" to custody.groupingBy { it.huoltajanLaji ?: "MISSING" }.eachCount(),
        "withRights" to custody.count { !it.oikeudet.isNullOrEmpty() },
        "withLivingArrangement" to custody.count { it.asuminen != null },
    )
}

data class DvvText(val fi: String? = null, val sv: String? = null)

data class DvvDate(val arvo: String, val tarkkuus: String) {
    /** Null when DVV does not know the date */
    fun asLocalDate(): LocalDate? = if (tarkkuus == "EI_TIEDOSSA") null else LocalDate.parse(arvo)
}
