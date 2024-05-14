// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'

import {
  careAreaFixture,
  daycareFixture,
  DaycareGroupBuilder,
  daycareGroupFixture,
  EmployeeBuilder,
  enduserChildFixtureJari,
  enduserChildFixtureKaarina,
  Fixture
} from '../../dev-api/fixtures'
import {
  createDefaultServiceNeedOptions,
  postAttendances,
  postReservations,
  resetServiceState
} from '../../generated/api-clients'
import { UnitPage } from '../../pages/employee/units/unit'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let unitPage: UnitPage

const today = LocalDate.of(2023, 3, 1)
let group: DaycareGroupBuilder
let unitSupervisor: EmployeeBuilder

beforeEach(async () => {
  await resetServiceState()

  await createDefaultServiceNeedOptions()
  const careArea = await Fixture.careArea().with(careAreaFixture).save()
  await Fixture.daycare().with(daycareFixture).careArea(careArea).save()
  await Fixture.person().with(enduserChildFixtureKaarina).save()
  await Fixture.child(enduserChildFixtureKaarina.id).save()
  group = await Fixture.daycareGroup().with(daycareGroupFixture).save()

  unitSupervisor = await Fixture.employeeUnitSupervisor(
    daycareFixture.id
  ).save()

  page = await Page.open({
    mockedTime: today.toHelsinkiDateTime(LocalTime.of(8, 0)),
    employeeCustomizations: { featureFlags: { timeUsageInfo: true } }
  })
  await employeeLogin(page, unitSupervisor.data)
  unitPage = new UnitPage(page)
})

