// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

package evaka.core.shared.dev.seed

import evaka.core.decision.reasoning.DecisionReasoningCollectionType
import evaka.core.shared.ApplicationId
import evaka.core.shared.AreaId
import evaka.core.shared.DaycareId
import evaka.core.shared.domain.Coordinate
import evaka.core.shared.domain.FiniteDateRange
import evaka.core.shared.domain.europeHelsinki
import evaka.core.shared.security.PilotFeature
import java.time.LocalDate
import java.util.UUID

enum class SeedKind {
    DAYCARE,
    PRESCHOOL,
    PRESCHOOL_DAYCARE,
}

data class SeedKindWeight(val kind: SeedKind, val weight: Int)

data class GenericReasoningContent(
    val collectionType: DecisionReasoningCollectionType,
    val textFi: String,
    val textSv: String,
)

data class IndividualReasoningContent(
    val collectionType: DecisionReasoningCollectionType,
    val titleFi: String,
    val titleSv: String,
    val textFi: String,
    val textSv: String,
)

data class SeededUnitPerson(val firstName: String, val lastName: String)

data class SeededUnit(
    val name: String,
    val capacity: Int,
    val phone: String,
    val email: String,
    val url: String,
    val streetAddress: String,
    val postalCode: String,
    val postOffice: String,
    val location: Coordinate,
    val supervisor: SeededUnitPerson,
    val staff: SeededUnitPerson,
)

object SeedApplicationsConfig {
    // ----- Season -----

    const val SEASON_START_YEAR = 2026

    private val seasonEndYear = SEASON_START_YEAR + 1

    // ----- Volume -----

    const val APPLICATION_COUNT = 75
    const val TWO_CHILD_FAMILY_EVERY = 3
    const val SINGLE_GUARDIAN_FAMILY_EVERY = 4
    const val ASSISTANCE_NEED_EVERY = 7
    const val PREPARATORY_EVERY = 5

    // ----- Application type distribution -----

    val TYPE_DISTRIBUTION: List<SeedKindWeight> =
        listOf(
            SeedKindWeight(SeedKind.DAYCARE, 8),
            SeedKindWeight(SeedKind.PRESCHOOL_DAYCARE, 7),
            SeedKindWeight(SeedKind.PRESCHOOL, 5),
        )

    // ----- Naming -----

    const val SEED_LABEL = "Kylvö"

    val SEED_AREA_ID = AreaId(UUID.fromString("5eed5eed-0000-4000-8000-000000000001"))
    val SEED_DAYCARE_UNIT_ID = DaycareId(UUID.fromString("5eed5eed-0000-4000-8000-000000000002"))
    val SEED_PRESCHOOL_UNIT_ID = DaycareId(UUID.fromString("5eed5eed-0000-4000-8000-000000000003"))
    val MARKER_APPLICATION_ID =
        ApplicationId(UUID.fromString("5eed5eed-0000-4000-8000-000000000004"))

    const val CITIZEN_STREET_NAME = "Kylvötie"

    val FAMILY_LAST_NAMES: List<String> =
        listOf(
            "Aalto",
            "Aaltonen",
            "Aho",
            "Ahonen",
            "Anttila",
            "Eskelinen",
            "Hakala",
            "Halonen",
            "Harju",
            "Heikkilä",
            "Heikkinen",
            "Heinonen",
            "Hiltunen",
            "Hirvonen",
            "Honkanen",
            "Huttunen",
            "Hämäläinen",
            "Hänninen",
            "Ikonen",
            "Jokinen",
            "Järvinen",
            "Kallio",
            "Karjalainen",
            "Kauppinen",
            "Kemppainen",
            "Ketola",
            "Kettunen",
            "Kinnunen",
            "Koivisto",
            "Korhonen",
            "Koskinen",
            "Laaksonen",
            "Lahtinen",
            "Laine",
            "Laitinen",
            "Lampinen",
            "Laurila",
            "Lehtinen",
            "Lehto",
            "Lehtonen",
            "Leppänen",
            "Lindholm",
            "Manninen",
            "Mattila",
            "Mikkonen",
            "Moilanen",
            "Mustonen",
            "Mäkelä",
            "Mäkinen",
            "Niemi",
            "Nieminen",
            "Nurmi",
            "Ojala",
            "Oksanen",
            "Peltonen",
            "Pitkänen",
            "Pulkkinen",
            "Rantanen",
            "Rissanen",
            "Räsänen",
            "Saarinen",
            "Salminen",
            "Salo",
            "Salonen",
            "Savolainen",
            "Toivonen",
            "Tuominen",
            "Turunen",
            "Vainio",
            "Virtanen",
            "Vuorinen",
            "Väisänen",
        )

