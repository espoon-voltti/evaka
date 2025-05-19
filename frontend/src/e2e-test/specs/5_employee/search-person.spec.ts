// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import { randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'

import config from '../../config'
import {
  applicationFixture,
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testChild2,
  testDaycare
} from '../../dev-api/fixtures'
import {
  createApplicationPlacementPlan,
  createApplications,
  resetServiceState
} from '../../generated/api-clients'
import type { DevEmployee } from '../../generated/api-types'
import PersonSearchPage from '../../pages/employee/person-search'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let admin: DevEmployee
let page: Page

beforeEach(async () => {
  await resetServiceState()
  await testCareArea.save()
  await testDaycare.save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2]
  }).save()
  admin = await Fixture.employee().admin().save()
})

async function openPage(
  employee: DevEmployee = admin
): Promise<PersonSearchPage> {
  page = await Page.open()
  await employeeLogin(page, employee)
  await page.goto(`${config.employeeUrl}/search`)
  return new PersonSearchPage(page)
}

describe('Search person', () => {
  test('Special education teacher (VEO) sees person from application only if the application has assistance needed selected', async () => {
    const careArea1 = await Fixture.careArea().save()
    const daycare1 = await Fixture.daycare({ areaId: careArea1.id }).save()
    const specialEducationTeacher = await Fixture.employee()
      .specialEducationTeacher(daycare1.id)
      .save()
    const preferredStartDate = LocalDate.of(2021, 8, 16)

    const childWithAssistanceNeed = testChild
    const childWithoutAssistanceNeed = testChild2

    const appWithAssistanceNeeded = {
      ...applicationFixture(
        childWithAssistanceNeed,
        testAdult,
        undefined,
        'DAYCARE',
        null,
        [daycare1.id],
        false,
        'WAITING_PLACEMENT',
        preferredStartDate,
        false,
        true,
        true,
        true
      ),
      id: randomId<ApplicationId>()
    }

    const appWithoutAssistanceNeeded = {
      ...applicationFixture(
        childWithoutAssistanceNeed,
        testAdult,
        undefined,
        'DAYCARE',
        null,
        [daycare1.id],
        false,
        'WAITING_PLACEMENT',
        preferredStartDate,
        false,
        false,
        true,
        false
      ),
      id: randomId<ApplicationId>()
    }

    await createApplications({
      body: [appWithAssistanceNeeded, appWithoutAssistanceNeeded]
    })

    await createApplicationPlacementPlan({
      applicationId: appWithAssistanceNeeded.id,
      body: {
        unitId: daycare1.id,
        period: new FiniteDateRange(preferredStartDate, preferredStartDate),
        preschoolDaycarePeriod: null
      }
    })

    await createApplicationPlacementPlan({
      applicationId: appWithoutAssistanceNeeded.id,
      body: {
        unitId: daycare1.id,
        period: new FiniteDateRange(preferredStartDate, preferredStartDate),
        preschoolDaycarePeriod: null
      }
    })

    const searchPage = await openPage(specialEducationTeacher)

    await searchPage.searchInput.fill(childWithAssistanceNeed.firstName)
    await searchPage.searchResults.assertCount(1)

    await searchPage.searchInput.fill(childWithoutAssistanceNeed.firstName)
    await searchPage.searchResults.assertCount(0)

    // Child with assistance need and guardian who sent application
    await searchPage.searchInput.fill(childWithoutAssistanceNeed.lastName)
    await searchPage.searchResults.assertCount(2)
  })
})
