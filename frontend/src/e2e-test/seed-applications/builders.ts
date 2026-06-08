// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  AreaId,
  ApplicationId,
  DaycareId,
  DecisionGenericReasoningId,
  DecisionIndividualReasoningId
} from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'

import { Fixture, applicationFixture } from '../dev-api/fixtures'
import {
  createApplications,
  createDecisionReasoningGeneric,
  createDecisionReasoningIndividual
} from '../generated/api-clients'
import type {
  DevApplicationWithForm,
  DevPerson,
  MockVtjDataset,
  MockVtjPerson
} from '../generated/api-types'

import {
  APPLICATION_COUNT,
  ASSISTANCE_NEED_EVERY,
  CHILD_FIRST_NAMES,
  CITIZEN_STREET_NAME,
  CLUB_TERM,
  CLUB_TERM_APPLICATION_PERIOD,
  DAYCARE_CHILD_BIRTH_YEAR,
  DECISION_HANDLER,
  DECISION_HANDLER_ADDRESS,
  FAMILY_LAST_NAMES,
  GENERIC_REASONINGS,
  GUARDIAN_FIRST_NAMES,
  INDIVIDUAL_REASONINGS,
  MARKER_APPLICATION_ID,
  PREFERRED_START_DATE,
  PREPARATORY_EVERY,
  PRESCHOOL_CHILD_BIRTH_YEAR,
  PRESCHOOL_TERM_APPLICATION_PERIOD,
  PRESCHOOL_TERM_EXTENDED,
  PRESCHOOL_TERM_FINNISH,
  PRESCHOOL_TERM_SWEDISH,
  REASONING_VALID_FROM,
  SEED_AREA_ID,
  SEED_DAYCARE_UNIT_ID,
  SEED_LABEL,
  SEED_PRESCHOOL_UNIT_ID,
  SEEDED_UNITS,
  SENT_DATE,
  SINGLE_GUARDIAN_FAMILY_EVERY,
  TERM_BREAKS,
  TWO_CHILD_FAMILY_EVERY,
  TYPE_DISTRIBUTION,
  UNIT_OPENING_DATE,
  UNIT_PILOT_FEATURES,
  type SeededUnit,
  type SeedKind
} from './config'

const SSN_CHECKSUM_CHARS = '0123456789ABCDEFHJKLMNPRSTUVWXY'

// Builds a checksum-valid Finnish personal identity code from a date of birth
// and a serial number (the 3-digit individual number, 2-899).
export function generateSsn(dateOfBirth: LocalDate, serial: number): string {
  const dd = String(dateOfBirth.date).padStart(2, '0')
  const mm = String(dateOfBirth.month).padStart(2, '0')
  const yy = String(dateOfBirth.year % 100).padStart(2, '0')
  const centurySign = dateOfBirth.year >= 2000 ? 'A' : '-'
  const nnn = String(serial).padStart(3, '0')
  const checksum = SSN_CHECKSUM_CHARS[Number(`${dd}${mm}${yy}${nnn}`) % 31]
  return `${dd}${mm}${yy}${centurySign}${nnn}${checksum}`
}

// The first seeded guardian gets this fixed birth date and SSN. The SSN is the
// idempotency marker: index.ts probes the citizen list for it.
const MARKER_GUARDIAN_BIRTH_DATE = LocalDate.of(1980, 1, 1)
export const MARKER_SSN = generateSsn(MARKER_GUARDIAN_BIRTH_DATE, 1)

// Lowercases and ASCII-folds a Finnish name for use in an email local part.
function slugify(name: string): string {
  return name
    .toLowerCase()
    .replace(/ä/g, 'a')
    .replace(/ö/g, 'o')
    .replace(/å/g, 'a')
}

// Builds a firstname.lastname email address for a seeded citizen.
function seedEmail(firstName: string, lastName: string): string {
  return `${slugify(firstName)}.${slugify(lastName)}@evaka.test`
}

