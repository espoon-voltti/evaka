// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  DevCalendarEventTime,
  DevCareArea,
  DevDaycare
} from 'e2e-test/generated/api-types'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'

import {
  createDaycarePlacementFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { PersonDetail } from '../../dev-api/types'
import {
  createChildren,
  createDaycarePlacements,
  resetServiceState
} from '../../generated/api-clients'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import CitizenHeader, { EnvType } from '../../pages/citizen/citizen-header'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

const e: EnvType[] = ['desktop', 'mobile']

let page: Page
let header: CitizenHeader
let calendarPage: CitizenCalendarPage
let children: PersonDetail[]
const today = LocalDate.of(2022, 1, 3)

const groupEventId = uuidv4()
const unitEventId = uuidv4()
const individualEventId = uuidv4()
const reservationId = uuidv4()

export const enduserGuardianFixture: PersonDetail = {
  id: '87a5c962-9b3d-11ea-bb37-0242ac130002',
  ssn: '070644-937X',
  firstName: 'Johannes Olavi Antero Tapio',
  lastName: 'Karhula',
  email: 'johannes.karhula@evaka.test',
  phone: '123456789',
  language: 'fi',
  dateOfBirth: LocalDate.of(1944, 7, 7),
  streetAddress: 'Kamreerintie 1',
  postalCode: '00340',
  postOffice: 'Espoo',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
}

const enduserChildFixtureJari: PersonDetail = {
  id: '572adb7e-9b3d-11ea-bb37-0242ac130002',
  ssn: '070714A9126',
  firstName: 'Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani',
  lastName: 'Karhula',
  preferredName: 'Jari',
  email: '',
  phone: '',
  language: 'fi',
  dateOfBirth: LocalDate.of(2014, 7, 7),
  streetAddress: enduserGuardianFixture.streetAddress,
  postalCode: enduserGuardianFixture.postalCode,
  postOffice: enduserGuardianFixture.postOffice,
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
}

export const careAreaFixture: DevCareArea = {
  id: '674dfb66-8849-489e-b094-e6a0ebfb3c71',
  name: 'Superkeskus',
  shortName: 'super-keskus',
  areaCode: 299,
  subCostCenter: '99'
}

export const fullDayTimeRange: TimeRange = new TimeRange(
  LocalTime.MIN,
  LocalTime.parse('23:59')
)
export const daycareFixture: DevDaycare = {
  id: '4f3a32f5-d1bd-4b8b-aa4e-4fd78b18354b',
  areaId: careAreaFixture.id,
  name: 'Alkuräjähdyksen päiväkoti',
  type: ['CENTRE', 'PRESCHOOL', 'PREPARATORY_EDUCATION'],
  dailyPreschoolTime: new TimeRange(LocalTime.of(9, 0), LocalTime.of(13, 0)),
  dailyPreparatoryTime: new TimeRange(LocalTime.of(9, 0), LocalTime.of(14, 0)),
  costCenter: '31500',
  visitingAddress: {
    streetAddress: 'Kamreerintie 1',
    postalCode: '02210',
    postOffice: 'Espoo'
  },
  decisionCustomization: {
    daycareName: 'Päiväkoti päätöksellä',
    preschoolName: 'Päiväkoti päätöksellä',
    handler: 'Käsittelijä',
    handlerAddress: 'Käsittelijän osoite'
  },
  providerType: 'MUNICIPAL',
  operationTimes: [
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    null,
    null
  ],
  shiftCareOperationTimes: [
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange
  ],
  shiftCareOpenOnHolidays: true,
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
  ],
  businessId: '',
  iban: '',
  providerId: '',
  capacity: 0,
  openingDate: null,
  closingDate: null,
  ghostUnit: false,
  invoicedByMunicipality: true,
  uploadChildrenToVarda: true,
  uploadToVarda: true,
  uploadToKoski: true,
  language: 'fi',
  mailingAddress: {
    poBox: null,
    postOffice: null,
    postalCode: null,
    streetAddress: null
  },
  unitManager: {
    email: '',
    name: 'Unit Manager',
    phone: ''
  },
  financeDecisionHandler: null,
  clubApplyPeriod: null,
  daycareApplyPeriod: new DateRange(LocalDate.of(2020, 3, 1), null),
  preschoolApplyPeriod: new DateRange(LocalDate.of(2020, 3, 1), null),
  email: null,
  phone: null,
  url: null,
  ophUnitOid: '1.2.3.4.5',
  ophOrganizerOid: '1.2.3.4.5',
  additionalInfo: null,
  dwCostCenter: 'dw-test',
  mealtimeBreakfast: null,
  mealtimeLunch: null,
  mealtimeSnack: null,
  mealtimeSupper: null,
  mealtimeEveningSnack: null
}

let reservationData: DevCalendarEventTime

