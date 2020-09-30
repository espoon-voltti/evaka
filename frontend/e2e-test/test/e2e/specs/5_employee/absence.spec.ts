// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import {
  createDaycarePlacementFixture,
  daycareGroupFixture
} from '../../dev-api/fixtures'
import AdminHome from '../../pages/home'
import EmployeeHome from '../../pages/employee/home'
import UnitPage, {
  daycareGroupElement
} from '../../pages/employee/units/unit-page'
import { DaycarePlacement } from '../../dev-api/types'
import config from '../../config'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from '../../dev-api/data-init'
import {
  deleteEmployeeFixture,
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertEmployeeFixture,
  setAclForDaycares
} from '../../dev-api'
import { logConsoleMessages } from '../../utils/fixture'
import AbsencesPage from '../../pages/employee/absences/absences-page'
import { t } from 'testcafe'
import { seppoManagerRole } from '../../config/users'

const adminHome = new AdminHome()
const employeeHome = new EmployeeHome()
const unitPage = new UnitPage()
const absencesPage = new AbsencesPage()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>
let daycarePlacementFixture: DaycarePlacement

fixture('Employee - Absences')
  .meta({ type: 'regression', subType: 'absences' })
  .page(adminHome.homePage('admin'))
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
    await insertDaycareGroupFixtures([daycareGroupFixture])
    await insertEmployeeFixture({
      aad: config.supervisorAad,
      firstName: 'Seppo',
      lastName: 'Sorsa',
      roles: []
    })
    await setAclForDaycares(config.supervisorAad, fixtures.daycareFixture.id)

    daycarePlacementFixture = createDaycarePlacementFixture(
      fixtures.enduserChildFixtureJari.id,
      fixtures.daycareFixture.id
    )
    await insertDaycarePlacementFixtures([daycarePlacementFixture])
  })
  .beforeEach(async () => {
    await t.useRole(seppoManagerRole)
    await employeeHome.navigateToUnits()
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    await cleanUp()
    await deleteEmployeeFixture(config.supervisorAad)
  })

test('User can place a child into a group and remove the child from the group', async (t) => {
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openGroups()
  const group = daycareGroupElement(unitPage.groups.nth(0))

  await absencesPage.addDaycareGroupPlacement()

  await t.expect(unitPage.missingPlacementRows.count).eql(0)
  await t.expect(group.groupPlacementRows.count).eql(1)

  await absencesPage.removeDaycareGroupPlacement()
  await t.expect(unitPage.missingPlacementRows.count).eql(1)
  await t.expect(group.groupPlacementRows.count).eql(0)
})

test('User can open the absence dialog', async (t) => {
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openGroups()
  const group = daycareGroupElement(unitPage.groups.nth(0))
  await absencesPage.addDaycareGroupPlacement()

  await t.click(absencesPage.btnOpenAbsenceDiary)
  await t
    .expect(absencesPage.absencesTitle.innerText)
    .eql(fixtures.daycareFixture.name)

  await t.click(absencesPage.btnAbsencesPageReturn)

  await unitPage.openGroups()
  await absencesPage.removeDaycareGroupPlacement()
  await t.expect(group.groupPlacementRows.count).eql(0)
})

test('User can find the child in the absence dialog', async (t) => {
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openGroups()
  const group = daycareGroupElement(unitPage.groups.nth(0))

  await absencesPage.addDaycareGroupPlacement()

  await t.click(absencesPage.btnOpenAbsenceDiary)
  await absencesPage.tryToFindAnyChildWithinNext24Months(
    createDaycarePlacementFixture(
      fixtures.enduserChildFixtureJari.id,
      fixtures.daycareFixture.id
    )
  )
  await t.expect(absencesPage.absenceTableChildLink.exists).ok()
  await t.click(absencesPage.btnAbsencesPageReturn)

  await unitPage.openGroups()
  await absencesPage.removeDaycareGroupPlacement()
  await t.expect(group.groupPlacementRows.count).eql(0)
})

