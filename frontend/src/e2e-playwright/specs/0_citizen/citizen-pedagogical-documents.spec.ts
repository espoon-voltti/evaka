// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { Page } from 'playwright'
import { resetDatabase } from '../../../e2e-test-common/dev-api'
import { newBrowserContext } from '../../browser'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { enduserLogin } from '../../utils/user'
import CitizenPedagogicalDocumentsPage from '../../pages/citizen/citizen-pedagogical-documents'
import { Fixture } from '../../../e2e-test-common/dev-api/fixtures'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../../e2e-test-common/dev-api/data-init'

let fixtures: AreaAndPersonFixtures
let page: Page
let header: CitizenHeader
let pedagogicalDocumentsPage: CitizenPedagogicalDocumentsPage

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()

  page = await (await newBrowserContext()).newPage()
  await enduserLogin(page)
  header = new CitizenHeader(page)
  pedagogicalDocumentsPage = new CitizenPedagogicalDocumentsPage(page)
})
afterEach(async () => {
  await page.close()
})

describe('Citizen pedagogical documents', () => {
  describe('Citizen main page pedagogical documents header', () => {
    test('Number of unread pedagogical documents is show correctly', async () => {
      await page.reload()
      await pedagogicalDocumentsPage.assertUnreadPedagogicalDocumentIndicatorIsNotShown()

      await Fixture.pedagogicalDocument()
        .with({
          childId: fixtures.enduserChildFixtureJari.id,
          description: 'e2e test description'
        })
        .save()

      await page.reload()
      await pedagogicalDocumentsPage.assertUnreadPedagogicalDocumentIndicatorCount(
        1
      )
    })
  })

  describe('Pedagogical documents view', () => {
    test('Existing pedagogical document without attachment is shown', async () => {
      const pd = await Fixture.pedagogicalDocument()
        .with({
          childId: fixtures.enduserChildFixtureJari.id,
          description: 'e2e test description'
        })
        .save()

      await header.selectTab('pedagogical-documents')

      await pedagogicalDocumentsPage.assertPedagogicalDocumentExists(
        pd.data.id,
        LocalDate.today().format(),
        pd.data.description
      )
    })
  })
})