beforeEach(async () => {
  await resetServiceState()
  const careArea = await Fixture.careArea().with(careAreaFixture).save()
  await Fixture.daycare().with(daycareFixture).careArea(careArea).save()

  await Fixture.person().with(enduserChildFixtureJari).saveAndUpdateMockVtj()

  await Fixture.person()
    .with(enduserGuardianFixture)
    .withDependants(enduserChildFixtureJari)
    .saveAndUpdateMockVtj()

  children = [enduserChildFixtureJari]

  const cs = children.map(({ id }) => ({
    id,
    additionalInfo: '',
    allergies: '',
    diet: '',
    dietId: null,
    mealTextureId: null,
    languageAtHome: '',
    languageAtHomeDetails: '',
    medication: ''
  }))

  await createChildren({ body: cs })

  const placementIds = new Map(children.map((child) => [child.id, uuidv4()]))

  await createDaycarePlacements({
    body: children.map((child) =>
      createDaycarePlacementFixture(
        placementIds.get(child.id) ?? '',
        child.id,
        daycareFixture.id,
        today,
        today.addYears(1)
      )
    )
  })

  const daycareGroup = await Fixture.daycareGroup()
    .with({
      daycareId: daycareFixture.id,
      name: 'Group 1'
    })
    .save()

  for (const child of children) {
    await Fixture.groupPlacement()
      .with({
        startDate: today,
        endDate: today.addYears(1),
        daycareGroupId: daycareGroup.data.id,
        daycarePlacementId: placementIds.get(child.id) ?? ''
      })
      .save()
  }

  const { data: groupEvent } = await Fixture.calendarEvent()
    .with({
      id: groupEventId,
      title: 'Group-wide survey',
      description: 'Whole group',
      period: new FiniteDateRange(today.addDays(3), today.addDays(3)),
      modifiedAt: HelsinkiDateTime.fromLocal(today, LocalTime.MIN),
      eventType: 'DISCUSSION_SURVEY'
    })
    .save()

  await Fixture.calendarEventAttendee()
    .with({
      calendarEventId: groupEvent.id,
      unitId: daycareFixture.id,
      groupId: daycareGroup.data.id
    })
    .save()

  await Fixture.calendarEventTime()
    .with({
      calendarEventId: groupEvent.id,
      date: today.addDays(3),
      start: LocalTime.of(8, 0),
      end: LocalTime.of(8, 30),
      childId: null
    })
    .save()
  await Fixture.calendarEventTime()
    .with({
      calendarEventId: groupEvent.id,
      date: today.addDays(3),
      start: LocalTime.of(9, 0),
      end: LocalTime.of(9, 30),
      childId: null
    })
    .save()

  const { data: individualEvent } = await Fixture.calendarEvent()
    .with({
      id: individualEventId,
      title: 'Individual survey',
      description: 'Just Jari',
      period: new FiniteDateRange(today.addDays(3), today.addDays(3)),
      modifiedAt: HelsinkiDateTime.fromLocal(today, LocalTime.MIN),
      eventType: 'DISCUSSION_SURVEY'
    })
    .save()

  await Fixture.calendarEventAttendee()
    .with({
      calendarEventId: individualEvent.id,
      unitId: daycareFixture.id,
      groupId: daycareGroup.data.id,
      childId: enduserChildFixtureJari.id
    })
    .save()

  reservationData = (
    await Fixture.calendarEventTime()
      .with({
        id: reservationId,
        calendarEventId: individualEvent.id,
        date: today.addDays(3),
        start: LocalTime.of(12, 0),
        end: LocalTime.of(12, 30),
        childId: enduserChildFixtureJari.id
      })
      .save()
  ).data

  const { data: unitEvent } = await Fixture.calendarEvent()
    .with({
      id: unitEventId,
      title: 'Unit event',
      description: 'For everyone in the unit',
      period: new FiniteDateRange(today, today.addDays(4)),
      modifiedAt: HelsinkiDateTime.fromLocal(today, LocalTime.MIN)
    })
    .save()

  await Fixture.calendarEventAttendee()
    .with({
      calendarEventId: unitEvent.id,
      unitId: daycareFixture.id
    })
    .save()
})

describe.each(e)('Citizen calendar discussion surveys (%s)', (env) => {
  beforeEach(async () => {
    const viewport =
      env === 'mobile'
        ? { width: 375, height: 812 }
        : { width: 1920, height: 1080 }

    page = await Page.open({
      viewport,
      screen: viewport,
      mockedTime: today.toHelsinkiDateTime(LocalTime.of(12, 0))
    })
    await enduserLogin(page)
    header = new CitizenHeader(page, env)
    calendarPage = new CitizenCalendarPage(page, env)
    await header.selectTab('calendar')
  })

  test('Citizen sees correct amount of event counts', async () => {
    await calendarPage.assertEventCount(today, 1) // unit event (1 attendee)
    await calendarPage.assertEventCount(today.addDays(3), 2) // unit event + reservation for individual survey
    await calendarPage.assertEventCount(today.addDays(4), 1) // unit event
  })

  test('Citizen sees discussions toast message', async () => {
    await waitUntilEqual(
      () => calendarPage.getActiveDiscussionsCtaContent(),
      'Sinua on pyydetty varaamaan aika lastasi koskevaan keskusteluun.\nSiirry ajanvaraukseen'
    )
  })

  test('Day modals have correct events', async () => {
    await calendarPage.closeToasts()
    let dayView = await calendarPage.openDayView(today)

    for (const child of children) {
      await dayView.assertEvent(child.id, unitEventId, {
        title: 'Unit event / Alkuräjähdyksen päiväkoti',
        description: 'For everyone in the unit'
      })
    }

    await dayView.close()

    dayView = await calendarPage.openDayView(today.addDays(3))

    await dayView.assertEvent(enduserChildFixtureJari.id, unitEventId, {
      title: 'Unit event / Alkuräjähdyksen päiväkoti',
      description: 'For everyone in the unit'
    })
    await dayView.assertDiscussionReservation(
      enduserChildFixtureJari.id,
      individualEventId,
      reservationId,
      true,
      {
        title: 'Individual survey',
        description: 'Just Jari',
        reservationText: `klo ${reservationData.start.format()} - ${reservationData.end.format()}`
      }
    )

    await dayView.close()

    dayView = await calendarPage.openDayView(today.addDays(4))

    for (const child of children) {
      await dayView.assertEvent(child.id, unitEventId, {
        title: 'Unit event / Alkuräjähdyksen päiväkoti',
        description: 'For everyone in the unit'
      })
    }

    await dayView.close()
  })
})
