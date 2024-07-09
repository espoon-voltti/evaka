// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'

import config from '../../config'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { applicationFixture, Fixture, uuidv4 } from '../../dev-api/fixtures'
import {
  createApplicationPlacementPlan,
  createApplications,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import PersonSearchPage from '../../pages/employee/person-search'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let fixtures: AreaAndPersonFixtures
let admin: DevEmployee
let page: Page

beforeEach(async () => {
  await resetServiceState()
  fixtures = await initializeAreaAndPersonData()
  admin = await Fixture.employeeAdmin().save()
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
    const daycare1 = await Fixture.daycare().careArea(careArea1).save()
    const specialEducationTeacher =
      await Fixture.employeeSpecialEducationTeacher(daycare1.id).save()
    const preferredStartDate = LocalDate.of(2021, 8, 16)

    const childWithAssistanceNeed = fixtures.enduserChildFixtureJari
    const childWithoutAssistanceNeed = fixtures.enduserChildFixtureKaarina

    const appWithAssistanceNeeded = {
      ...applicationFixture(
        childWithAssistanceNeed,
        fixtures.enduserGuardianFixture,
        undefined,
        'DAYCARE',
        null,
        [daycare1.id],
        false,
        'WAITING_PLACEMENT',
        preferredStartDate,
        false,
        true
      ),
      id: uuidv4()
    }

    const appWithoutAssistanceNeeded = {
      ...applicationFixture(
        childWithoutAssistanceNeed,
        fixtures.enduserGuardianFixture,
        undefined,
        'DAYCARE',
        null,
        [daycare1.id],
        false,
        'WAITING_PLACEMENT',
        preferredStartDate,
        false,
        false
      ),
      id: uuidv4()
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