// Seeds the preschool term and club term for the configured season. A term
// for the season may already exist (e.g. entered manually); the insert is then
// rejected by a no-overlap constraint, which is treated as "already present"
// and the run continues.
export async function seedTerms(): Promise<void> {
  try {
    await Fixture.preschoolTerm({
      finnishPreschool: PRESCHOOL_TERM_FINNISH,
      swedishPreschool: PRESCHOOL_TERM_SWEDISH,
      extendedTerm: PRESCHOOL_TERM_EXTENDED,
      applicationPeriod: PRESCHOOL_TERM_APPLICATION_PERIOD,
      termBreaks: TERM_BREAKS
    }).save()
  } catch {
    console.warn(
      'Skipped preschool term — a term for this season already exists.'
    )
  }

  try {
    await Fixture.clubTerm({
      term: CLUB_TERM,
      applicationPeriod: CLUB_TERM_APPLICATION_PERIOD,
      termBreaks: TERM_BREAKS
    }).save()
  } catch {
    console.warn('Skipped club term — a term for this season already exists.')
  }
}

export interface SeededReasonings {
  generic: number
  individual: number
}

// Seeds the generic and individual decision reasonings ('perustelut'). These
// rows have no overlap constraints, so they simply add to whatever already
// exists in the environment.
export async function seedReasonings(): Promise<SeededReasonings> {
  const now = HelsinkiDateTime.now()

  await createDecisionReasoningGeneric({
    body: GENERIC_REASONINGS.map((r) => ({
      id: randomId<DecisionGenericReasoningId>(),
      collectionType: r.collectionType,
      validFrom: REASONING_VALID_FROM,
      ready: true,
      textFi: r.textFi,
      textSv: r.textSv,
      createdAt: now,
      modifiedAt: now
    }))
  })

  await createDecisionReasoningIndividual({
    body: INDIVIDUAL_REASONINGS.map((r) => ({
      id: randomId<DecisionIndividualReasoningId>(),
      collectionType: r.collectionType,
      titleFi: r.titleFi,
      titleSv: r.titleSv,
      textFi: r.textFi,
      textSv: r.textSv,
      removedAt: null,
      createdAt: now,
      modifiedAt: now
    }))
  })

  return {
    generic: GENERIC_REASONINGS.length,
    individual: INDIVIDUAL_REASONINGS.length
  }
}

export interface SeededUnits {
  daycareUnitId: DaycareId
  preschoolUnitId: DaycareId
}

// Creates one operational unit from a SeededUnit descriptor: a daycare with an
// opening date, the standard pilot features and care types, realistic contact
// and decision details, a group, plus a named unit supervisor and staff member
// so children can actually be placed and the unit managed. Each create call is
// guarded so a partial-failure rerun can recover.
async function seedUnit(
  areaId: AreaId,
  id: DaycareId,
  spec: SeededUnit
): Promise<void> {
  const managerName = `${spec.supervisor.firstName} ${spec.supervisor.lastName}`
  const address = {
    streetAddress: spec.streetAddress,
    postalCode: spec.postalCode,
    postOffice: spec.postOffice
  }

  try {
    await Fixture.daycare({
      id,
      areaId,
      name: spec.name,
      capacity: spec.capacity,
      type: ['CENTRE', 'PRESCHOOL', 'PREPARATORY_EDUCATION'],
      openingDate: UNIT_OPENING_DATE,
      enabledPilotFeatures: UNIT_PILOT_FEATURES,
      phone: spec.phone,
      email: spec.email,
      url: spec.url,
      visitingAddress: address,
      mailingAddress: { poBox: null, ...address },
      location: spec.location,
      decisionCustomization: {
        daycareName: spec.name,
        preschoolName: spec.name,
        handler: DECISION_HANDLER,
        handlerAddress: DECISION_HANDLER_ADDRESS
      },
      unitManager: {
        name: managerName,
        phone: spec.phone,
        email: spec.email
      }
    }).save()
  } catch {
    console.warn(`Skipped unit ${spec.name} — already exists.`)
  }

  try {
    await Fixture.daycareGroup({
      daycareId: id,
      name: `${spec.name} – ryhmä`
    }).save()
  } catch {
    console.warn(`Skipped group for ${spec.name} — already exists.`)
  }

  try {
    await Fixture.employee(spec.supervisor).unitSupervisor(id).save()
  } catch {
    // Random employee ids may clash on rerun; skip silently.
  }
  try {
    await Fixture.employee(spec.staff).staff(id).save()
  } catch {
    // Same.
  }
}

