// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { insertDefaultServiceNeedOptions, resetDatabase } from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { daycareGroupFixture, Fixture } from '../../dev-api/fixtures'
import { UnitPage } from '../../pages/employee/units/unit'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let fixtures: AreaAndPersonFixtures
let page: Page
let unitPage: UnitPage

const today = LocalDate.of(2023, 3, 1)

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  await insertDefaultServiceNeedOptions()
  const group = await Fixture.daycareGroup().with(daycareGroupFixture).save()

  await Fixture.placement()
    .with({
      childId: fixtures.enduserChildFixtureJari.id,
      unitId: fixtures.daycareFixture.id,
      startDate: today,
      endDate: today.addYears(1)
    })
    .save()
  const kaarinaPlacement = await Fixture.placement()
    .with({
      childId: fixtures.enduserChildFixtureKaarina.id,
      unitId: fixtures.daycareFixture.id,
      type: 'PRESCHOOL_DAYCARE',
      startDate: today,
      endDate: today.addYears(1)
    })
    .save()
  await Fixture.groupPlacement()
    .withPlacement(kaarinaPlacement)
    .withGroup(group)
    .save()

  const unitSupervisor = await Fixture.employeeUnitSupervisor(
    fixtures.daycareFixture.id
  ).save()

  page = await Page.open({
    mockedTime: today.toHelsinkiDateTime(LocalTime.of(8, 0)).toSystemTzDate()
  })
  await employeeLogin(page, unitSupervisor.data)
  unitPage = new UnitPage(page)
})

