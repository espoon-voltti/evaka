// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { FeatureFlags } from 'lib-customizations/types'

import { startTest } from '../../browser'
import {
  testDaycare2,
  testDaycareGroup,
  Fixture,
  fullDayTimeRange,
  uuidv4,
  familyWithTwoGuardians
} from '../../dev-api/fixtures'
import {
  createDefaultServiceNeedOptions,
  getStaffAttendances
} from '../../generated/api-clients'
import { DevDaycareGroup, DevEmployee } from '../../generated/api-types'
import MobileNav from '../../pages/mobile/mobile-nav'
import {
  StaffAttendanceEditPage,
  StaffAttendancePage
} from '../../pages/mobile/staff-page'
import { pairMobileDevice } from '../../utils/mobile'
import { Page } from '../../utils/page'

let page: Page
let nav: MobileNav
let mobileSignupUrl: string
let staffAttendancePage: StaffAttendancePage
let staffFixture: DevEmployee
let employeeName: string

const pin = '4242'

const daycareGroup2Fixture: DevDaycareGroup = {
  ...testDaycareGroup,
  id: uuidv4(),
  name: 'Ryhmä 2'
}

beforeEach(async () => {
  await startTest()
  await Fixture.family(familyWithTwoGuardians).save()
  await createDefaultServiceNeedOptions()

  const area = await Fixture.careArea().save()
  await Fixture.daycare({
    ...testDaycare2,
    areaId: area.id,
    enabledPilotFeatures: ['REALTIME_STAFF_ATTENDANCE'],
    operationTimes: [
      fullDayTimeRange,
      fullDayTimeRange,
      fullDayTimeRange,
      fullDayTimeRange,
      fullDayTimeRange,
      null,
      null
    ],
    shiftCareOperationTimes: null,
    shiftCareOpenOnHolidays: false
  }).save()

  await Fixture.daycareGroup({
    ...testDaycareGroup,
    daycareId: testDaycare2.id
  }).save()
  await Fixture.daycareGroup({
    ...daycareGroup2Fixture,
    daycareId: testDaycare2.id
  }).save()
  const daycarePlacementFixture = await Fixture.placement({
    childId: familyWithTwoGuardians.children[0].id,
    unitId: testDaycare2.id
  }).save()
  await Fixture.groupPlacement({
    daycarePlacementId: daycarePlacementFixture.id,
    daycareGroupId: testDaycareGroup.id,
    startDate: daycarePlacementFixture.startDate,
    endDate: daycarePlacementFixture.endDate
  }).save()
  staffFixture = await Fixture.employee({
    preferredFirstName: 'Kutsumanimi'
  })
    .staff(testDaycare2.id)
    .withGroupAcl(testDaycareGroup.id)
    .withGroupAcl(daycareGroup2Fixture.id)
    .save()
  await Fixture.employeePin({ userId: staffFixture.id, pin }).save()
  employeeName = `${staffFixture.lastName} Kutsumanimi`
})

const initPages = async (
  mockedTime: HelsinkiDateTime,
  featureFlags: Partial<FeatureFlags> = {}
) => {
  page = await Page.open({
    mockedTime,
    employeeMobileCustomizations: { featureFlags }
  })
  nav = new MobileNav(page)

  mobileSignupUrl = await pairMobileDevice(testDaycare2.id)
  await page.goto(mobileSignupUrl)
  await nav.staff.click()
  staffAttendancePage = new StaffAttendancePage(page)
}

