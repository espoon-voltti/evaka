// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ScopedRole } from 'lib-common/api-types/employee-auth'
import { DecisionIncome } from 'lib-common/api-types/income'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  ApplicationForm,
  ApplicationStatus,
  ApplicationType,
  OtherGuardianAgreementStatus
} from 'lib-common/generated/api-types/application'
import {
  FixedPeriodQuestionnaire,
  HolidayPeriod
} from 'lib-common/generated/api-types/holidayperiod'
import {
  FeeDecision,
  FeeDecisionStatus,
  FeeThresholds,
  Invoice,
  VoucherValueDecision
} from 'lib-common/generated/api-types/invoicing'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import {
  Application,
  AssistanceNeed,
  BackupCare,
  CareArea,
  Child,
  ChildAttendance,
  Daycare,
  DaycareCaretakers,
  DaycareGroup,
  DaycareGroupPlacement,
  DaycarePlacement,
  DecisionFixture,
  DevIncome,
  DevStaffOccupancyCoefficient,
  DevVardaReset,
  DevVardaServiceNeed,
  EmployeeDetail,
  EmployeePin,
  FridgeChild,
  PedagogicalDocument,
  PersonDetail,
  PersonDetailWithDependantsAndGuardians,
  PlacementPlan,
  ServiceNeedFixture
} from './types'

import {
  addVardaReset,
  addVardaServiceNeed,
  insertAssistanceNeedFixtures,
  insertBackupCareFixtures,
  insertCareAreaFixtures,
  insertChildAttendanceFixtures,
  insertChildFixtures,
  insertDaycareCaretakerFixtures,
  insertDaycareFixtures,
  insertDaycareGroupFixtures,
  insertDaycareGroupPlacementFixtures,
  insertDaycarePlacementFixtures,
  insertDecisionFixtures,
  insertEmployeeFixture,
  insertEmployeePins,
  insertFeeThresholds,
  insertFridgeChildren,
  insertGuardianFixtures,
  insertHolidayPeriod,
  insertHolidayQuestionnaire,
  insertIncome,
  insertPedagogicalDocuments,
  insertPersonFixture,
  insertPlacementPlan,
  insertServiceNeedOptions,
  insertServiceNeeds,
  insertVtjPersonFixture,
  setAclForDaycareGroups,
  setAclForDaycares,
  upsertOccupancyCoefficient
} from './index'

export const careAreaFixture: CareArea = {
  id: '674dfb66-8849-489e-b094-e6a0ebfb3c71',
  name: 'Superkeskus',
  shortName: 'super-keskus',
  areaCode: '299',
  subCostCenter: '99'
}

export const careArea2Fixture: CareArea = {
  id: '7a5b42db-451b-4394-b6a6-86993ea0ed45',
  name: 'Hyperkeskus',
  shortName: 'hyper-keskus',
  areaCode: '298',
  subCostCenter: '98'
}

export const clubFixture: Daycare = {
  id: '0b5ffd40-2f1a-476a-ad06-2861f433b0d1',
  careAreaId: careAreaFixture.id,
  name: 'Alkuräjähdyksen kerho',
  type: ['CLUB'],
  openingDate: '2020-01-01',
  costCenter: '31500',
  streetAddress: 'Kamreerintie 1',
  postalCode: '02210',
  postOffice: 'Espoo',
  decisionDaycareName: 'Päiväkoti päätöksellä',
  decisionPreschoolName: '-',
  decisionHandler: 'Käsittelijä',
  decisionHandlerAddress: 'Käsittelijän osoite',
  daycareApplyPeriod: null,
  preschoolApplyPeriod: null,
  clubApplyPeriod: new DateRange(LocalDate.of(2020, 3, 1), null),
  providerType: 'MUNICIPAL',
  operationDays: [1, 2, 3, 4, 5],
  roundTheClock: true,
  enabledPilotFeatures: ['MESSAGING', 'MOBILE']
}

export const daycareFixture: Daycare = {
  id: '4f3a32f5-d1bd-4b8b-aa4e-4fd78b18354b',
  careAreaId: careAreaFixture.id,
  name: 'Alkuräjähdyksen päiväkoti',
  type: ['CENTRE', 'PRESCHOOL', 'PREPARATORY_EDUCATION'],
  costCenter: '31500',
  streetAddress: 'Kamreerintie 1',
  postalCode: '02210',
  postOffice: 'Espoo',
  decisionDaycareName: 'Päiväkoti päätöksellä',
  decisionPreschoolName: 'Päiväkoti päätöksellä',
  decisionHandler: 'Käsittelijä',
  decisionHandlerAddress: 'Käsittelijän osoite',
  providerType: 'MUNICIPAL',
  operationDays: [1, 2, 3, 4, 5],
  roundTheClock: true,
  location: {
    lat: 60.20377343765089,
    lon: 24.655715743526994
  },
  enabledPilotFeatures: [
    'MESSAGING',
    'MOBILE',
    'RESERVATIONS',
    'VASU_AND_PEDADOC',
    'MOBILE_MESSAGING',
    'PLACEMENT_TERMINATION'
  ]
}

export const daycare2Fixture: Daycare = {
  id: '6f540c39-e7f6-4222-a004-c527403378ec',
  careAreaId: careArea2Fixture.id,
  name: 'Mustan aukon päiväkoti',
  type: ['CENTRE'],
  costCenter: '31501',
  streetAddress: 'Kamreerintie 2',
  postalCode: '02210',
  postOffice: 'Espoo',
  decisionDaycareName: 'Päiväkoti 2 päätöksellä',
  decisionPreschoolName: 'Päiväkoti 2 päätöksellä',
  decisionHandler: 'Käsittelijä 2',
  decisionHandlerAddress: 'Käsittelijän 2 osoite',
  providerType: 'MUNICIPAL',
  operationDays: [1, 2, 3, 4, 5, 6, 7],
  roundTheClock: true,
  location: {
    lat: 60.20350901607783,
    lon: 24.669
  },

  enabledPilotFeatures: ['MESSAGING', 'MOBILE', 'RESERVATIONS']
}

