// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { BrowserContextOptions } from 'playwright'

import DateRange from 'lib-common/date-range'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type { PersonId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { evakaUserId, randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import TimeRange from 'lib-common/time-range'

import type { EvakaBrowserContextOptions } from '../../browser'
import { mobileViewport } from '../../browser'
import {
  testCareArea,
  testDaycare2,
  testDaycare,
  testDaycareGroup,
  testChild,
  testChild2,
  testChildRestricted,
  familyWithTwoGuardians,
  Fixture,
  preschoolTerm2023
} from '../../dev-api/fixtures'
import {
  createDefaultServiceNeedOptions,
  createFosterParent,
  resetServiceState
} from '../../generated/api-clients'
import type { DevPlacement } from '../../generated/api-types'
import ChildAttendancePage from '../../pages/mobile/child-attendance-page'
import MobileChildPage from '../../pages/mobile/child-page'
import MobileListPage from '../../pages/mobile/list-page'
import MobileNotePage from '../../pages/mobile/note-page'
import { waitUntilEqual } from '../../utils'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let listPage: MobileListPage
let childPage: MobileChildPage
let childAttendancePage: ChildAttendancePage

const now = HelsinkiDateTime.of(2024, 5, 17, 13, 0, 0)

const group2 = Fixture.daycareGroup({
  name: '#2',
  daycareId: testDaycare.id,
  startDate: LocalDate.of(2021, 1, 1)
})

const careArea = testCareArea

const openPage = async (
  options?: BrowserContextOptions & EvakaBrowserContextOptions
) => {
  page = await Page.open({
    viewport: mobileViewport,
    ...(options ?? { mockedTime: now })
  })
  listPage = new MobileListPage(page)
  childPage = new MobileChildPage(page)
  childAttendancePage = new ChildAttendancePage(page)
}

beforeEach(async () => {
  await resetServiceState()
  await createDefaultServiceNeedOptions()
  await preschoolTerm2023.save()

  await careArea.save()
  await Fixture.daycare({ ...testDaycare, areaId: careArea.id }).save()
  await testDaycareGroup.save()
  await group2.save()

  await testChild2.saveChild()
  await testChild.saveChild()
  await familyWithTwoGuardians.save()
  await testChildRestricted.saveChild()

  await Fixture.employee({ roles: ['ADMIN'] }).save()
})

async function createPlacements(
  childId: PersonId,
  groupId = testDaycareGroup.id,
  placementType: PlacementType = 'DAYCARE',
  today: LocalDate = now.toLocalDate()
) {
  const daycarePlacementFixture = await Fixture.placement({
    childId,
    unitId: testDaycare.id,
    type: placementType,
    startDate: today.subMonths(3),
    endDate: today.addMonths(3)
  }).save()
  await Fixture.groupPlacement({
    daycarePlacementId: daycarePlacementFixture.id,
    daycareGroupId: groupId,
    startDate: daycarePlacementFixture.startDate,
    endDate: daycarePlacementFixture.endDate
  }).save()
  return daycarePlacementFixture
}

const createPlacementAndReload = async (
  placementType: PlacementType
): Promise<DevPlacement> => {
  const daycarePlacementFixture = await createPlacements(
    familyWithTwoGuardians.children[0].id,
    testDaycareGroup.id,
    placementType
  )

  const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
  await page.goto(mobileSignupUrl)

  return daycarePlacementFixture
}

const markChildArrivedAndDeparted = async (
  arrivalTime = '08:15',
  departureTime = '16:00',
  nonBillableAbsenceExpected: boolean,
  billableAbsenceExpected: boolean
) => {
  await listPage.selectChild(familyWithTwoGuardians.children[0].id)
  await childPage.markPresentLink.click()
  await childAttendancePage.setTime(arrivalTime)
  await childAttendancePage.selectMarkPresent()
  await childAttendancePage.selectPresentTab()
  await childAttendancePage.selectChildLink(0)
  await childAttendancePage.selectMarkDepartedLink()

  await childAttendancePage.setTime(departureTime)

  if (nonBillableAbsenceExpected) {
    await childAttendancePage
      .markAbsentByCategoryAndTypeButton('NONBILLABLE', 'OTHER_ABSENCE')
      .click()
  }
  if (billableAbsenceExpected) {
    await childAttendancePage
      .markAbsentByCategoryAndTypeButton('BILLABLE', 'OTHER_ABSENCE')
      .click()
  }
  await childAttendancePage.markDepartedButton.click()

  await childAttendancePage.assertNoChildrenPresentIndicatorIsShown()
}

describe('Child mobile attendances', () => {
  test('Child a full day in daycare placement is not required to mark absence types', async () => {
    await openPage()
    await createPlacementAndReload('DAYCARE')
    await markChildArrivedAndDeparted('08:00', '16:00', false, false)
  })

  test('Child a part day in daycare placement is not required to mark absence types', async () => {
    await openPage()
    await createPlacementAndReload('DAYCARE_PART_TIME')
    await markChildArrivedAndDeparted('08:00', '11:00', false, false)
  })

  test('Child a full day in preschool placement is not required to mark absence types', async () => {
    await openPage()
    await createPlacementAndReload('PRESCHOOL')
    await markChildArrivedAndDeparted('08:50', '12:58', false, false)
  })

  test('Child in preschool placement for less than 1 hour is required to mark absence type', async () => {
    await openPage()
    await createPlacementAndReload('PRESCHOOL')
    await markChildArrivedAndDeparted('12:10', '13:15', true, false)
  })

  test('Child in preschool daycare placement is not required to mark absence types', async () => {
    await openPage()
    await createPlacementAndReload('PRESCHOOL_DAYCARE')
    await markChildArrivedAndDeparted('08:00', '16:00', false, false)
  })

  test('Child in preschool daycare placement for preschool only is required to mark billable absence type', async () => {
    await openPage()
    await createPlacementAndReload('PRESCHOOL_DAYCARE')
    await markChildArrivedAndDeparted('08:55', '13:05', false, true)
  })

  test('Child in preschool daycare placement late for preschool is required to mark nonbillable absence type', async () => {
    await openPage()
    await createPlacementAndReload('PRESCHOOL_DAYCARE')
    await markChildArrivedAndDeparted('12:35', '16:05', true, false)
  })

  test('Child in preschool daycare placement for very short time is required to mark absence types', async () => {
    await openPage()
    await createPlacementAndReload('PRESCHOOL_DAYCARE')
    await markChildArrivedAndDeparted('08:55', '09:05', true, true)
  })

  test('noAbsenceType feature flag allows marking no absence despite expected absence', async () => {
    await openPage({
      mockedTime: now,
      employeeCustomizations: { featureFlags: { noAbsenceType: true } }
    })

    await createPlacementAndReload('PRESCHOOL_DAYCARE')
    await listPage.selectChild(familyWithTwoGuardians.children[0].id)
    await childPage.markPresentLink.click()
    await childAttendancePage.setTime('07:00')
    await childAttendancePage.selectMarkPresent()
    await childAttendancePage.selectPresentTab()
    await childAttendancePage.selectChildLink(0)
    await childAttendancePage.selectMarkDepartedLink()
    await childAttendancePage.setTime('08:55')

    await childAttendancePage
      .markAbsentByCategoryAndTypeButton('NONBILLABLE', 'NO_ABSENCE')
      .click()
    await childAttendancePage.markDepartedButton.click()
    await childAttendancePage.assertNoChildrenPresentIndicatorIsShown()
  })
})

const assertAttendanceCounts = async (
  coming: number,
  present: number,
  departed: number,
  absent: number,
  total: number
) =>
  await waitUntilEqual(() => listPage.getAttendanceCounts(), {
    coming,
    present,
    departed,
    absent,
    total
  })

describe('Child mobile attendance list', () => {
  test('Child can be marked as present and as departed', async () => {
    await openPage()
    const child1 = testChild2.id
    await createPlacements(child1)
    await createPlacements(testChildRestricted.id)
    await createPlacements(testChild.id, group2.id)

    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    await page.goto(mobileSignupUrl)
    await assertAttendanceCounts(3, 0, 0, 0, 3)
    await listPage.comingChildrenTab.click()
    await listPage.selectChild(child1)
    await childPage.markPresentLink.click()
    await childAttendancePage.setTime('08:00')
    await childAttendancePage.selectMarkPresent()

    await assertAttendanceCounts(2, 1, 0, 0, 3)

    await listPage.presentChildrenTab.click()
    await listPage.selectChild(child1)
    await childPage.markDepartedLink.click()
    await childAttendancePage.setTime('14:00')
    await childAttendancePage.markDepartedButton.click()

    await assertAttendanceCounts(2, 0, 1, 0, 3)

    await listPage.departedChildrenTab.click()
    await listPage.assertChildExists(child1)
  })

  test('Multiple children can be marked as present and departed', async () => {
    await openPage()
    await createPlacements(testChild.id)
    await createPlacements(testChild2.id, group2.id)
    await createPlacements(testChildRestricted.id)

    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    await page.goto(mobileSignupUrl)
    await assertAttendanceCounts(3, 0, 0, 0, 3)
    await listPage.comingChildrenTab.click()

    await listPage.multiselectToggle.check()
    await listPage.selectChild(testChild.id)
    await listPage.selectChild(testChild2.id)
    await listPage.markMultipleArrivedButton.click()
    await childAttendancePage.setTime('08:00')
    await childAttendancePage.selectMarkPresent()

    await assertAttendanceCounts(1, 2, 0, 0, 3)
    await listPage.presentChildrenTab.click()
    await listPage.assertChildExists(testChild.id)
    await listPage.assertChildExists(testChild2.id)

    await listPage.multiselectToggle.check()
    await listPage.selectChild(testChild.id)
    await listPage.selectChild(testChild2.id)
    await listPage.markMultipleDepartedutton.click()
    await childAttendancePage.setTime('14:00')
    await childAttendancePage.markDepartedButton.click()

    await assertAttendanceCounts(1, 0, 2, 0, 3)
    await listPage.departedChildrenTab.click()
    await listPage.assertChildExists(testChild.id)
    await listPage.assertChildExists(testChild2.id)
  })

  test('Coming tab sorting works', async () => {
    await createPlacements(testChild.id)
    await createPlacements(testChild2.id, group2.id)
    await createPlacements(testChildRestricted.id)
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: testChild.id,
      date: now.toLocalDate(),
      reservation: new TimeRange(
        now.subHours(3).toLocalTime(),
        now.addHours(3).toLocalTime()
      ),
      secondReservation: null
    }).save()
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: testChild2.id,
      date: now.toLocalDate(),
      reservation: new TimeRange(
        now.subHours(5).toLocalTime(),
        now.addHours(3).toLocalTime()
      ),
      secondReservation: null
    }).save()
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: testChildRestricted.id,
      date: now.toLocalDate(),
      reservation: new TimeRange(
        now.subHours(4).toLocalTime(),
        now.addHours(3).toLocalTime()
      ),
      secondReservation: null
    }).save()
    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)

    await openPage()
    await page.goto(mobileSignupUrl)
    await listPage.comingChildrenTab.click()
    await listPage.assertChildNames([
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`,
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`
    ])
    await listPage.selectSortType('RESERVATION_START_TIME')
    await listPage.assertChildNames([
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`,
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`
    ])
    await listPage.multiselectToggle.check()
    await listPage.selectChild(testChild2.id)
    await listPage.markMultipleArrivedButton.click()
    await childAttendancePage.setTime(now.subHours(5).toLocalTime().format())
    await childAttendancePage.selectMarkPresent()
    await listPage.assertChildNames([
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`,
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`
    ])
    await listPage.selectSortType('CHILD_FIRST_NAME')
    await listPage.assertChildNames([
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`
    ])
  })

  test('Coming tab sorting works with two reservations', async () => {
    await createPlacements(testChild.id)
    await createPlacements(testChild2.id, group2.id)
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: testChild.id,
      date: now.toLocalDate(),
      reservation: new TimeRange(
        now.subHours(9).toLocalTime(),
        now.subHours(1).toLocalTime()
      ),
      secondReservation: new TimeRange(
        now.addHours(1).toLocalTime(),
        now.addHours(9).toLocalTime()
      )
    }).save()
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: testChild2.id,
      date: now.toLocalDate(),
      reservation: new TimeRange(
        now.subHours(1).toLocalTime(),
        now.addHours(9).toLocalTime()
      ),
      secondReservation: null
    }).save()
    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)

    await openPage({ mockedTime: now })
    await page.goto(mobileSignupUrl)
    await listPage.selectSortType('RESERVATION_START_TIME')
    await listPage.assertChildNames([
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`
    ])

    await openPage({ mockedTime: now.subHours(5) })
    await page.goto(mobileSignupUrl)
    await listPage.selectSortType('RESERVATION_START_TIME')
    await listPage.assertChildNames([
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`,
      `${testChild2.firstName} ${testChild2.lastName}`
    ])
  })

  test('Coming tab sorting works with regular daily service time', async () => {
    await createPlacements(testChild.id)
    await createPlacements(testChild2.id, group2.id)
    await createPlacements(testChildRestricted.id)
    await Fixture.dailyServiceTime({
      childId: testChild.id,
      validityPeriod: new DateRange(now.toLocalDate(), now.toLocalDate()),
      type: 'REGULAR',
      regularTimes: new TimeRange(
        now.subHours(3).toLocalTime(),
        now.addHours(3).toLocalTime()
      )
    }).save()
    await Fixture.dailyServiceTime({
      childId: testChild2.id,
      validityPeriod: new DateRange(now.toLocalDate(), now.toLocalDate()),
      type: 'REGULAR',
      regularTimes: new TimeRange(
        now.subHours(5).toLocalTime(),
        now.addHours(3).toLocalTime()
      )
    }).save()
    await Fixture.dailyServiceTime({
      childId: testChildRestricted.id,
      validityPeriod: new DateRange(now.toLocalDate(), now.toLocalDate()),
      type: 'REGULAR',
      regularTimes: new TimeRange(
        now.subHours(4).toLocalTime(),
        now.addHours(3).toLocalTime()
      )
    }).save()
    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)

    await openPage()
    await page.goto(mobileSignupUrl)
    await listPage.comingChildrenTab.click()
    await listPage.assertChildNames([
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`,
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`
    ])
    await listPage.selectSortType('RESERVATION_START_TIME')
    await listPage.assertChildNames([
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`,
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`
    ])
    await listPage.selectSortType('CHILD_FIRST_NAME')
    await listPage.assertChildNames([
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`,
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`
    ])
  })

  test('Coming tab sorting works with irregular daily service time', async () => {
    await createPlacements(testChild.id)
    await createPlacements(testChild2.id, group2.id)
    await createPlacements(testChildRestricted.id)
    await Fixture.dailyServiceTime({
      childId: testChild.id,
      validityPeriod: new DateRange(now.toLocalDate(), now.toLocalDate()),
      type: 'IRREGULAR',
      regularTimes: null,
      fridayTimes: new TimeRange(
        now.subHours(3).toLocalTime(),
        now.addHours(3).toLocalTime()
      )
    }).save()
    await Fixture.dailyServiceTime({
      childId: testChild2.id,
      validityPeriod: new DateRange(now.toLocalDate(), now.toLocalDate()),
      type: 'IRREGULAR',
      regularTimes: null,
      fridayTimes: new TimeRange(
        now.subHours(5).toLocalTime(),
        now.addHours(3).toLocalTime()
      )
    }).save()
    await Fixture.dailyServiceTime({
      childId: testChildRestricted.id,
      validityPeriod: new DateRange(now.toLocalDate(), now.toLocalDate()),
      type: 'IRREGULAR',
      regularTimes: null,
      fridayTimes: new TimeRange(
        now.subHours(4).toLocalTime(),
        now.addHours(3).toLocalTime()
      )
    }).save()
    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)

    await openPage()
    await page.goto(mobileSignupUrl)
    await listPage.comingChildrenTab.click()
    await listPage.assertChildNames([
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`,
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`
    ])
    await listPage.selectSortType('RESERVATION_START_TIME')
    await listPage.assertChildNames([
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`,
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`
    ])
    await listPage.selectSortType('CHILD_FIRST_NAME')
    await listPage.assertChildNames([
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`,
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`
    ])
  })

  test('Present tab sorting works', async () => {
    await createPlacements(testChild.id)
    await createPlacements(testChild2.id, group2.id)
    await createPlacements(testChildRestricted.id)
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: testChild.id,
      date: now.toLocalDate(),
      reservation: new TimeRange(
        now.subHours(3).toLocalTime(),
        now.addHours(3).toLocalTime()
      ),
      secondReservation: null
    }).save()
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: testChild2.id,
      date: now.toLocalDate(),
      reservation: new TimeRange(
        now.subHours(3).toLocalTime(),
        now.addHours(1).toLocalTime()
      ),
      secondReservation: null
    }).save()
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: testChildRestricted.id,
      date: now.toLocalDate(),
      reservation: new TimeRange(
        now.subHours(3).toLocalTime(),
        now.addHours(2).toLocalTime()
      ),
      secondReservation: null
    }).save()
    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)

    await openPage()
    await page.goto(mobileSignupUrl)
    await listPage.comingChildrenTab.click()
    await listPage.multiselectToggle.check()
    await listPage.selectChild(testChild.id)
    await listPage.selectChild(testChild2.id)
    await listPage.selectChild(testChildRestricted.id)
    await listPage.markMultipleArrivedButton.click()
    await childAttendancePage.setTime(now.subHours(3).toLocalTime().format())
    await childAttendancePage.selectMarkPresent()
    await listPage.presentChildrenTab.click()
    await listPage.assertChildNames([
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`,
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`
    ])
    await listPage.selectSortType('RESERVATION_END_TIME')
    await listPage.assertChildNames([
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`,
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`
    ])
    await listPage.multiselectToggle.check()
    await listPage.selectChild(testChild2.id)
    await listPage.markMultipleDepartedutton.click()
    await childAttendancePage.setTime(now.addHours(1).toLocalTime().format())
    await childAttendancePage.markDepartedButton.click()
    await listPage.assertChildNames([
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`,
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`
    ])
    await listPage.selectSortType('CHILD_FIRST_NAME')
    await listPage.assertChildNames([
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`
    ])
  })

  test('Present tab sorting works with two reservations', async () => {
    await createPlacements(testChild.id)
    await createPlacements(testChild2.id, group2.id)
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: testChild.id,
      date: now.toLocalDate(),
      reservation: new TimeRange(
        now.subHours(9).toLocalTime(),
        now.subHours(1).toLocalTime()
      ),
      secondReservation: new TimeRange(
        now.addHours(1).toLocalTime(),
        now.addHours(9).toLocalTime()
      )
    }).save()
    await Fixture.attendanceReservation({
      type: 'RESERVATIONS',
      childId: testChild2.id,
      date: now.toLocalDate(),
      reservation: new TimeRange(
        now.addHours(1).toLocalTime(),
        now.addHours(8).toLocalTime()
      ),
      secondReservation: null
    }).save()
    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)

    await openPage({ mockedTime: now })
    await page.goto(mobileSignupUrl)
    await listPage.comingChildrenTab.click()
    await listPage.multiselectToggle.check()
    await listPage.selectChild(testChild.id)
    await listPage.selectChild(testChild2.id)
    await listPage.markMultipleArrivedButton.click()
    await childAttendancePage.setTime(now.toLocalTime().format())
    await childAttendancePage.selectMarkPresent()
    await listPage.presentChildrenTab.click()
    await listPage.selectSortType('RESERVATION_END_TIME')
    await listPage.assertChildNames([
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`,
      `${testChild2.firstName} ${testChild2.lastName}`
    ])

    await openPage({ mockedTime: now.addHours(5) })
    await page.goto(mobileSignupUrl)
    await listPage.presentChildrenTab.click()
    await listPage.selectSortType('RESERVATION_END_TIME')
    await listPage.assertChildNames([
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`
    ])
  })

  test('Present tab sorting works with regular daily service time', async () => {
    await createPlacements(testChild.id)
    await createPlacements(testChild2.id, group2.id)
    await createPlacements(testChildRestricted.id)
    await Fixture.dailyServiceTime({
      childId: testChild.id,
      validityPeriod: new DateRange(now.toLocalDate(), now.toLocalDate()),
      type: 'REGULAR',
      regularTimes: new TimeRange(
        now.subHours(3).toLocalTime(),
        now.addHours(3).toLocalTime()
      )
    }).save()
    await Fixture.dailyServiceTime({
      childId: testChild2.id,
      validityPeriod: new DateRange(now.toLocalDate(), now.toLocalDate()),
      type: 'REGULAR',
      regularTimes: new TimeRange(
        now.subHours(3).toLocalTime(),
        now.addHours(1).toLocalTime()
      )
    }).save()
    await Fixture.dailyServiceTime({
      childId: testChildRestricted.id,
      validityPeriod: new DateRange(now.toLocalDate(), now.toLocalDate()),
      type: 'REGULAR',
      regularTimes: new TimeRange(
        now.subHours(3).toLocalTime(),
        now.addHours(2).toLocalTime()
      )
    }).save()
    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)

    await openPage()
    await page.goto(mobileSignupUrl)
    await listPage.comingChildrenTab.click()
    await listPage.multiselectToggle.check()
    await listPage.selectChild(testChild.id)
    await listPage.selectChild(testChild2.id)
    await listPage.selectChild(testChildRestricted.id)
    await listPage.markMultipleArrivedButton.click()
    await childAttendancePage.setTime(now.subHours(3).toLocalTime().format())
    await childAttendancePage.selectMarkPresent()
    await listPage.presentChildrenTab.click()
    await listPage.assertChildNames([
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`,
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`
    ])
    await listPage.selectSortType('RESERVATION_END_TIME')
    await listPage.assertChildNames([
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`,
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`
    ])
    await listPage.selectSortType('CHILD_FIRST_NAME')
    await listPage.assertChildNames([
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`,
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`
    ])
  })

  test('Present tab sorting works with irregular daily service time', async () => {
    await createPlacements(testChild.id)
    await createPlacements(testChild2.id, group2.id)
    await createPlacements(testChildRestricted.id)
    await Fixture.dailyServiceTime({
      childId: testChild.id,
      validityPeriod: new DateRange(now.toLocalDate(), now.toLocalDate()),
      type: 'IRREGULAR',
      regularTimes: null,
      fridayTimes: new TimeRange(
        now.subHours(3).toLocalTime(),
        now.addHours(3).toLocalTime()
      )
    }).save()
    await Fixture.dailyServiceTime({
      childId: testChild2.id,
      validityPeriod: new DateRange(now.toLocalDate(), now.toLocalDate()),
      type: 'IRREGULAR',
      regularTimes: null,
      fridayTimes: new TimeRange(
        now.subHours(3).toLocalTime(),
        now.addHours(1).toLocalTime()
      )
    }).save()
    await Fixture.dailyServiceTime({
      childId: testChildRestricted.id,
      validityPeriod: new DateRange(now.toLocalDate(), now.toLocalDate()),
      type: 'IRREGULAR',
      regularTimes: null,
      fridayTimes: new TimeRange(
        now.subHours(3).toLocalTime(),
        now.addHours(2).toLocalTime()
      )
    }).save()
    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)

    await openPage()
    await page.goto(mobileSignupUrl)
    await listPage.comingChildrenTab.click()
    await listPage.multiselectToggle.check()
    await listPage.selectChild(testChild.id)
    await listPage.selectChild(testChild2.id)
    await listPage.selectChild(testChildRestricted.id)
    await listPage.markMultipleArrivedButton.click()
    await childAttendancePage.setTime(now.subHours(3).toLocalTime().format())
    await childAttendancePage.selectMarkPresent()
    await listPage.presentChildrenTab.click()
    await listPage.assertChildNames([
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`,
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`
    ])
    await listPage.selectSortType('RESERVATION_END_TIME')
    await listPage.assertChildNames([
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`,
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`
    ])
    await listPage.selectSortType('CHILD_FIRST_NAME')
    await listPage.assertChildNames([
      `${testChild.firstName} ${testChild.lastName} (${testChild.preferredName})`,
      `${testChild2.firstName} ${testChild2.lastName}`,
      `${testChildRestricted.firstName} ${testChildRestricted.lastName}`
    ])
  })

  test('Child can be marked as absent for the whole day', async () => {
    await openPage()
    const child = testChild2.id
    await createPlacements(child)

    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    await page.goto(mobileSignupUrl)

    await listPage.selectChild(child)
    await childPage.markAbsentLink.click()
    await childAttendancePage.selectMarkAbsentByType('OTHER_ABSENCE')
    await childAttendancePage.selectMarkAbsentButton()
    await childPage.goBack.click()
    await listPage.absentChildrenTab.click()
    await listPage.assertChildExists(child)
  })

  test('Child can be marked present and returned to coming', async () => {
    await openPage()
    const child = testChild2.id
    await createPlacements(child)

    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    await page.goto(mobileSignupUrl)

    await listPage.selectChild(child)
    await childPage.markPresentLink.click()
    await childAttendancePage.setTime('08:00')
    await childAttendancePage.selectMarkPresent()

    await listPage.presentChildrenTab.click()
    await listPage.selectChild(child)
    await childPage.cancelArrivalButton.click()
    await childPage.modalOkButton.click()

    await listPage.comingChildrenTab.click()
    await listPage.assertChildExists(child)
  })

  test('User can undo the whole flow of marking present at 08:30 and leaving at 16:00', async () => {
    await openPage()
    const child = testChild2.id
    await createPlacements(child)

    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    await page.goto(mobileSignupUrl)

    await listPage.selectChild(child)
    await childPage.markPresentLink.click()
    await childAttendancePage.setTime('08:30')
    await childAttendancePage.selectMarkPresent()

    await listPage.presentChildrenTab.click()
    await listPage.selectChild(child)
    await childPage.markDepartedLink.click()
    await childAttendancePage.setTime('16:00')
    await childAttendancePage.markDepartedButton.click()

    await listPage.departedChildrenTab.click()
    await listPage.selectChild(child)
    await childPage.returnToPresentButton.click()
    await childPage.returnToPresentButton.click()

    await listPage.presentChildrenTab.click()
    await listPage.selectChild(child)
    await childPage.cancelArrivalButton.click()
    await childPage.modalOkButton.click()

    await listPage.comingChildrenTab.click()
    await listPage.assertChildExists(child)
  })

  test('Child can have multiple attendances in one day', async () => {
    await openPage()
    const child = testChild2.id
    await createPlacements(child)

    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    await page.goto(mobileSignupUrl)

    await listPage.selectChild(child)
    await childPage.markPresentLink.click()
    await childAttendancePage.setTime('08:30')
    await childAttendancePage.selectMarkPresent()

    await listPage.presentChildrenTab.click()
    await listPage.selectChild(child)
    await childPage.markDepartedLink.click()
    await childAttendancePage.setTime('15:00')
    await childAttendancePage.markDepartedButton.click()

    await listPage.departedChildrenTab.click()
    await listPage.selectChild(child)
    await childPage.returnToPresentButton.click()

    // Cannot overlap previous departure
    await childAttendancePage.setTime('15:00')
    await childAttendancePage.assertMarkPresentButtonDisabled(true)

    await childAttendancePage.setTime('15:15')
    await childAttendancePage.selectMarkPresent()

    await listPage.presentChildrenTab.click()
    await listPage.selectChild(child)
    await childPage.assertArrivalTimeInfoIsShown(
      'Saapumisaika08:30,Saapumisaika15:15'
    )
    await childPage.assertDepartureTimeInfoIsShown('Lähtöaika15:00')
    await childPage.markDepartedLink.click()

    await childAttendancePage.setTime('15:15')
    await childAttendancePage.setTimeInfo.assertTextEquals('Saapui 15:15')
    await childAttendancePage.setTime('15:20')
    await childAttendancePage.markDepartedButton.click()
  })

  test('Group selector works consistently', async () => {
    await openPage()
    const child1 = testChild2.id
    await createPlacements(child1)
    await createPlacements(testChildRestricted.id)
    await createPlacements(testChild.id, group2.id)

    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    await page.goto(mobileSignupUrl)

    await assertAttendanceCounts(3, 0, 0, 0, 3)

    await listPage.selectChild(child1)
    await childPage.markPresentLink.click()
    await childAttendancePage.setTime('08:00')
    await childAttendancePage.selectMarkPresent()

    await assertAttendanceCounts(2, 1, 0, 0, 3)

    await listPage.selectGroup(group2.id)
    await assertAttendanceCounts(1, 0, 0, 0, 1)

    await listPage.selectGroup('all')
    await assertAttendanceCounts(2, 1, 0, 0, 3)
  })

  test('Group name is visible only when all-group selected', async () => {
    await openPage()
    const childId = testChild2.id
    await createPlacements(childId)
    await createPlacements(testChildRestricted.id)
    await createPlacements(testChild.id, group2.id)

    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    await page.goto(mobileSignupUrl)

    await listPage.selectGroup('all')
    await waitUntilEqual(
      () => listPage.readChildGroupName(childId),
      testDaycareGroup.name.toUpperCase()
    )

    await listPage.selectGroup(testDaycareGroup.id)
    await waitUntilEqual(() => listPage.readChildGroupName(childId), '')
  })

  test('Child will not be visible in two groups at the same time', async () => {
    await openPage()
    const childId = testChild2.id

    await Fixture.daycare({ ...testDaycare2, areaId: testCareArea.id }).save()

    const daycareGroup2Fixture = await Fixture.daycareGroup({
      name: 'testgroup',
      daycareId: testDaycare2.id,
      startDate: LocalDate.of(2022, 1, 1)
    }).save()

    const today = now.toLocalDate()
    const placement1StartDate = today.subMonths(5)
    const placement1EndDate = today.subMonths(1)

    const placement2StartDate = placement1EndDate.addDays(1)
    const placement2EndDate = today.addMonths(3)

    const daycarePlacementFixture = await Fixture.placement({
      childId,
      unitId: testDaycare.id,
      startDate: placement1StartDate,
      endDate: placement1EndDate
    }).save()

    const daycarePlacement2Fixture = await Fixture.placement({
      childId,
      unitId: testDaycare2.id,
      startDate: placement2StartDate,
      endDate: placement2EndDate
    }).save()

    await Fixture.groupPlacement({
      daycarePlacementId: daycarePlacementFixture.id,
      daycareGroupId: testDaycareGroup.id,
      startDate: placement1StartDate,
      endDate: placement2EndDate
    }).save()

    await Fixture.groupPlacement({
      daycarePlacementId: daycarePlacement2Fixture.id,
      daycareGroupId: daycareGroup2Fixture.id,
      startDate: placement1StartDate,
      endDate: placement2EndDate
    }).save()

    await page.goto(await pairMobileDevice(testDaycare.id))
    await assertAttendanceCounts(0, 0, 0, 0, 0)

    await page.goto(await pairMobileDevice(testDaycare2.id))
    await assertAttendanceCounts(1, 0, 0, 0, 1)
  })

  test('Fixed schedule child\'s "reservation" is correct', async () => {
    await openPage()
    const child = testChild2.id
    await createPlacements(child, testDaycareGroup.id, 'PRESCHOOL')

    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    await page.goto(mobileSignupUrl)

    await assertAttendanceCounts(1, 0, 0, 0, 1)
    await listPage.selectChild(child)
    await childPage.reservation.assertTextEquals('Läsnä')
  })

  test('Term break child is shown in absent list', async () => {
    // change mocked now to be during term break
    const testNow = HelsinkiDateTime.of(2024, 1, 1, 13, 0, 0)
    await openPage({ mockedTime: testNow })

    const child = testChild2.id
    await createPlacements(
      child,
      testDaycareGroup.id,
      'PRESCHOOL',
      testNow.toLocalDate()
    )

    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    await page.goto(mobileSignupUrl)

    await assertAttendanceCounts(0, 0, 0, 1, 1)
    await listPage.absentChildrenTab.click()
    await listPage.selectChild(child)
    await childPage.termBreak.waitUntilVisible()
  })

  test('Non operational day child is shown in absent list', async () => {
    // change mocked now to be during weekend
    const testNow = HelsinkiDateTime.of(2024, 5, 18, 13, 0, 0)
    await openPage({ mockedTime: testNow })

    const child = testChild2.id
    await createPlacements(
      child,
      testDaycareGroup.id,
      'PRESCHOOL',
      testNow.toLocalDate()
    )

    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    await page.goto(mobileSignupUrl)

    await assertAttendanceCounts(0, 0, 0, 1, 1)
    await listPage.absentChildrenTab.click()
    await listPage.selectChild(child)
    await childPage.termBreak.waitUntilVisible()
  })

  test('Child with shift care is shown in coming list even on weekend', async () => {
    // change mocked now to be during weekend
    const testNow = HelsinkiDateTime.of(2024, 5, 18, 13, 0, 0)
    await openPage({ mockedTime: testNow })

    const child = testChild2.id
    const placement = await createPlacements(
      child,
      testDaycareGroup.id,
      'PRESCHOOL',
      testNow.toLocalDate()
    )
    const employee = await Fixture.employee().save()
    const serviceNeedOption = await Fixture.serviceNeedOption().save()
    await Fixture.serviceNeed({
      placementId: placement.id,
      startDate: placement.startDate,
      endDate: placement.endDate,
      shiftCare: 'FULL',
      optionId: serviceNeedOption.id,
      confirmedBy: evakaUserId(employee.id)
    }).save()

    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    await page.goto(mobileSignupUrl)

    await assertAttendanceCounts(1, 0, 0, 0, 1)
  })

  test('If child does not have guardians a message about this is shown ', async () => {
    await openPage()
    await createPlacements(testChild.id, group2.id)

    const childWithGuardians = familyWithTwoGuardians.children[0]
    await Fixture.guardian(
      childWithGuardians,
      familyWithTwoGuardians.guardian
    ).save()
    await createPlacements(childWithGuardians.id, group2.id)

    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)

    await page.goto(mobileSignupUrl)
    await listPage.comingChildrenTab.click()
    await listPage.selectChild(testChild.id)
    await childPage.noGuardiansInfoBox.waitUntilVisible()

    await page.goto(mobileSignupUrl)
    await listPage.selectChild(childWithGuardians.id)
    await childPage.noGuardiansInfoBox.waitUntilHidden()
  })

  test('If child does has foster parent a message about missing guardian is not shown ', async () => {
    await openPage()
    await createPlacements(testChild.id, group2.id)
    const fosterParent = await Fixture.person().saveAdult()
    const employee = await Fixture.employee().save()

    await createFosterParent({
      body: [
        {
          id: randomId(),
          childId: testChild.id,
          parentId: fosterParent.id,
          validDuring: new DateRange(
            HelsinkiDateTime.now().toLocalDate(),
            HelsinkiDateTime.now().toLocalDate()
          ),
          modifiedAt: HelsinkiDateTime.now(),
          modifiedBy: evakaUserId(employee.id)
        }
      ]
    })
    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)

    await page.goto(mobileSignupUrl)
    await listPage.comingChildrenTab.click()
    await listPage.selectChild(testChild.id)
    await childPage.noGuardiansInfoBox.waitUntilHidden()
  })
})