describe('Realtime staff attendance page', () => {
  test('Staff member can be marked as arrived and departed', async () => {
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 6, 0))
    const arrivalTime = '05:59'
    const departureTime = '12:45'

    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.selectTab('absent')
    await staffAttendancePage.openStaffPage(employeeName)

    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.markStaffArrived({
      pin,
      time: arrivalTime,
      group: testDaycareGroup,
      hasOccupancyEffect: true
    })
    await staffAttendancePage.assertEmployeeStatus('Läsnä')
    await staffAttendancePage.assertEmployeeAttendances([
      `Paikalla ${arrivalTime}–`
    ])

    await initPages(HelsinkiDateTime.of(2022, 5, 5, 13, 30))
    await staffAttendancePage.assertPresentStaffCount(1)

    await staffAttendancePage.selectTab('present')
    await staffAttendancePage.openStaffPage(employeeName)

    await staffAttendancePage.markStaffDeparted({ pin, time: departureTime })
    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.assertEmployeeAttendances([
      `Paikalla ${arrivalTime}–${departureTime}`
    ])
    await staffAttendancePage.goBackFromMemberPage()
    await staffAttendancePage.assertPresentStaffCount(0)
  })

  test('Occupancy effect can not be unchecked on arrival if it has been given permanently but can be edited', async () => {
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 6, 0))
    await Fixture.staffOccupancyCoefficient(
      testDaycare2.id,
      staffFixture.id
    ).save()
    const arrivalTime = '05:59'

    await staffAttendancePage.selectTab('absent')
    await staffAttendancePage.openStaffPage(employeeName)
    await staffAttendancePage.assertEmployeeStatus('Poissa')

    await staffAttendancePage.staffMemberPage.markArrivedBtn.click()
    await staffAttendancePage.pinInput.locator.type(pin)
    await staffAttendancePage.anyArrivalPage.arrivedInput.fill(arrivalTime)
    await staffAttendancePage.staffArrivalPage.groupSelect.selectOption(
      testDaycareGroup.id
    )
    await staffAttendancePage.staffArrivalPage.occupancyEffectCheckbox.waitUntilHidden()
    await staffAttendancePage.anyArrivalPage.markArrived.click()

    await staffAttendancePage.assertEmployeeStatus('Läsnä')
    await staffAttendancePage.editButton.click()

    const editPage = new StaffAttendanceEditPage(page)
    await editPage.occupancyEffect.waitUntilChecked(true)
    await editPage.occupancyEffect.uncheck()
    await editPage.submit(pin)

    await staffAttendancePage.editButton.click()
    await editPage.occupancyEffect.waitUntilChecked(false)
  })

  test('Staff member cannot use departure time that is before last arrival time', async () => {
    const arrivalTime = '05:59'
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 6, 0))

    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.selectTab('absent')
    await staffAttendancePage.openStaffPage(employeeName)

    await staffAttendancePage.markStaffArrived({
      pin,
      time: arrivalTime,
      group: testDaycareGroup
    })
    await staffAttendancePage.assertEmployeeStatus('Läsnä')
    await staffAttendancePage.assertEmployeeAttendances([
      `Paikalla ${arrivalTime}–`
    ])
    await staffAttendancePage.anyMemberPage.markDeparted.click()
    await staffAttendancePage.pinInput.locator.type(pin)

    for (const departureTime of ['05:58', '05:59']) {
      await staffAttendancePage.staffDeparturePage.departureTime.fill(
        departureTime
      )
      await staffAttendancePage.staffDeparturePage.departureIsBeforeArrival.waitUntilVisible()
      await staffAttendancePage.staffDeparturePage.departureIsBeforeArrival.assertText(
        (text) => text.endsWith(arrivalTime)
      )
      await staffAttendancePage.staffDeparturePage.markDepartedBtn.assertDisabled(
        true
      )
    }
  })

  test('Staff member cannot use arrival time that is before last departure time', async () => {
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 8, 0))

    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.selectTab('absent')
    await staffAttendancePage.openStaffPage(employeeName)

    const arrivalTime = '07:30'
    const departureTime = '07:55'
    await staffAttendancePage.markStaffArrived({
      pin,
      time: arrivalTime,
      group: testDaycareGroup
    })
    await staffAttendancePage.markStaffDeparted({
      pin,
      time: departureTime
    })

    await staffAttendancePage.staffMemberPage.markArrivedBtn.click()
    await staffAttendancePage.pinInput.locator.type(pin)

    await staffAttendancePage.anyArrivalPage.arrivedInput.fill('07:54')
    await staffAttendancePage.staffArrivalPage.groupSelect.selectOption(
      testDaycareGroup.id
    )
    await staffAttendancePage.staffArrivalPage.arrivalIsBeforeDeparture.waitUntilVisible()
    await staffAttendancePage.staffArrivalPage.arrivalIsBeforeDeparture.assertText(
      (text) => text.endsWith(departureTime)
    )
    await staffAttendancePage.anyArrivalPage.markArrived.assertDisabled(true)
  })

  test('Staff member can use arrival time that is equal to last departure time', async () => {
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 8, 0))

    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.selectTab('absent')
    await staffAttendancePage.openStaffPage(employeeName)

    const arrivalTime = '07:30'
    const departureTime = '07:55'
    await staffAttendancePage.markStaffArrived({
      pin,
      time: arrivalTime,
      group: testDaycareGroup
    })
    await staffAttendancePage.markStaffDeparted({
      pin,
      time: departureTime
    })

    await staffAttendancePage.markStaffArrived({
      pin,
      time: departureTime,
      group: testDaycareGroup
    })
    await staffAttendancePage.assertEmployeeStatus('Läsnä')
    await staffAttendancePage.assertEmployeeAttendances([
      `Paikalla ${arrivalTime}–${departureTime}`,
      `Paikalla ${departureTime}–`
    ])
  })

  test('Staff member cannot be marked as arrived on a non-operational day', async () => {
    const saturday = LocalDate.of(2022, 5, 7)
    await initPages(HelsinkiDateTime.fromLocal(saturday, LocalTime.of(16, 0)))

    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.selectTab('absent')
    await staffAttendancePage.openStaffPage(employeeName)

    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.staffMemberPage.markArrivedBtn.assertDisabled(
      true
    )
  })

  test('Staff arrival page behaves correctly with different time values when no plan exists', async () => {
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 12, 0))

    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.selectTab('absent')
    await staffAttendancePage.openStaffPage(employeeName)

    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.clickStaffArrivedAndSetPin(pin)

    // Within 30min from now so ok
    await staffAttendancePage.setArrivalTime('12:30')
    await staffAttendancePage.selectGroup(testDaycareGroup.id)
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
    await Fixture.staffAttendancePlan({
      id: uuidv4(),
      employeeId: staffFixture.id,
      startTime: planStart,
      endTime: planEnd
    }).save()

    // Now it is 7:30
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 7, 30))

    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.selectTab('absent')
    await staffAttendancePage.openStaffPage(employeeName)

    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.clickStaffArrivedAndSetPin(pin)

    // Within 30min from planned start so ok, type required
    await staffAttendancePage.setArrivalTime('07:30')
    await staffAttendancePage.selectGroup(testDaycareGroup.id)
    await staffAttendancePage.assertDoneButtonEnabled(false)
    await staffAttendancePage.selectAttendanceType('JUSTIFIED_CHANGE')
    await staffAttendancePage.assertDoneButtonEnabled(true)

    // Within 5min from planned start so ok, type not required
    await staffAttendancePage.setArrivalTime('07:55')
    await staffAttendancePage.selectGroup(testDaycareGroup.id)
    await staffAttendancePage.assertDoneButtonEnabled(true)

    // Not ok because >+-30min from current time
    await staffAttendancePage.setArrivalTime('08:01')
    await staffAttendancePage.assertArrivalTimeInputInfo(
      'Oltava välillä 07:00-08:00'
    )
    // Not ok because >30min from current time
    await staffAttendancePage.setArrivalTime('06:59')
    await staffAttendancePage.assertArrivalTimeInputInfo(
      'Oltava välillä 07:00-08:00'
    )
  })

  test('Staff arrival page behaves correctly with different time values in the past when plan exists', async () => {
    // Planned attendance 08:00 - 16:00
    const planStart = HelsinkiDateTime.of(2022, 5, 5, 8, 0)
    const planEnd = HelsinkiDateTime.of(2022, 5, 5, 16, 0)
    await Fixture.staffAttendancePlan({
      id: uuidv4(),
      employeeId: staffFixture.id,
      startTime: planStart,
      endTime: planEnd
    }).save()

    // Now it is 8:30
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 8, 30))

    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.selectTab('absent')
    await staffAttendancePage.openStaffPage(employeeName)

    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.clickStaffArrivedAndSetPin(pin)

    // Within 5min from planned start so ok, type not required
    await staffAttendancePage.setArrivalTime('08:00')
    await staffAttendancePage.selectGroup(testDaycareGroup.id)
    await staffAttendancePage.assertDoneButtonEnabled(true)

    // More than 5min from planned start, type is not required but can be selected
    await staffAttendancePage.setArrivalTime('08:15')
    await staffAttendancePage.selectGroup(testDaycareGroup.id)
    await staffAttendancePage.assertDoneButtonEnabled(true)
    await staffAttendancePage.selectAttendanceType('JUSTIFIED_CHANGE')
    await staffAttendancePage.assertDoneButtonEnabled(true)

    // Not ok because >+-30min from current time
    await staffAttendancePage.setArrivalTime('07:59')
    await staffAttendancePage.assertArrivalTimeInputInfo(
      'Oltava välillä 08:00-09:00'
    )
  })

  test('Staff departure page behaves correctly with different time values in the future when plan exists', async () => {
    // Planned attendance 08:00 - 16:00
    const planStart = HelsinkiDateTime.of(2022, 5, 5, 8, 0)
    const planEnd = HelsinkiDateTime.of(2022, 5, 5, 16, 0)
    await Fixture.staffAttendancePlan({
      id: uuidv4(),
      employeeId: staffFixture.id,
      startTime: planStart,
      endTime: planEnd
    }).save()

    await Fixture.realtimeStaffAttendance({
      employeeId: staffFixture.id,
      groupId: testDaycareGroup.id,
      arrived: HelsinkiDateTime.of(2022, 5, 5, 8, 0),
      departed: null,
      occupancyCoefficient: 0,
      type: 'PRESENT'
    }).save()

    // Now it is 15:30
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 15, 30))

    await staffAttendancePage.assertPresentStaffCount(1)
    await staffAttendancePage.selectTab('present')
    await staffAttendancePage.openStaffPage(employeeName)

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
    await Fixture.staffAttendancePlan({
      id: uuidv4(),
      employeeId: staffFixture.id,
      startTime: planStart,
      endTime: planEnd
    }).save()

    await Fixture.realtimeStaffAttendance({
      employeeId: staffFixture.id,
      groupId: testDaycareGroup.id,
      arrived: HelsinkiDateTime.of(2022, 5, 5, 8, 0),
      departed: null,
      occupancyCoefficient: 0,
      type: 'PRESENT'
    }).save()

    // Now it is 16:30
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 16, 30))

    await staffAttendancePage.assertPresentStaffCount(1)
    await staffAttendancePage.selectTab('present')
    await staffAttendancePage.openStaffPage(employeeName)

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
    await Fixture.staffAttendancePlan({
      id: uuidv4(),
      employeeId: staffFixture.id,
      startTime: planStart,
      endTime: planEnd
    }).save()

    await Fixture.realtimeStaffAttendance({
      employeeId: staffFixture.id,
      groupId: testDaycareGroup.id,
      arrived: HelsinkiDateTime.of(2022, 5, 5, 8, 2),
      departed: null,
      occupancyCoefficient: 0,
      type: 'PRESENT'
    }).save()

    // Now it is 14:02
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 14, 2))
    await staffAttendancePage.assertPresentStaffCount(1)
    await staffAttendancePage.selectTab('present')
    await staffAttendancePage.openStaffPage(employeeName)

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
    await Fixture.staffAttendancePlan({
      id: uuidv4(),
      employeeId: staffFixture.id,
      startTime: planStart,
      endTime: planEnd
    }).save()

    await Fixture.realtimeStaffAttendance({
      employeeId: staffFixture.id,
      groupId: testDaycareGroup.id,
      arrived: HelsinkiDateTime.of(2022, 5, 5, 8, 2),
      departed: null,
      occupancyCoefficient: 0,
      type: 'PRESENT'
    }).save()

    // Now it is 14:02
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 14, 2))
    await staffAttendancePage.assertPresentStaffCount(1)
    await staffAttendancePage.selectTab('present')
    await staffAttendancePage.openStaffPage(employeeName)
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

    await initPages(HelsinkiDateTime.of(2022, 5, 5, 15, 0))
    await staffAttendancePage.openStaffPage(employeeName)

    await staffAttendancePage.clickStaffArrivedAndSetPin(pin)

    await staffAttendancePage.setArrivalTime('15:00')
    await staffAttendancePage.selectGroup(testDaycareGroup.id)
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
    await Fixture.staffAttendancePlan({
      id: uuidv4(),
      employeeId: staffFixture.id,
      startTime: planStart,
      endTime: planEnd
    }).save()

    await initPages(HelsinkiDateTime.of(2022, 5, 5, 12, 4))
    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.selectTab('absent')
    await staffAttendancePage.openStaffPage(employeeName)

    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.clickStaffArrivedAndSetPin(pin)

    await staffAttendancePage.setArrivalTime('12:04')
    await staffAttendancePage.selectGroup(testDaycareGroup.id)
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
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 14, 2))
    await staffAttendancePage.assertPresentStaffCount(1)
    await staffAttendancePage.selectTab('present')
    await staffAttendancePage.openStaffPage(employeeName)
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
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 16, 0))

    const name = 'Nomen Estomen'
    const arrivalTime = '03:20'

    await staffAttendancePage.assertPresentStaffCount(0)

    await staffAttendancePage.markNewExternalStaffArrived(
      arrivalTime,
      name,
      testDaycareGroup
    )
    await staffAttendancePage.assertPresentStaffCount(1)

    await staffAttendancePage.selectTab('present')
    await staffAttendancePage.openStaffPage(name)

    await staffAttendancePage.assertEmployeeStatus('Läsnä')
    await staffAttendancePage.assertExternalStaffArrivalTime(arrivalTime)
    await staffAttendancePage.markExternalStaffDeparted('11:09')
    await staffAttendancePage.assertPresentStaffCount(0)
  })

  test('External staff member cannot use departure time that is before arrival', async () => {
    await initPages(HelsinkiDateTime.of(2022, 5, 5, 16, 0))

    const name = 'Nomen Estomen'
    const arrivalTime = '08:00'
    const departureTime = '07:55'

    await staffAttendancePage.assertPresentStaffCount(0)

    await staffAttendancePage.markNewExternalStaffArrived(
      arrivalTime,
      name,
      testDaycareGroup
    )
    await staffAttendancePage.selectTab('present')
    await staffAttendancePage.openStaffPage(name)

    await staffAttendancePage.externalMemberPage.departureTimeInput.fill(
      departureTime
    )
    await staffAttendancePage.externalMemberPage.departureIsBeforeArrival.waitUntilVisible()
    await staffAttendancePage.externalMemberPage.departureIsBeforeArrival.assertText(
      (text) => text.endsWith(arrivalTime)
    )
    await staffAttendancePage.anyMemberPage.markDeparted.assertDisabled(true)
  })

  test('New external staff member cannot be added on a non-operational day', async () => {
    const saturday = LocalDate.of(2022, 5, 7)
    await initPages(HelsinkiDateTime.fromLocal(saturday, LocalTime.of(16, 0)))

    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.assertMarkNewExternalStaffDisabled()
  })
})