export const preschoolFixture: Daycare = {
  id: 'b53d80e0-319b-4d2b-950c-f5c3c9f834bc',
  careAreaId: careAreaFixture.id,
  name: 'Alkuräjähdyksen eskari',
  type: ['CENTRE', 'PRESCHOOL', 'PREPARATORY_EDUCATION'],
  costCenter: '31501',
  streetAddress: 'Kamreerintie 1',
  postalCode: '02210',
  postOffice: 'Espoo',
  decisionDaycareName: 'Eskari päätöksellä',
  decisionPreschoolName: 'Eskari päätöksellä',
  decisionHandler: 'Käsittelijä',
  decisionHandlerAddress: 'Käsittelijän osoite',
  providerType: 'MUNICIPAL',
  operationDays: [1, 2, 3, 4, 5],
  roundTheClock: true,
  location: {
    lat: 60.2040261560435,
    lon: 24.65517745652623
  },
  enabledPilotFeatures: [
    'MESSAGING',
    'MOBILE',
    'VASU_AND_PEDADOC',
    'PLACEMENT_TERMINATION'
  ]
}

export const enduserGuardianFixture: PersonDetail = {
  id: '87a5c962-9b3d-11ea-bb37-0242ac130002',
  ssn: '070644-937X',
  firstName: 'Johannes Olavi Antero Tapio',
  lastName: 'Karhula',
  email: 'johannes.karhula@evaka.test',
  phone: '123456789',
  language: 'fi',
  dateOfBirth: '1944-07-07',
  streetAddress: 'Kamreerintie 1',
  postalCode: '00340',
  postOffice: 'Espoo',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
}

export const enduserChildFixtureJari: PersonDetail = {
  id: '572adb7e-9b3d-11ea-bb37-0242ac130002',
  ssn: '070714A9126',
  firstName: 'Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani',
  lastName: 'Karhula',
  preferredName: 'Jari',
  email: '',
  phone: '',
  language: 'fi',
  dateOfBirth: '2014-07-07',
  streetAddress: enduserGuardianFixture.streetAddress,
  postalCode: enduserGuardianFixture.postalCode,
  postOffice: enduserGuardianFixture.postOffice,
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
}

export const enduserChildFixtureKaarina: PersonDetail = {
  id: '5a4f3ccc-5270-4d28-bd93-d355182b6768',
  ssn: '160616A978U',
  firstName: 'Kaarina Veera Nelli',
  lastName: 'Karhula',
  email: '',
  phone: '',
  language: 'fi',
  dateOfBirth: '2016-06-16',
  streetAddress: enduserGuardianFixture.streetAddress,
  postalCode: enduserGuardianFixture.postalCode,
  postOffice: enduserGuardianFixture.postOffice,
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
}

export const enduserChildFixturePorriHatterRestricted: PersonDetail = {
  id: '28e189d7-abbe-4be9-9074-6e4c881f18de',
  ssn: '160620A999J',
  firstName: 'Porri Hatter',
  lastName: 'Karhula',
  email: '',
  phone: '',
  language: 'fi',
  dateOfBirth: '2014-07-07',
  streetAddress: enduserGuardianFixture.streetAddress,
  postalCode: enduserGuardianFixture.postalCode,
  postOffice: enduserGuardianFixture.postOffice,
  nationalities: ['FI'],
  restrictedDetailsEnabled: true,
  restrictedDetailsEndDate: null
}

export const enduserChildJariOtherGuardianFixture: PersonDetail = {
  id: 'fb915d31-738f-453f-a2ca-2e7f61db641d',
  ssn: '311299-999E',
  firstName: 'Ville',
  lastName: 'Vilkas',
  email: '',
  phone: '555-2580',
  language: 'fi',
  dateOfBirth: '1999-12-31',
  streetAddress: 'Toistie 33',
  postalCode: '02230',
  postOffice: 'Espoo',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
}

export const enduserDeceasedChildFixture: PersonDetail = {
  id: 'b8711722-0c1b-4044-a794-5b308207d78b',
  ssn: '150515-999T',
  firstName: 'Unelma',
  lastName: 'Aapinen',
  email: '',
  phone: '',
  language: 'fi',
  dateOfBirth: '2015-05-15',
  dateOfDeath: '2020-06-01',
  streetAddress: 'Aapiskatu 1',
  postalCode: '00340',
  postOffice: 'Espoo',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
}

export const enduserNonSsnChildFixture: PersonDetail = {
  id: 'a5e87ec8-6221-46f8-8b2b-9ab124d51c22',
  firstName: 'Heluna',
  lastName: 'Hetuton',
  email: '',
  phone: '',
  language: 'fi',
  dateOfBirth: '2018-08-15',
  streetAddress: 'Suosiellä 1',
  postalCode: '00340',
  postOffice: 'Espoo',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
}

const twoGuardiansGuardian1 = {
  id: '9d6289ba-9ffd-11ea-bb37-0242ac130002',
  ssn: '220281-9456',
  firstName: 'Mikael Ilmari Juhani Johannes',
  lastName: 'Högfors',
  email: 'mikael.hogfors@evaka.test',
  phone: '123456789',
  language: 'fi',
  dateOfBirth: '1981-02-22',
  streetAddress: 'Kamreerintie 4',
  postalCode: '02100',
  postOffice: 'Espoo',
  residenceCode: 'twoGuardiansSameAddressResidenceCode',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
}
const twoGuardiansGuardian2 = {
  id: 'd1c30734-c02f-4546-8123-856f8101565e',
  ssn: '170590-9540',
  firstName: 'Kaarina Marjatta Anna Liisa',
  lastName: 'Högfors',
  email: 'kaarina.hogfors@evaka.test',
  phone: '123456789',
  language: 'fi',
  dateOfBirth: '1990-05-17',
  streetAddress: 'Kamreerintie 4',
  postalCode: '02100',
  postOffice: 'Espoo',
  residenceCode: 'twoGuardiansSameAddressResidenceCode',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
}
const twoGuardiansChildren = [
  {
    id: '6ec99620-9ffd-11ea-bb37-0242ac130002',
    ssn: '071013A960W',
    firstName: 'Antero Onni Leevi Aatu',
    lastName: 'Högfors',
    email: '',
    phone: '',
    language: 'fi',
    dateOfBirth: '2013-10-07',
    streetAddress: 'Kamreerintie 4',
    postalCode: '02100',
    postOffice: 'Espoo',
    nationalities: ['FI'],
    restrictedDetailsEnabled: false,
    restrictedDetailsEndDate: null
  }
]
export const familyWithTwoGuardians = {
  guardian: {
    ...twoGuardiansGuardian1,
    dependants: twoGuardiansChildren
  },
  otherGuardian: {
    ...twoGuardiansGuardian2,
    dependants: twoGuardiansChildren
  },
  children: twoGuardiansChildren.map((child) => ({
    ...child,
    guardians: [twoGuardiansGuardian1, twoGuardiansGuardian2]
  }))
}