test('User can add a sickleave to a child', async (t) => {
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openGroups()
  const group = daycareGroupElement(unitPage.groups.nth(0))

  await absencesPage.addDaycareGroupPlacement()

  await t.click(absencesPage.btnOpenAbsenceDiary)
  await absencesPage.tryToFindAnyChildWithinNext24Months(
    createDaycarePlacementFixture(
      fixtures.enduserChildFixtureJari.id,
      fixtures.daycareFixture.id
    )
  )
  await absencesPage.addBillableAbsence('SICKLEAVE')
  await t.expect(absencesPage.absenceIndicatorRight('SICKLEAVE').exists).ok()

  await t.click(absencesPage.btnAbsencesPageReturn)

  await unitPage.openGroups()
  await absencesPage.removeDaycareGroupPlacement()
  await t.expect(group.groupPlacementRows.count).eql(0)
})

test('Adding another leave type will override the previous one', async (t) => {
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openGroups()
  const group = daycareGroupElement(unitPage.groups.nth(0))

  await absencesPage.addDaycareGroupPlacement()

  await t.click(absencesPage.btnOpenAbsenceDiary)
  await absencesPage.tryToFindAnyChildWithinNext24Months(
    createDaycarePlacementFixture(
      fixtures.enduserChildFixtureJari.id,
      fixtures.daycareFixture.id
    )
  )
  await absencesPage.addBillableAbsence('SICKLEAVE')
  await t.expect(absencesPage.absenceIndicatorRight('SICKLEAVE').exists).ok()
  await absencesPage.addBillableAbsence('UNKNOWN_ABSENCE')
  await t.expect(absencesPage.absenceIndicatorRight('SICKLEAVE').exists).notOk()
  await t
    .expect(absencesPage.absenceIndicatorRight('UNKNOWN_ABSENCE').exists)
    .ok()

  await t.click(absencesPage.btnAbsencesPageReturn)

  await unitPage.openGroups()
  await absencesPage.removeDaycareGroupPlacement()
  await t.expect(group.groupPlacementRows.count).eql(0)
})

test('User can clear an absence', async (t) => {
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openGroups()
  const group = daycareGroupElement(unitPage.groups.nth(0))

  await absencesPage.addDaycareGroupPlacement()

  await t.click(absencesPage.btnOpenAbsenceDiary)
  await absencesPage.tryToFindAnyChildWithinNext24Months(
    createDaycarePlacementFixture(
      fixtures.enduserChildFixtureJari.id,
      fixtures.daycareFixture.id
    )
  )
  await absencesPage.addBillableAbsence('SICKLEAVE')
  await t.expect(absencesPage.absenceIndicatorRight('SICKLEAVE').exists).ok()
  await absencesPage.addBillableAbsence('PRESENCE')
  await t.expect(absencesPage.absenceIndicatorRight('SICKLEAVE').exists).notOk()

  await t.click(absencesPage.btnAbsencesPageReturn)

  await unitPage.openGroups()
  await absencesPage.removeDaycareGroupPlacement()
  await t.expect(group.groupPlacementRows.count).eql(0)
})

test('User can add a staff attendance', async (t) => {
  await unitPage.navigateHere(fixtures.daycareFixture.id)
  await unitPage.openGroups()
  const group = daycareGroupElement(unitPage.groups.nth(0))

  await absencesPage.addDaycareGroupPlacement()

  await t.click(absencesPage.btnOpenAbsenceDiary)
  await absencesPage.tryToFindAnyChildWithinNext24Months(
    createDaycarePlacementFixture(
      fixtures.enduserChildFixtureJari.id,
      fixtures.daycareFixture.id
    )
  )

  await t.expect(absencesPage.staffAttendanceInput.value).eql('')
  await t.typeText(absencesPage.staffAttendanceInput, '3')
  await t.wait(1000) // without this the page wont save the staff attendance
  await t.click(absencesPage.btnAbsencesPageReturn)
  await t.click(absencesPage.btnOpenAbsenceDiary)
  await absencesPage.tryToFindAnyChildWithinNext24Months(
    createDaycarePlacementFixture(
      fixtures.enduserChildFixtureJari.id,
      fixtures.daycareFixture.id
    )
  )
  await t.expect(absencesPage.staffAttendanceInput.value).eql('3')

  await t.click(absencesPage.btnAbsencesPageReturn)
  await unitPage.openGroups()
  await absencesPage.removeDaycareGroupPlacement()
  await t.expect(group.groupPlacementRows.count).eql(0)
})
