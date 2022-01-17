// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import {
  insertBackupCareFixtures,
  insertDaycareGroupFixtures,
  resetDatabase
} from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import {
  createBackupCareFixture,
  daycareGroupFixture,
  Fixture
} from '../../dev-api/fixtures'
import { BackupCare, PersonDetail } from '../../dev-api/types'
import { UnitPage } from '../../pages/employee/units/unit'
import { UnitGroupsPage } from '../../pages/employee/units/unit-groups-page'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let fixtures: AreaAndPersonFixtures
let childFixture: PersonDetail
let backupCareFixture: BackupCare
let page: Page
let groupsPage: UnitGroupsPage

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  childFixture = fixtures.enduserChildFixtureKaarina
  backupCareFixture = createBackupCareFixture(
    childFixture.id,
    fixtures.daycareFixture.id
  )
  const unitSupervisor = await Fixture.employeeUnitSupervisor(
    fixtures.daycareFixture.id
  ).save()
  await insertDaycareGroupFixtures([daycareGroupFixture])
  await insertBackupCareFixtures([backupCareFixture])

  page = await Page.open()
  await employeeLogin(page, unitSupervisor.data)
  const unitPage = new UnitPage(page)
  await unitPage.navigateToUnit(fixtures.daycareFixture.id)
  groupsPage = await unitPage.openGroupsPage()
})

describe('Employee - Backup care', () => {
  test('daycare has one backup care child missing group', async () => {
    await groupsPage.selectPeriod('1 year')

    await groupsPage.missingPlacementsSection.assertRowCount(1)
    await groupsPage.missingPlacementsSection.assertRowFields(0, {
      childName: `${childFixture.lastName} ${childFixture.firstName}`,
      dateOfBirth: LocalDate.parseIso(childFixture.dateOfBirth).format(),
      placementDuration: '01.02.2022 - 03.02.2022',
      groupMissingDuration: '01.02.2022 - 03.02.2022'
    })
  })

  test('backup care child can be placed into a group and removed from it', async () => {
    await groupsPage.selectPeriod('1 year')
    await groupsPage.setFilterStartDate('01.01.2022')

    // open the group placement modal and submit it with default values
    await groupsPage.missingPlacementsSection.createGroupPlacementForChild(0)
    await groupsPage.waitUntilLoaded()

    // no more missing placements
    await groupsPage.missingPlacementsSection.assertRowCount(0)

    // check child is listed in group
    let group = await groupsPage.openGroupCollapsible(daycareGroupFixture.id)
    await group.assertChildCount(1)

    const childRow = group.childRow(childFixture.id)
    await childRow.assertFields({
      childName: `${childFixture.lastName} ${childFixture.firstName}`,
      placementDuration: '01.02.2022- 03.02.2022'
    })

    // after removing the child is again visible at missing groups and no longer at the group
    await childRow.remove()

    await groupsPage.missingPlacementsSection.assertRowCount(1)
    await groupsPage.missingPlacementsSection.assertRowFields(0, {
      childName: `${childFixture.lastName} ${childFixture.firstName}`,
      dateOfBirth: LocalDate.parseIso(childFixture.dateOfBirth).format(),
      placementDuration: '01.02.2022 - 03.02.2022',
      groupMissingDuration: '01.02.2022 - 03.02.2022'
    })

    group = await groupsPage.openGroupCollapsible(daycareGroupFixture.id)
    await group.assertChildCount(0)
  })
})