describe('Employee - Unit month calendar', () => {
  test('Child is not shown in calendar for term break days', async () => {
    const term = new FiniteDateRange(today, today.addYears(1))
    const monday = LocalDate.of(2023, 3, 6)
    const termBreak = new FiniteDateRange(monday.addDays(1), monday.addDays(3))
    await Fixture.preschoolTerm()
      .with({
        finnishPreschool: term,
        swedishPreschool: term,
        applicationPeriod: term,
        extendedTerm: term,
        termBreaks: [termBreak]
      })
      .save()

    const placement = await Fixture.placement()
      .with({
        childId: enduserChildFixtureKaarina.id,
        unitId: daycareFixture.id,
        type: 'PRESCHOOL',
        startDate: today,
        endDate: today.addYears(1)
      })
      .save()
    await Fixture.groupPlacement()
      .withPlacement(placement)
      .withGroup(group)
      .save()

    await unitPage.navigateToUnit(daycareFixture.id)
    const groupsPage = await unitPage.openGroupsPage()

    const groupSection = await groupsPage.openGroupCollapsible(
      daycareGroupFixture.id
    )
    const monthCalendarPage = await groupSection.openMonthCalendar()

    // Monday is visible
    await monthCalendarPage
      .absenceCell(enduserChildFixtureKaarina.id, monday)
      .waitUntilVisible()

    // Tue-Thu are term break days
    for (const date of termBreak.dates()) {
      await monthCalendarPage
        .absenceCell(enduserChildFixtureKaarina.id, date)
        .waitUntilHidden()
    }

    // Fri is visible
    await monthCalendarPage
      .absenceCell(enduserChildFixtureKaarina.id, monday.addDays(4))
      .waitUntilVisible()
  })

  test('User can open the month calendar and add an absence for a child with only one absence category', async () => {
    const placement = await Fixture.placement()
      .with({
        childId: enduserChildFixtureKaarina.id,
        unitId: daycareFixture.id,
        startDate: today,
        endDate: today.addYears(1)
      })
      .save()
    await Fixture.groupPlacement()
      .withPlacement(placement)
      .withGroup(group)
      .save()

    await unitPage.navigateToUnit(daycareFixture.id)
    const groupsPage = await unitPage.openGroupsPage()

    const groupSection = await groupsPage.openGroupCollapsible(
      daycareGroupFixture.id
    )
    const monthCalendarPage = await groupSection.openMonthCalendar()

    // Can add an absence
    await monthCalendarPage.addAbsenceToChild(
      enduserChildFixtureKaarina.id,
      today,
      'SICKLEAVE'
    )
    await monthCalendarPage.childHasAbsence(
      enduserChildFixtureKaarina.id,
      today,
      'SICKLEAVE',
      'BILLABLE'
    )

    // Can change the absence type
    await monthCalendarPage.addAbsenceToChild(
      enduserChildFixtureKaarina.id,
      today,
      'UNKNOWN_ABSENCE'
    )
    await monthCalendarPage.childHasAbsence(
      enduserChildFixtureKaarina.id,
      today,
      'UNKNOWN_ABSENCE',
      'BILLABLE'
    )

    // Hover shows type and who is the absence maker
    await monthCalendarPage.assertTooltipContains(
      enduserChildFixtureKaarina.id,
      today,
      [
        'Varhaiskasvatus: Ilmoittamaton poissaolo',
        `${LocalDate.todayInSystemTz().formatIso()} Henkilökunta)`
      ]
    )

    // Can clear an absence
    await monthCalendarPage.addAbsenceToChild(
      enduserChildFixtureKaarina.id,
      today,
      'NO_ABSENCE'
    )
    await monthCalendarPage.childHasNoAbsence(
      enduserChildFixtureKaarina.id,
      today,
      'BILLABLE'
    )
  })

  test('User can open the month calendar and add an absence for a child with two absence categories', async () => {
    const placement = await Fixture.placement()
      .with({
        childId: enduserChildFixtureKaarina.id,
        unitId: daycareFixture.id,
        type: 'PRESCHOOL_DAYCARE',
        startDate: today,
        endDate: today.addYears(1)
      })
      .save()
    await Fixture.groupPlacement()
      .withPlacement(placement)
      .withGroup(group)
      .save()

    await unitPage.navigateToUnit(daycareFixture.id)
    const groupsPage = await unitPage.openGroupsPage()
    const groupSection = await groupsPage.openGroupCollapsible(
      daycareGroupFixture.id
    )
    const monthCalendarPage = await groupSection.openMonthCalendar()

    // Can add an absence
    await monthCalendarPage.addAbsenceToChild(
      enduserChildFixtureKaarina.id,
      today,
      'SICKLEAVE',
      ['BILLABLE']
    )
    await monthCalendarPage.childHasAbsence(
      enduserChildFixtureKaarina.id,
      today,
      'SICKLEAVE',
      'BILLABLE'
    )

    // Can change the absence type
    await monthCalendarPage.addAbsenceToChild(
      enduserChildFixtureKaarina.id,
      today,
      'UNKNOWN_ABSENCE',
      ['NONBILLABLE']
    )
    await monthCalendarPage.childHasAbsence(
      enduserChildFixtureKaarina.id,
      today,
      'UNKNOWN_ABSENCE',
      'NONBILLABLE'
    )

    // Hover shows type and who is the absence maker
    await monthCalendarPage.assertTooltipContains(
      enduserChildFixtureKaarina.id,
      today,
      [
        'Varhaiskasvatus: Ilmoittamaton poissaolo',
        `${LocalDate.todayInSystemTz().formatIso()} Henkilökunta)`
      ]
    )

    // Can clear an absence
    await monthCalendarPage.addAbsenceToChild(
      enduserChildFixtureKaarina.id,
      today,
      'NO_ABSENCE',
      ['BILLABLE', 'NONBILLABLE']
    )
    await monthCalendarPage.childHasNoAbsence(
      enduserChildFixtureKaarina.id,
      today,
      'BILLABLE'
    )
    await monthCalendarPage.childHasNoAbsence(
      enduserChildFixtureKaarina.id,
      today,
      'NONBILLABLE'
    )
  })

  test('User can add a staff attendance', async () => {
    await unitPage.navigateToUnit(daycareFixture.id)
    const groupsPage = await unitPage.openGroupsPage()

    const groupSection = await groupsPage.openGroupCollapsible(
      daycareGroupFixture.id
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
      const kaarinaPlacement = await Fixture.placement()
        .with({
          childId: enduserChildFixtureKaarina.id,
          unitId: daycareFixture.id,
          type: 'PRESCHOOL_DAYCARE',
          startDate: today,
          endDate: today.addYears(1)
        })
        .save()
      await Fixture.groupPlacement()
        .withPlacement(kaarinaPlacement)
        .withGroup(group)
        .save()
    })

    test('Missing holiday reservations are shown for holiday period dates that have no reservation or absence', async () => {
      await Fixture.holidayPeriod()
        .with({
          period: holidayRange,
          reservationDeadline: today.addWeeks(2)
        })
        .save()

      await unitPage.navigateToUnit(daycareFixture.id)
      const groupsPage = await unitPage.openGroupsPage()
      const groupSection = await groupsPage.openGroupCollapsible(
        daycareGroupFixture.id
      )
      const monthCalendarPage = await groupSection.openMonthCalendar()

      // Today is not in a holiday period
      await monthCalendarPage
        .absenceCell(enduserChildFixtureKaarina.id, today)
        .waitUntilVisible()
      await monthCalendarPage
        .absenceCell(enduserChildFixtureKaarina.id, today)
        .missingHolidayReservation.waitUntilHidden()

      // Missing holiday reservation is shown for holiday period dates
      await monthCalendarPage.nextWeekButton.click()
      let date = holidayStart
      while (date <= holidayEnd) {
        await monthCalendarPage
          .absenceCell(enduserChildFixtureKaarina.id, date)
          .missingHolidayReservation.waitUntilVisible()
        date = date.addDays(1)
      }

      // Adding an absence hides the missing holiday reservation marker
      await monthCalendarPage.addAbsenceToChild(
        enduserChildFixtureKaarina.id,
        holidayStart,
        'UNKNOWN_ABSENCE',
        ['NONBILLABLE']
      )
      await monthCalendarPage
        .absenceCell(enduserChildFixtureKaarina.id, holidayStart)
        .waitUntilVisible()
      await monthCalendarPage
        .absenceCell(enduserChildFixtureKaarina.id, holidayStart)
        .missingHolidayReservation.waitUntilHidden()
    })
  })

  test('Total reservations and attendances/used service', async () => {
    const serviceNeedOption140h = await Fixture.serviceNeedOption()
      .with({ daycareHoursPerMonth: 140 })
      .save()
    const serviceNeedOptionDaycare35 = await Fixture.serviceNeedOption()
      .with({ daycareHoursPerWeek: 35 })
      .save()

    const placementStart = today.addMonths(-1)
    const placementEnd = today.addMonths(1)

    const kaarinaPlacement = await Fixture.placement()
      .with({
        childId: enduserChildFixtureKaarina.id,
        unitId: daycareFixture.id,
        type: 'DAYCARE',
        startDate: placementStart,
        endDate: placementEnd
      })
      .save()
    await Fixture.serviceNeed()
      .with({
        placementId: kaarinaPlacement.data.id,
        optionId: serviceNeedOption140h.data.id,
        startDate: placementStart,
        endDate: placementEnd,
        confirmedBy: unitSupervisor.data.id
      })
      .save()
    await Fixture.groupPlacement()
      .withPlacement(kaarinaPlacement)
      .withGroup(group)
      .save()

    await Fixture.person().with(enduserChildFixtureJari).save()
    await Fixture.child(enduserChildFixtureJari.id).save()
    const jariPlacement = await Fixture.placement()
      .with({
        childId: enduserChildFixtureJari.id,
        unitId: daycareFixture.id,
        type: 'DAYCARE',
        startDate: placementStart,
        endDate: placementEnd
      })
      .save()
    await Fixture.serviceNeed()
      .with({
        placementId: jariPlacement.data.id,
        optionId: serviceNeedOptionDaycare35.data.id,
        startDate: placementStart,
        endDate: placementEnd,
        confirmedBy: unitSupervisor.data.id
      })
      .save()
    await Fixture.groupPlacement()
      .withPlacement(jariPlacement)
      .withGroup(group)
      .save()

    const lastMonthWeekdays = [
      ...new FiniteDateRange(today.addMonths(-1), today.addDays(-1)).dates()
    ].filter((date) => !date.isWeekend())
    await postReservations({
      body: lastMonthWeekdays.flatMap((date) =>
        [enduserChildFixtureKaarina.id, enduserChildFixtureJari.id].map(
          (childId) => ({
            childId,
            date,
            type: 'RESERVATIONS',
            reservation: new TimeRange(LocalTime.of(8, 0), LocalTime.of(16, 0)),
            secondReservation: null
          })
        )
      )
    })
    await postAttendances({
      body: lastMonthWeekdays.flatMap((date) =>
        date.isWeekend()
          ? []
          : [enduserChildFixtureKaarina.id, enduserChildFixtureJari.id].map(
              (childId) => ({
                childId,
                unitId: daycareFixture.id,
                date,
                arrived: LocalTime.of(8, 15),
                departed: LocalTime.of(16, 30)
              })
            )
      )
    })

    await unitPage.navigateToUnit(daycareFixture.id)
    const groupsPage = await unitPage.openGroupsPage()
    const groupSection = await groupsPage.openGroupCollapsible(
      daycareGroupFixture.id
    )
    const monthCalendarPage = await groupSection.openMonthCalendar()
    await monthCalendarPage.previousWeekButton.click()
    await monthCalendarPage.assertChildTotalHours(
      enduserChildFixtureKaarina.id,
      {
        reservedHours: 160,
        reservedHoursWarning: true,
        usedHours: 170,
        usedHoursWarning: true
      }
    )
    await monthCalendarPage.assertChildTotalHours(enduserChildFixtureJari.id, {
      reservedHours: 160,
      reservedHoursWarning: false,
      usedHours: 165,
      usedHoursWarning: true
    })
  })
})
