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
  setAclForDaycares
} from 'e2e-test-common/dev-api'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { employeeLogin, UserRole } from 'e2e-playwright/utils/user'
import { UUID } from 'lib-common/types'
import ChildInformationPage, {
  ChildAssistanceNeed
} from '../../pages/employee/child-information-page'
import { format, subDays } from 'date-fns'
import { PlacementType } from 'lib-common/generated/enums'
import { Page } from '../../utils/page'

let page: Page
let childInformationPage: ChildInformationPage
let assistanceNeeds: ChildAssistanceNeed
let childId: UUID
let unitId: UUID

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  await insertDefaultServiceNeedOptions()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id
  page = await Page.open()
})

const setupPlacement = async (childPlacementType: PlacementType) => {
  await insertDaycarePlacementFixtures([
    createDaycarePlacementFixture(
      uuidv4(),
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
  await childInformationPage.assistanceCollapsible.click()
  assistanceNeeds = new ChildAssistanceNeed(page)
}

describe('Child Information assistance need functionality for employees', () => {
  test('assistance need can be added', async () => {
    await setupPlacement('DAYCARE')
    await setupUser(config.unitSupervisorAad)
    await logUserIn('UNIT_SUPERVISOR')
    await assistanceNeeds.createNewAssistanceNeed()
    await assistanceNeeds.setAssistanceNeedMultiplier('1,5')
    await assistanceNeeds.confirmAssistanceNeed()
    await assistanceNeeds.assertAssistanceNeedMultiplier('1,5')
  })

  test('assistance need before preschool for a child in preschool is not shown for unit manager', async () => {
    await setupPlacement('PRESCHOOL')
    await setupUser(config.unitSupervisorAad)
    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: subDays(new Date(), 1),
        description:
          'Test service need to be hidden because it starts before preschool started',
        updatedBy: config.unitSupervisorAad
      })
      .save()

    await logUserIn('UNIT_SUPERVISOR')
    await assistanceNeeds.assertAssistanceNeedCount(0)
  })

  test('assistance need for preschool for a child in preschool is shown for unit manager', async () => {
    await setupPlacement('PRESCHOOL')
    await setupUser(config.unitSupervisorAad)
    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: new Date(),
        description:
          'Test service need to be shown because it starts when preschool started',
        updatedBy: config.unitSupervisorAad
      })
      .save()

    await logUserIn('UNIT_SUPERVISOR')
    await assistanceNeeds.assertAssistanceNeedCount(1)
  })

  test('assistance need before preschool for a child in preschool is shown for admin', async () => {
    await setupPlacement('PRESCHOOL')
    await setupUser(config.adminAad)
    await Fixture.assistanceNeed()
      .with({
        childId: childId,
        startDate: subDays(new Date(), 1),
        description: 'Test service need to be shown because user is admin',
        updatedBy: config.adminAad
      })
      .save()

    await logUserIn('ADMIN')
    await assistanceNeeds.assertAssistanceNeedCount(1)
  })
})
