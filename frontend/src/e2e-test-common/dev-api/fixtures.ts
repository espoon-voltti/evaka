// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { format } from 'date-fns'
import config from '../config'
import {
  Application,
  ApplicationForm,
  ApplicationPersonDetail,
  BackupCare,
  CareArea,
  Child,
  Daycare,
  DaycareGroup,
  DaycarePlacement,
  DecisionFixture,
  EmployeeDetail,
  FeeDecision,
  FeeDecisionStatus,
  Invoice,
  OtherGuardianAgreementStatus,
  PersonDetail,
  PlacementPlan,
  VoucherValueDecision,
  UUID,
  DaycareGroupPlacement,
  EmployeePin
} from './types'
import {
  deleteCareAreaFixture,
  deleteDaycare,
  deleteDaycareGroup,
  deleteDecisionFixture,
  deleteEmployeeById,
  deleteEmployeePin,
  deletePersonFixture,
  deleteVtjPerson,
  insertCareAreaFixtures,
  insertDaycareFixtures,
  insertDaycareGroupFixtures,
  insertDecisionFixtures,
  insertEmployeeFixture,
  insertEmployeePins,
  insertPersonFixture,
  insertVtjPersonFixture,
  PersonDetailWithDependantsAndGuardians
} from './index'
import LocalDate from 'lib-common/local-date'
import DateRange from 'lib-common/date-range'
import { ApplicationStatus } from 'lib-common/api-types/application/enums'

export const supervisor: EmployeeDetail = {
  id: '552e5bde-92fb-4807-a388-40016f85f593',
  externalId: config.supervisorExternalId,
  firstName: 'Eva',
  lastName: 'Esihenkilo',
  email: 'eva.esihenkilo@espoo.fi',
  roles: ['SERVICE_WORKER', 'ADMIN']
}

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
  roundTheClock: true
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
  roundTheClock: true,
  location: {
    lat: 60.20377343765089,
    lon: 24.655715743526994
  }
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
  roundTheClock: true,
  location: {
    lat: 60.20350901607783,
    lon: 24.669
  }
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
  roundTheClock: true,
  location: {
    lat: 60.2040261560435,
    lon: 24.65517745652623
  }
}

