// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { UUID } from 'lib-common/types'

import config from '../../config'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  EmployeeBuilder,
  Fixture,
  PlacementBuilder,
  ServiceNeedOptionBuilder
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import ChildInformationPage from '../../pages/employee/child-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let admin: EmployeeBuilder
let childId: UUID
let placement: PlacementBuilder
let activeServiceNeedOption: ServiceNeedOptionBuilder

beforeEach(async () => {
  await resetServiceState()
  const fixtures = await initializeAreaAndPersonData()
  const unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id
  await Fixture.employee()
    .with({ roles: ['ADMIN'] })
    .save()
  placement = await Fixture.placement()
    .with({
      childId,
      unitId
    })
    .save()
  activeServiceNeedOption = await Fixture.serviceNeedOption()
    .with({ validPlacementType: placement.data.type })
    .save()

  admin = await Fixture.employeeAdmin().save()

  page = await Page.open({
    employeeCustomizations: {
      featureFlags: { intermittentShiftCare: true }
    }
  })
  await employeeLogin(page, admin.data)
})

const openCollapsible = async () => {
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  const childInformationPage = new ChildInformationPage(page)
  await childInformationPage.waitUntilLoaded()
  return await childInformationPage.openCollapsible('placements')
}

describe('Intermittent shiftcare', () => {
  test('service need can be added with intermittent shift care', async () => {
    const section = await openCollapsible()
    await section.addMissingServiceNeed(
      placement.data.id,
      activeServiceNeedOption.data.nameFi,
      'INTERMITTENT',
      true
    )
    await section.assertNthServiceNeedShiftCare(0, 'INTERMITTENT')
  })

  test('service need can be edited to have intermittent shift care', async () => {
    const section = await openCollapsible()
    await section.addMissingServiceNeed(
      placement.data.id,
      activeServiceNeedOption.data.nameFi
    )
    await section.assertNthServiceNeedShiftCare(0, 'NONE')

    await section.setShiftCareTypeOfNthServiceNeed(0, 'INTERMITTENT')
    await section.assertNthServiceNeedShiftCare(0, 'INTERMITTENT')
  })
})
