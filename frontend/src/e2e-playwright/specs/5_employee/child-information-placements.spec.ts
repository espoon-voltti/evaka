// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from 'e2e-test-common/config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertDefaultServiceNeedOptions,
  insertEmployeeFixture,
  resetDatabase,
  setAclForDaycares,
  terminatePlacement
} from 'e2e-test-common/dev-api'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  familyWithTwoGuardians,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { employeeLogin, UserRole } from 'e2e-playwright/utils/user'
import { UUID } from 'lib-common/types'
import ChildInformationPage, {
  ChildPlacements
} from '../../pages/employee/child-information-page'
import { format } from 'date-fns'
import { PlacementType } from 'lib-common/generated/enums'
import { Page } from '../../utils/page'
import LocalDate from 'lib-common/local-date'

let page: Page
let childInformationPage: ChildInformationPage
let childId: UUID
let unitId: UUID
let childPlacementsPage: ChildPlacements

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  await insertDefaultServiceNeedOptions()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id
  page = await Page.open()
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

const setupUser = async (id: UUID) => {
  await insertEmployeeFixture({
    id,
    externalId: `espoo-ad:${id}`,
    email: 'essi.esimies@evaka.test',
    firstName: 'Essi',
    lastName: 'Esimies',
    roles: []
  })
  await setAclForDaycares(`espoo-ad:${id}`, unitId)
}

const logUserIn = async (role: UserRole) => {
  await employeeLogin(page, role)
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  childInformationPage = new ChildInformationPage(page)
  await childInformationPage.placementsCollapsible.click()
  childPlacementsPage = new ChildPlacements(page)
}

describe('Child Information placement info', () => {
  test('A terminated placement is indicated', async () => {
    const placementId = uuidv4()
    await setupPlacement(placementId, 'DAYCARE')
    await setupUser(config.unitSupervisorAad)
    await logUserIn('UNIT_SUPERVISOR')

    await childPlacementsPage.assertTerminatedByGuardianIsNotShown()

    await terminatePlacement(
      placementId,
      LocalDate.today(),
      LocalDate.today(),
      familyWithTwoGuardians.guardian.id
    )
    await logUserIn('UNIT_SUPERVISOR')

    await childPlacementsPage.assertTerminatedByGuardianIsShown()
  })
})