export const enduserGuardianFixture: ApplicationPersonDetail = {
  id: '87a5c962-9b3d-11ea-bb37-0242ac130002',
  ssn: '070644-937X',
  firstName: 'Johannes Olavi Antero Tapio',
  lastName: 'Karhula',
  email: 'johannes.karhula@test.com',
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

export const enduserChildFixtureJari: ApplicationPersonDetail = {
  id: '572adb7e-9b3d-11ea-bb37-0242ac130002',
  ssn: '070714A9126',
  firstName: 'Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani',
  lastName: 'Karhula',
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

export const enduserChildFixtureKaarina: ApplicationPersonDetail = {
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

export const enduserChildFixturePorriHatterRestricted: ApplicationPersonDetail = {
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

export const enduserChildJariOtherGuardianFixture: ApplicationPersonDetail = {
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

const twoGuardiansGuardian1 = {
  id: '9d6289ba-9ffd-11ea-bb37-0242ac130002',
  ssn: '220281-9456',
  firstName: 'Mikael Ilmari Juhani Johannes',
  lastName: 'Högfors',
  email: 'mikael.hogfors@test.com',
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
  email: 'kaarina.hogfors@test.com',
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
    dateOfBirth: '2014-07-07',
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

export const personFixtureChildZeroYearOld: PersonDetail = {
  id: '0909e93d-3aa8-44f8-ac30-ecd77339d849',
  ssn: undefined,
  firstName: 'Vasta Syntynyt',
  lastName: 'Korhonen-Hämäläinen',
  email: '',
  phone: '',
  language: 'fi',
  dateOfBirth: format(new Date(), 'yyyy-MM-dd'), // Always a zero-year-old
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
  type: 'DAYCARE' | 'PRESCHOOL' | 'CLUB',
  child: PersonDetail,
  guardian: PersonDetail,
  guardian2Phone = '',
  guardian2Email = '',
  otherGuardianAgreementStatus: OtherGuardianAgreementStatus = null,
  preferredUnits: string[] = [daycareFixture.id],
  connectedDaycare = false
): ApplicationForm => ({
  type,
  additionalDetails: {
    allergyType: '',
    dietType: '',
    otherInfo: ''
  },
  apply: {
    preferredUnits: preferredUnits,
    siblingBasis: false
  },
  careDetails: {
    assistanceNeeded: false
  },
  child: {
    firstName: child.firstName,
    lastName: child.lastName,
    socialSecurityNumber: child.ssn,
    address: {
      street: child.streetAddress,
      postalCode: child.postalCode,
      city: child.postOffice,
      editable: false
    },
    nationality: 'FI',
    language: 'fi',
    restricted: false
  },
  connectedDaycare: type === 'PRESCHOOL' && connectedDaycare,
  docVersion: 0,
  extendedCare: false,
  guardian: {
    firstName: guardian.firstName,
    lastName: guardian.lastName,
    socialSecurityNumber: guardian.ssn,
    address: {
      street: guardian.streetAddress,
      postalCode: guardian.postalCode,
      city: guardian.postOffice,
      editable: false
    },
    phoneNumber: guardian.phone,
    email: guardian.email,
    restricted: false
  },
  guardian2: {
    firstName: '',
    lastName: '',
    socialSecurityNumber: '',
    address: {
      street: '',
      postalCode: '',
      city: '',
      editable: false
    },
    phoneNumber: guardian2Phone,
    email: guardian2Email,
    restricted: false
  },
  hasOtherAdult: false,
  hasOtherChildren: false,
  otherAdults: [],
  otherChildren: [],
  partTime: false,
  preferredStartDate: new Date().toISOString(),
  serviceEnd: '08:00',
  serviceStart: '16:00',
  urgent: false,
  otherGuardianAgreementStatus
})

export const applicationFixtureId = '9dd0e1ba-9b3b-11ea-bb37-0242ac130002'
export const applicationFixture = (
  child: ApplicationPersonDetail,
  guardian: ApplicationPersonDetail,
  otherGuardian: ApplicationPersonDetail | undefined = undefined,
  type: 'DAYCARE' | 'PRESCHOOL' | 'CLUB' = 'DAYCARE',
  otherGuardianAgreementStatus: OtherGuardianAgreementStatus = null,
  preferredUnits: string[] = [daycareFixture.id],
  connectedDaycare = false,
  status: ApplicationStatus = 'SENT'
): Application => ({
  id: applicationFixtureId,
  childId: child.id,
  guardianId: guardian.id,
  otherGuardianId: otherGuardian?.id,
  form: applicationForm(
    type,
    child,
    guardian,
    otherGuardian?.phone,
    otherGuardian?.email,
    otherGuardianAgreementStatus,
    preferredUnits,
    connectedDaycare
  ),
  checkedByAdmin: false,
  hideFromGuardian: false,
  origin: 'ELECTRONIC',
  status,
  transferApplication: false
})

export const placementPlanFixture = (
  unitId: string,
  periodStart: string,
  periodEnd: string
): PlacementPlan => ({
  unitId,
  periodStart,
  periodEnd
})

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
  adult: ApplicationPersonDetail,
  child: ApplicationPersonDetail,
  daycareId: UUID
): FeeDecision => ({
  id: 'bcc42d48-765d-4fe1-bc90-7a7b4c8205fe',
  status,
  decisionType: 'NORMAL',
  validFrom: LocalDate.today().subYears(1).formatIso(),
  validTo: LocalDate.today().addYears(1).formatIso(),
  headOfFamily: { id: adult.id },
  familySize: 2,
  pricing: {
    multiplier: '0.1070',
    maxThresholdDifference: 269700,
    minThreshold2: 210200,
    minThreshold3: 271300,
    minThreshold4: 308000,
    minThreshold5: 344700,
    minThreshold6: 381300,
    thresholdIncrease6Plus: 14200
  },
  parts: [
    {
      child: { id: child.id, dateOfBirth: child.dateOfBirth },
      placement: {
        unit: daycareId,
        type: 'DAYCARE',
        serviceNeed: 'GTE_35'
      },
      baseFee: 28900,
      fee: 28900,
      siblingDiscount: 0.0,
      feeAlterations: []
    }
  ]
})

export const voucherValueDecisionsFixture = (
  id: UUID,
  adultId: UUID,
  childId: UUID,
  daycareId: UUID,
  status: 'DRAFT' | 'SENT' = 'DRAFT',
  validFrom = LocalDate.today().subYears(1).formatIso(),
  validTo = LocalDate.today().addYears(1).formatIso()
): VoucherValueDecision => ({
  id,
  status,
  validFrom,
  validTo,
  headOfFamily: { id: adultId },
  familySize: 2,
  pricing: {
    multiplier: '0.1070',
    maxThresholdDifference: 269700,
    minThreshold2: 210200,
    minThreshold3: 271300,
    minThreshold4: 308000,
    minThreshold5: 344700,
    minThreshold6: 381300,
    thresholdIncrease6Plus: 14200
  },
  child: { id: childId, dateOfBirth: '2017-06-30' },
  placement: {
    unit: { id: daycareId },
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
  ageCoefficient: 1.0,
  voucherValue: 87000
})

export const invoiceFixture = (
  adultId: UUID,
  childId: UUID,
  daycareId: UUID,
  status: Invoice['status'],
  periodStart = '2019-01-01',
  periodEnd = '2019-01-01'
): Invoice => ({
  id: 'bcc42d48-765d-4fe1-bc90-7a7b4c8205fe',
  status,
  headOfFamily: { id: adultId },
  agreementType: 200,
  periodStart,
  periodEnd,
  rows: [
    {
      id: '592ddd9b-a99a-44f7-bd84-adaa68891df4',
      child: { id: childId, dateOfBirth: '2017-06-30' },
      placementUnit: { id: daycareId },
      amount: 1,
      unitPrice: 10000,
      periodStart: periodStart,
      periodEnd: periodEnd,
      product: 'DAYCARE',
      costCenter: 'cost center',
      subCostCenter: 'sub cost center',
      modifiers: []
    }
  ]
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
  startDate = '2020-05-01',
  endDate = '2021-08-31'
): DaycarePlacement {
  return {
    id,
    type: 'DAYCARE',
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
  startDate = '2020-05-01',
  endDate = '2021-08-31'
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

export function createDaycareGroupPlacementFixture(
  placementId: string,
  groupId: string,
  startDate = '2020-05-01',
  endDate = '2021-08-31'
): DaycareGroupPlacement {
  return {
    id: '2e19e400-ca0c-4059-b872-cce8b5282a72',
    daycareGroupId: groupId,
    daycarePlacementId: placementId,
    startDate,
    endDate
  }
}

export function createBackupCareFixture(
  childId: string,
  unitId: string
): BackupCare {
  return {
    id: 'b501d5e7-efcd-49e9-9371-23262b5cd51f',
    childId,
    unitId: unitId,
    period: {
      start: '2021-02-01',
      end: '2021-02-03'
    }
  }
}

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

type CleanupOperation = () => Promise<void>

export class Fixture {
  static cleanupOperations: CleanupOperation[] = []

  static async cleanup() {
    try {
      for (const op of this.cleanupOperations) {
        try {
          await op()
        } catch (e) {
          console.log('Error while cleaning up fixtures, continuing', e)
        }
      }
    } finally {
      Fixture.cleanupOperations = []
    }
  }

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
      roundTheClock: true
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
      email: `email_${id}@test.com`,
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
      email: `email_${id}@espoo.fi`,
      externalId: `e2etest:${uuidv4()}`,
      firstName: `first_name_${id}`,
      lastName: `last_name_${id}`,
      roles: ['ADMIN']
    })
  }

  static decision(): DecisionBuilder {
    return new DecisionBuilder({
      id: uuidv4(),
      applicationId: nullUUID,
      //eslint-disable-next-line @typescript-eslint/no-non-null-assertion
      employeeId: supervisor.externalId!,
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
}

export class DaycareBuilder {
  data: Daycare

  constructor(data: Daycare) {
    this.data = data
  }

  with(value: Partial<Daycare>): DaycareBuilder {
    this.data = {
      ...this.data,
      ...value
    }
    return this
  }

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

  async save(): Promise<DaycareBuilder> {
    await insertDaycareFixtures([this.data])
    Fixture.cleanupOperations.push(
      async () => await deleteDaycare(this.data.id)
    )
    return this
  }

  async delete(): Promise<DaycareBuilder> {
    await deleteDaycare(this.data.id)
    return this
  }

  // Note: shallow copy
  copy(): DaycareBuilder {
    return new DaycareBuilder({ ...this.data })
  }
}

export class DaycareGroupBuilder {
  data: DaycareGroup

  constructor(data: DaycareGroup) {
    this.data = data
  }

  with(value: Partial<DaycareGroup>): DaycareGroupBuilder {
    this.data = {
      ...this.data,
      ...value
    }
    return this
  }

  daycare(daycare: DaycareBuilder): DaycareGroupBuilder {
    this.data.daycareId = daycare.data.id
    return this
  }

  async save(): Promise<DaycareGroupBuilder> {
    await insertDaycareGroupFixtures([this.data])
    Fixture.cleanupOperations.push(async () => {
      await deleteDaycareGroup(this.data.id)
    })
    return this
  }

  async delete(): Promise<DaycareGroupBuilder> {
    await deleteDaycareGroup(this.data.id)
    return this
  }

  // Note: shallow copy
  copy(): DaycareGroupBuilder {
    return new DaycareGroupBuilder({ ...this.data })
  }
}

export class CareAreaBuilder {
  data: CareArea

  constructor(data: CareArea) {
    this.data = data
  }

  with(value: Partial<CareArea>): CareAreaBuilder {
    this.data = {
      ...this.data,
      ...value
    }
    return this
  }

  id(id: string): CareAreaBuilder {
    this.data.id = id
    return this
  }

  async save(): Promise<CareAreaBuilder> {
    await insertCareAreaFixtures([this.data])
    Fixture.cleanupOperations.push(async () => {
      await deleteCareAreaFixture(this.data.id)
    })
    return this
  }

  async delete(): Promise<CareAreaBuilder> {
    await deleteCareAreaFixture(this.data.id)
    return this
  }

  // Note: shallow copy
  copy(): CareAreaBuilder {
    return new CareAreaBuilder({ ...this.data })
  }
}

export class PersonBuilder {
  data: PersonDetailWithDependantsAndGuardians

  constructor(data: PersonDetailWithDependantsAndGuardians) {
    this.data = data
  }

  with(value: Partial<PersonDetailWithDependantsAndGuardians>): PersonBuilder {
    this.data = {
      ...this.data,
      ...value
    }
    return this
  }

  async save(): Promise<PersonBuilder> {
    await insertPersonFixture(this.data)
    Fixture.cleanupOperations.push(async () => {
      await deletePersonFixture(this.data.id)
    })
    return this
  }

  async saveAndUpdateMockVtj(): Promise<PersonBuilder> {
    await this.save()
    await insertVtjPersonFixture(this.data)
    Fixture.cleanupOperations.push(async () => {
      if (this.data.ssn) await deleteVtjPerson(this.data.ssn)
      if (this.data.dependants) {
        for (const dependant of this.data.dependants) {
          if (dependant.ssn) await deleteVtjPerson(dependant.ssn)
        }
      }
    })
    return this
  }

  async delete(): Promise<PersonBuilder> {
    if (this.data.ssn) await deleteVtjPerson(this.data.ssn)
    return this
  }

  // Note: shallow copy
  copy(): PersonBuilder {
    return new PersonBuilder({ ...this.data })
  }
}

export class EmployeeBuilder {
  data: EmployeeDetail

  constructor(data: EmployeeDetail) {
    this.data = data
  }

  with(value: Partial<EmployeeDetail>): EmployeeBuilder {
    this.data = {
      ...this.data,
      ...value
    }
    return this
  }

  async save(): Promise<EmployeeBuilder> {
    await insertEmployeeFixture(this.data)
    Fixture.cleanupOperations.push(async () => {
      await this.delete()
    })
    return this
  }

  async delete(): Promise<EmployeeBuilder> {
    //eslint-disable-next-line @typescript-eslint/no-non-null-assertion
    await deleteEmployeeById(this.data.id!)
    return this
  }

  // Note: shallow copy
  copy(): EmployeeBuilder {
    return new EmployeeBuilder({ ...this.data })
  }
}

export class DecisionBuilder {
  data: DecisionFixture

  constructor(data: DecisionFixture) {
    this.data = data
  }

  with(value: Partial<DecisionFixture>): DecisionBuilder {
    this.data = {
      ...this.data,
      ...value
    }
    return this
  }

  async save(): Promise<DecisionBuilder> {
    await insertDecisionFixtures([this.data])
    Fixture.cleanupOperations.push(async () => {
      await deleteDecisionFixture(this.data.id)
    })
    return this
  }

  async delete(): Promise<DecisionBuilder> {
    await deleteDecisionFixture(this.data.id)
    return this
  }

  // Note: shallow copy
  copy(): DecisionBuilder {
    return new DecisionBuilder({ ...this.data })
  }
}

export class EmployeePinBuilder {
  data: EmployeePin

  constructor(data: EmployeePin) {
    this.data = data
  }

  with(value: Partial<EmployeePin>): EmployeePinBuilder {
    this.data = {
      ...this.data,
      ...value
    }
    return this
  }

  async save(): Promise<EmployeePinBuilder> {
    await insertEmployeePins([this.data])
    Fixture.cleanupOperations.push(async () => {
      await deleteEmployeePin(this.data.id)
    })
    return this
  }

  async delete(): Promise<EmployeePinBuilder> {
    await deleteEmployeePin(this.data.id)
    return this
  }

  // Note: shallow copy
  copy(): EmployeePinBuilder {
    return new EmployeePinBuilder({ ...this.data })
  }
}
