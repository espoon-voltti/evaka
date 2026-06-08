// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import type { DecisionReasoningCollectionType } from 'lib-common/generated/api-types/decision'
import type {
  AreaId,
  ApplicationId,
  DaycareId,
  PilotFeature
} from 'lib-common/generated/api-types/shared'
import { fromUuid } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'

// ----- Season -----

// The calendar year the seeded season starts (its August). Every term date,
// application date, and child birth year below is derived from this — change
// this one constant to seed a different season.
export const SEASON_START_YEAR = 2026

// ----- Volume -----

// How many applications to create. Must exceed the applications-list page
// size (50) so paging is exercised. Family count is derived from this and
// TWO_CHILD_FAMILY_EVERY.
export const APPLICATION_COUNT = 75

// Every Nth family (0-based index) gets two children instead of one.
export const TWO_CHILD_FAMILY_EVERY = 3

// Every Nth family (0-based index) is single-guardian; the rest get a second
// guardian who shares the family surname.
export const SINGLE_GUARDIAN_FAMILY_EVERY = 4

// Every Nth application (0-based index) is marked as needing assistance.
export const ASSISTANCE_NEED_EVERY = 7

// Every Nth preschool application (counted among preschool-kind applications
// only) also requests preparatory education ('valmistava opetus'), which makes
// it derive a PREPARATORY / PREPARATORY_DAYCARE placement type. Only PRESCHOOL
// and PRESCHOOL_DAYCARE applications can carry this.
export const PREPARATORY_EVERY = 5

// ----- Application type distribution -----

export type SeedKind = 'DAYCARE' | 'PRESCHOOL' | 'PRESCHOOL_DAYCARE'

// Relative weights for the application-kind mix. The seeder lays the kinds out
// in a repeating cycle whose length is the sum of the weights, so 8/7/5
// yields 40% / 35% / 25%.
export const TYPE_DISTRIBUTION: { kind: SeedKind; weight: number }[] = [
  { kind: 'DAYCARE', weight: 8 },
  { kind: 'PRESCHOOL_DAYCARE', weight: 7 },
  { kind: 'PRESCHOOL', weight: 5 }
]

// ----- Naming -----

// Human-readable label used in seeded care-area and unit names, so seeded
// units are easy to spot.
export const SEED_LABEL = 'Kylvö'

// Deterministic UUIDs for the seeded area and units. Re-running the seeder
// after a partial failure must not create duplicates — the create calls fall
// through a try/catch on these ids, and downstream steps reference them
// directly. The '5eed5eed' prefix marks them as seeder-owned at a glance.
export const SEED_AREA_ID = fromUuid<AreaId>(
  '5eed5eed-0000-4000-8000-000000000001'
)
export const SEED_DAYCARE_UNIT_ID = fromUuid<DaycareId>(
  '5eed5eed-0000-4000-8000-000000000002'
)
export const SEED_PRESCHOOL_UNIT_ID = fromUuid<DaycareId>(
  '5eed5eed-0000-4000-8000-000000000003'
)

// The marker family's first application gets this fixed id, so the seeder can
// probe Postgres (GET /dev-api/applications/{id}) to tell whether the seed data
// still exists in the database — independently of the in-memory VTJ mock, which
// a backend restart wipes.
export const MARKER_APPLICATION_ID = fromUuid<ApplicationId>(
  '5eed5eed-0000-4000-8000-000000000004'
)

// Street name shared by all seeded citizens; the house number varies per
// family.
export const CITIZEN_STREET_NAME = 'Kylvötie'

