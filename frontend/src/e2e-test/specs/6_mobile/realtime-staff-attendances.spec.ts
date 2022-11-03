// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import { insertDefaultServiceNeedOptions, resetDatabase } from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  daycare2Fixture,
  daycareGroupFixture,
  EmployeeBuilder,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { DaycareGroup } from '../../dev-api/types'
import MobileNav from '../../pages/mobile/mobile-nav'
import { StaffAttendancePage } from '../../pages/mobile/staff-page'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let nav: MobileNav
let mobileSignupUrl: string
let staffAttendancePage: StaffAttendancePage
let staffFixture: EmployeeBuilder

const pin = '4242'

const daycareGroup2Fixture: DaycareGroup = {
  ...daycareGroupFixture,
  id: uuidv4(),
  name: 'Ryhmä 2'
}

beforeEach(async () => {
  await resetDatabase()
  const fixtures = await initializeAreaAndPersonData()
  await insertDefaultServiceNeedOptions()

  await Fixture.daycare()
    .with({
      ...daycare2Fixture,
      areaId: fixtures.careAreaFixture.id,
      enabledPilotFeatures: ['REALTIME_STAFF_ATTENDANCE']
    })
    .save()

  await Fixture.daycareGroup()
    .with({
      ...daycareGroupFixture,
      daycareId: daycare2Fixture.id
    })
    .save()
  await Fixture.daycareGroup()
    .with({
      ...daycareGroup2Fixture,
      daycareId: daycare2Fixture.id
    })
    .save()
  const daycarePlacementFixture = await Fixture.placement()
    .with({
      childId: fixtures.familyWithTwoGuardians.children[0].id,
      unitId: daycare2Fixture.id
    })
    .save()
  await Fixture.groupPlacement()
    .with({
      daycarePlacementId: daycarePlacementFixture.data.id,
      daycareGroupId: daycareGroupFixture.id
    })
    .save()
  staffFixture = await Fixture.employeeStaff(daycare2Fixture.id)
    .withGroupAcl(daycareGroupFixture.id)
    .withGroupAcl(daycareGroup2Fixture.id)
    .save()
  await Fixture.employeePin().with({ userId: staffFixture.data.id, pin }).save()
})

const initPages = async (mockedTime: Date) => {
  page = await Page.open({
    mockedTime
  })
  nav = new MobileNav(page)

  mobileSignupUrl = await pairMobileDevice(daycare2Fixture.id)
  await page.goto(mobileSignupUrl)
  await nav.openPage('staff')
  staffAttendancePage = new StaffAttendancePage(page)
}

