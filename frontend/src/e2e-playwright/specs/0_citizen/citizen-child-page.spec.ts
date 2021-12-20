// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { enduserLogin } from 'e2e-playwright/utils/user'
import {
  insertDaycarePlacementFixtures,
  resetDatabase
} from 'e2e-test-common/dev-api'
import LocalDate from 'lib-common/local-date'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../../e2e-test-common/dev-api/data-init'
import { uuidv4 } from '../../../e2e-test-common/dev-api/fixtures'
import {
  CitizenChildPage,
  CitizenChildrenPage
} from '../../pages/citizen/citizen-children'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'

let fixtures: AreaAndPersonFixtures
let page: Page
let header: CitizenHeader
let childPage: CitizenChildPage
let childrenPage: CitizenChildrenPage

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()

  page = await Page.open()
  await enduserLogin(page)
  header = new CitizenHeader(page)
  childPage = new CitizenChildPage(page)
  childrenPage = new CitizenChildrenPage(page)
})

describe('Citizen children', () => {
  describe('Child page', () => {
    test('Citizen can see its children and navigate to their page', async () => {
      await header.selectTab('children')
      await childrenPage.assertChildCount(3)
      await childrenPage.navigateToChild(0)
      await childPage.assertChildNameIsShown(
        'Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani Karhula'
      )
      await childPage.goBack()

      await childrenPage.navigateToChild(1)
      await childPage.assertChildNameIsShown('Kaarina Veera Nelli Karhula')
    })
  })

  describe('Placement termination', () => {
    test('Simple daycare placement can be terminated', async () => {
      const endDate = LocalDate.today().addMonths(2)
      await insertDaycarePlacementFixtures([
        {
          id: uuidv4(),
          type: 'DAYCARE',
          childId: fixtures.enduserChildFixtureKaarina.id,
          unitId: fixtures.daycareFixture.id,
          startDate: LocalDate.today().subMonths(2).formatIso(),
          endDate: endDate.formatIso()
        }
      ])

      await header.selectTab('children')
      await childrenPage.openChildPage('Kaarina')
      await childPage.openTerminationCollapsible()

      await childPage.assertTerminatedPlacementCount(0)
      await childPage.assertTerminatablePlacementCount(1)
      await childPage.togglePlacement(
        `Varhaiskasvatus, Alkuräjähdyksen päiväkoti, voimassa ${endDate.format()}`
      )
      await childPage.fillTerminationDate(LocalDate.today())
      await childPage.submitTermination()
      await childPage.assertTerminatablePlacementCount(0)

      await childPage.assertTerminatedPlacementCount(1)
      await childPage.assertTerminatedPlacement(
        'Varhaiskasvatus, Alkuräjähdyksen päiväkoti, viimeinen läsnäolopäivä: ' +
          LocalDate.today().format()
      )
    })
  })
})