// Name pools. Each family takes one surname; guardians and children take a
// first name from their respective pool. With the default volume the surname
// pool is large enough to give every family a distinct surname.
export const FAMILY_LAST_NAMES: string[] = [
  'Aalto',
  'Aaltonen',
  'Aho',
  'Ahonen',
  'Anttila',
  'Eskelinen',
  'Hakala',
  'Halonen',
  'Harju',
  'Heikkilä',
  'Heikkinen',
  'Heinonen',
  'Hiltunen',
  'Hirvonen',
  'Honkanen',
  'Huttunen',
  'Hämäläinen',
  'Hänninen',
  'Ikonen',
  'Jokinen',
  'Järvinen',
  'Kallio',
  'Karjalainen',
  'Kauppinen',
  'Kemppainen',
  'Ketola',
  'Kettunen',
  'Kinnunen',
  'Koivisto',
  'Korhonen',
  'Koskinen',
  'Laaksonen',
  'Lahtinen',
  'Laine',
  'Laitinen',
  'Lampinen',
  'Laurila',
  'Lehtinen',
  'Lehto',
  'Lehtonen',
  'Leppänen',
  'Lindholm',
  'Manninen',
  'Mattila',
  'Mikkonen',
  'Moilanen',
  'Mustonen',
  'Mäkelä',
  'Mäkinen',
  'Niemi',
  'Nieminen',
  'Nurmi',
  'Ojala',
  'Oksanen',
  'Peltonen',
  'Pitkänen',
  'Pulkkinen',
  'Rantanen',
  'Rissanen',
  'Räsänen',
  'Saarinen',
  'Salminen',
  'Salo',
  'Salonen',
  'Savolainen',
  'Toivonen',
  'Tuominen',
  'Turunen',
  'Vainio',
  'Virtanen',
  'Vuorinen',
  'Väisänen'
]
export const GUARDIAN_FIRST_NAMES: string[] = [
  'Anna',
  'Antti',
  'Elina',
  'Hanna',
  'Heikki',
  'Janne',
  'Jari',
  'Johanna',
  'Juha',
  'Kari',
  'Laura',
  'Maria',
  'Marko',
  'Mika',
  'Minna',
  'Pekka',
  'Petri',
  'Päivi',
  'Riitta',
  'Sari',
  'Satu',
  'Tiina',
  'Timo',
  'Ville'
]
export const CHILD_FIRST_NAMES: string[] = [
  'Aada',
  'Aino',
  'Eemil',
  'Eino',
  'Elias',
  'Ella',
  'Emma',
  'Helmi',
  'Iida',
  'Joel',
  'Kasper',
  'Lauri',
  'Leo',
  'Lilja',
  'Nea',
  'Niilo',
  'Onni',
  'Otto',
  'Saana',
  'Sofia',
  'Toivo',
  'Venla',
  'Vilma',
  'Väinö'
]

// ----- Decision reasonings ('perustelut') -----

// The seeded generic reasonings resolve as in-force from this date — a bit
// before today, so they are immediately usable.
export const REASONING_VALID_FROM = LocalDate.todayInHelsinkiTz().subMonths(1)

export interface GenericReasoningContent {
  collectionType: DecisionReasoningCollectionType
  textFi: string
  textSv: string
}

// One in-force generic reasoning per collection type. Extend the array to
// cover more cases.
export const GENERIC_REASONINGS: GenericReasoningContent[] = [
  {
    collectionType: 'DAYCARE',
    textFi:
      'Varhaiskasvatuslain mukaisesti kunta osoittaa lapselle ' +
      'varhaiskasvatuspaikan hakemuksessa esitettyjen toiveiden ja vapaana ' +
      'olevien paikkojen perusteella.',
    textSv:
      'I enlighet med lagen om småbarnspedagogik anvisar kommunen barnet en ' +
      'plats inom småbarnspedagogiken utifrån önskemålen i ansökan och de ' +
      'lediga platserna.'
  },
  {
    collectionType: 'PRESCHOOL',
    textFi:
      'Perusopetuslain 4 § ja 6 § mukaisesti kunta osoittaa ' +
      'oppivelvollisuuden alkamista edeltävänä vuonna lapselle ' +
      'esiopetuspaikan lähikoulusta tai muusta soveltuvasta paikasta.',
    textSv:
      'I enlighet med 4 § och 6 § i lagen om grundläggande utbildning ' +
      'anvisar kommunen barnet en plats i förskoleundervisningen i en ' +
      'närskola eller på en annan lämplig plats året innan läroplikten börjar.'
  }
]

export interface IndividualReasoningContent {
  collectionType: DecisionReasoningCollectionType
  titleFi: string
  titleSv: string
  textFi: string
  textSv: string
}