const separatedGuardiansGuardian1 = {
  id: '1c1b2946-fdf3-4e02-a3e4-2c2a797bafc3',
  firstName: 'John',
  lastName: 'Doe',
  dateOfBirth: '1980-01-01',
  ssn: '010180-1232',
  streetAddress: 'Kamreerintie 2',
  postalCode: '02770',
  postOffice: 'Espoo'
}
const separatedGuardiansGuardian2 = {
  id: '56064714-649f-457e-893a-44832936166c',
  firstName: 'Joan',
  lastName: 'Doe',
  dateOfBirth: '1979-02-01',
  ssn: '010279-123L',
  streetAddress: 'Testikatu 1',
  postalCode: '02770',
  postOffice: 'Espoo'
}
const separatedGuardiansChildren = [
  {
    id: '5474ee62-16cf-4cfe-a297-40559e165a32',
    firstName: 'Ricky',
    lastName: 'Doe',
    dateOfBirth: '2017-06-01',
    ssn: '010617A123U',
    streetAddress: 'Kamreerintie 2',
    postalCode: '02770',
    postOffice: 'Espoo'
  }
]
export const familyWithSeparatedGuardians = {
  guardian: {
    ...separatedGuardiansGuardian1,
    dependants: separatedGuardiansChildren
  },
  otherGuardian: {
    ...separatedGuardiansGuardian2,
    dependants: separatedGuardiansChildren
  },
  children: separatedGuardiansChildren.map((child) => ({
    ...child,
    guardians: [separatedGuardiansGuardian1, separatedGuardiansGuardian2]
  }))
}

const restrictedDetailsGuardian = {
  id: '7699f488-3fdc-11eb-b378-0242ac130002',
  ssn: '080884-999H',
  firstName: 'Kaj Erik',
  lastName: 'Pelimerkki',
  email: 'kaj@example.com',
  phone: '123456789',
  language: 'fi',
  dateOfBirth: '1984-08-08',
  streetAddress: 'Kamreerintie 4',
  postalCode: '02100',
  postOffice: 'Espoo',
  nationalities: ['FI'],
  restrictedDetailsEnabled: true,
  restrictedDetailsEndDate: null
}

const guardian2WithNoRestrictions = {
  id: '1fd05a42-3fdd-11eb-b378-0242ac130002',
  ssn: '130486-9980',
  firstName: 'Helga Helen',
  lastName: 'Lehtokurppa',
  email: 'helga@example.com',
  phone: '123456789',
  language: 'fi',
  dateOfBirth: '1986-04-13',
  streetAddress: 'Westendinkatu 3',
  postalCode: '02100',
  postOffice: 'Espoo',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
}

const restrictedDetailsGuardiansChildren = [
  {
    id: '82a2586e-3fdd-11eb-b378-0242ac130002',
    firstName: 'Vadelma',
    lastName: 'Pelimerkki',
    dateOfBirth: '2017-05-15',
    ssn: '150517A9989',
    streetAddress: 'Kamreerintie 4',
    postalCode: '02100',
    postOffice: 'Espoo'
  }
]

export const familyWithRestrictedDetailsGuardian = {
  guardian: {
    ...restrictedDetailsGuardian,
    dependants: restrictedDetailsGuardiansChildren
  },
  otherGuardian: {
    ...guardian2WithNoRestrictions,
    dependants: restrictedDetailsGuardiansChildren
  },
  children: restrictedDetailsGuardiansChildren.map((child) => ({
    ...child,
    guardians: [restrictedDetailsGuardian, guardian2WithNoRestrictions]
  }))
}

const deadGuardian = {
  id: 'faacfd43-878f-4a70-9e74-2051a18480e6',
  ssn: '080581-999A',
  firstName: 'Kuisma',
  lastName: 'Kuollut',
  email: 'kuisma@example.com',
  phone: '123456789',
  language: 'fi',
  dateOfBirth: '1981-05-08',
  dateOfDeath: '2021-05-01',
  streetAddress: 'Kamreerintie 4',
  postalCode: '02100',
  postOffice: 'Espoo',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
}

const deadGuardianChild = {
  id: '1ad3469b-593d-45e4-a68b-a09f759bd029',
  firstName: 'Kuopus',
  lastName: 'Kuollut',
  dateOfBirth: '2019-09-09',
  ssn: '090917A998M',
  streetAddress: 'Kamreerintie 4',
  postalCode: '02100',
  postOffice: 'Espoo'
}

export const familyWithDeadGuardian = {
  guardian: {
    ...deadGuardian,
    dependants: [deadGuardianChild]
  },
  children: [
    {
      ...deadGuardianChild,
      guardians: [deadGuardian]
    }
  ]
}

export const personFixtureChildZeroYearOld: PersonDetail = {
  id: '0909e93d-3aa8-44f8-ac30-ecd77339d849',
  ssn: undefined,
  firstName: 'Vasta Syntynyt',
  lastName: 'Korhonen-Hämäläinen',
  email: '',
  phone: '',
  language: 'fi',
  dateOfBirth: LocalDate.today().formatIso(), // Always a zero-year-old
  streetAddress: 'Kamreerintie 2',
  postalCode: '00370',
  postOffice: 'Espoo',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
}

export const restrictedPersonFixture: PersonDetail = {
  id: '92d707e9-6cbc-487b-8bde-0097d90044cd',
  ssn: '031083-910S',
  firstName: 'Seija Anna Kaarina',
  lastName: 'Sotka',
  email: '',
  phone: '',
  language: 'fi',
  dateOfBirth: '1983-10-03',
  streetAddress: '',
  postalCode: '',
  postOffice: '',
  nationalities: ['FI'],
  restrictedDetailsEnabled: true,
  restrictedDetailsEndDate: null
}

export const adultFixtureWihtoutSSN = {
  id: 'a6cf0ec0-4573-4816-be30-6b87fd943817',
  firstName: 'Aikuinen',
  lastName: 'Hetuton',
  dateOfBirth: '1980-01-01',
  streetAddress: 'Kamreerintie 2',
  postalCode: '02770',
  postOffice: 'Espoo'
}