    val GUARDIAN_FIRST_NAMES: List<String> =
        listOf(
            "Anna",
            "Antti",
            "Elina",
            "Hanna",
            "Heikki",
            "Janne",
            "Jari",
            "Johanna",
            "Juha",
            "Kari",
            "Laura",
            "Maria",
            "Marko",
            "Mika",
            "Minna",
            "Pekka",
            "Petri",
            "Päivi",
            "Riitta",
            "Sari",
            "Satu",
            "Tiina",
            "Timo",
            "Ville",
        )

    val CHILD_FIRST_NAMES: List<String> =
        listOf(
            "Aada",
            "Aino",
            "Eemil",
            "Eino",
            "Elias",
            "Ella",
            "Emma",
            "Helmi",
            "Iida",
            "Joel",
            "Kasper",
            "Lauri",
            "Leo",
            "Lilja",
            "Nea",
            "Niilo",
            "Onni",
            "Otto",
            "Saana",
            "Sofia",
            "Toivo",
            "Venla",
            "Vilma",
            "Väinö",
        )

    // ----- Decision reasonings ('perustelut') -----

    val REASONING_VALID_FROM: LocalDate = LocalDate.now(europeHelsinki).minusMonths(1)

    val GENERIC_REASONINGS: List<GenericReasoningContent> =
        listOf(
            GenericReasoningContent(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                textFi =
                    "Varhaiskasvatuslain mukaisesti kunta osoittaa lapselle varhaiskasvatuspaikan hakemuksessa esitettyjen toiveiden ja vapaana olevien paikkojen perusteella.",
                textSv =
                    "I enlighet med lagen om småbarnspedagogik anvisar kommunen barnet en plats inom småbarnspedagogiken utifrån önskemålen i ansökan och de lediga platserna.",
            ),
            GenericReasoningContent(
                collectionType = DecisionReasoningCollectionType.PRESCHOOL,
                textFi =
                    "Perusopetuslain 4 § ja 6 § mukaisesti kunta osoittaa oppivelvollisuuden alkamista edeltävänä vuonna lapselle esiopetuspaikan lähikoulusta tai muusta soveltuvasta paikasta.",
                textSv =
                    "I enlighet med 4 § och 6 § i lagen om grundläggande utbildning anvisar kommunen barnet en plats i förskoleundervisningen i en närskola eller på en annan lämplig plats året innan läroplikten börjar.",
            ),
        )