describe('Employee - Absences', () => {
  test('User can place a child into a group and remove the child from the group', async () => {
    await unitPage.navigateToUnit(fixtures.daycareFixture.id)
    const groupsPage = await unitPage.openGroupsPage()

    const missingPlacementsSection = groupsPage.missingPlacementsSection
    await missingPlacementsSection.createGroupPlacementForChild(0)

    const groupSection = await groupsPage.openGroupCollapsible(
      daycareGroupFixture.id
    )

    await missingPlacementsSection.assertRowCount(0)
    await groupSection.assertChildCount(2)

    await groupSection.removeGroupPlacement(0)
    await missingPlacementsSection.assertRowCount(1)
    await groupSection.assertChildCount(1)
  })

  test('User can open the month calendar and add an absence for a child with only one absence category', async () => {
    await unitPage.navigateToUnit(fixtures.daycareFixture.id)
    const groupsPage = await unitPage.openGroupsPage()

    const missingPlacementsSection = groupsPage.missingPlacementsSection
    await missingPlacementsSection.createGroupPlacementForChild(0)

    const groupSection = await groupsPage.openGroupCollapsible(
      daycareGroupFixture.id
    )
    const monthCalendarPage = await groupSection.openMonthCalendar()

    // Can add an absence
    await monthCalendarPage.addAbsenceToChild(
      fixtures.enduserChildFixtureJari.id,
      today,
      'SICKLEAVE'
    )
    await monthCalendarPage.childHasAbsence(
      fixtures.enduserChildFixtureJari.id,
      today,
      'SICKLEAVE',
      'BILLABLE'
    )

    // Can change the absence type
    await monthCalendarPage.addAbsenceToChild(
      fixtures.enduserChildFixtureJari.id,
      today,
      'UNKNOWN_ABSENCE'
    )
    await monthCalendarPage.childHasAbsence(
      fixtures.enduserChildFixtureJari.id,
      today,
      'UNKNOWN_ABSENCE',
      'BILLABLE'
    )

    // Hover shows type and who is the absence maker
    await monthCalendarPage.assertTooltipContains(
      fixtures.enduserChildFixtureJari.id,
      today,
      [
        'Varhaiskasvatus: Ilmoittamaton poissaolo',
        `${LocalDate.todayInSystemTz().formatIso()} Henkilökunta)`
      ]
    )

    // Can clear an absence
    await monthCalendarPage.addAbsenceToChild(
      fixtures.enduserChildFixtureJari.id,
      today,
      'NO_ABSENCE'
    )
    await monthCalendarPage.childHasNoAbsence(
      fixtures.enduserChildFixtureJari.id,
      today,
      'BILLABLE'
    )
  })

  test('User can open the month calendar and add an absence for a child with two absence categories', async () => {
    await unitPage.navigateToUnit(fixtures.daycareFixture.id)
    const groupsPage = await unitPage.openGroupsPage()
    const groupSection = await groupsPage.openGroupCollapsible(
      daycareGroupFixture.id
    )
    const monthCalendarPage = await groupSection.openMonthCalendar()

    // Can add an absence
    await monthCalendarPage.addAbsenceToChild(
      fixtures.enduserChildFixtureKaarina.id,
      today,
      'SICKLEAVE',
      ['BILLABLE']
    )
    await monthCalendarPage.childHasAbsence(
      fixtures.enduserChildFixtureKaarina.id,
      today,
      'SICKLEAVE',
      'BILLABLE'
    )

    // Can change the absence type
    await monthCalendarPage.addAbsenceToChild(
      fixtures.enduserChildFixtureKaarina.id,
      today,
      'UNKNOWN_ABSENCE',
      ['NONBILLABLE']
    )
    await monthCalendarPage.childHasAbsence(
      fixtures.enduserChildFixtureKaarina.id,
      today,
      'UNKNOWN_ABSENCE',
      'NONBILLABLE'
    )

    // Hover shows type and who is the absence maker
    await monthCalendarPage.assertTooltipContains(
      fixtures.enduserChildFixtureKaarina.id,
      today,
      [
        'Varhaiskasvatus: Ilmoittamaton poissaolo',
        `${LocalDate.todayInSystemTz().formatIso()} Henkilökunta)`
      ]
    )

    // Can clear an absence
    await monthCalendarPage.addAbsenceToChild(
      fixtures.enduserChildFixtureKaarina.id,
      today,
      'NO_ABSENCE',
      ['BILLABLE', 'NONBILLABLE']
    )
    await monthCalendarPage.childHasNoAbsence(
      fixtures.enduserChildFixtureKaarina.id,
      today,
      'BILLABLE'
    )
    await monthCalendarPage.childHasNoAbsence(
      fixtures.enduserChildFixtureKaarina.id,
      today,
      'NONBILLABLE'
    )
  })

  test('User can add a staff attendance', async () => {
    await unitPage.navigateToUnit(fixtures.daycareFixture.id)
    const groupsPage = await unitPage.openGroupsPage()

    const missingPlacementsSection = groupsPage.missingPlacementsSection
    await missingPlacementsSection.createGroupPlacementForChild(0)

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

    test('Missing holiday reservations are shown for holiday period dates that have no reservation or absence', async () => {
      await Fixture.holidayPeriod()
        .with({
          period: holidayRange,
          reservationDeadline: today.addWeeks(2)
        })
        .save()

      await unitPage.navigateToUnit(fixtures.daycareFixture.id)
      const groupsPage = await unitPage.openGroupsPage()
      const groupSection = await groupsPage.openGroupCollapsible(
        daycareGroupFixture.id
      )
      const monthCalendarPage = await groupSection.openMonthCalendar()

      // Today is not in a holiday period
      await monthCalendarPage
        .absenceCell(fixtures.enduserChildFixtureKaarina.id, today)
        .waitUntilVisible()
      await monthCalendarPage
        .absenceCell(fixtures.enduserChildFixtureKaarina.id, today)
        .missingHolidayReservation.waitUntilHidden()

      // Missing holiday reservation is shown for holiday period dates
      await monthCalendarPage.nextWeekButton.click()
      let date = holidayStart
      while (date <= holidayEnd) {
        await monthCalendarPage
          .absenceCell(fixtures.enduserChildFixtureKaarina.id, date)
          .missingHolidayReservation.waitUntilVisible()
        date = date.addDays(1)
      }

      // Adding an absence hides the missing holiday reservation marker
      await monthCalendarPage.addAbsenceToChild(
        fixtures.enduserChildFixtureKaarina.id,
        holidayStart,
        'UNKNOWN_ABSENCE',
        ['NONBILLABLE']
      )
      await monthCalendarPage
        .absenceCell(fixtures.enduserChildFixtureKaarina.id, holidayStart)
        .waitUntilVisible()
      await monthCalendarPage
        .absenceCell(fixtures.enduserChildFixtureKaarina.id, holidayStart)
        .missingHolidayReservation.waitUntilHidden()
    })
  })
})