// Individual reasonings. The preschool-collection entries are parsed from the
// designer's Figma sample; the daycare-collection entries are seed equivalents.
export const INDIVIDUAL_REASONINGS: IndividualReasoningContent[] = [
  {
    collectionType: 'PRESCHOOL',
    titleFi: 'Jatkumo',
    titleSv: 'Kontinuitet',
    textFi: 'Jatkumo lapsen nykyiseen varhaiskasvatuspaikkaan.',
    textSv: 'Kontinuitet i barnets nuvarande plats inom småbarnspedagogiken.'
  },
  {
    collectionType: 'PRESCHOOL',
    titleFi: 'Tuki',
    titleSv: 'Stöd',
    textFi:
      'Kasvun ja oppimisen tukeen liittyvä tai muu erityinen syy. Tuen tarpeen toteaminen ei tarkoita, että lapselle osoitetaan esiopetuspaikka huoltajan toiveen mukaiseen yksikköön, vaan yksikköön, jossa lapsen tarvitsema tuki voidaan järjestää lapsen edun edellyttämällä tavalla.',
    textSv:
      'Ett skäl som anknyter till stöd för utveckling och lärande eller annat särskilt skäl. Fastställandet av behovet av stöd innebär inte att barnet anvisas en förskoleplats i den enhet som vårdnadshavaren önskar, utan i en enhet där det stöd som barnet behöver kan ordnas på ett sätt som förutsätter barnets bästa.'
  },
  {
    collectionType: 'PRESCHOOL',
    titleFi: 'Sisarusperuste',
    titleSv: 'Syskongrund',
    textFi: 'Sisarusperuste',
    textSv: 'Syskongrund'
  },
  {
    collectionType: 'PRESCHOOL',
    titleFi: 'Matka',
    titleSv: 'Väglängd',
    textFi:
      'Matkaperuste. Esiopetuspäätös on tehty matkaperusteella siten, että kaikki lapset huomioiden matkat esiopetukseen ovat mahdollisimman lyhyitä ja turvallisia. Matkan turvallisuutta arvioidaan siitä näkökulmasta, että huoltajat vastaavat lapsen kulkemisesta esiopetukseen.',
    textSv:
      'Grund baserad på väglängd. Beslutet om förskoleundervisning har gjorts utifrån väglängden så att vägen till förskoleundervisningen är så kort och trygg som möjligt för alla barn i beaktande. Vägens trygghet bedöms ur det perspektiv att vårdnadshavarna ansvarar för barnets väg till förskoleundervisningen.'
  },
  {
    collectionType: 'DAYCARE',
    titleFi: 'Vuorohoito',
    titleSv: 'Skiftomsorg',
    textFi: 'Vuorohoito',
    textSv: 'Skiftomsorg'
  },
  {
    collectionType: 'DAYCARE',
    titleFi: 'Tuki',
    titleSv: 'Stöd',
    textFi:
      'Kasvun ja oppimisen tukeen liittyvä tai muu erityinen syy. Tuen tarpeen toteaminen ei tarkoita, että lapselle osoitetaan esiopetuspaikka huoltajan toiveen mukaiseen yksikköön, vaan yksikköön, jossa lapsen tarvitsema tuki voidaan järjestää lapsen edun edellyttämällä tavalla.',
    textSv:
      'Ett skäl som anknyter till stöd för utveckling och lärande eller annat särskilt skäl. Fastställandet av behovet av stöd innebär inte att barnet anvisas en förskoleplats i den enhet som vårdnadshavaren önskar, utan i en enhet där det stöd som barnet behöver kan ordnas på ett sätt som förutsätter barnets bästa.'
  },
  {
    collectionType: 'DAYCARE',
    titleFi: 'Sisarusperuste',
    titleSv: 'Syskongrund',
    textFi: 'Sisarusperuste',
    textSv: 'Syskongrund'
  },
  {
    collectionType: 'DAYCARE',
    titleFi: 'Paikkatilanne',
    titleSv: 'Platssituationen',
    textFi:
      'Alueen kokonaispaikkatilanne ja varhaiskasvatuspaikkojen tarkoituksenmukainen käyttö.',
    textSv:
      'Den totala platssituationen i området och en ändamålsenlig användning av platserna inom småbarnspedagogiken.'
  }
]

// ----- Units -----

// Seeded units open from this date, so they are operational for the season.
export const UNIT_OPENING_DATE = LocalDate.of(2020, 8, 1)

// Pilot features enabled on seeded units ('yksiköille avatut toiminnot'),
// matching the set the standard dev units carry.
export const UNIT_PILOT_FEATURES: PilotFeature[] = [
  'MESSAGING',
  'MOBILE',
  'RESERVATIONS',
  'VASU_AND_PEDADOC',
  'OTHER_DECISION',
  'CITIZEN_BASIC_DOCUMENT',
  'MOBILE_MESSAGING',
  'PLACEMENT_TERMINATION',
  'SERVICE_APPLICATIONS'
]

// Shared decision-handler details shown on generated decision PDFs.
export const DECISION_HANDLER = 'Varhaiskasvatuksen palveluohjaus'
export const DECISION_HANDLER_ADDRESS = 'PL 3125, 02070 Espoon kaupunki'

export interface SeededUnitPerson {
  firstName: string
  lastName: string
}