describe('Notes on child departure page', () => {
  test('All group notes are shown on the child departure page', async () => {
    await openPage()
    const child1 = testChild2.id
    await createPlacements(child1)
    await createPlacements(testChildRestricted.id)
    await createPlacements(testChild.id, group2.id)

    const mobileSignupUrl = await pairMobileDevice(testDaycare.id)
    await page.goto(mobileSignupUrl)

    await listPage.selectChild(child1)
    childPage = new MobileChildPage(page)
    await childPage.openNotes()
    const notePage = new MobileNotePage(page)
    await notePage.selectTab('group')
    await notePage.typeAndSaveStickyNote('This is a group note')
    await notePage.initNewStickyNote()
    await notePage.typeAndSaveStickyNote('This is another group note')
    await notePage.assertStickyNote('This is another group note', 1)

    await page.goto(mobileSignupUrl)
    await listPage.comingChildrenTab.click()
    await listPage.selectChild(child1)
    await childPage.markPresentLink.click()
    await childAttendancePage.setTime('08:00')
    await childAttendancePage.selectMarkPresent()

    await listPage.presentChildrenTab.click()
    await listPage.selectChild(child1)
    await childPage.markDepartedLink.click()
    await waitUntilEqual(() => childAttendancePage.groupNotes.count(), 2)
  })
})