const applicationForm = (
  type: ApplicationType,
  child: PersonDetail,
  guardian: PersonDetail,
  guardian2Phone: string,
  guardian2Email: string,
  otherGuardianAgreementStatus: OtherGuardianAgreementStatus | null,
  preferredStartDate: LocalDate,
  preferredUnits: string[],
  connectedDaycare = false
): ApplicationForm => {
  const secondGuardian =
    guardian2Phone || guardian2Email || otherGuardianAgreementStatus
      ? {
          phoneNumber: guardian2Phone,
          email: guardian2Email,
          agreementStatus: otherGuardianAgreementStatus
        }
      : null

  const serviceNeed =
    type === 'PRESCHOOL' && !connectedDaycare
      ? null
      : {
          startTime: '08:00',
          endTime: '16:00',
          partTime: false,
          shiftCare: false,
          serviceNeedOption: null
        }

  return {
    child: {
      dateOfBirth: LocalDate.parseNullableIso(child.dateOfBirth),
      person: {
        ...child,
        socialSecurityNumber: child.ssn ?? null
      },
      address: {
        street: child.streetAddress ?? '',
        postalCode: child.postalCode ?? '',
        postOffice: child.postOffice ?? ''
      },
      futureAddress: null,
      nationality: 'FI',
      language: 'fi',
      allergies: '',
      diet: '',
      assistanceNeeded: false,
      assistanceDescription: ''
    },
    guardian: {
      person: {
        ...guardian,
        socialSecurityNumber: guardian.ssn ?? null
      },
      phoneNumber: guardian.phone ?? '',
      email: guardian.email ?? '',
      address: {
        street: guardian.streetAddress ?? '',
        postalCode: guardian.postalCode ?? '',
        postOffice: guardian.postOffice ?? ''
      },
      futureAddress: null
    },
    secondGuardian,
    otherPartner: null,
    maxFeeAccepted: false,
    otherChildren: [],
    preferences: {
      preferredStartDate,
      preferredUnits: preferredUnits.map((id) => ({ id, name: id })),
      preparatory: false,
      serviceNeed,
      siblingBasis: null,
      urgent: false
    },
    clubDetails: {
      wasOnClubCare: false,
      wasOnDaycare: false
    },
    otherInfo: ''
  }
}

export const applicationFixtureId = '9dd0e1ba-9b3b-11ea-bb37-0242ac130002'
export const applicationFixture = (
  child: PersonDetail,
  guardian: PersonDetail,
  otherGuardian: PersonDetail | undefined = undefined,
  type: 'DAYCARE' | 'PRESCHOOL' | 'CLUB' = 'DAYCARE',
  otherGuardianAgreementStatus: OtherGuardianAgreementStatus | null = null,
  preferredUnits: string[] = [daycareFixture.id],
  connectedDaycare = false,
  status: ApplicationStatus = 'SENT',
  preferredStartDate: LocalDate = LocalDate.of(2021, 8, 16),
  transferApplication = false
): Application => ({
  id: applicationFixtureId,
  type: type,
  childId: child.id,
  guardianId: guardian.id,
  otherGuardianId: otherGuardian?.id,
  form: applicationForm(
    type,
    child,
    guardian,
    otherGuardian?.phone ?? '',
    otherGuardian?.email ?? '',
    otherGuardianAgreementStatus,
    preferredStartDate,
    preferredUnits,
    connectedDaycare
  ),
  checkedByAdmin: false,
  hideFromGuardian: false,
  origin: 'ELECTRONIC',
  status,
  transferApplication
})

export const placementPlanFixture = (
  applicationId: string,
  unitId: string,
  periodStart: string,
  periodEnd: string
): PlacementPlan => ({
  applicationId,
  unitId,
  periodStart,
  periodEnd
})

const feeThresholds = {
  minIncomeThreshold: 210200,
  maxIncomeThreshold: 479900,
  incomeMultiplier: 0.107,
  minFee: 2700,
  maxFee: 28900
}

export const decisionFixture = (
  applicationId: string,
  startDate: string,
  endDate: string
): DecisionFixture => ({
  id: '9dd0e1ba-9b3b-11ea-bb37-0242ac130987',
  employeeId: 'SET_THIS',
  applicationId: applicationId,
  unitId: daycareFixture.id,
  type: 'DAYCARE',
  startDate: startDate,
  endDate: endDate
})

export const feeDecisionsFixture = (
  status: FeeDecisionStatus,
  adult: PersonDetail,
  child: PersonDetail,
  daycareId: UUID,
  partner: PersonDetail | null,
  validDuring: DateRange = new DateRange(
    LocalDate.today().subYears(1),
    LocalDate.today().addYears(1)
  ),
  sentAt: Date | null = null,
  id = 'bcc42d48-765d-4fe1-bc90-7a7b4c8205fe'
): FeeDecision => ({
  id,
  status,
  decisionType: 'NORMAL',
  validDuring,
  validFrom: validDuring.start,
  validTo: validDuring.end,
  headOfFamilyId: adult.id,
  partnerId: partner?.id ?? null,
  headOfFamilyIncome: null,
  partnerIncome: null,
  familySize: 2,
  feeThresholds: feeThresholds,
  children: [
    {
      child: {
        id: child.id,
        dateOfBirth: LocalDate.parseIso(child.dateOfBirth)
      },
      placement: {
        unitId: daycareId,
        type: 'DAYCARE'
      },
      serviceNeed: {
        feeCoefficient: 1.0,
        contractDaysPerMonth: null,
        descriptionFi: 'palveluntarve',
        descriptionSv: 'vårdbehövet',
        missing: false
      },
      baseFee: 28900,
      fee: 28900,
      siblingDiscount: 0.0,
      feeAlterations: [],
      finalFee: 28900,
      childIncome: null
    }
  ],
  totalFee: 28900,
  sentAt,
  approvedAt: null,
  approvedById: null,
  decisionHandlerId: null,
  decisionNumber: null,
  documentKey: null,
  created: new Date()
})

export const voucherValueDecisionsFixture = (
  id: UUID,
  adultId: UUID,
  childId: UUID,
  daycareId: UUID,
  partner: PersonDetail | null = null,
  status: 'DRAFT' | 'SENT' = 'DRAFT',
  validFrom = LocalDate.today().subYears(1),
  validTo = LocalDate.today().addYears(1),
  sentAt: Date | null = null
): VoucherValueDecision => ({
  id,
  status,
  validFrom,
  validTo,
  headOfFamilyId: adultId,
  partnerId: partner?.id ?? null,
  headOfFamilyIncome: null,
  partnerIncome: null,
  childIncome: null,
  decisionType: 'NORMAL',
  familySize: 2,
  feeThresholds: feeThresholds,
  child: { id: childId, dateOfBirth: LocalDate.of(2017, 6, 30) },
  placement: {
    unitId: daycareId,
    type: 'DAYCARE'
  },
  serviceNeed: {
    feeCoefficient: 1.0,
    voucherValueCoefficient: 1.0,
    feeDescriptionFi: '',
    feeDescriptionSv: '',
    voucherValueDescriptionFi: '',
    voucherValueDescriptionSv: ''
  },
  baseCoPayment: 28900,
  coPayment: 28900,
  siblingDiscount: 0.0,
  feeAlterations: [],
  finalCoPayment: 28900,
  baseValue: 87000,
  capacityFactor: 1.0,
  voucherValue: 87000,
  sentAt,
  approvedAt: null,
  approvedById: null,
  decisionNumber: null,
  documentKey: null,
  created: new Date(),
  decisionHandler: null
})

