// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'

import {
  testCareArea,
  testDaycare,
  testDaycareGroup,
  testChild,
  testChild2,
  Fixture
} from '../../dev-api/fixtures'
import {
  createDefaultServiceNeedOptions,
  postAttendances,
  postReservations,
  resetServiceState
} from '../../generated/api-clients'
import { DevDaycareGroup, DevEmployee } from '../../generated/api-types'
import { UnitPage } from '../../pages/employee/units/unit'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let unitPage: UnitPage

const today = LocalDate.of(2023, 3, 1)
let group: DevDaycareGroup
let unitSupervisor: DevEmployee

beforeEach(async () => {
  await resetServiceState()

  await createDefaultServiceNeedOptions()
  const careArea = await Fixture.careArea(testCareArea).save()
  await Fixture.daycare({ ...testDaycare, areaId: careArea.id }).save()
  await Fixture.person(testChild2).saveChild()
  group = await Fixture.daycareGroup(testDaycareGroup).save()

  unitSupervisor = await Fixture.employee()
    .unitSupervisor(testDaycare.id)
    .save()

  page = await Page.open({
    mockedTime: today.toHelsinkiDateTime(LocalTime.of(8, 0))
  })
  await employeeLogin(page, unitSupervisor)
  unitPage = new UnitPage(page)
})