    val INDIVIDUAL_REASONINGS: List<IndividualReasoningContent> =
        listOf(
            IndividualReasoningContent(
                collectionType = DecisionReasoningCollectionType.PRESCHOOL,
                titleFi = "Jatkumo",
                titleSv = "Kontinuitet",
                textFi = "Jatkumo lapsen nykyiseen varhaiskasvatuspaikkaan.",
                textSv = "Kontinuitet i barnets nuvarande plats inom småbarnspedagogiken.",
            ),
            IndividualReasoningContent(
                collectionType = DecisionReasoningCollectionType.PRESCHOOL,
                titleFi = "Tuki",
                titleSv = "Stöd",
                textFi =
                    "Kasvun ja oppimisen tukeen liittyvä tai muu erityinen syy. Tuen tarpeen toteaminen ei tarkoita, että lapselle osoitetaan esiopetuspaikka huoltajan toiveen mukaiseen yksikköön, vaan yksikköön, jossa lapsen tarvitsema tuki voidaan järjestää lapsen edun edellyttämällä tavalla.",
                textSv =
                    "Ett skäl som anknyter till stöd för utveckling och lärande eller annat särskilt skäl. Fastställandet av behovet av stöd innebär inte att barnet anvisas en förskoleplats i den enhet som vårdnadshavaren önskar, utan i en enhet där det stöd som barnet behöver kan ordnas på ett sätt som förutsätter barnets bästa.",
            ),
            IndividualReasoningContent(
                collectionType = DecisionReasoningCollectionType.PRESCHOOL,
                titleFi = "Sisarusperuste",
                titleSv = "Syskongrund",
                textFi = "Sisarusperuste",
                textSv = "Syskongrund",
            ),
            IndividualReasoningContent(
                collectionType = DecisionReasoningCollectionType.PRESCHOOL,
                titleFi = "Matka",
                titleSv = "Väglängd",
                textFi =
                    "Matkaperuste. Esiopetuspäätös on tehty matkaperusteella siten, että kaikki lapset huomioiden matkat esiopetukseen ovat mahdollisimman lyhyitä ja turvallisia. Matkan turvallisuutta arvioidaan siitä näkökulmasta, että huoltajat vastaavat lapsen kulkemisesta esiopetukseen.",
                textSv =
                    "Grund baserad på väglängd. Beslutet om förskoleundervisning har gjorts utifrån väglängden så att vägen till förskoleundervisningen är så kort och trygg som möjligt för alla barn i beaktande. Vägens trygghet bedöms ur det perspektiv att vårdnadshavarna ansvarar för barnets väg till förskoleundervisningen.",
            ),
            IndividualReasoningContent(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                titleFi = "Vuorohoito",
                titleSv = "Skiftomsorg",
                textFi = "Vuorohoito",
                textSv = "Skiftomsorg",
            ),
            IndividualReasoningContent(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                titleFi = "Tuki",
                titleSv = "Stöd",
                textFi =
                    "Kasvun ja oppimisen tukeen liittyvä tai muu erityinen syy. Tuen tarpeen toteaminen ei tarkoita, että lapselle osoitetaan esiopetuspaikka huoltajan toiveen mukaiseen yksikköön, vaan yksikköön, jossa lapsen tarvitsema tuki voidaan järjestää lapsen edun edellyttämällä tavalla.",
                textSv =
                    "Ett skäl som anknyter till stöd för utveckling och lärande eller annat särskilt skäl. Fastställandet av behovet av stöd innebär inte att barnet anvisas en förskoleplats i den enhet som vårdnadshavaren önskar, utan i en enhet där det stöd som barnet behöver kan ordnas på ett sätt som förutsätter barnets bästa.",
            ),
            IndividualReasoningContent(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                titleFi = "Sisarusperuste",
                titleSv = "Syskongrund",
                textFi = "Sisarusperuste",
                textSv = "Syskongrund",
            ),
            IndividualReasoningContent(
                collectionType = DecisionReasoningCollectionType.DAYCARE,
                titleFi = "Paikkatilanne",
                titleSv = "Platssituationen",
                textFi =
                    "Alueen kokonaispaikkatilanne ja varhaiskasvatuspaikkojen tarkoituksenmukainen käyttö.",
                textSv =
                    "Den totala platssituationen i området och en ändamålsenlig användning av platserna inom småbarnspedagogiken.",
            ),
        )

    // ----- Units -----

    val UNIT_OPENING_DATE: LocalDate = LocalDate.of(2020, 8, 1)

    val UNIT_PILOT_FEATURES: List<PilotFeature> =
        listOf(
            PilotFeature.MESSAGING,
            PilotFeature.MOBILE,
            PilotFeature.RESERVATIONS,
            PilotFeature.VASU_AND_PEDADOC,
            PilotFeature.OTHER_DECISION,
            PilotFeature.CITIZEN_BASIC_DOCUMENT,
            PilotFeature.MOBILE_MESSAGING,
            PilotFeature.PLACEMENT_TERMINATION,
            PilotFeature.SERVICE_APPLICATIONS,
        )

