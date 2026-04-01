// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { evakaUserId } from 'lib-common/id-type'
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
  preschoolTerm2021
} from '../../dev-api/fixtures'
import {
  createDefaultServiceNeedOptions,
  resetServiceState
} from '../../generated/api-clients'
import type { DevPerson } from '../../generated/api-types'
import ConfirmedDayReservationPage from '../../pages/mobile/child-confimed-reservations-page'
import MobileListPage from '../../pages/mobile/list-page'
import { test, expect } from '../../playwright'
import { pairMobileDevice } from '../../utils/mobile'
import type { Page } from '../../utils/page'

const now = HelsinkiDateTime.of(2022, 5, 17, 13, 0, 0)

let shiftCareChild1: DevPerson
let shiftCareChild2: DevPerson

const group2 = Fixture.daycareGroup({
  name: '#2',
  daycareId: testDaycare.id,
  startDate: LocalDate.of(2021, 1, 1)
})

test.use({ evakaOptions: { mockedTime: now } })

test.describe('Child confirmed reservations', () => {
  let page: Page
  let confirmedReservationPage: ConfirmedDayReservationPage
  let attendanceListPage: MobileListPage

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    await createDefaultServiceNeedOptions()
    await insertConfirmedDaysTestData()

    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    page = evaka

    await page.goto(mobileSignupUrl)

    attendanceListPage = new MobileListPage(page)
    await attendanceListPage.confirmedDaysTab.click()

    confirmedReservationPage = new ConfirmedDayReservationPage(page)
  })

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
        presentCount: '4',
        presentCalc: '3,75',
        absentCount: '2'
      },
      {
        date: LocalDate.of(2022, 5, 19),
        presentCount: '4',
        presentCalc: '2,75',
        absentCount: '2'
      },
      {
        date: LocalDate.of(2022, 5, 20),
        presentCount: '5',
        presentCalc: '3,75',
        absentCount: '1'
      },
      {
        date: LocalDate.of(2022, 5, 23),
        presentCount: '6',
        presentCalc: '4,25',
        absentCount: '0'
      },
      {
        date: LocalDate.of(2022, 5, 24),
        presentCount: '5',
        presentCalc: '3,25',
        absentCount: '1'
      },
      {
        date: LocalDate.of(2022, 5, 25),
        presentCount: '6',
        presentCalc: '4,25',
        absentCount: '0'
      },
      {
        date: LocalDate.of(2022, 5, 27),
        presentCount: '6',
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
      },
      {
        date: testDay,
        childId: shiftCareChild1.id,
        reservationTexts: ['08:00–14:00'],
        childDetails: {
          firstName: shiftCareChild1.firstName,
          lastName: shiftCareChild1.lastName,
          preferredName: shiftCareChild1.preferredName
        }
      },
      {
        date: testDay,
        childId: shiftCareChild2.id,
        reservationTexts: ['Ilmoitus puuttuu'],
        childDetails: {
          firstName: shiftCareChild2.firstName.split(/\s/)[0],
          lastName: shiftCareChild2.lastName,
          preferredName: shiftCareChild2.preferredName
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

  test('Shift care view shows only shift care children', async () => {
    await attendanceListPage.selectGroup('shift-care')
    await attendanceListPage.confirmedDaysTab.click()
    const testDay = LocalDate.of(2022, 5, 19)
    await confirmedReservationPage.openDayItem(testDay)

    await expect(
      confirmedReservationPage.childItem(testDay, shiftCareChild1.id)
    ).toBeVisible()
    await expect(
      confirmedReservationPage.childItem(testDay, shiftCareChild2.id)
    ).toBeVisible()

    await expect(
      confirmedReservationPage.childItem(testDay, testChild.id)
    ).toBeHidden()
    await expect(
      confirmedReservationPage.childItem(testDay, testChild2.id)
    ).toBeHidden()
    await expect(
      confirmedReservationPage.childItem(testDay, testChildRestricted.id)
    ).toBeHidden()
    await expect(
      confirmedReservationPage.childItem(testDay, testChildNoSsn.id)
    ).toBeHidden()
  })

  test('Shift care daily counts are correct', async () => {
    await attendanceListPage.selectGroup('shift-care')
    await attendanceListPage.confirmedDaysTab.click()

    // 2022-05-18: shiftCareChild1 has reservation (present), shiftCareChild2 has absence
    await confirmedReservationPage.assertDailyCounts(
      LocalDate.of(2022, 5, 18),
      '1',
      '0',
      '1'
    )

    // 2022-05-19: both present (shiftCareChild1 has reservation, shiftCareChild2 no reservation)
    await confirmedReservationPage.assertDailyCounts(
      LocalDate.of(2022, 5, 19),
      '2',
      '0',
      '0'
    )
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
      },
      {
        date: testDay,
        childId: shiftCareChild1.id,
        reservationTexts: ['08:00–14:00'],
        childDetails: {
          firstName: shiftCareChild1.firstName,
          lastName: shiftCareChild1.lastName,
          preferredName: shiftCareChild1.preferredName
        }
      },
      {
        date: testDay,
        childId: shiftCareChild2.id,
        reservationTexts: ['Lomavaraus puuttuu'],
        childDetails: {
          firstName: shiftCareChild2.firstName.split(/\s/)[0],
          lastName: shiftCareChild2.lastName,
          preferredName: shiftCareChild2.preferredName
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
  childId: PersonId,
  groupId = testDaycareGroup.id,
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

  const careArea = await testCareArea.save()
  await Fixture.daycare({
    ...testDaycare,
    areaId: careArea.id,
    shiftCareOperationTimes: null,
    shiftCareOpenOnHolidays: false
  }).save()
  await Fixture.daycare({ ...testDaycare2, areaId: careArea.id }).save()
  await testDaycareGroup.save()
  await group2.save()

  await testChild2.saveChild()
  await testChild.saveChild()
  await Fixture.person(familyWithTwoGuardians.children[0]).saveChild()
  await Fixture.person({
    ...testChildRestricted,
    dateOfBirth: LocalDate.of(2021, 4, 1)
  }).saveChild()
  await testChildNoSsn.saveChild()

  const admin = await Fixture.employee({ roles: ['ADMIN'] }).save()

  await createPlacements(testChildRestricted.id, testDaycareGroup.id)
  await createPlacements(testChild.id, testDaycareGroup.id)
  await createPlacements(testChild2.id, testDaycareGroup.id)
  await createPlacements(testChildNoSsn.id, testDaycareGroup.id, 'PRESCHOOL')

  shiftCareChild1 = await Fixture.person({
    firstName: 'Vuoro',
    lastName: 'Lapsi',
    ssn: null
  }).saveChild()
  shiftCareChild2 = await Fixture.person({
    firstName: 'Toinen Vuoro',
    lastName: 'Lapsi',
    ssn: null
  }).saveChild()

  const scPlacement1 = await createPlacements(
    shiftCareChild1.id,
    testDaycareGroup.id
  )
  const scPlacement2 = await createPlacements(
    shiftCareChild2.id,
    testDaycareGroup.id
  )

  const serviceNeedOption = await Fixture.serviceNeedOption().save()
  await Fixture.serviceNeed({
    placementId: scPlacement1.id,
    startDate: scPlacement1.startDate,
    endDate: scPlacement1.endDate,
    shiftCare: 'FULL',
    optionId: serviceNeedOption.id,
    confirmedBy: evakaUserId(admin.id)
  }).save()
  await Fixture.serviceNeed({
    placementId: scPlacement2.id,
    startDate: scPlacement2.startDate,
    endDate: scPlacement2.endDate,
    shiftCare: 'INTERMITTENT',
    optionId: serviceNeedOption.id,
    confirmedBy: evakaUserId(admin.id)
  }).save()

  await Fixture.attendanceReservation({
    type: 'RESERVATIONS',
    childId: shiftCareChild1.id,
    date: LocalDate.of(2022, 5, 18),
    reservation: new TimeRange(LocalTime.of(8, 0), LocalTime.of(14, 0)),
    secondReservation: null
  }).save()
  await Fixture.attendanceReservation({
    type: 'RESERVATIONS',
    childId: shiftCareChild1.id,
    date: LocalDate.of(2022, 5, 19),
    reservation: new TimeRange(LocalTime.of(8, 0), LocalTime.of(14, 0)),
    secondReservation: null
  }).save()
  await Fixture.absence({
    childId: shiftCareChild2.id,
    date: LocalDate.of(2022, 5, 18)
  }).save()

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