// Seeds one care area with two operational units. Idempotent: a partial-run
// can be safely re-attempted, and a fully-seeded environment becomes a no-op.
export async function seedUnits(): Promise<SeededUnits> {
  try {
    await Fixture.careArea({
      id: SEED_AREA_ID,
      name: `${SEED_LABEL}-alue`,
      shortName: `${SEED_LABEL}-alue`
    }).save()
  } catch {
    console.warn('Skipped care area — already exists.')
  }

  await seedUnit(SEED_AREA_ID, SEED_DAYCARE_UNIT_ID, SEEDED_UNITS[0])
  await seedUnit(SEED_AREA_ID, SEED_PRESCHOOL_UNIT_ID, SEEDED_UNITS[1])

  return {
    daycareUnitId: SEED_DAYCARE_UNIT_ID,
    preschoolUnitId: SEED_PRESCHOOL_UNIT_ID
  }
}

interface ChildSpec {
  kind: SeedKind
  dateOfBirth: LocalDate
  assistanceNeeded: boolean
  preparatory: boolean
}

export interface SeedSummary {
  families: number
  applications: number
  byKind: Record<SeedKind, number>
}

export interface SeededFamily {
  guardian: DevPerson
  otherGuardian?: DevPerson
  children: DevPerson[]
}

interface GeneratedFamily {
  family: SeededFamily
  childSpecs: ChildSpec[]
}

// The kinds laid out in a repeating cycle sized by the TYPE_DISTRIBUTION
// weights, so the mix is spread evenly across the run.
const KIND_CYCLE: SeedKind[] = TYPE_DISTRIBUTION.flatMap(({ kind, weight }) =>
  Array<SeedKind>(weight).fill(kind)
)

function applicationKindFor(index: number): SeedKind {
  return KIND_CYCLE[index % KIND_CYCLE.length]
}

// Mirrors ApplicationStateService.calculateDueDate for the cases the seeded
// applications fall into: never a transfer, never urgent, no attachments.
// Preschool applications get a due date equal to the sent date; daycare
// applications get one four months out (Espoo's
// preferredStartRelativeApplicationDueDate is off, so the preferred start date
// does not move it).
function dueDateFor(kind: SeedKind): LocalDate {
  return kind === 'DAYCARE' ? SENT_DATE.addMonths(4) : SENT_DATE
}

// Daycare applicants are toddlers; preschool applicants belong to this
// season's preschool cohort. Day/month are varied per index so no two children
// of the same kind share a birth date.
function childBirthDate(kind: SeedKind, index: number): LocalDate {
  const year =
    kind === 'DAYCARE' ? DAYCARE_CHILD_BIRTH_YEAR : PRESCHOOL_CHILD_BIRTH_YEAR
  const month = (index % 12) + 1
  const day = ((index * 7) % 27) + 1
  return LocalDate.of(year, month, day)
}

function planChildren(): ChildSpec[] {
  const specs: ChildSpec[] = []
  let preschoolCount = 0
  for (let i = 0; i < APPLICATION_COUNT; i++) {
    const kind = applicationKindFor(i)
    const isPreschool = kind !== 'DAYCARE'
    specs.push({
      kind,
      dateOfBirth: childBirthDate(kind, i),
      assistanceNeeded: i % ASSISTANCE_NEED_EVERY === 0,
      preparatory: isPreschool && preschoolCount++ % PREPARATORY_EVERY === 0
    })
  }
  return specs
}