    const val DECISION_HANDLER = "Varhaiskasvatuksen palveluohjaus"
    const val DECISION_HANDLER_ADDRESS = "PL 3125, 02070 Espoon kaupunki"

    val SEEDED_UNITS: List<SeededUnit> =
        listOf(
            SeededUnit(
                name = "Kylvökujan päiväkoti",
                capacity = 96,
                phone = "+358 9 1234 5678",
                email = "kylvokujan.paivakoti@evaka.test",
                url = "https://example.com/toimipisteet/kylvokujan-paivakoti",
                streetAddress = "Kylvökuja 4",
                postalCode = "02230",
                postOffice = "Espoo",
                location = Coordinate(24.6522, 60.2052),
                supervisor = SeededUnitPerson("Marjatta", "Lahti-Kylväjä"),
                staff = SeededUnitPerson("Pekka", "Nurmi-Kylväjä"),
            ),
            SeededUnit(
                name = "Kylvömäen esiopetusyksikkö",
                capacity = 63,
                phone = "+358 9 1234 5679",
                email = "kylvomaen.esiopetus@evaka.test",
                url = "https://example.com/toimipisteet/kylvomaen-esiopetusyksikko",
                streetAddress = "Kylvömäentie 12",
                postalCode = "02250",
                postOffice = "Espoo",
                location = Coordinate(24.759, 60.224),
                supervisor = SeededUnitPerson("Helena", "Koski-Kylväjä"),
                staff = SeededUnitPerson("Antero", "Vainio-Kylväjä"),
            ),
        )

    // ----- Derived dates (all keyed off SEASON_START_YEAR) -----

    val PRESCHOOL_CHILD_BIRTH_YEAR = SEASON_START_YEAR - 6
    val DAYCARE_CHILD_BIRTH_YEAR = SEASON_START_YEAR - 3

    val PREFERRED_START_DATE: LocalDate = LocalDate.of(SEASON_START_YEAR, 8, 13)
    val SENT_DATE: LocalDate = LocalDate.of(SEASON_START_YEAR, 3, 15)

    val TERM_BREAKS: List<FiniteDateRange> =
        listOf(
            FiniteDateRange(
                LocalDate.of(SEASON_START_YEAR, 10, 13),
                LocalDate.of(SEASON_START_YEAR, 10, 17),
            ),
            FiniteDateRange(
                LocalDate.of(SEASON_START_YEAR, 12, 22),
                LocalDate.of(seasonEndYear, 1, 6),
            ),
            FiniteDateRange(LocalDate.of(seasonEndYear, 2, 16), LocalDate.of(seasonEndYear, 2, 20)),
        )

    val PRESCHOOL_TERM_FINNISH: FiniteDateRange =
        FiniteDateRange(LocalDate.of(SEASON_START_YEAR, 8, 7), LocalDate.of(seasonEndYear, 5, 29))
    val PRESCHOOL_TERM_SWEDISH: FiniteDateRange = PRESCHOOL_TERM_FINNISH
    val PRESCHOOL_TERM_EXTENDED: FiniteDateRange =
        FiniteDateRange(LocalDate.of(SEASON_START_YEAR, 8, 1), LocalDate.of(seasonEndYear, 5, 29))
    val PRESCHOOL_TERM_APPLICATION_PERIOD: FiniteDateRange =
        FiniteDateRange(LocalDate.of(SEASON_START_YEAR, 1, 8), LocalDate.of(seasonEndYear, 5, 29))

    val CLUB_TERM: FiniteDateRange =
        FiniteDateRange(LocalDate.of(SEASON_START_YEAR, 8, 7), LocalDate.of(seasonEndYear, 5, 29))
    val CLUB_TERM_APPLICATION_PERIOD: FiniteDateRange =
        FiniteDateRange(LocalDate.of(SEASON_START_YEAR, 3, 1), LocalDate.of(seasonEndYear, 5, 29))
}
