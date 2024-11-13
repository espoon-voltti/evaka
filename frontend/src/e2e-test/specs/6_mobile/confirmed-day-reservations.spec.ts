// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'

import {
  testCareArea,
  testDaycare2,
  testDaycare,
  testDaycareGroup,
  testChild,
  testChild2,
  testChildRestricted,
  testChildNoSsn,
  familyWithTwoGuardians,
  Fixture,
  preschoolTerm2021,
  uuidv4
} from '../../dev-api/fixtures'
import {
  createDefaultServiceNeedOptions,
  resetServiceState
} from '../../generated/api-clients'
import ConfirmedDayReservationPage from '../../pages/mobile/child-confimed-reservations-page'
import MobileListPage from '../../pages/mobile/list-page'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let confirmedReservationPage: ConfirmedDayReservationPage
let attendanceListPage: MobileListPage
const now = HelsinkiDateTime.of(2022, 5, 17, 13, 0, 0)

const group2 = {
  id: uuidv4(),
  name: '#2',
  daycareId: testDaycare.id,
  startDate: LocalDate.of(2021, 1, 1)
}

beforeEach(async () => {
  await resetServiceState()
  await createDefaultServiceNeedOptions()
  await insertConfirmedDaysTestData()

  const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
  page = await Page.open({ mockedTime: now })

  await page.goto(mobileSignupUrl)

  attendanceListPage = new MobileListPage(page)
  await attendanceListPage.confirmedDaysTab.click()

  confirmedReservationPage = new ConfirmedDayReservationPage(page)
})

describe('Child confirmed reservations', () => {
  test('Confirmed days are present', async () => {
    const confirmedDaysOnTestDate = [
      LocalDate.of(2022, 5, 18),
      LocalDate.of(2022, 5, 19),
      LocalDate.of(2022, 5, 20),
      LocalDate.of(2022, 5, 23),
      LocalDate.of(2022, 5, 24),
      LocalDate.of(2022, 5, 25),
      // 2022-05-26: Helatorstai
      LocalDate.of(2022, 5, 27)
    ]

    for (const day of confirmedDaysOnTestDate) {
      await confirmedReservationPage.assertDayExists(day)
    }

    const nonConfirmedDaysOnTestDate = [
      LocalDate.of(2022, 5, 17),
      LocalDate.of(2022, 5, 26),
      LocalDate.of(2022, 5, 28)
    ]

    for (const day of nonConfirmedDaysOnTestDate) {
      await confirmedReservationPage.assertDayDoesNotExist(day)
    }
  })

  test('Daily counts are correct', async () => {
    const dayCounts = [
      {
        date: LocalDate.of(2022, 5, 18),
        presentCount: '3',
        presentCalc: '3,75',
        absentCount: '1'
      },
      {
        date: LocalDate.of(2022, 5, 19),
        presentCount: '2',
        presentCalc: '2,75',
        absentCount: '2'
      },
      {
        date: LocalDate.of(2022, 5, 20),
        presentCount: '3',
        presentCalc: '3,75',
        absentCount: '1'
      },
      {
        date: LocalDate.of(2022, 5, 23),
        presentCount: '4',
        presentCalc: '4,25',
        absentCount: '0'
      },
      {
        date: LocalDate.of(2022, 5, 24),
        presentCount: '3',
        presentCalc: '3,25',
        absentCount: '1'
      },
      {
        date: LocalDate.of(2022, 5, 25),
        presentCount: '4',
        presentCalc: '4,25',
        absentCount: '0'
      },
      {
        date: LocalDate.of(2022, 5, 27),
        presentCount: '4',
        presentCalc: '4,75',
        absentCount: '0'
      }
    ]
    for (const day of dayCounts) {
      await confirmedReservationPage.assertDailyCounts(
        day.date,
        day.presentCount,
        day.presentCalc,
        day.absentCount
      )
    }
  })
  test('Daily children are correct (Thursday)', async () => {
    const testDay = LocalDate.of(2022, 5, 19)
    await confirmedReservationPage.openDayItem(testDay)
    const expectedChildItems = [
      {
        date: testDay,
        childId: testChildNoSsn.id,
        reservationTexts: ['Ei toimintaa'],
        childDetails: {
          firstName: testChildNoSsn.firstName.split(/\s/)[0],
          lastName: testChildNoSsn.lastName,
          preferredName: testChildNoSsn.preferredName
        }
      },
      {
        date: testDay,
        childId: testChild2.id,
        reservationTexts: ['Poissa'],
        childDetails: {
          firstName: testChild2.firstName.split(/\s/)[0],
          lastName: testChild2.lastName,
          preferredName: testChild2.preferredName
        }
      },
      {
        date: testDay,
        childId: testChild.id,
        reservationTexts: ['08:12–13:45', '14:30–16:45'],
        childDetails: {
          firstName: testChild.firstName.split(/\s/)[0],
          lastName: testChild.lastName,
          preferredName: testChild.preferredName
        }
      },
      {
        date: testDay,
        childId: testChildRestricted.id,
        reservationTexts: ['Ilmoitus puuttuu'],
        childDetails: {
          firstName: testChildRestricted.firstName.split(/\s/)[0],
          lastName: testChildRestricted.lastName,
          preferredName: testChildRestricted.preferredName
        }
      }
    ]

    for (const childItem of expectedChildItems) {
      await confirmedReservationPage.assertChildDetails(
        childItem.date,
        childItem.childId,
        childItem.reservationTexts,
        childItem.childDetails
      )
    }
  })

  test('Holiday period daily children are correct (Thursday)', async () => {
    await insertTestHolidayPeriod(
      new FiniteDateRange(LocalDate.of(2022, 5, 19), LocalDate.of(2022, 5, 20))
    )
    const testDay = LocalDate.of(2022, 5, 19)
    await confirmedReservationPage.openDayItem(testDay)
    const expectedChildItems = [
      {
        date: testDay,
        childId: testChildNoSsn.id,
        reservationTexts: ['Ei toimintaa'],
        childDetails: {
          firstName: testChildNoSsn.firstName.split(/\s/)[0],
          lastName: testChildNoSsn.lastName,
          preferredName: testChildNoSsn.preferredName
        }
      },
      {
        date: testDay,
        childId: testChild2.id,
        reservationTexts: ['Poissa'],
        childDetails: {
          firstName: testChild2.firstName.split(/\s/)[0],
          lastName: testChild2.lastName,
          preferredName: testChild2.preferredName
        }
      },
      {
        date: testDay,
        childId: testChild.id,
        reservationTexts: ['08:12–13:45', '14:30–16:45'],
        childDetails: {
          firstName: testChild.firstName.split(/\s/)[0],
          lastName: testChild.lastName,
          preferredName: testChild.preferredName
        }
      },
      {
        date: testDay,
        childId: testChildRestricted.id,
        reservationTexts: ['Lomavaraus puuttuu'],
        childDetails: {
          firstName: testChildRestricted.firstName.split(/\s/)[0],
          lastName: testChildRestricted.lastName,
          preferredName: testChildRestricted.preferredName
        }
      }
    ]

    for (const childItem of expectedChildItems) {
      await confirmedReservationPage.assertChildDetails(
        childItem.date,
        childItem.childId,
        childItem.reservationTexts,
        childItem.childDetails
      )
    }
  })
})