export const invoiceFixture = (
  adultId: UUID,
  childId: UUID,
  areaId: UUID,
  unitId: UUID,
  status: Invoice['status'],
  periodStart = LocalDate.of(2019, 1, 1),
  periodEnd = LocalDate.of(2019, 1, 1)
): Invoice => ({
  id: uuidv4(),
  status,
  headOfFamily: adultId,
  codebtor: null,
  areaId,
  periodStart,
  periodEnd,
  invoiceDate: periodStart,
  dueDate: periodEnd,
  number: null,
  sentAt: null,
  sentBy: null,
  rows: [
    {
      id: uuidv4(),
      child: childId,
      amount: 1,
      unitPrice: 10000,
      periodStart: periodStart,
      periodEnd: periodEnd,
      product: 'DAYCARE',
      unitId,
      description: '',
      price: 10000,
      correctionId: null
    }
  ],
  totalPrice: 10000
})

export const daycareGroupFixture: DaycareGroup = {
  id: '2f998c23-0f90-4afd-829b-d09ecf2f6188',
  daycareId: daycareFixture.id,
  name: 'Kosmiset vakiot',
  startDate: '2000-01-01'
}

export function createChildFixture(childId: string): Child {
  return {
    id: childId
  }
}

export function createDaycarePlacementFixture(
  id: string,
  childId: string,
  unitId: string,
  startDate = '2021-05-01',
  endDate = '2022-08-31',
  type: PlacementType = 'DAYCARE'
): DaycarePlacement {
  return {
    id,
    type,
    childId,
    unitId,
    startDate,
    endDate
  }
}

export function createPreschoolDaycarePlacementFixture(
  id: string,
  childId: string,
  unitId: string,
  startDate = '2021-05-01',
  endDate = '2022-08-31'
): DaycarePlacement {
  return {
    id,
    type: 'PRESCHOOL_DAYCARE',
    childId,
    unitId,
    startDate,
    endDate
  }
}

export function createBackupCareFixture(
  childId: string,
  unitId: string,
  groupId?: string
): BackupCare {
  return {
    id: 'b501d5e7-efcd-49e9-9371-23262b5cd51f',
    childId,
    unitId: unitId,
    groupId,
    period: {
      start: '2023-02-01',
      end: '2023-02-03'
    }
  }
}

export const DecisionIncomeFixture = (
  total: number,
  validFrom: LocalDate = LocalDate.today(),
  validTo: LocalDate = LocalDate.today()
): DecisionIncome => ({
  data: { MAIN_INCOME: total },
  effect: 'INCOME',
  total: total,
  totalExpenses: 0,
  totalIncome: total,
  validFrom,
  validTo,
  worksAtECHA: false
})

export const nullUUID = '00000000-0000-0000-0000-000000000000'

