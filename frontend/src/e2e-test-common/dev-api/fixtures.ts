// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { format } from 'date-fns'
import config from '../config'
import {
  Application,
  ApplicationForm,
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
  PersonDetailWithDependantsAndGuardians,
  PlacementPlan,
  VoucherValueDecision,
  DaycareGroupPlacement,
  EmployeePin,
  PedagogicalDocument,
  ServiceNeedFixture
} from './types'
import {
  insertCareAreaFixtures,
  insertDaycareFixtures,
  insertDaycareGroupFixtures,
  insertDecisionFixtures,
  insertEmployeeFixture,
  insertEmployeePins,
  insertPedagogicalDocuments,
  insertPersonFixture,
  insertServiceNeedOptions,
  insertServiceNeeds,
  insertVtjPersonFixture
} from './index'
import LocalDate from 'lib-common/local-date'
import DateRange from 'lib-common/date-range'
import { ApplicationStatus } from 'lib-common/generated/enums'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import { UUID } from 'lib-common/types'

export const supervisor: EmployeeDetail = {
  id: '552e5bde-92fb-4807-a388-40016f85f593',
  externalId: config.supervisorExternalId,
  firstName: 'Eva',
  lastName: 'Esihenkilo',
  email: 'eva.esihenkilo@evaka.test',
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
  roundTheClock: true,
  location: {
    lat: 60.20377343765089,
    lon: 24.655715743526994
  },
  enabledPilotFeatures: ['MESSAGING', 'MOBILE', 'RESERVATIONS']
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
  },
  enabledPilotFeatures: ['MESSAGING', 'MOBILE']
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
  },
  enabledPilotFeatures: ['MESSAGING', 'MOBILE']
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
): ApplicationForm => {
  // Try to make sure there's an active preschool term for the preferred start date
  let startDate = LocalDate.today()
  if (
    type === 'PRESCHOOL' &&
    // May-Aug
    [5, 6, 7, 8].includes(startDate.month)
  ) {
    // => Sep
    startDate = startDate.withMonth(9)
  }

  // Move daycare application start date to August if current date is in July
  if (type === 'DAYCARE' && 7 === startDate.month) {
    startDate = startDate.withMonth(8)
  }

  return {
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
    preferredStartDate: startDate.formatIso(),
    serviceEnd: '08:00',
    serviceStart: '16:00',
    urgent: false,
    otherGuardianAgreementStatus
  }
}

export const applicationFixtureId = '9dd0e1ba-9b3b-11ea-bb37-0242ac130002'
export const applicationFixture = (
  child: PersonDetail,
  guardian: PersonDetail,
  otherGuardian: PersonDetail | undefined = undefined,
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
  headOfFamily: { id: adult.id },
  familySize: 2,
  feeThresholds: feeThresholds,
  children: [
    {
      child: { id: child.id, dateOfBirth: child.dateOfBirth },
      placement: {
        unit: { id: daycareId },
        type: 'DAYCARE'
      },
      serviceNeed: {
        feeCoefficient: 1.0,
        descriptionFi: 'palveluntarve',
        descriptionSv: 'vårdbehövet',
        missing: false
      },
      baseFee: 28900,
      fee: 28900,
      siblingDiscount: 0.0,
      feeAlterations: [],
      finalFee: 28900
    }
  ],
  sentAt
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
  decisionType: 'NORMAL',
  familySize: 2,
  feeThresholds: feeThresholds,
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
  capacityFactor: 1.0,
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
  id: uuidv4(),
  status,
  headOfFamily: { id: adultId },
  agreementType: 200,
  periodStart,
  periodEnd,
  rows: [
    {
      id: uuidv4(),
      child: { id: childId, dateOfBirth: '2017-06-30' },
      placementUnit: { id: daycareId },
      amount: 1,
      unitPrice: 10000,
      periodStart: periodStart,
      periodEnd: periodEnd,
      product: 'DAYCARE',
      costCenter: '20000',
      subCostCenter: '00',
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

export function createDaycareGroupPlacementFixture(
  placementId: string,
  groupId: string,
  startDate = '2021-05-01',
  endDate = '2022-08-31'
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
      start: '2022-02-01',
      end: '2022-02-03'
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

  static pedagogicalDocument(): PedagogicalDocumentBuilder {
    return new PedagogicalDocumentBuilder({
      id: uuidv4(),
      childId: 'not set',
      description: 'not set'
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
      defaultOption: false,
      feeCoefficient: 0.0,
      feeDescriptionFi: `Test service need option ${id}`,
      feeDescriptionSv: `Test service need option ${id}`,
      nameFi: `test_service_need_option_${id}`,
      nameSv: `test_service_need_option_${id}`,
      nameEn: `test_service_need_option_${id}`,
      occupancyCoefficient: 0,
      partDay: false,
      partWeek: false,
      updated: new Date(),
      validPlacementType: 'DAYCARE',
      voucherValueCoefficient: 0,
      voucherValueDescriptionFi: `Test service need option ${id}`,
      voucherValueDescriptionSv: `Test service need option ${id}`
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
    return this
  }

  async saveAndUpdateMockVtj(): Promise<PersonBuilder> {
    await this.save()
    await insertVtjPersonFixture(this.data)
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
    return this
  }

  // Note: shallow copy
  copy(): EmployeePinBuilder {
    return new EmployeePinBuilder({ ...this.data })
  }
}

export class PedagogicalDocumentBuilder {
  data: PedagogicalDocument

  constructor(data: PedagogicalDocument) {
    this.data = data
  }

  with(value: Partial<PedagogicalDocument>): PedagogicalDocumentBuilder {
    this.data = {
      ...this.data,
      ...value
    }
    return this
  }

  async save(): Promise<PedagogicalDocumentBuilder> {
    await insertPedagogicalDocuments([this.data])
    return this
  }

  // Note: shallow copy
  copy(): PedagogicalDocumentBuilder {
    return new PedagogicalDocumentBuilder({ ...this.data })
  }
}

export class ServiceNeedOptionBuilder {
  data: ServiceNeedOption

  constructor(data: ServiceNeedOption) {
    this.data = data
  }

  with(value: Partial<ServiceNeedOption>): ServiceNeedOptionBuilder {
    this.data = {
      ...this.data,
      ...value
    }
    return this
  }

  async save(): Promise<ServiceNeedOptionBuilder> {
    await insertServiceNeedOptions([this.data])
    return this
  }

  // Note: shallow copy
  copy(): ServiceNeedOptionBuilder {
    return new ServiceNeedOptionBuilder({ ...this.data })
  }
}

export class ServiceNeedBuilder {
  data: ServiceNeedFixture

  constructor(data: ServiceNeedFixture) {
    this.data = data
  }

  with(value: Partial<ServiceNeedFixture>): ServiceNeedBuilder {
    this.data = {
      ...this.data,
      ...value
    }
    return this
  }

  async save(): Promise<ServiceNeedBuilder> {
    await insertServiceNeeds([this.data])
    return this
  }

  // Note: shallow copy
  copy(): ServiceNeedBuilder {
    return new ServiceNeedBuilder({ ...this.data })
  }
}
