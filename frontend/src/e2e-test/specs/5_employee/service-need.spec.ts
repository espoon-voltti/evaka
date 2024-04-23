// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
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
let employee: EmployeeBuilder
let placement: PlacementBuilder
let activeServiceNeedOption: ServiceNeedOptionBuilder
let inactiveServiceNeedOption: ServiceNeedOptionBuilder
let partiallyInactiveServiceNeedOption: ServiceNeedOptionBuilder
let serviceNeedOptionPartWeekNull: ServiceNeedOptionBuilder

const mockToday = LocalDate.of(2024, 3, 1)
const mockedTime = HelsinkiDateTime.fromLocal(mockToday, LocalTime.of(12, 0))

beforeEach(async () => {
  await resetServiceState()
  const fixtures = await initializeAreaAndPersonData()
  const unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id
  employee = await Fixture.employee()
    .with({ roles: ['ADMIN'] })
    .save()
  placement = await Fixture.placement()
    .with({
      childId,
      unitId,
      startDate: mockToday,
      endDate: mockToday.addDays(10)
    })
    .save()
  activeServiceNeedOption = await Fixture.serviceNeedOption()
    .with({ validPlacementType: placement.data.type })
    .save()
  inactiveServiceNeedOption = await Fixture.serviceNeedOption()
    .with({
      validPlacementType: placement.data.type,
      validTo: mockToday.subDays(1)
    })
    .save()
  partiallyInactiveServiceNeedOption = await Fixture.serviceNeedOption()
    .with({
      validPlacementType: placement.data.type,
      validTo: mockToday.addDays(5)
    })
    .save()
  serviceNeedOptionPartWeekNull = await Fixture.serviceNeedOption()
    .with({ validPlacementType: placement.data.type, partWeek: null })
    .save()

  admin = await Fixture.employeeAdmin().save()

  page = await Page.open({ mockedTime })
  await employeeLogin(page, admin.data)
})

const openCollapsible = async () => {
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  const childInformationPage = new ChildInformationPage(page)
  await childInformationPage.waitUntilLoaded()
  return await childInformationPage.openCollapsible('placements')
}

describe('Service need', () => {
  test('add service need to a placement', async () => {
    const section = await openCollapsible()
    await section.addMissingServiceNeed(
      placement.data.id,
      activeServiceNeedOption.data.nameFi
    )
    await section.assertNthServiceNeedName(
      0,
      activeServiceNeedOption.data.nameFi
    )
    await section.assertNthServiceNeedPartWeek(0, false)
  })

  test('only active service need options can be selected', async () => {
    const section = await openCollapsible()

    await section.assertServiceNeedOptions(placement.data.id, [
      activeServiceNeedOption.data.id,
      partiallyInactiveServiceNeedOption.data.id,
      serviceNeedOptionPartWeekNull.data.id
    ])
  })

  test('selecting partially inactive option shows validation error', async () => {
    const section = await openCollapsible()
    await section.openPlacement(placement.data.id)
    await section.addMissingServiceNeedButton.click()
    await section.serviceNeedOptionSelect.selectOption(
      partiallyInactiveServiceNeedOption.data.id
    )
    await section.serviceNeedSaveButton.assertDisabled(true)
    await section.partiallyInvalidWarning.waitUntilVisible()

    await section.serviceNeedEndDate.fill(mockToday.addDays(5).format())
    await section.serviceNeedSaveButton.assertDisabled(false)
    await section.partiallyInvalidWarning.waitUntilHidden()
    await section.serviceNeedSaveButton.click()
    await section.assertNthServiceNeedName(
      0,
      partiallyInactiveServiceNeedOption.data.nameFi
    )
  })

  test('inactive service need name is shown on placement', async () => {
    await Fixture.serviceNeed()
      .with({
        placementId: placement.data.id,
        optionId: inactiveServiceNeedOption.data.id,
        confirmedBy: employee.data.id
      })
      .save()
    const section = await openCollapsible()

    await section.assertNthServiceNeedName(
      0,
      inactiveServiceNeedOption.data.nameFi
    )
  })

  test('add service need to a placement and choose partWeek', async () => {
    const section = await openCollapsible()
    await section.addMissingServiceNeed(
      placement.data.id,
      serviceNeedOptionPartWeekNull.data.nameFi,
      'NONE',
      undefined,
      true
    )
    await section.assertNthServiceNeedName(
      0,
      serviceNeedOptionPartWeekNull.data.nameFi
    )
    await section.assertNthServiceNeedPartWeek(0, true)
  })
})
