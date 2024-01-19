// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { insertDefaultServiceNeedOptions, resetDatabase } from '../../dev-api'
import {
  careAreaFixture,
  daycareFixture,
  DaycareGroupBuilder,
  daycareGroupFixture,
  enduserChildFixtureKaarina,
  Fixture
} from '../../dev-api/fixtures'
import { UnitPage } from '../../pages/employee/units/unit'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let unitPage: UnitPage

const today = LocalDate.of(2023, 3, 1)
let group: DaycareGroupBuilder

beforeEach(async () => {
  await resetDatabase()

  await insertDefaultServiceNeedOptions()
  const careArea = await Fixture.careArea().with(careAreaFixture).save()
  await Fixture.daycare().with(daycareFixture).careArea(careArea).save()
  await Fixture.person().with(enduserChildFixtureKaarina).save()
  await Fixture.child(enduserChildFixtureKaarina.id).save()
  group = await Fixture.daycareGroup().with(daycareGroupFixture).save()

  const unitSupervisor = await Fixture.employeeUnitSupervisor(
    daycareFixture.id
  ).save()

  page = await Page.open({
    mockedTime: today.toHelsinkiDateTime(LocalTime.of(8, 0))
  })
  await employeeLogin(page, unitSupervisor.data)
  unitPage = new UnitPage(page)
})

describe('Employee - Absences', () => {
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
})