async function createPlacements(
  childId: string,
  groupId: string = testDaycareGroup.id,
  placementType: PlacementType = 'DAYCARE'
) {
  const daycarePlacementFixture = await Fixture.placement({
    childId,
    unitId: testDaycare.id,
    type: placementType,
    startDate: LocalDate.of(2021, 5, 1),
    endDate: LocalDate.of(2022, 8, 31)
  }).save()
  await Fixture.groupPlacement({
    daycarePlacementId: daycarePlacementFixture.id,
    daycareGroupId: groupId,
    startDate: daycarePlacementFixture.startDate,
    endDate: daycarePlacementFixture.endDate
  }).save()
  return daycarePlacementFixture
}

async function insertConfirmedDaysTestData() {
  await Fixture.preschoolTerm({
    ...preschoolTerm2021,
    termBreaks: [
      new FiniteDateRange(LocalDate.of(2022, 5, 18), LocalDate.of(2022, 5, 20))
    ]
  }).save()

  const careArea = await Fixture.careArea(testCareArea).save()
  await Fixture.daycare({
    ...testDaycare,
    areaId: careArea.id,
    shiftCareOperationTimes: null,
    shiftCareOpenOnHolidays: false
  }).save()
  await Fixture.daycare({ ...testDaycare2, areaId: careArea.id }).save()
  await Fixture.daycareGroup(testDaycareGroup).save()
  await Fixture.daycareGroup(group2).save()

  await Fixture.person(testChild2).saveChild()
  await Fixture.person(testChild).saveChild()
  await Fixture.person(familyWithTwoGuardians.children[0]).saveChild()
  await Fixture.person({
    ...testChildRestricted,
    dateOfBirth: LocalDate.of(2021, 4, 1)
  }).saveChild()
  await Fixture.person(testChildNoSsn).saveChild()

  await Fixture.employee({ roles: ['ADMIN'] }).save()

  await createPlacements(testChildRestricted.id, testDaycareGroup.id)
  await createPlacements(testChild.id, testDaycareGroup.id)
  await createPlacements(testChild2.id, testDaycareGroup.id)
  await createPlacements(testChildNoSsn.id, testDaycareGroup.id, 'PRESCHOOL')

  await Fixture.absence({
    childId: testChild2.id,
    date: LocalDate.of(2022, 5, 19)
  }).save()
  await Fixture.assistanceFactor({
    childId: testChild2.id,
    capacityFactor: 1.5,
    validDuring: new FiniteDateRange(
      LocalDate.of(2022, 5, 27),
      LocalDate.of(2022, 5, 27)
    )
  }).save()

  await Fixture.backupCare({
    childId: testChild.id,
    unitId: testDaycare2.id,
    period: new FiniteDateRange(
      LocalDate.of(2022, 5, 24),
      LocalDate.of(2022, 5, 24)
    ),
    groupId: null
  }).save()

  await Fixture.backupCare({
    childId: testChild.id,
    unitId: testDaycare.id,
    period: new FiniteDateRange(
      LocalDate.of(2022, 5, 27),
      LocalDate.of(2022, 5, 27)
    ),
    groupId: group2.id
  }).save()

  await Fixture.attendanceReservation({
    type: 'RESERVATIONS',
    childId: testChild.id,
    date: LocalDate.of(2022, 5, 19),
    reservation: new TimeRange(LocalTime.of(8, 12), LocalTime.of(13, 45)),
    secondReservation: new TimeRange(LocalTime.of(14, 30), LocalTime.of(16, 45))
  }).save()
}

async function insertTestHolidayPeriod(period: FiniteDateRange) {
  await Fixture.holidayPeriod({
    period: period,
    reservationDeadline: LocalDate.of(2022, 4, 20)
  }).save()
}
