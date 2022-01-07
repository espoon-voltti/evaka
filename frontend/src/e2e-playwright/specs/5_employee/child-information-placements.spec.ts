// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { format } from 'date-fns'
import { employeeLogin } from 'e2e-playwright/utils/user'
import config from 'e2e-test-common/config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertDefaultServiceNeedOptions,
  resetDatabase,
  terminatePlacement
} from 'e2e-test-common/dev-api'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  familyWithTwoGuardians,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { PlacementType } from 'lib-common/generated/enums'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import ChildInformationPage from '../../pages/employee/child-information'
import { Page } from '../../utils/page'

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
      format(new Date(), 'yyyy-MM-dd'),
      format(new Date(), 'yyyy-MM-dd'),
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
      LocalDate.today(),
      LocalDate.today(),
      familyWithTwoGuardians.guardian.id
    )

    childPlacements = await openChildPlacements()
    await childPlacements.assertTerminatedByGuardianIsShown(placementId)
  })
})
