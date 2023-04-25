// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { insertDefaultServiceNeedOptions, resetDatabase } from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import type { EmployeeBuilder } from '../../dev-api/fixtures'
import {
  daycare2Fixture,
  daycareGroupFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import type { DaycareGroup } from '../../dev-api/types'
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
      enabledPilotFeatures: ['REALTIME_STAFF_ATTENDANCE'],
      operationDays: [1, 2, 3, 4, 5]
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
  await nav.staff.click()
  staffAttendancePage = new StaffAttendancePage(page)
}

describe('Realtime staff attendance page', () => {
  test('Staff member can be marked as arrived and departed', async () => {
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 6, 0).toSystemTzDate())
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

    await initPages(HelsinkiDateTime.of(2022, 5, 5, 13, 30).toSystemTzDate())
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

  test('Staff member cannot be marked as arrived on a non-operational day', async () => {
    const saturday = LocalDate.of(2022, 5, 7)
    await initPages(
      HelsinkiDateTime.fromLocal(saturday, LocalTime.of(16, 0)).toSystemTzDate()
    )
    const name = `${staffFixture.data.lastName} ${staffFixture.data.firstName}`

    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.selectTab('absent')
    await staffAttendancePage.openStaffPage(name)
    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.staffMemberPage.markArrivedBtn.assertDisabled(
      true
    )
  })

  test('Staff arrival page behaves correctly with different time values when no plan exists', async () => {
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 12, 0).toSystemTzDate())

    const name = `${staffFixture.data.lastName} ${staffFixture.data.firstName}`

    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.selectTab('absent')
    await staffAttendancePage.openStaffPage(name)
    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.clickStaffArrivedAndSetPin(pin)

    // Within 30min from now so ok
    await staffAttendancePage.setArrivalTime('12:30')
    await staffAttendancePage.selectGroup(daycareGroupFixture.id)
    await staffAttendancePage.assertDoneButtonEnabled(true)

    // 1min too far in the future
    await staffAttendancePage.setArrivalTime('12:31')
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
    await staffAttendancePage.setArrivalTime('7:30')
    await staffAttendancePage.selectGroup(daycareGroupFixture.id)
    await staffAttendancePage.assertDoneButtonEnabled(false)
    await staffAttendancePage.selectAttendanceType('JUSTIFIED_CHANGE')
    await staffAttendancePage.assertDoneButtonEnabled(true)

    // Within 5min from planned start so ok, type not required
    await staffAttendancePage.setArrivalTime('7:55')
    await staffAttendancePage.selectGroup(daycareGroupFixture.id)
    await staffAttendancePage.assertDoneButtonEnabled(true)

    // Not ok because >+-30min from current time
    await staffAttendancePage.setArrivalTime('8:01')
    await staffAttendancePage.assertArrivalTimeInputInfo(
      'Oltava välillä 07:00-08:00'
    )
    // Not ok because >30min from current time
    await staffAttendancePage.setArrivalTime('6:59')
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
    await staffAttendancePage.setArrivalTime('8:00')
    await staffAttendancePage.selectGroup(daycareGroupFixture.id)
    await staffAttendancePage.assertDoneButtonEnabled(true)

    // More than 5min from planned start, type is not required but can be selected
    await staffAttendancePage.setArrivalTime('8:15')
    await staffAttendancePage.selectGroup(daycareGroupFixture.id)
    await staffAttendancePage.assertDoneButtonEnabled(true)
    await staffAttendancePage.selectAttendanceType('JUSTIFIED_CHANGE')
    await staffAttendancePage.assertDoneButtonEnabled(true)

    // Not ok because >+-30min from current time
    await staffAttendancePage.setArrivalTime('7:59')
    await staffAttendancePage.assertArrivalTimeInputInfo(
      'Oltava välillä 08:00-09:00'
    )
  })

  test('Staff departure page behaves correctly with different time values in the future when plan exists', async () => {
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

    await Fixture.realtimeStaffAttendance()
      .with({
        employeeId: staffFixture.data.id,
        groupId: daycareGroupFixture.id,
        arrived: HelsinkiDateTime.of(2022, 5, 5, 8, 0),
        departed: null,
        occupancyCoefficient: 0,
        type: 'PRESENT'
      })
      .save()

    // Now it is 15:30
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 15, 30).toSystemTzDate())

    const name = `${staffFixture.data.lastName} ${staffFixture.data.firstName}`

    await staffAttendancePage.assertPresentStaffCount(1)
    await staffAttendancePage.selectTab('present')
    await staffAttendancePage.openStaffPage(name)
    await staffAttendancePage.assertEmployeeStatus('Läsnä')
    await staffAttendancePage.clickStaffDepartedAndSetPin(pin)

    // Before planned end no type is required
    await staffAttendancePage.setDepartureTime('15:30')
    await staffAttendancePage.assertMarkDepartedButtonEnabled(true)

    // Departure time in the future is not allowed
    await staffAttendancePage.setDepartureTime('16:00')
    await staffAttendancePage.assertDepartureTimeInputInfo(
      'Oltava 15:30 tai aikaisemmin'
    )
    await staffAttendancePage.assertMarkDepartedButtonEnabled(false)
  })

  test('Staff departure page behaves correctly with different time values in the past when plan exists', async () => {
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

    await Fixture.realtimeStaffAttendance()
      .with({
        employeeId: staffFixture.data.id,
        groupId: daycareGroupFixture.id,
        arrived: HelsinkiDateTime.of(2022, 5, 5, 8, 0),
        departed: null,
        occupancyCoefficient: 0,
        type: 'PRESENT'
      })
      .save()

    // Now it is 16:30
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 16, 30).toSystemTzDate())

    const name = `${staffFixture.data.lastName} ${staffFixture.data.firstName}`

    await staffAttendancePage.assertPresentStaffCount(1)
    await staffAttendancePage.selectTab('present')
    await staffAttendancePage.openStaffPage(name)
    await staffAttendancePage.assertEmployeeStatus('Läsnä')
    await staffAttendancePage.clickStaffDepartedAndSetPin(pin)

    // Departure is possible now
    await staffAttendancePage.setDepartureTime('16:30')
    await staffAttendancePage.assertMarkDepartedButtonEnabled(true)

    // Departure is possible when the plan ends
    await staffAttendancePage.setDepartureTime('16:00')
    await staffAttendancePage.assertMarkDepartedButtonEnabled(true)
    await staffAttendancePage.assertDepartureTypeVisible(
      'JUSTIFIED_CHANGE',
      false
    )

    // More than 5 min from the plan -> ask for reason
    await staffAttendancePage.setDepartureTime('15:54')
    await staffAttendancePage.assertMarkDepartedButtonEnabled(true)
    await staffAttendancePage.assertDepartureTypeVisible(
      'JUSTIFIED_CHANGE',
      true
    )
  })

  test('Staff departs in the middle of the planned day', async () => {
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

    await Fixture.realtimeStaffAttendance()
      .with({
        employeeId: staffFixture.data.id,
        groupId: daycareGroupFixture.id,
        arrived: HelsinkiDateTime.of(2022, 5, 5, 8, 2),
        departed: null,
        occupancyCoefficient: 0,
        type: 'PRESENT'
      })
      .save()

    // Now it is 14:02
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 14, 2).toSystemTzDate())
    const name = `${staffFixture.data.lastName} ${staffFixture.data.firstName}`
    await staffAttendancePage.assertPresentStaffCount(1)
    await staffAttendancePage.selectTab('present')
    await staffAttendancePage.openStaffPage(name)
    await staffAttendancePage.assertEmployeeStatus('Läsnä')
    await staffAttendancePage.clickStaffDepartedAndSetPin(pin)

    await staffAttendancePage.setDepartureTime('14:02')
    await staffAttendancePage.assertMarkDepartedButtonEnabled(true)
    await staffAttendancePage.selectAttendanceType('OTHER_WORK')

    await staffAttendancePage.clickMarkDepartedButton()
    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.assertShiftTimeTextShown(
      'Työvuoro tänään 08:00–16:00'
    )
    await staffAttendancePage.assertAttendanceTimeTextShown(
      'Paikalla 08:02–14:02,Työasia 14:02–'
    )
  })

  test('Staff departs to non work in the middle of the planned day and comes back later', async () => {
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

    await Fixture.realtimeStaffAttendance()
      .with({
        employeeId: staffFixture.data.id,
        groupId: daycareGroupFixture.id,
        arrived: HelsinkiDateTime.of(2022, 5, 5, 8, 2),
        departed: null,
        occupancyCoefficient: 0,
        type: 'PRESENT'
      })
      .save()

    // Now it is 14:02
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 14, 2).toSystemTzDate())
    const name = `${staffFixture.data.lastName} ${staffFixture.data.firstName}`
    await staffAttendancePage.assertPresentStaffCount(1)
    await staffAttendancePage.selectTab('present')
    await staffAttendancePage.openStaffPage(name)
    await staffAttendancePage.assertEmployeeStatus('Läsnä')
    await staffAttendancePage.clickStaffDepartedAndSetPin(pin)

    await staffAttendancePage.setDepartureTime('14:02')
    await staffAttendancePage.assertMarkDepartedButtonEnabled(true)

    await staffAttendancePage.clickMarkDepartedButton()
    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.assertShiftTimeTextShown(
      'Työvuoro tänään 08:00–16:00'
    )
    await staffAttendancePage.assertAttendanceTimeTextShown(
      'Paikalla 08:02–14:02'
    )

    await initPages(HelsinkiDateTime.of(2022, 5, 5, 15, 0).toSystemTzDate())
    await staffAttendancePage.openStaffPage(name)
    await staffAttendancePage.clickStaffArrivedAndSetPin(pin)

    await staffAttendancePage.setArrivalTime('15:00')
    await staffAttendancePage.selectGroup(daycareGroupFixture.id)
    await staffAttendancePage.assertDoneButtonEnabled(true)
    await staffAttendancePage.clickDoneButton()
    await staffAttendancePage.assertAttendanceTimeTextShown(
      'Paikalla 08:02–14:02,Paikalla 15:00–'
    )
  })

  test('Staff has been in training and arrives and then departs in the middle of the planned day', async () => {
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

    await initPages(HelsinkiDateTime.of(2022, 5, 5, 12, 4).toSystemTzDate())
    const name = `${staffFixture.data.lastName} ${staffFixture.data.firstName}`
    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.selectTab('absent')
    await staffAttendancePage.openStaffPage(name)
    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.clickStaffArrivedAndSetPin(pin)

    await staffAttendancePage.setArrivalTime('12:04')
    await staffAttendancePage.selectGroup(daycareGroupFixture.id)
    await staffAttendancePage.assertDoneButtonEnabled(true)
    await staffAttendancePage.selectAttendanceType('TRAINING')
    await staffAttendancePage.clickDoneButton()
    await staffAttendancePage.assertShiftTimeTextShown(
      'Työvuoro tänään 08:00–16:00'
    )
    await staffAttendancePage.assertAttendanceTimeTextShown(
      'Koulutus 08:00–12:04,Paikalla 12:04–'
    )

    // Clock is now 14:02
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 14, 2).toSystemTzDate())
    await staffAttendancePage.assertPresentStaffCount(1)
    await staffAttendancePage.selectTab('present')
    await staffAttendancePage.openStaffPage(
      `${staffFixture.data.lastName} ${staffFixture.data.firstName}`
    )
    await staffAttendancePage.assertEmployeeStatus('Läsnä')
    await staffAttendancePage.clickStaffDepartedAndSetPin(pin)

    await staffAttendancePage.setDepartureTime('14:02')
    await staffAttendancePage.assertMarkDepartedButtonEnabled(true)
    await staffAttendancePage.selectAttendanceType('OTHER_WORK')
    await staffAttendancePage.clickMarkDepartedButton()
    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.assertShiftTimeTextShown(
      'Työvuoro tänään 08:00–16:00'
    )
    await staffAttendancePage.assertAttendanceTimeTextShown(
      'Koulutus 08:00–12:04,Paikalla 12:04–14:02,Työasia 14:02–'
    )
  })

  test('New external staff member can be added and marked as departed', async () => {
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 16, 0).toSystemTzDate())

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
  test('New external staff member cannot be added on a non-operational day', async () => {
    const saturday = LocalDate.of(2022, 5, 7)
    await initPages(
      HelsinkiDateTime.fromLocal(saturday, LocalTime.of(16, 0)).toSystemTzDate()
    )

    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.assertMarkNewExternalStaffDisabled()
  })
})