export interface SeededUnit {
  name: string
  // 'Yksikön laskennallinen lapsimäärä' — a static unit setting, not computed.
  capacity: number
  phone: string
  email: string
  url: string
  streetAddress: string
  postalCode: string
  postOffice: string
  location: { lat: number; lon: number }
  supervisor: SeededUnitPerson
  staff: SeededUnitPerson
}

// The two seeded units, in order: a daycare unit then a preschool unit. Detail
// fields carry realistic values so unit views and generated PDFs read cleanly.
export const SEEDED_UNITS: SeededUnit[] = [
  {
    name: 'Kylvökujan päiväkoti',
    capacity: 96,
    phone: '+358 9 1234 5678',
    email: 'kylvokujan.paivakoti@evaka.test',
    url: 'https://example.com/toimipisteet/kylvokujan-paivakoti',
    streetAddress: 'Kylvökuja 4',
    postalCode: '02230',
    postOffice: 'Espoo',
    location: { lat: 60.2052, lon: 24.6522 },
    supervisor: { firstName: 'Marjatta', lastName: 'Lahti-Kylväjä' },
    staff: { firstName: 'Pekka', lastName: 'Nurmi-Kylväjä' }
  },
  {
    name: 'Kylvömäen esiopetusyksikkö',
    capacity: 63,
    phone: '+358 9 1234 5679',
    email: 'kylvomaen.esiopetus@evaka.test',
    url: 'https://example.com/toimipisteet/kylvomaen-esiopetusyksikko',
    streetAddress: 'Kylvömäentie 12',
    postalCode: '02250',
    postOffice: 'Espoo',
    location: { lat: 60.224, lon: 24.759 },
    supervisor: { firstName: 'Helena', lastName: 'Koski-Kylväjä' },
    staff: { firstName: 'Antero', lastName: 'Vainio-Kylväjä' }
  }
]

// ----- Derived dates (all keyed off SEASON_START_YEAR) -----

const seasonEndYear = SEASON_START_YEAR + 1

// Preschool applicants belong to the cohort starting preschool this season
// (~6 years old); daycare applicants are toddlers (~3 years old).
export const PRESCHOOL_CHILD_BIRTH_YEAR = SEASON_START_YEAR - 6
export const DAYCARE_CHILD_BIRTH_YEAR = SEASON_START_YEAR - 3

export const PREFERRED_START_DATE = LocalDate.of(SEASON_START_YEAR, 8, 13)
export const SENT_DATE = LocalDate.of(SEASON_START_YEAR, 3, 15)

// Term breaks: autumn break in the start year, winter break spanning the new
// year, sports break in the end year.
export const TERM_BREAKS: FiniteDateRange[] = [
  new FiniteDateRange(
    LocalDate.of(SEASON_START_YEAR, 10, 13),
    LocalDate.of(SEASON_START_YEAR, 10, 17)
  ),
  new FiniteDateRange(
    LocalDate.of(SEASON_START_YEAR, 12, 22),
    LocalDate.of(seasonEndYear, 1, 6)
  ),
  new FiniteDateRange(
    LocalDate.of(seasonEndYear, 2, 16),
    LocalDate.of(seasonEndYear, 2, 20)
  )
]

// 2025-2026 dev-data term shapes shifted to the configured season
// (service/src/main/resources/dev-data/preschool-terms.sql, club-terms.sql).
export const PRESCHOOL_TERM_FINNISH = new FiniteDateRange(
  LocalDate.of(SEASON_START_YEAR, 8, 7),
  LocalDate.of(seasonEndYear, 5, 29)
)
export const PRESCHOOL_TERM_SWEDISH = PRESCHOOL_TERM_FINNISH
export const PRESCHOOL_TERM_EXTENDED = new FiniteDateRange(
  LocalDate.of(SEASON_START_YEAR, 8, 1),
  LocalDate.of(seasonEndYear, 5, 29)
)
export const PRESCHOOL_TERM_APPLICATION_PERIOD = new FiniteDateRange(
  LocalDate.of(SEASON_START_YEAR, 1, 8),
  LocalDate.of(seasonEndYear, 5, 29)
)

export const CLUB_TERM = new FiniteDateRange(
  LocalDate.of(SEASON_START_YEAR, 8, 7),
  LocalDate.of(seasonEndYear, 5, 29)
)
export const CLUB_TERM_APPLICATION_PERIOD = new FiniteDateRange(
  LocalDate.of(SEASON_START_YEAR, 3, 1),
  LocalDate.of(seasonEndYear, 5, 29)
)
