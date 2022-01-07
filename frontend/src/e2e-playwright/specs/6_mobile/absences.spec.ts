// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import MobileAbsencesPage from 'e2e-playwright/pages/mobile/absences-page'
import MobileChildPage from 'e2e-playwright/pages/mobile/child-page'
import MobileListPage from 'e2e-playwright/pages/mobile/list-page'
import { waitUntilEqual } from 'e2e-playwright/utils'
import { pairMobileDevice } from 'e2e-playwright/utils/mobile'
import { resetDatabase } from 'e2e-test-common/dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from 'e2e-test-common/dev-api/data-init'
import { daycareGroupFixture, Fixture } from 'e2e-test-common/dev-api/fixtures'
import LocalDate from 'lib-common/local-date'
import { Page } from '../../utils/page'

let fixtures: AreaAndPersonFixtures
let page: Page
let listPage: MobileListPage
let childPage: MobileChildPage
let absencesPage: MobileAbsencesPage

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()

  await Fixture.daycareGroup().with(daycareGroupFixture).save()
  const daycarePlacementFixture = await Fixture.placement()
    .with({
      childId: fixtures.familyWithTwoGuardians.children[0].id,
      unitId: fixtures.daycareFixture.id
    })
    .save()
  await Fixture.groupPlacement()
    .with({
      daycarePlacementId: daycarePlacementFixture.data.id,
      daycareGroupId: daycareGroupFixture.id
    })
    .save()

  page = await Page.open()
  listPage = new MobileListPage(page)
  childPage = new MobileChildPage(page)
  absencesPage = new MobileAbsencesPage(page)

  const mobileSignupUrl = await pairMobileDevice(fixtures.daycareFixture.id)
  await page.goto(mobileSignupUrl)
})

describe('Future absences', () => {
  test('User can set and delete future absence periods', async () => {
    await listPage.selectChild(fixtures.familyWithTwoGuardians.children[0].id)
    await childPage.markFutureAbsences()
    await waitUntilEqual(() => absencesPage.getAbsencesCount(), 0)

    await absencesPage.markNewAbsencePeriod(
      LocalDate.today().addWeeks(1),
      LocalDate.today().addWeeks(2),
      'SICKLEAVE'
    )
    await childPage.markFutureAbsences()
    await waitUntilEqual(() => absencesPage.getAbsencesCount(), 1)

    await absencesPage.markNewAbsencePeriod(
      LocalDate.today().addWeeks(4),
      LocalDate.today().addWeeks(5),
      'SICKLEAVE'
    )
    await childPage.markFutureAbsences()
    await waitUntilEqual(() => absencesPage.getAbsencesCount(), 2)

    await absencesPage.deleteFirstAbsencePeriod()
    await waitUntilEqual(() => absencesPage.getAbsencesCount(), 1)
  })
})