// Generates the seeded families deterministically, with no I/O. Because the
// generation is a pure function of config.ts, re-running it reproduces the same
// SSNs — which is what lets the VTJ mock be reloaded to match a database that
// was already seeded (see buildVtjDataset).
export function generateFamilies(): GeneratedFamily[] {
  const children = planChildren()
  const families: GeneratedFamily[] = []

  let serial = 100
  let childIndex = 0
  let familyIndex = 0

  while (childIndex < children.length) {
    const familySize = familyIndex % TWO_CHILD_FAMILY_EVERY === 0 ? 2 : 1
    const isMarkerFamily = familyIndex === 0
    const lastName = FAMILY_LAST_NAMES[familyIndex % FAMILY_LAST_NAMES.length]
    const guardianFirstName =
      GUARDIAN_FIRST_NAMES[familyIndex % GUARDIAN_FIRST_NAMES.length]
    const streetAddress = `${CITIZEN_STREET_NAME} ${familyIndex * 2 + 1}`

    const guardianBirthDate = isMarkerFamily
      ? MARKER_GUARDIAN_BIRTH_DATE
      : LocalDate.of(1988, (familyIndex % 12) + 1, ((familyIndex * 5) % 27) + 1)
    const guardian: DevPerson = Fixture.person({
      firstName: guardianFirstName,
      lastName,
      dateOfBirth: guardianBirthDate,
      ssn: isMarkerFamily
        ? MARKER_SSN
        : generateSsn(guardianBirthDate, serial++),
      phone: '0401234567',
      streetAddress,
      email: seedEmail(guardianFirstName, lastName)
    })

    let otherGuardian: DevPerson | undefined
    if (familyIndex % SINGLE_GUARDIAN_FAMILY_EVERY !== 0) {
      const otherFirstName =
        GUARDIAN_FIRST_NAMES[(familyIndex + 12) % GUARDIAN_FIRST_NAMES.length]
      const otherBirthDate = LocalDate.of(
        1988,
        ((familyIndex * 7) % 12) + 1,
        ((familyIndex * 11) % 27) + 1
      )
      otherGuardian = Fixture.person({
        firstName: otherFirstName,
        lastName,
        dateOfBirth: otherBirthDate,
        ssn: generateSsn(otherBirthDate, serial++),
        phone: '0402345678',
        streetAddress,
        email: seedEmail(otherFirstName, lastName)
      })
    }

    const familyChildren: DevPerson[] = []
    const childSpecs: ChildSpec[] = []
    for (let c = 0; c < familySize && childIndex < children.length; c++) {
      const spec = children[childIndex]
      const childFirstName =
        CHILD_FIRST_NAMES[childIndex % CHILD_FIRST_NAMES.length]
      const child: DevPerson = Fixture.person({
        firstName: childFirstName,
        lastName,
        dateOfBirth: spec.dateOfBirth,
        ssn: generateSsn(spec.dateOfBirth, serial++),
        streetAddress,
        email: seedEmail(childFirstName, lastName)
      })
      familyChildren.push(child)
      childSpecs.push(spec)
      childIndex++
    }

    families.push({
      family: {
        guardian,
        children: familyChildren,
        ...(otherGuardian ? { otherGuardian } : {})
      },
      childSpecs
    })

    familyIndex++
  }

  return families
}

