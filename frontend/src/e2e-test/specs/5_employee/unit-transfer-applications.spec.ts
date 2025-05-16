// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { TransferApplicationUnitSummary } from 'lib-common/generated/api-types/application'
import type {
  ApplicationId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'

import { applicationFixture, Fixture } from '../../dev-api/fixtures'
import {
  createApplications,
  resetServiceState
} from '../../generated/api-clients'
import type {
  DevCareArea,
  DevDaycare,
  DevEmployee
} from '../../generated/api-types'
import { UnitPage } from '../../pages/employee/units/unit'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

beforeEach(() => resetServiceState())

describe('unit transfer applications', () => {
  let area: DevCareArea
  let placementUnit1: DevDaycare
  let applicationUnit1: DevDaycare
  let expected: TransferApplicationUnitSummary[]

  beforeEach(async () => {
    area = await Fixture.careArea().save()
    placementUnit1 = await Fixture.daycare({ areaId: area.id }).save()
    applicationUnit1 = await Fixture.daycare({ areaId: area.id }).save()
    const guardian = await Fixture.person({ ssn: null }).saveAdult()
    const child1 = await Fixture.person({
      ssn: null,
      lastName: 'child 1'
    }).saveChild()
    await Fixture.placement({
      childId: child1.id,
      unitId: placementUnit1.id,
      startDate: LocalDate.of(2020, 1, 1),
      endDate: LocalDate.of(2025, 12, 31)
    }).save()
    const child2 = await Fixture.person({
      ssn: null,
      lastName: 'child 2'
    }).saveChild()
    await Fixture.placement({
      childId: child2.id,
      unitId: placementUnit1.id,
      startDate: LocalDate.of(2020, 1, 1),
      endDate: LocalDate.of(2025, 12, 31)
    }).save()
    const children = [child1, child2]
    const preferredStartDate = LocalDate.of(2021, 1, 1)
    const applications = children.map((child) => ({
      ...applicationFixture(
        child,
        guardian,
        undefined,
        'DAYCARE',
        null,
        [applicationUnit1.id],
        false,
        'SENT',
        preferredStartDate,
        true
      ),
      id: randomId<ApplicationId>()
    }))
    const applicationIds = await createApplications({
      body: applications
    })
    expected = applicationIds.map((applicationId, index) => ({
      applicationId,
      firstName: children[index].firstName,
      lastName: children[index].lastName,
      dateOfBirth: children[index].dateOfBirth,
      preferredStartDate
    }))
  })

  test('admin sees transfer applications', async () => {
    const clock = HelsinkiDateTime.of(2020, 8, 10, 8, 0)
    const user = await Fixture.employee().admin().save()
    const page = await openApplicationProcessTab(clock, user, placementUnit1.id)
    await page.transferApplications.waitUntilVisible()
    await page.transferApplications.assertTable(expected)
  })

  test("unit supervisor doesn't see transfer applications section", async () => {
    const clock = HelsinkiDateTime.of(2020, 8, 10, 8, 0)
    const user = await Fixture.employee()
      .unitSupervisor(placementUnit1.id)
      .save()
    const page = await openApplicationProcessTab(clock, user, placementUnit1.id)
    await page.transferApplications.waitUntilHidden()
  })
})

const openApplicationProcessTab = async (
  clock: HelsinkiDateTime,
  user: DevEmployee,
  unitId: DaycareId
) => {
  const page = await Page.open({ mockedTime: clock })
  await employeeLogin(page, user)
  const unitPage = await UnitPage.openUnit(page, unitId)
  return await unitPage.openApplicationProcessTab()
}
