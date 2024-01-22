// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
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

  const unitSupervisor = await Fixture.employeeUnitSupervisor(
    fixtures.daycareFixture.id
  ).save()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  const now = HelsinkiDateTime.of(2023, 2, 1, 12, 10, 0)
  const startDate = LocalDate.of(2023, 2, 1).subYears(1)
  const endDate = LocalDate.of(2023, 2, 3).addYears(1)
  const placement = await Fixture.placement()
    .with({
      childId: childFixture.id,
      unitId: fixtures.daycareFixturePrivateVoucher.id,
      startDate: startDate,
      endDate: endDate
    })
    .save()
  const serviceNeedOption = await Fixture.serviceNeedOption()
    .with({ validPlacementType: placement.data.type })
    .save()
  await Fixture.serviceNeed()
    .with({
      placementId: placement.data.id,
      startDate: startDate,
      endDate: endDate,
      optionId: serviceNeedOption.data.id,
      confirmedBy: unitSupervisor.data.id
    })
    .save()
  backupCareFixture = createBackupCareFixture(
    childFixture.id,
    fixtures.daycareFixture.id
  )
  await insertBackupCareFixtures([backupCareFixture])

  page = await Page.open({ mockedTime: now })
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
      dateOfBirth: childFixture.dateOfBirth.format(),
      placementDuration: '01.02.2023 - 03.02.2023',
      groupMissingDuration: '01.02.2023 - 03.02.2023'
    })
  })

  test('backup care child can be placed into a group and removed from it', async () => {
    await groupsPage.selectPeriod('1 year')
    await groupsPage.setFilterStartDate('01.01.2023')

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
      placementDuration: '01.02.2023- 03.02.2023'
    })

    // after removing the child is again visible at missing groups and no longer at the group
    await childRow.remove()

    await groupsPage.missingPlacementsSection.assertRowCount(1)
    await groupsPage.missingPlacementsSection.assertRowFields(0, {
      childName: `${childFixture.lastName} ${childFixture.firstName}`,
      dateOfBirth: childFixture.dateOfBirth.format(),
      placementDuration: '01.02.2023 - 03.02.2023',
      groupMissingDuration: '01.02.2023 - 03.02.2023'
    })

    group = await groupsPage.openGroupCollapsible(daycareGroupFixture.id)
    await group.assertChildCount(0)
  })
})