describe('Realtime staff attendance page', () => {
  test('Staff member can be marked as arrived and departed', async () => {
    await initPages(new Date('2022-05-05T03:00Z'))

    const name = `${staffFixture.data.lastName} ${staffFixture.data.firstName}`
    const arrivalTime = '05:59'
    const departureTime = '12:45'

    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.selectTab('absent')
    await staffAttendancePage.openStaffPage(name)
    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.markStaffArrived({
      pin,
      time: arrivalTime,
      group: daycareGroupFixture
    })
    await staffAttendancePage.assertEmployeeStatus('Läsnä')
    await staffAttendancePage.assertEmployeeAttendanceTimes(
      0,
      `Paikalla ${arrivalTime}–`
    )

    await initPages(new Date('2022-05-05T10:30Z'))

    await staffAttendancePage.assertPresentStaffCount(1)

    await staffAttendancePage.selectTab('present')
    await staffAttendancePage.openStaffPage(name)
    await staffAttendancePage.markStaffDeparted({ pin, time: departureTime })
    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.assertEmployeeAttendanceTimes(
      0,
      `Paikalla ${arrivalTime}–${departureTime}`
    )
    await staffAttendancePage.goBackFromMemberPage()
    await staffAttendancePage.assertPresentStaffCount(0)
  })

  test('Staff arrival page behaves correctly with different time values when no plan exists', async () => {
    await initPages(new Date('2022-05-05T09:00Z'))

    const name = `${staffFixture.data.lastName} ${staffFixture.data.firstName}`

    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.selectTab('absent')
    await staffAttendancePage.openStaffPage(name)
    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.clickStaffArrivedAndSetPin(pin)

    // Within 30min from now so ok
    await staffAttendancePage.setTime('12:30')
    await staffAttendancePage.selectGroup(daycareGroupFixture.id)
    await staffAttendancePage.assertDoneButtonEnabled(true)

    // 1min too far in the future
    await staffAttendancePage.setTime('12:31')
    await staffAttendancePage.assertArrivalTimeInputInfo(
      'Oltava välillä 11:30-12:30'
    )
    await staffAttendancePage.assertGroupSelectionVisible(false)
    await staffAttendancePage.assertDoneButtonEnabled(false)
  })

  test('Staff arrival page behaves correctly with different time values in the future when plan exists', async () => {
    // Planned attendance 08:00 - 16:00
    const planStart = HelsinkiDateTime.of(2022, 5, 5, 8, 0)
    const planEnd = HelsinkiDateTime.of(2022, 5, 5, 16, 0)
    await Fixture.staffAttendancePlan()
      .with({
        id: uuidv4(),
        employeeId: staffFixture.data.id,
        startTime: planStart,
        endTime: planEnd
      })
      .save()

    // Now it is 7:30
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 7, 30).toSystemTzDate())

    const name = `${staffFixture.data.lastName} ${staffFixture.data.firstName}`

    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.selectTab('absent')
    await staffAttendancePage.openStaffPage(name)
    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.clickStaffArrivedAndSetPin(pin)

    // Within 30min from planned start so ok, type required
    await staffAttendancePage.setTime('7:30')
    await staffAttendancePage.selectGroup(daycareGroupFixture.id)
    await staffAttendancePage.assertDoneButtonEnabled(false)
    await staffAttendancePage.selectArrivalType('JUSTIFIED_CHANGE')
    await staffAttendancePage.assertDoneButtonEnabled(true)

    // Within 5min from planned start so ok, type not required
    await staffAttendancePage.setTime('7:55')
    await staffAttendancePage.selectGroup(daycareGroupFixture.id)
    await staffAttendancePage.assertDoneButtonEnabled(true)

    // Not ok because >+-30min from current time
    await staffAttendancePage.setTime('8:01')
    await staffAttendancePage.assertArrivalTimeInputInfo(
      'Oltava välillä 07:00-08:00'
    )
    // Not ok because >30min from current time
    await staffAttendancePage.setTime('6:59')
    await staffAttendancePage.assertArrivalTimeInputInfo(
      'Oltava välillä 07:00-08:00'
    )
  })

  test('Staff arrival page behaves correctly with different time values in the past when plan exists', async () => {
    // Planned attendance 08:00 - 16:00
    const planStart = HelsinkiDateTime.of(2022, 5, 5, 8, 0)
    const planEnd = HelsinkiDateTime.of(2022, 5, 5, 16, 0)
    await Fixture.staffAttendancePlan()
      .with({
        id: uuidv4(),
        employeeId: staffFixture.data.id,
        startTime: planStart,
        endTime: planEnd
      })
      .save()

    // Now it is 8:30
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 8, 30).toSystemTzDate())

    const name = `${staffFixture.data.lastName} ${staffFixture.data.firstName}`

    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.selectTab('absent')
    await staffAttendancePage.openStaffPage(name)
    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.clickStaffArrivedAndSetPin(pin)

    // Within 5min from planned start so ok, type not required
    await staffAttendancePage.setTime('8:00')
    await staffAttendancePage.selectGroup(daycareGroupFixture.id)
    await staffAttendancePage.assertDoneButtonEnabled(true)

    // More than 5min from planned start, type is not required but can be selected
    await staffAttendancePage.setTime('8:15')
    await staffAttendancePage.selectGroup(daycareGroupFixture.id)
    await staffAttendancePage.assertDoneButtonEnabled(true)
    await staffAttendancePage.selectArrivalType('JUSTIFIED_CHANGE')
    await staffAttendancePage.assertDoneButtonEnabled(true)

    // Not ok because >+-30min from current time
    await staffAttendancePage.setTime('7:59')
    await staffAttendancePage.assertArrivalTimeInputInfo(
      'Oltava välillä 08:00-09:00'
    )
  })

  test('New external staff member can be added and marked as departed', async () => {
    await initPages(new Date('2022-05-05T13:00Z'))

    const name = 'Nomen Estomen'
    const arrivalTime = '03:20'

    await staffAttendancePage.assertPresentStaffCount(0)

    await staffAttendancePage.markNewExternalStaffArrived(
      arrivalTime,
      name,
      daycareGroupFixture
    )
    await staffAttendancePage.assertPresentStaffCount(1)

    await staffAttendancePage.selectTab('present')
    await staffAttendancePage.openStaffPage(name)
    await staffAttendancePage.assertEmployeeStatus('Läsnä')
    await staffAttendancePage.assertExternalStaffArrivalTime(arrivalTime)
    await staffAttendancePage.markExternalStaffDeparted('11:09')
    await staffAttendancePage.assertPresentStaffCount(0)
  })
})