export const uuidv4 = (): string => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function (c) {
    const r = (Math.random() * 16) | 0,
      v = c == 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

export const uniqueLabel = (l = 7): string =>
  Math.random().toString(36).substring(0, l)

export class Fixture {
  static daycare(): DaycareBuilder {
    const id = uniqueLabel()
    return new DaycareBuilder({
      id: uuidv4(),
      careAreaId: '',
      name: `daycare_${id}`,
      type: ['CENTRE'],
      costCenter: `costCenter_${id}`,
      streetAddress: `streetAddress_${id}`,
      postalCode: '02230',
      postOffice: 'Espoo',
      decisionDaycareName: `decisionDaycareName_${id}`,
      decisionPreschoolName: `decisionPreschoolName_${id}`,
      decisionHandler: `decisionHandler_${id}`,
      decisionHandlerAddress: `decisionHandlerAddress_${id}`,
      providerType: 'MUNICIPAL',
      operationDays: [1, 2, 3, 4, 5],
      roundTheClock: true,
      enabledPilotFeatures: ['MESSAGING', 'MOBILE']
    })
  }

  static daycareGroup(): DaycareGroupBuilder {
    const id = uniqueLabel()
    return new DaycareGroupBuilder({
      id: uuidv4(),
      daycareId: '',
      name: `daycareGroup_${id}`,
      startDate: '2020-01-01'
    })
  }

  static careArea(): CareAreaBuilder {
    const id = uniqueLabel()
    return new CareAreaBuilder({
      id: uuidv4(),
      name: `Care Area ${id}`,
      shortName: `careArea_${id}`,
      areaCode: '02230',
      subCostCenter: `subCostCenter_${id}`
    })
  }

  static person(): PersonBuilder {
    const id = uniqueLabel()
    return new PersonBuilder({
      id: uuidv4(),
      dateOfBirth: '2020-05-05',
      ssn: '050520A999M',
      email: `email_${id}@evaka.test`,
      firstName: `firstName_${id}`,
      lastName: `lastName_${id}`,
      language: `fi`,
      nationalities: [],
      phone: '123456789',
      postalCode: '02230',
      postOffice: 'Espoo',
      residenceCode: `residenceCode_${id}`,
      restrictedDetailsEnabled: false,
      restrictedDetailsEndDate: undefined,
      streetAddress: `streetAddress_${id}`
    })
  }

  static employee(): EmployeeBuilder {
    const id = uniqueLabel()
    return new EmployeeBuilder({
      id: uuidv4(),
      email: `email_${id}@evaka.test`,
      externalId: `espoo-ad:${uuidv4()}`,
      firstName: `first_name_${id}`,
      lastName: `last_name_${id}`,
      roles: []
    })
  }

  static employeeAdmin(): EmployeeBuilder {
    return Fixture.employee().with({
      email: 'seppo.sorsa@evaka.test',
      firstName: 'Seppo',
      lastName: 'Sorsa',
      roles: ['ADMIN']
    })
  }

  static employeeFinanceAdmin(): EmployeeBuilder {
    return Fixture.employee().with({
      email: 'lasse.laskuttaja@evaka.test',
      firstName: 'Lasse',
      lastName: 'Laskuttaja',
      roles: ['FINANCE_ADMIN']
    })
  }

  static employeeDirector(): EmployeeBuilder {
    return Fixture.employee().with({
      email: 'hemmo.hallinto@evaka.test',
      firstName: 'Hemmo',
      lastName: 'Hallinto',
      roles: ['DIRECTOR']
    })
  }

  static employeeReportViewer(): EmployeeBuilder {
    return Fixture.employee().with({
      email: 'raisa.raportoija@evaka.test',
      firstName: 'Raisa',
      lastName: 'Raportoija',
      roles: ['REPORT_VIEWER']
    })
  }

  static employeeServiceWorker(): EmployeeBuilder {
    return Fixture.employee().with({
      email: 'paula.palveluohjaaja@evaka.test',
      firstName: 'Paula',
      lastName: 'Palveluohjaaja',
      roles: ['SERVICE_WORKER']
    })
  }

  static employeeUnitSupervisor(unitId: string): EmployeeBuilder {
    return Fixture.employee()
      .with({
        email: 'essi.esimies@evaka.test',
        firstName: 'Essi',
        lastName: 'Esimies',
        roles: []
      })
      .withDaycareAcl(unitId, 'UNIT_SUPERVISOR')
  }

  static employeeSpecialEducationTeacher(unitId: string): EmployeeBuilder {
    return Fixture.employee()
      .with({
        email: 'erkki.erityisopettaja@evaka.test',
        firstName: 'Erkki',
        lastName: 'Erityisopettaja',
        roles: []
      })
      .withDaycareAcl(unitId, 'SPECIAL_EDUCATION_TEACHER')
  }

  static employeeStaff(unitId: string): EmployeeBuilder {
    return Fixture.employee()
      .with({
        email: 'kaisa.kasvattaja@evaka.test',
        firstName: 'Kaisa',
        lastName: 'Kasvattaja',
        roles: []
      })
      .withDaycareAcl(unitId, 'STAFF')
  }

  static decision(): DecisionBuilder {
    return new DecisionBuilder({
      id: uuidv4(),
      applicationId: nullUUID,
      employeeId: 'not set',
      unitId: nullUUID,
      type: 'DAYCARE',
      startDate: '2020-01-01',
      endDate: '2021-01-01'
    })
  }

  static employeePin(): EmployeePinBuilder {
    return new EmployeePinBuilder({
      id: uuidv4(),
      userId: 'not_set',
      pin: uniqueLabel(4)
    })
  }

  static pedagogicalDocument(): PedagogicalDocumentBuilder {
    return new PedagogicalDocumentBuilder({
      id: uuidv4(),
      childId: 'not set',
      description: 'not set'
    })
  }

  static placement(): PlacementBuilder {
    return new PlacementBuilder({
      id: uuidv4(),
      childId: 'not set',
      unitId: 'not set',
      type: 'DAYCARE',
      startDate: LocalDate.today().formatIso(),
      endDate: LocalDate.today().addYears(1).formatIso()
    })
  }

  static groupPlacement(): GroupPlacementBuilder {
    return new GroupPlacementBuilder({
      id: uuidv4(),
      daycareGroupId: 'not set',
      daycarePlacementId: 'not set',
      startDate: LocalDate.today().formatIso(),
      endDate: LocalDate.today().addYears(1).formatIso()
    })
  }

  static backupCare(): BackupCareBuilder {
    return new BackupCareBuilder({
      id: uuidv4(),
      childId: 'not set',
      unitId: 'not set',
      groupId: 'not set',
      period: {
        start: LocalDate.today().formatIso(),
        end: LocalDate.today().formatIso()
      }
    })
  }

  static serviceNeed(): ServiceNeedBuilder {
    return new ServiceNeedBuilder({
      id: uuidv4(),
      placementId: 'not set',
      startDate: new Date(),
      endDate: new Date(),
      optionId: 'not set',
      shiftCare: false,
      confirmedBy: 'not set',
      confirmedAt: LocalDate.today()
    })
  }

  static serviceNeedOption(): ServiceNeedOptionBuilder {
    const id = uniqueLabel()

    return new ServiceNeedOptionBuilder({
      id: uuidv4(),
      daycareHoursPerWeek: 0,
      contractDaysPerMonth: null,
      defaultOption: false,
      feeCoefficient: 0.0,
      feeDescriptionFi: `Test service need option ${id}`,
      feeDescriptionSv: `Test service need option ${id}`,
      nameFi: `test_service_need_option_${id}`,
      nameSv: `test_service_need_option_${id}`,
      nameEn: `test_service_need_option_${id}`,
      occupancyCoefficient: 0,
      occupancyCoefficientUnder3y: 0,
      partDay: false,
      partWeek: false,
      updated: new Date(),
      validPlacementType: 'DAYCARE',
      voucherValueCoefficient: 0,
      voucherValueDescriptionFi: `Test service need option ${id}`,
      voucherValueDescriptionSv: `Test service need option ${id}`,
      active: true
    })
  }

  static child(id: string): ChildBuilder {
    return new ChildBuilder({ id })
  }

  static assistanceNeed(): AssistanceNeedBuilder {
    return new AssistanceNeedBuilder({
      id: uuidv4(),
      capacityFactor: 1.0,
      childId: 'not_set',
      description: '',
      startDate: new Date(),
      endDate: new Date(),
      otherBasis: '',
      updatedBy: ''
    })
  }

  static daycareCaretakers(): DaycareCaretakersBuilder {
    return new DaycareCaretakersBuilder({
      groupId: 'not_set',
      amount: 1,
      startDate: LocalDate.today(),
      endDate: null
    })
  }

  static childAttendances(): ChildAttendanceBuilder {
    return new ChildAttendanceBuilder({
      childId: '',
      unitId: '',
      arrived: new Date(),
      departed: new Date()
    })
  }

  static feeThresholds(): FeeThresholdBuilder {
    return new FeeThresholdBuilder({
      validDuring: new DateRange(LocalDate.of(2020, 1, 1), null),
      maxFee: 10000,
      minFee: 1000,
      minIncomeThreshold2: 100000,
      incomeMultiplier2: 0.1,
      maxIncomeThreshold2: 200000,
      minIncomeThreshold3: 200000,
      incomeMultiplier3: 0.1,
      maxIncomeThreshold3: 300000,
      minIncomeThreshold4: 300000,
      incomeMultiplier4: 0.1,
      maxIncomeThreshold4: 400000,
      minIncomeThreshold5: 400000,
      incomeMultiplier5: 0.1,
      maxIncomeThreshold5: 500000,
      minIncomeThreshold6: 500000,
      incomeMultiplier6: 0.1,
      maxIncomeThreshold6: 600000,
      incomeThresholdIncrease6Plus: 100000,
      siblingDiscount2: 0.5,
      siblingDiscount2Plus: 0.8,
      temporaryFee: 2800,
      temporaryFeePartDay: 1500,
      temporaryFeeSibling: 1500,
      temporaryFeeSiblingPartDay: 800
    })
  }

  static income(): IncomeBuilder {
    return new IncomeBuilder({
      id: uuidv4(),
      personId: 'not_set',
      validFrom: LocalDate.today(),
      validTo: LocalDate.today().addYears(1),
      data: {
        MAIN_INCOME: {
          amount: 100000,
          monthlyAmount: 100000,
          coefficient: 'MONTHLY_NO_HOLIDAY_BONUS'
        }
      },
      effect: 'INCOME',
      updatedAt: new Date(),
      updatedBy: 'not_set'
    })
  }

  static vardaReset(): VardaResetBuilder {
    return new VardaResetBuilder({
      evakaChildId: uuidv4(),
      resetTimestamp: new Date()
    })
  }

  static vardaServiceNeed(): VardaServiceNeedBuilder {
    return new VardaServiceNeedBuilder({
      evakaServiceNeedId: uuidv4(),
      evakaChildId: uuidv4(),
      evakaServiceNeedUpdated: new Date(),
      updateFailed: false,
      errors: []
    })
  }

  static placementPlan(): PlacementPlanBuilder {
    return new PlacementPlanBuilder({
      applicationId: uuidv4(),
      unitId: uuidv4(),
      periodStart: '',
      periodEnd: ''
    })
  }

  static holidayPeriod(): HolidayPeriodBuilder {
    return new HolidayPeriodBuilder({
      id: uuidv4(),
      period: new FiniteDateRange(LocalDate.today(), LocalDate.today()),
      reservationDeadline: LocalDate.today()
    })
  }

  static holidayQuestionnaire(): HolidayQuestionnaireBuilder {
    return new HolidayQuestionnaireBuilder({
      id: uuidv4(),
      type: 'FIXED_PERIOD',
      absenceType: 'OTHER_ABSENCE',
      title: { fi: '', sv: '', en: '' },
      description: { fi: '', sv: '', en: '' },
      descriptionLink: { fi: '', sv: '', en: '' },
      active: new FiniteDateRange(LocalDate.today(), LocalDate.today()),
      conditions: {
        continuousPlacement: null
      },
      periodOptions: [],
      periodOptionLabel: { fi: '', sv: '', en: '' },
      requiresStrongAuth: false
    })
  }

  static guardian(child: PersonBuilder, guardian: PersonBuilder) {
    return new GuardianBuilder({
      childId: child.data.id,
      guardianId: guardian.data.id
    })
  }

  static fridgeChild() {
    return new FridgeChildBuilder({
      id: uuidv4(),
      headOfChild: 'not set',
      childId: 'not set',
      startDate: LocalDate.of(2020, 1, 1),
      endDate: LocalDate.of(2020, 12, 31)
    })
  }

  static staffOccupancyCoefficient(unitId: string, employeeId: string) {
    return new StaffOccupancyCoefficientBuilder({
      coefficient: 7,
      employeeId,
      unitId
    })
  }
}

abstract class FixtureBuilder<T> {
  constructor(public data: T) {}

  with<K extends keyof T>(value: Pick<T, K>): this {
    this.data = {
      ...this.data,
      ...value
    }
    return this
  }

  abstract copy(): FixtureBuilder<T>

  abstract save(): Promise<FixtureBuilder<T>>
}

export class DaycareBuilder extends FixtureBuilder<Daycare> {
  id(id: string): DaycareBuilder {
    this.data.id = id
    return this
  }

  withRandomId(): DaycareBuilder {
    this.data.id = uuidv4()
    return this
  }

  careArea(careArea: CareAreaBuilder): DaycareBuilder {
    this.data.careAreaId = careArea.data.id
    return this
  }

  async save() {
    await insertDaycareFixtures([this.data])
    return this
  }

  // Note: shallow copy
  copy() {
    return new DaycareBuilder({ ...this.data })
  }
}

export class DaycareGroupBuilder extends FixtureBuilder<DaycareGroup> {
  daycare(daycare: DaycareBuilder): DaycareGroupBuilder {
    this.data.daycareId = daycare.data.id
    return this
  }

  async save() {
    await insertDaycareGroupFixtures([this.data])
    return this
  }

  // Note: shallow copy
  copy() {
    return new DaycareGroupBuilder({ ...this.data })
  }
}

export class CareAreaBuilder extends FixtureBuilder<CareArea> {
  id(id: string): CareAreaBuilder {
    this.data.id = id
    return this
  }

  async save() {
    await insertCareAreaFixtures([this.data])
    return this
  }

  // Note: shallow copy
  copy() {
    return new CareAreaBuilder({ ...this.data })
  }
}

export class PersonBuilder extends FixtureBuilder<PersonDetailWithDependantsAndGuardians> {
  async save() {
    await insertPersonFixture(this.data)
    return this
  }

  async saveAndUpdateMockVtj(): Promise<PersonBuilder> {
    await this.save()
    await insertVtjPersonFixture(this.data)
    return this
  }

  // Note: shallow copy
  copy() {
    return new PersonBuilder({ ...this.data })
  }
}

export class EmployeeBuilder extends FixtureBuilder<EmployeeDetail> {
  daycareAcl: { unitId: string; role: ScopedRole }[] = []
  groupAcl: string[] = []

  withDaycareAcl(unitId: string, role: ScopedRole): this {
    this.daycareAcl.push({ unitId, role })
    return this
  }

  withGroupAcl(groupId: string): this {
    this.groupAcl.push(groupId)
    return this
  }

  async save() {
    await insertEmployeeFixture(this.data)
    for (const { unitId, role } of this.daycareAcl) {
      await setAclForDaycares(this.data.externalId, unitId, role)
    }
    if (this.groupAcl.length > 0) {
      await setAclForDaycareGroups(this.data.id, this.groupAcl)
    }
    return this
  }

  // Note: shallow copy
  copy() {
    return new EmployeeBuilder({ ...this.data })
  }
}

export class DecisionBuilder extends FixtureBuilder<DecisionFixture> {
  async save() {
    await insertDecisionFixtures([this.data])
    return this
  }

  // Note: shallow copy
  copy() {
    return new DecisionBuilder({ ...this.data })
  }
}

export class EmployeePinBuilder extends FixtureBuilder<EmployeePin> {
  async save() {
    await insertEmployeePins([this.data])
    return this
  }

  // Note: shallow copy
  copy() {
    return new EmployeePinBuilder({ ...this.data })
  }
}

export class PedagogicalDocumentBuilder extends FixtureBuilder<PedagogicalDocument> {
  async save() {
    await insertPedagogicalDocuments([this.data])
    return this
  }

  // Note: shallow copy
  copy() {
    return new PedagogicalDocumentBuilder({ ...this.data })
  }
}

export class ServiceNeedOptionBuilder extends FixtureBuilder<ServiceNeedOption> {
  async save() {
    await insertServiceNeedOptions([this.data])
    return this
  }

  // Note: shallow copy
  copy() {
    return new ServiceNeedOptionBuilder({ ...this.data })
  }
}

export class PlacementBuilder extends FixtureBuilder<DaycarePlacement> {
  async save() {
    await insertDaycarePlacementFixtures([this.data])
    return this
  }

  // Note: shallow copy
  copy() {
    return new PlacementBuilder({ ...this.data })
  }

  child(child: PersonBuilder) {
    this.data = {
      ...this.data,
      childId: child.data.id
    }
    return this
  }

  daycare(daycare: DaycareBuilder) {
    this.data = {
      ...this.data,
      unitId: daycare.data.id
    }
    return this
  }
}

export class GroupPlacementBuilder extends FixtureBuilder<DaycareGroupPlacement> {
  withGroup(group: DaycareGroupBuilder): this {
    this.data = {
      ...this.data,
      daycareGroupId: group.data.id
    }
    return this
  }

  withPlacement(placement: PlacementBuilder): this {
    this.data = {
      ...this.data,
      daycarePlacementId: placement.data.id,
      startDate: placement.data.startDate,
      endDate: placement.data.endDate
    }
    return this
  }

  async save() {
    await insertDaycareGroupPlacementFixtures([this.data])
    return this
  }

  // Note: shallow copy
  copy() {
    return new GroupPlacementBuilder({ ...this.data })
  }
}

export class BackupCareBuilder extends FixtureBuilder<BackupCare> {
  withChild(child: ChildBuilder) {
    this.data = {
      ...this.data,
      childId: child.data.id
    }
    return this
  }

  withGroup(group: DaycareGroupBuilder) {
    this.data = {
      ...this.data,
      groupId: group.data.id,
      unitId: group.data.daycareId
    }
    return this
  }

  async save() {
    await insertBackupCareFixtures([this.data])
    return this
  }

  // Note: shallow copy
  copy() {
    return new BackupCareBuilder({ ...this.data })
  }
}

export class ServiceNeedBuilder extends FixtureBuilder<ServiceNeedFixture> {
  async save() {
    await insertServiceNeeds([this.data])
    return this
  }

  // Note: shallow copy
  copy() {
    return new ServiceNeedBuilder({ ...this.data })
  }
}

export class ChildBuilder extends FixtureBuilder<Child> {
  async save() {
    await insertChildFixtures([this.data])
    return this
  }

  // Note: shallow copy
  copy() {
    return new ChildBuilder({ ...this.data })
  }
}

export class AssistanceNeedBuilder extends FixtureBuilder<AssistanceNeed> {
  async save() {
    await insertAssistanceNeedFixtures([this.data])
    return this
  }

  // Note: shallow copy
  copy() {
    return new AssistanceNeedBuilder({ ...this.data })
  }
}

export class DaycareCaretakersBuilder extends FixtureBuilder<DaycareCaretakers> {
  async save(): Promise<FixtureBuilder<DaycareCaretakers>> {
    await insertDaycareCaretakerFixtures([this.data])
    return this
  }

  copy(): FixtureBuilder<DaycareCaretakers> {
    return new DaycareCaretakersBuilder({ ...this.data })
  }
}

export class ChildAttendanceBuilder extends FixtureBuilder<ChildAttendance> {
  async save(): Promise<FixtureBuilder<ChildAttendance>> {
    await insertChildAttendanceFixtures([this.data])
    return this
  }

  copy(): FixtureBuilder<ChildAttendance> {
    return new ChildAttendanceBuilder({ ...this.data })
  }
}

export class IncomeBuilder extends FixtureBuilder<DevIncome> {
  async save() {
    await insertIncome(this.data)
    return this
  }

  copy() {
    return new IncomeBuilder({ ...this.data })
  }
}

export class VardaResetBuilder extends FixtureBuilder<DevVardaReset> {
  async save() {
    await addVardaReset(this.data)
    return this
  }

  copy() {
    return new VardaResetBuilder({ ...this.data })
  }
}

export class VardaServiceNeedBuilder extends FixtureBuilder<DevVardaServiceNeed> {
  async save() {
    await addVardaServiceNeed(this.data)
    return this
  }

  copy() {
    return new VardaServiceNeedBuilder({ ...this.data })
  }
}

export class FeeThresholdBuilder extends FixtureBuilder<FeeThresholds> {
  async save() {
    await insertFeeThresholds(this.data)
    return this
  }

  copy() {
    return new FeeThresholdBuilder({ ...this.data })
  }
}

export class PlacementPlanBuilder extends FixtureBuilder<PlacementPlan> {
  async save() {
    await insertPlacementPlan(this.data)
    return this
  }

  copy() {
    return new PlacementPlanBuilder({ ...this.data })
  }
}

export class HolidayPeriodBuilder extends FixtureBuilder<HolidayPeriod> {
  async save() {
    await insertHolidayPeriod(this.data)
    return this
  }

  copy() {
    return new HolidayPeriodBuilder({ ...this.data })
  }
}

export class HolidayQuestionnaireBuilder extends FixtureBuilder<FixedPeriodQuestionnaire> {
  async save() {
    await insertHolidayQuestionnaire(this.data)
    return this
  }

  copy() {
    return new HolidayQuestionnaireBuilder({ ...this.data })
  }
}

export class GuardianBuilder extends FixtureBuilder<{
  guardianId: string
  childId: string
}> {
  async save() {
    await insertGuardianFixtures([this.data])
    return this
  }

  copy() {
    return new GuardianBuilder({ ...this.data })
  }
}

export class FridgeChildBuilder extends FixtureBuilder<FridgeChild> {
  async save() {
    await insertFridgeChildren([this.data])
    return this
  }

  copy() {
    return new FridgeChildBuilder({ ...this.data })
  }
}

export class StaffOccupancyCoefficientBuilder extends FixtureBuilder<DevStaffOccupancyCoefficient> {
  async save() {
    await upsertOccupancyCoefficient(this.data)
    return this
  }

  copy() {
    return new StaffOccupancyCoefficientBuilder({ ...this.data })
  }
}
