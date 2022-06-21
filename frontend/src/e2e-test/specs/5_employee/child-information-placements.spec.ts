// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { PlacementType } from 'lib-common/generated/api-types/placement'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertDefaultServiceNeedOptions,
  resetDatabase,
  terminatePlacement
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  familyWithTwoGuardians,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import ChildInformationPage from '../../pages/employee/child-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let childInformationPage: ChildInformationPage
let childId: UUID
let unitId: UUID

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  await insertDefaultServiceNeedOptions()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id
  const unitSupervisor = await Fixture.employeeUnitSupervisor(unitId).save()

  page = await Page.open()
  await employeeLogin(page, unitSupervisor.data)
})

const setupPlacement = async (
  placementId: string,
  childPlacementType: PlacementType
) => {
  await insertDaycarePlacementFixtures([
    createDaycarePlacementFixture(
      placementId,
      childId,
      unitId,
      LocalDate.todayInSystemTz().formatIso(),
      LocalDate.todayInSystemTz().formatIso(),
      childPlacementType
    )
  ])
}

async function openChildPlacements() {
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  childInformationPage = new ChildInformationPage(page)
  await childInformationPage.waitUntilLoaded()
  return await childInformationPage.openCollapsible('placements')
}

describe('Child Information placement info', () => {
  test('A terminated placement is indicated', async () => {
    const placementId = uuidv4()
    await setupPlacement(placementId, 'DAYCARE')

    let childPlacements = await openChildPlacements()
    await childPlacements.assertTerminatedByGuardianIsNotShown(placementId)

    await terminatePlacement(
      placementId,
      LocalDate.todayInSystemTz(),
      LocalDate.todayInSystemTz(),
      familyWithTwoGuardians.guardian.id
    )

    childPlacements = await openChildPlacements()
    await childPlacements.assertTerminatedByGuardianIsShown(placementId)
  })
})
