// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertDefaultServiceNeedOptions,
  resetDatabase
} from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { DaycarePlacement } from '../../dev-api/types'
import { UnitPage } from '../../pages/employee/units/unit'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let fixtures: AreaAndPersonFixtures
let daycarePlacementFixture: DaycarePlacement
let page: Page
let unitPage: UnitPage

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  await insertDefaultServiceNeedOptions()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    fixtures.enduserChildFixtureJari.id,
    fixtures.daycareFixture.id
  )
  await insertDaycarePlacementFixtures([daycarePlacementFixture])
  const unitSupervisor = await Fixture.employeeUnitSupervisor(
    fixtures.daycareFixture.id
  ).save()

  page = await Page.open()
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
    await groupSection.assertChildCount(1)

    await groupSection.removeGroupPlacement(0)
    await missingPlacementsSection.assertRowCount(1)
    await groupSection.assertChildCount(0)
  })

  test('User can open the diary page and add an absence for a child', async () => {
    await unitPage.navigateToUnit(fixtures.daycareFixture.id)
    const groupsPage = await unitPage.openGroupsPage()

    const missingPlacementsSection = groupsPage.missingPlacementsSection
    await missingPlacementsSection.createGroupPlacementForChild(0)

    const groupSection = await groupsPage.openGroupCollapsible(
      daycareGroupFixture.id
    )
    const diaryPage = await groupSection.openDiary()
    await diaryPage.assertUnitName(fixtures.daycareFixture.name)
    await diaryPage.assertSelectedGroup(daycareGroupFixture.id)

    // Can add an absence
    await diaryPage.addAbsenceToChild(0, 'SICKLEAVE')
    await diaryPage.childHasAbsence(0, 'SICKLEAVE')

    // Can change the absence type
    await diaryPage.addAbsenceToChild(0, 'UNKNOWN_ABSENCE')
    await diaryPage.childHasAbsence(0, 'UNKNOWN_ABSENCE')

    // Hover shows type and who is the absence maker
    await diaryPage.assertTooltipContains(0, [
      'Varhaiskasvatus: Ilmoittamaton poissaolo',
      `${LocalDate.todayInSystemTz().formatIso()} Henkilökunta)`
    ])

    // Can clear an absence
    await diaryPage.addAbsenceToChild(0, 'NO_ABSENCE')
    await diaryPage.childHasNoAbsence(0)
  })

  test('User can add a staff attendance', async () => {
    await unitPage.navigateToUnit(fixtures.daycareFixture.id)
    const groupsPage = await unitPage.openGroupsPage()

    const missingPlacementsSection = groupsPage.missingPlacementsSection
    await missingPlacementsSection.createGroupPlacementForChild(0)

    const groupSection = await groupsPage.openGroupCollapsible(
      daycareGroupFixture.id
    )
    const diaryPage = await groupSection.openDiary()

    await diaryPage.fillStaffAttendance(0, 3)

    // Change to another page and back to reload data
    await unitPage.openGroupsPage()
    await groupSection.openDiary()

    await diaryPage.assertStaffAttendance(0, 3)
  })
})