// Creates the seeded families and one SENT application per child. Families are
// saved via Fixture.family().save(), which also registers each guardian in the
// VTJ mock — that is what makes them appear in the dev Suomi.fi login picker.
// The families are returned so the caller can snapshot their VTJ dataset.
export async function seedFamiliesAndApplications(
  units: SeededUnits
): Promise<{ summary: SeedSummary; families: SeededFamily[] }> {
  const generated = generateFamilies()
  const applications: DevApplicationWithForm[] = []
  const byKind: Record<SeedKind, number> = {
    DAYCARE: 0,
    PRESCHOOL: 0,
    PRESCHOOL_DAYCARE: 0
  }
  let isFirstApplication = true

  for (const { family, childSpecs } of generated) {
    const { guardian, otherGuardian, children } = family
    await Fixture.family({
      guardian,
      children,
      ...(otherGuardian ? { otherGuardian } : {})
    }).save()

    children.forEach((child, i) => {
      const spec = childSpecs[i]
      const isPreschool = spec.kind !== 'DAYCARE'
      const application = applicationFixture(
        child,
        guardian,
        otherGuardian,
        isPreschool ? 'PRESCHOOL' : 'DAYCARE',
        null,
        [isPreschool ? units.preschoolUnitId : units.daycareUnitId],
        spec.kind === 'PRESCHOOL_DAYCARE',
        'SENT',
        PREFERRED_START_DATE,
        false,
        spec.assistanceNeeded
      )
      const overrideForm = spec.kind === 'PRESCHOOL_DAYCARE' || spec.preparatory
      applications.push({
        ...application,
        id: isFirstApplication
          ? MARKER_APPLICATION_ID
          : randomId<ApplicationId>(),
        sentDate: SENT_DATE,
        dueDate: dueDateFor(spec.kind),
        form: overrideForm
          ? {
              ...application.form,
              preferences: {
                ...application.form.preferences,
                preparatory: spec.preparatory,
                ...(spec.kind === 'PRESCHOOL_DAYCARE'
                  ? {
                      connectedDaycarePreferredStartDate: PREFERRED_START_DATE
                    }
                  : {})
              }
            }
          : application.form
      })
      isFirstApplication = false
      byKind[spec.kind]++
    })
  }

  await createApplications({ body: applications })

  return {
    summary: {
      families: generated.length,
      applications: applications.length,
      byKind
    },
    families: generated.map((g) => g.family)
  }
}

// Mirrors the DevPerson -> MockVtjPerson mapping that Fixture.person().save*
// uses, so a reloaded snapshot reproduces the same mock VTJ state the original
// seed registered.
function toMockVtjPerson(person: DevPerson): MockVtjPerson {
  return {
    firstNames: person.firstName,
    lastName: person.lastName,
    socialSecurityNumber: person.ssn ?? '',
    address: {
      streetAddress: person.streetAddress || '',
      postalCode: person.postalCode || '',
      postOffice: person.postOffice || '',
      streetAddressSe: person.streetAddress || '',
      postOfficeSe: person.postalCode || ''
    },
    dateOfDeath: person.dateOfDeath ?? null,
    nationalities: [],
    nativeLanguage: null,
    residenceCode:
      person.residenceCode ??
      `${person.streetAddress ?? ''}${person.postalCode ?? ''}${
        person.postOffice ?? ''
      }`.replace(' ', ''),
    municipalityOfResidence: person.municipalityOfResidence ?? null,
    restrictedDetails: {
      enabled: person.restrictedDetailsEnabled || false,
      endDate: person.restrictedDetailsEndDate || null
    }
  }
}

// Builds the VTJ dataset (persons + guardian->child links) the seeded families
// registered in the mock. Pushing it via POST /dev-api/vtj-persons re-registers
// the families without recreating their database rows.
export function buildVtjDataset(families: SeededFamily[]): MockVtjDataset {
  const persons: MockVtjPerson[] = []
  const guardianDependants: Record<string, string[]> = {}

  for (const { guardian, otherGuardian, children } of families) {
    children.forEach((child) => persons.push(toMockVtjPerson(child)))
    const childSsns = children.map((c) => c.ssn ?? '')

    persons.push(toMockVtjPerson(guardian))
    guardianDependants[guardian.ssn ?? ''] = childSsns
    if (otherGuardian) {
      persons.push(toMockVtjPerson(otherGuardian))
      guardianDependants[otherGuardian.ssn ?? ''] = childSsns
    }
  }

  return { persons, guardianDependants }
}