describe('Realtime staff attendance edit page', () => {
  test('Staff member can add new attendance with attendace types disabled', async () => {
    const date = LocalDate.of(2022, 5, 5)
    const arrivalTime = '05:59'
    const departureTime = '12:45'
    await Fixture.realtimeStaffAttendance({
      employeeId: staffFixture.id,
      groupId: testDaycareGroup.id,
      arrived: HelsinkiDateTime.fromLocal(date, LocalTime.parse(arrivalTime)),
      departed: HelsinkiDateTime.fromLocal(date, LocalTime.parse(departureTime))
    }).save()

    await initPages(HelsinkiDateTime.fromLocal(date, LocalTime.of(16, 0)), {
      staffAttendanceTypes: false
    })
    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.openStaffPage(employeeName)
    await staffAttendancePage.editButton.click()

    const newDepartureTime = '16:00'
    const editPage = new StaffAttendanceEditPage(page)
    await editPage.addLink.click()
    await editPage.addLink.waitUntilHidden()
    await editPage.fillDeparted(1, newDepartureTime)
    await editPage.addLink.waitUntilVisible()
    await editPage.submit(pin)

    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.arrivalTime.assertTextEquals(departureTime)
    await staffAttendancePage.departureTime.assertTextEquals(newDepartureTime)
  })

  test('Staff member can add new attendance with group', async () => {
    const date = LocalDate.of(2022, 5, 5)
    const arrivalTime = '05:59'
    const departureTime = '12:45'
    await Fixture.realtimeStaffAttendance({
      employeeId: staffFixture.id,
      groupId: testDaycareGroup.id,
      arrived: HelsinkiDateTime.fromLocal(date, LocalTime.parse(arrivalTime)),
      departed: HelsinkiDateTime.fromLocal(date, LocalTime.parse(departureTime))
    }).save()

    await initPages(HelsinkiDateTime.fromLocal(date, LocalTime.of(16, 0)))
    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.openStaffPage(employeeName)
    await staffAttendancePage.editButton.click()

    const newDepartureTime = '16:00'
    const editPage = new StaffAttendanceEditPage(page)
    await editPage.addLink.click()
    await editPage.addLink.waitUntilHidden()
    await editPage.fillDeparted(1, newDepartureTime)
    await editPage.addLink.waitUntilVisible()
    await editPage.submit(pin)

    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.assertEmployeeAttendances([
      `Paikalla ${arrivalTime}–${departureTime}`,
      `Paikalla ${departureTime}–${newDepartureTime}`
    ])
  })

  test('Staff member can add new attendance without group', async () => {
    const date = LocalDate.of(2022, 5, 5)
    const arrivalTime = '05:59'
    const departureTime = '12:45'
    await Fixture.realtimeStaffAttendance({
      employeeId: staffFixture.id,
      type: 'TRAINING',
      groupId: null,
      arrived: HelsinkiDateTime.fromLocal(date, LocalTime.parse(arrivalTime)),
      departed: HelsinkiDateTime.fromLocal(date, LocalTime.parse(departureTime))
    }).save()

    await initPages(HelsinkiDateTime.fromLocal(date, LocalTime.of(16, 0)))
    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.openStaffPage(employeeName)
    await staffAttendancePage.editButton.click()

    const newDepartureTime = '16:00'
    const editPage = new StaffAttendanceEditPage(page)
    await editPage.addLink.click()
    await editPage.addLink.waitUntilHidden()
    await editPage.fillDeparted(1, newDepartureTime)
    await editPage.addLink.waitUntilVisible()
    await editPage.submit(pin)

    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.assertEmployeeAttendances([
      `Koulutus ${arrivalTime}–${departureTime}`,
      `Koulutus ${departureTime}–${newDepartureTime}`
    ])
  })

  test('Staff member can edit existing attendance', async () => {
    const date = LocalDate.of(2022, 5, 5)
    const arrivalTime = '05:59'
    const departureTime = '12:45'
    await Fixture.realtimeStaffAttendance({
      employeeId: staffFixture.id,
      groupId: testDaycareGroup.id,
      arrived: HelsinkiDateTime.fromLocal(date, LocalTime.parse(arrivalTime)),
      departed: HelsinkiDateTime.fromLocal(date, LocalTime.parse(departureTime))
    }).save()

    await initPages(HelsinkiDateTime.fromLocal(date, LocalTime.of(16, 0)))
    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.openStaffPage(employeeName)
    await staffAttendancePage.editButton.click()

    const newArrivalTime = '06:00'
    const newDepartureTime = '12:46'
    const editPage = new StaffAttendanceEditPage(page)
    await editPage.selectGroup(0, daycareGroup2Fixture.id)
    await editPage.selectType(0, 'OVERTIME')
    await editPage.fillArrived(0, newArrivalTime)
    await editPage.fillDeparted(0, newDepartureTime)
    await editPage.submit(pin)

    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.assertEmployeeAttendances([
      `Ylityö ${newArrivalTime}–${newDepartureTime}`
    ])
    const attendances = await getStaffAttendances()
    expect(attendances).toHaveLength(1)
    expect(attendances[0].groupId).toBe(daycareGroup2Fixture.id)
  })

  test('Staff member can remove existing attendance', async () => {
    const date = LocalDate.of(2022, 5, 5)
    await Fixture.realtimeStaffAttendance({
      employeeId: staffFixture.id,
      groupId: testDaycareGroup.id,
      arrived: HelsinkiDateTime.fromLocal(date, LocalTime.of(5, 59)),
      departed: HelsinkiDateTime.fromLocal(date, LocalTime.of(12, 45))
    }).save()

    await initPages(HelsinkiDateTime.fromLocal(date, LocalTime.of(16, 0)))
    await staffAttendancePage.assertPresentStaffCount(0)
    await staffAttendancePage.openStaffPage(employeeName)
    await staffAttendancePage.editButton.click()

    const editPage = new StaffAttendanceEditPage(page)
    await editPage.remove(0)
    await editPage.submit(pin)

    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.assertEmployeeAttendances([])
  })

  test('Staff member can edit ongoing attendance from yesterday', async () => {
    const date = LocalDate.of(2023, 1, 24)
    const arrivalTime = '22:00'
    await Fixture.realtimeStaffAttendance({
      employeeId: staffFixture.id,
      groupId: testDaycareGroup.id,
      arrived: HelsinkiDateTime.fromLocal(
        date.subDays(1),
        LocalTime.parse(arrivalTime)
      ),
      departed: null
    }).save()

    await initPages(HelsinkiDateTime.fromLocal(date, LocalTime.of(2, 0)))
    await staffAttendancePage.assertPresentStaffCount(1)
    await staffAttendancePage.selectTab('present')
    await staffAttendancePage.openStaffPage(employeeName)
    await staffAttendancePage.editButton.click()

    const newDepartureTime = '02:00'
    const editPage = new StaffAttendanceEditPage(page)
    await editPage.fillDeparted(0, newDepartureTime)
    await editPage.submit(pin)

    await staffAttendancePage.assertEmployeeStatus('Poissa')
    await staffAttendancePage.assertEmployeeAttendances([
      `Paikalla ${arrivalTime}–${newDepartureTime}`
    ])
  })
})