describe('Employee - Unit month calendar', () => {
  test('Child is not shown in calendar for term break days', async () => {
    const term = new FiniteDateRange(today, today.addYears(1))
    const monday = LocalDate.of(2023, 3, 6)
    const termBreak = new FiniteDateRange(monday.addDays(1), monday.addDays(3))
    await Fixture.preschoolTerm({
      finnishPreschool: term,
      swedishPreschool: term,
      applicationPeriod: term,
      extendedTerm: term,
      termBreaks: [termBreak]
    }).save()

    const placement = await Fixture.placement({
      childId: testChild2.id,
      unitId: testDaycare.id,
      type: 'PRESCHOOL',
      startDate: today,
      endDate: today.addYears(1)
    }).save()
    await Fixture.groupPlacement({
      daycarePlacementId: placement.id,
      daycareGroupId: group.id,
      startDate: placement.startDate,
      endDate: placement.endDate
    }).save()

    await unitPage.navigateToUnit(testDaycare.id)
    const groupsPage = await unitPage.openGroupsPage()

    const groupSection = await groupsPage.openGroupCollapsible(
      testDaycareGroup.id
    )
    const monthCalendarPage = await groupSection.openMonthCalendar()

    // Monday is visible
    await monthCalendarPage
      .absenceCell(testChild2.id, monday)
      .waitUntilVisible()

    // Tue-Thu are term break days
    for (const date of termBreak.dates()) {
      await monthCalendarPage.absenceCell(testChild2.id, date).waitUntilHidden()
    }

    // Fri is visible
    await monthCalendarPage
      .absenceCell(testChild2.id, monday.addDays(4))
      .waitUntilVisible()
  })

  test('User can open the month calendar and add an absence for a child with only one absence category', async () => {
    const placement = await Fixture.placement({
      childId: testChild2.id,
      unitId: testDaycare.id,
      startDate: today,
      endDate: today.addYears(1)
    }).save()
    await Fixture.groupPlacement({
      daycarePlacementId: placement.id,
      daycareGroupId: group.id,
      startDate: placement.startDate,
      endDate: placement.endDate
    }).save()

    await unitPage.navigateToUnit(testDaycare.id)
    const groupsPage = await unitPage.openGroupsPage()

    const groupSection = await groupsPage.openGroupCollapsible(
      testDaycareGroup.id
    )
    const monthCalendarPage = await groupSection.openMonthCalendar()

    // Can add an absence
    await monthCalendarPage.addAbsenceToChild(testChild2.id, today, 'SICKLEAVE')
    await monthCalendarPage.childHasAbsence(
      testChild2.id,
      today,
      'SICKLEAVE',
      'BILLABLE'
    )

    // Can change the absence type
    await monthCalendarPage.addAbsenceToChild(
      testChild2.id,
      today,
      'UNKNOWN_ABSENCE'
    )
    await monthCalendarPage.childHasAbsence(
      testChild2.id,
      today,
      'UNKNOWN_ABSENCE',
      'BILLABLE'
    )

    // Hover shows type and who is the absence maker
    await monthCalendarPage.assertTooltipContains(testChild2.id, today, [
      'Varhaiskasvatus: Ilmoittamaton poissaolo',
      `${LocalDate.todayInSystemTz().formatIso()} Henkilökunta)`
    ])

    // Can clear an absence
    await monthCalendarPage.addAbsenceToChild(
      testChild2.id,
      today,
      'NO_ABSENCE'
    )
    await monthCalendarPage.childHasNoAbsence(testChild2.id, today, 'BILLABLE')
  })

  test('User can open the month calendar and add an absence for a child with two absence categories', async () => {
    const placement = await Fixture.placement({
      childId: testChild2.id,
      unitId: testDaycare.id,
      type: 'PRESCHOOL_DAYCARE',
      startDate: today,
      endDate: today.addYears(1)
    }).save()
    await Fixture.groupPlacement({
      daycarePlacementId: placement.id,
      daycareGroupId: group.id,
      startDate: placement.startDate,
      endDate: placement.endDate
    }).save()

    await unitPage.navigateToUnit(testDaycare.id)
    const groupsPage = await unitPage.openGroupsPage()
    const groupSection = await groupsPage.openGroupCollapsible(
      testDaycareGroup.id
    )
    const monthCalendarPage = await groupSection.openMonthCalendar()

    // Can add an absence
    await monthCalendarPage.addAbsenceToChild(
      testChild2.id,
      today,
      'SICKLEAVE',
      ['BILLABLE']
    )
    await monthCalendarPage.childHasAbsence(
      testChild2.id,
      today,
      'SICKLEAVE',
      'BILLABLE'
    )

    // Can change the absence type
    await monthCalendarPage.addAbsenceToChild(
      testChild2.id,
      today,
      'UNKNOWN_ABSENCE',
      ['NONBILLABLE']
    )
    await monthCalendarPage.childHasAbsence(
      testChild2.id,
      today,
      'UNKNOWN_ABSENCE',
      'NONBILLABLE'
    )

    // Hover shows type and who is the absence maker
    await monthCalendarPage.assertTooltipContains(testChild2.id, today, [
      'Varhaiskasvatus: Ilmoittamaton poissaolo',
      `${LocalDate.todayInSystemTz().formatIso()} Henkilökunta)`
    ])

    // Can clear an absence
    await monthCalendarPage.addAbsenceToChild(
      testChild2.id,
      today,
      'NO_ABSENCE',
      ['BILLABLE', 'NONBILLABLE']
    )
    await monthCalendarPage.childHasNoAbsence(testChild2.id, today, 'BILLABLE')
    await monthCalendarPage.childHasNoAbsence(
      testChild2.id,
      today,
      'NONBILLABLE'
    )
  })

  test('User can add a staff attendance', async () => {
    await unitPage.navigateToUnit(testDaycare.id)
    const groupsPage = await unitPage.openGroupsPage()

    const groupSection = await groupsPage.openGroupCollapsible(
      testDaycareGroup.id
    )
    const monthCalendarPage = await groupSection.openMonthCalendar()

    await monthCalendarPage.fillStaffAttendance(0, 3)

    // Change to another page and back to reload data
    await unitPage.openGroupsPage()
    await groupSection.openMonthCalendar()

    await monthCalendarPage.assertStaffAttendance(0, 3)
  })

  describe('Holiday period reservations', () => {
    const holidayStart = today.addMonths(1).addDays(2) // Monday
    const holidayEnd = holidayStart.addDays(4) // Friday
    const holidayRange = new FiniteDateRange(holidayStart, holidayEnd)

    beforeEach(async () => {
      const kaarinaPlacement = await Fixture.placement({
        childId: testChild2.id,
        unitId: testDaycare.id,
        type: 'PRESCHOOL_DAYCARE',
        startDate: today,
        endDate: today.addYears(1)
      }).save()
      await Fixture.groupPlacement({
        daycarePlacementId: kaarinaPlacement.id,
        daycareGroupId: group.id,
        startDate: kaarinaPlacement.startDate,
        endDate: kaarinaPlacement.endDate
      }).save()
    })

    test('Missing holiday reservations are shown for holiday period dates that have no reservation or absence', async () => {
      await Fixture.holidayPeriod({
        period: holidayRange,
        reservationDeadline: today.addWeeks(2)
      }).save()

      await unitPage.navigateToUnit(testDaycare.id)
      const groupsPage = await unitPage.openGroupsPage()
      const groupSection = await groupsPage.openGroupCollapsible(
        testDaycareGroup.id
      )
      const monthCalendarPage = await groupSection.openMonthCalendar()

      // Today is not in a holiday period
      await monthCalendarPage
        .absenceCell(testChild2.id, today)
        .waitUntilVisible()
      await monthCalendarPage
        .absenceCell(testChild2.id, today)
        .missingHolidayReservation.waitUntilHidden()

      // Missing holiday reservation is shown for holiday period dates
      await monthCalendarPage.nextWeekButton.click()
      let date = holidayStart
      while (date <= holidayEnd) {
        await monthCalendarPage
          .absenceCell(testChild2.id, date)
          .missingHolidayReservation.waitUntilVisible()
        date = date.addDays(1)
      }

      // Adding an absence hides the missing holiday reservation marker
      await monthCalendarPage.addAbsenceToChild(
        testChild2.id,
        holidayStart,
        'UNKNOWN_ABSENCE',
        ['NONBILLABLE']
      )
      await monthCalendarPage
        .absenceCell(testChild2.id, holidayStart)
        .waitUntilVisible()
      await monthCalendarPage
        .absenceCell(testChild2.id, holidayStart)
        .missingHolidayReservation.waitUntilHidden()
    })
  })

  test('Total reservations and attendances/used service', async () => {
    const serviceNeedOption140h = await Fixture.serviceNeedOption({
      daycareHoursPerMonth: 140
    }).save()
    const serviceNeedOptionDaycare35 = await Fixture.serviceNeedOption({
      daycareHoursPerWeek: 35
    }).save()

    const placementStart = today.addMonths(-1)
    const placementEnd = today.addMonths(1)

    const kaarinaPlacement = await Fixture.placement({
      childId: testChild2.id,
      unitId: testDaycare.id,
      type: 'DAYCARE',
      startDate: placementStart,
      endDate: placementEnd
    }).save()
    await Fixture.serviceNeed({
      placementId: kaarinaPlacement.id,
      optionId: serviceNeedOption140h.id,
      startDate: placementStart,
      endDate: placementEnd,
      confirmedBy: unitSupervisor.id
    }).save()
    await Fixture.groupPlacement({
      daycarePlacementId: kaarinaPlacement.id,
      daycareGroupId: group.id,
      startDate: kaarinaPlacement.startDate,
      endDate: kaarinaPlacement.endDate
    }).save()

    await Fixture.person(testChild).saveChild()
    const jariPlacement = await Fixture.placement({
      childId: testChild.id,
      unitId: testDaycare.id,
      type: 'DAYCARE',
      startDate: placementStart,
      endDate: placementEnd
    }).save()
    await Fixture.serviceNeed({
      placementId: jariPlacement.id,
      optionId: serviceNeedOptionDaycare35.id,
      startDate: placementStart,
      endDate: placementEnd,
      confirmedBy: unitSupervisor.id
    }).save()
    await Fixture.groupPlacement({
      daycarePlacementId: jariPlacement.id,
      daycareGroupId: group.id,
      startDate: jariPlacement.startDate,
      endDate: jariPlacement.endDate
    }).save()

    const lastMonthWeekdays = [
      ...new FiniteDateRange(today.addMonths(-1), today.addDays(-1)).dates()
    ].filter((date) => !date.isWeekend())
    await postReservations({
      body: lastMonthWeekdays.flatMap((date) =>
        [testChild2.id, testChild.id].map((childId) => ({
          childId,
          date,
          type: 'RESERVATIONS',
          reservation: new TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
          secondReservation: null
        }))
      )
    })
    await postAttendances({
      body: lastMonthWeekdays.flatMap((date) =>
        date.isWeekend()
          ? []
          : [testChild2.id, testChild.id].map((childId) => ({
              childId,
              unitId: testDaycare.id,
              date,
              arrived: LocalTime.of(8, 15),
              departed: LocalTime.of(16, 30)
            }))
      )
    })

    await unitPage.navigateToUnit(testDaycare.id)
    const groupsPage = await unitPage.openGroupsPage()
    const groupSection = await groupsPage.openGroupCollapsible(
      testDaycareGroup.id
    )
    const monthCalendarPage = await groupSection.openMonthCalendar()
    await monthCalendarPage.previousWeekButton.click()
    await monthCalendarPage.assertChildTotalHours(testChild2.id, {
      reservedHours: 160,
      reservedHoursWarning: true,
      usedHours: 170,
      usedHoursWarning: true
    })
    await monthCalendarPage.assertChildTotalHours(testChild.id, {
      reservedHours: 160,
      reservedHoursWarning: false,
      usedHours: 165,
      usedHoursWarning: true
    })
  })
})
