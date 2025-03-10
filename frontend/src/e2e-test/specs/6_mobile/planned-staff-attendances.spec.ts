// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { mobileViewport } from '../../browser'
import { testDaycare2, testDaycareGroup, Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import {
  DevCareArea,
  DevDaycareGroup,
  DevEmployee
} from '../../generated/api-types'
import MobileNav from '../../pages/mobile/mobile-nav'
import {
  PlannedAttendancesPage,
  StaffAttendancePage
} from '../../pages/mobile/staff-page'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let nav: MobileNav
let staffPage: StaffAttendancePage
let plannedAttendancesPage: PlannedAttendancesPage

let careArea: DevCareArea
let aku: DevEmployee
let mikki: DevEmployee

const pin = '4242'

const today = LocalDate.of(2025, 3, 3) // Monday

const daycareGroup2Fixture: DevDaycareGroup = {
  ...testDaycareGroup,
  id: randomId(),
  name: 'Ryhmä 2'
}

beforeEach(async () => {
  await resetServiceState()

  careArea = await Fixture.careArea().save()
  await Fixture.daycare({
    ...testDaycare2,
    areaId: careArea.id,
    enabledPilotFeatures: ['REALTIME_STAFF_ATTENDANCE']
  }).save()

  await Fixture.daycareGroup({
    ...testDaycareGroup,
    daycareId: testDaycare2.id
  }).save()
  await Fixture.daycareGroup({
    ...daycareGroup2Fixture,
    daycareId: testDaycare2.id
  }).save()

  aku = await Fixture.employee({
    preferredFirstName: 'Aku',
    firstName: 'Antero',
    lastName: 'Ankka'
  })
    .staff(testDaycare2.id)
    .withGroupAcl(testDaycareGroup.id)
    .save()
  mikki = await Fixture.employee({
    firstName: 'Mikki',
    lastName: 'Hiiri'
  })
    .staff(testDaycare2.id)
    .withGroupAcl(testDaycareGroup.id)
    .withGroupAcl(daycareGroup2Fixture.id)
    .save()
  await Fixture.employeePin({ userId: aku.id, pin }).save()
  await Fixture.employeePin({ userId: mikki.id, pin }).save()
})

const initPages = async (mockedTime: HelsinkiDateTime) => {
  page = await Page.open({
    viewport: mobileViewport,
    mockedTime
  })
  nav = new MobileNav(page)
  staffPage = new StaffAttendancePage(page)
  plannedAttendancesPage = new PlannedAttendancesPage(page)

  const mobileSignupUrl = await pairMobileDevice(testDaycare2.id)
  await page.goto(mobileSignupUrl)
  await nav.staff.click()
}

describe('Planned staff attendances', () => {
  test('shows who has planned attendances during next days', async () => {
    const tuesday = today.addDays(1)
    const wednesday = today.addDays(2)
    const thursday = today.addDays(3)
    const friday = today.addDays(4)

    await Fixture.staffAttendancePlan({
      employeeId: aku.id,
      startTime: HelsinkiDateTime.fromLocal(tuesday, LocalTime.of(9, 0)),
      endTime: HelsinkiDateTime.fromLocal(tuesday, LocalTime.of(17, 0))
    }).save()
    await Fixture.staffAttendancePlan({
      employeeId: mikki.id,
      startTime: HelsinkiDateTime.fromLocal(wednesday, LocalTime.of(9, 0)),
      endTime: HelsinkiDateTime.fromLocal(wednesday, LocalTime.of(12, 0))
    }).save()
    await Fixture.staffAttendancePlan({
      employeeId: mikki.id,
      startTime: HelsinkiDateTime.fromLocal(wednesday, LocalTime.of(22, 0)),
      endTime: HelsinkiDateTime.fromLocal(thursday, LocalTime.of(7, 0))
    }).save()

    await initPages(HelsinkiDateTime.fromLocal(today, LocalTime.of(6, 0)))
    await staffPage.selectPrimaryTab('planned')
    await plannedAttendancesPage.assertPresentCount(tuesday, 1)
    await plannedAttendancesPage.assertPresentCount(wednesday, 1)
    await plannedAttendancesPage.assertPresentCount(thursday, 1)
    await plannedAttendancesPage.assertPresentCount(friday, 0)

    await plannedAttendancesPage.getExpandedDate(tuesday).waitUntilHidden()
    await plannedAttendancesPage.getDateRow(tuesday).click()
    await plannedAttendancesPage.getExpandedDate(tuesday).waitUntilVisible()
    await plannedAttendancesPage
      .getPresentEmployee(tuesday, aku.id)
      .assertText((s) => s.includes('Aku Ankka') && s.includes('09:00 - 17:00'))
    await plannedAttendancesPage
      .getConfidenceWarning(tuesday, aku.id)
      .waitUntilHidden()
    await plannedAttendancesPage
      .getAbsentEmployee(tuesday, mikki.id)
      .assertText((s) => s.includes('Mikki Hiiri'))

    await plannedAttendancesPage.getDateRow(wednesday).click()
    await plannedAttendancesPage.getExpandedDate(tuesday).waitUntilHidden()
    await plannedAttendancesPage.getExpandedDate(wednesday).waitUntilVisible()
    await plannedAttendancesPage
      .getAbsentEmployee(wednesday, aku.id)
      .waitUntilVisible()
    await plannedAttendancesPage
      .getPresentEmployee(wednesday, mikki.id)
      .assertText((s) => s.includes('09:00 - 12:00,\n22:00 - →'))
    await plannedAttendancesPage
      .getConfidenceWarning(wednesday, mikki.id)
      .assertText((s) => s.includes('Työvuoro voi olla toisessa ryhmässä'))

    await plannedAttendancesPage.getDateRow(thursday).click()
    await plannedAttendancesPage
      .getPresentEmployee(thursday, mikki.id)
      .assertText((s) => s.includes('→ - 07:00'))

    // select group where only Mikki has been authorized
    await nav.selectGroup(daycareGroup2Fixture.id)
    await plannedAttendancesPage.assertPresentCount(tuesday, 0)
    await plannedAttendancesPage.assertPresentCount(wednesday, 1)
    await plannedAttendancesPage.assertPresentCount(thursday, 1)
    await plannedAttendancesPage.assertPresentCount(friday, 0)

    // On Tuesday Mikki is absent while Aku is neither present nor absent
    await plannedAttendancesPage.getDateRow(tuesday).click()
    await plannedAttendancesPage
      .getAbsentEmployee(tuesday, mikki.id)
      .waitUntilVisible()
    await plannedAttendancesPage
      .getPresentEmployee(tuesday, aku.id)
      .waitUntilHidden()
    await plannedAttendancesPage
      .getAbsentEmployee(tuesday, aku.id)
      .waitUntilHidden()
  })
})
