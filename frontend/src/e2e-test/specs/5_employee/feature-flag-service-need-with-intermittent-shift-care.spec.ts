// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ServiceNeedOption } from 'lib-common/generated/api-types/application'
import type { PersonId } from 'lib-common/generated/api-types/shared'

import config from '../../config'
import {
  familyWithTwoGuardians,
  Fixture,
  testCareArea,
  testDaycare
} from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type { DevEmployee, DevPlacement } from '../../generated/api-types'
import ChildInformationPage from '../../pages/employee/child-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let admin: DevEmployee
let childId: PersonId
let placement: DevPlacement
let activeServiceNeedOption: ServiceNeedOption

beforeEach(async () => {
  await resetServiceState()
  await testCareArea.save()
  await testDaycare.save()
  await familyWithTwoGuardians.save()
  const unitId = testDaycare.id
  childId = familyWithTwoGuardians.children[0].id
  await Fixture.employee({ roles: ['ADMIN'] }).save()
  placement = await Fixture.placement({
    childId,
    unitId
  }).save()
  activeServiceNeedOption = await Fixture.serviceNeedOption({
    validPlacementType: placement.type
  }).save()

  admin = await Fixture.employee().admin().save()

  page = await Page.open({
    employeeCustomizations: {
      featureFlags: { intermittentShiftCare: true }
    }
  })
  await employeeLogin(page, admin)
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
      placement.id,
      activeServiceNeedOption.nameFi,
      'INTERMITTENT',
      true
    )
    await section.assertNthServiceNeedShiftCare(0, 'INTERMITTENT')
  })

  test('service need can be edited to have intermittent shift care', async () => {
    const section = await openCollapsible()
    await section.addMissingServiceNeed(
      placement.id,
      activeServiceNeedOption.nameFi
    )
    await section.assertNthServiceNeedShiftCare(0, 'NONE')

    await section.setShiftCareTypeOfNthServiceNeed(0, 'INTERMITTENT')
    await section.assertNthServiceNeedShiftCare(0, 'INTERMITTENT')
  })
})
