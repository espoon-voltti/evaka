// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'

import { startTest } from '../../browser'
import { insertPedagogicalDocumentAttachment } from '../../dev-api'
import {
  createDaycarePlacementFixture,
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testDaycare,
  uuidv4
} from '../../dev-api/fixtures'
import { createDaycarePlacements } from '../../generated/api-clients'
import { CitizenChildPage } from '../../pages/citizen/citizen-children'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenPedagogicalDocumentsPage from '../../pages/citizen/citizen-pedagogical-documents'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let header: CitizenHeader
let pedagogicalDocumentsPage: CitizenPedagogicalDocumentsPage

const testFileName = 'test_file.png'
const testFilePath = `src/e2e-test/assets`

const mockedNow = HelsinkiDateTime.of(2022, 7, 31, 13, 0)

beforeEach(async () => {
  await startTest()
  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family({ guardian: testAdult, children: [testChild] }).save()

  await createDaycarePlacements({
    body: [
      createDaycarePlacementFixture(uuidv4(), testChild.id, testDaycare.id)
    ]
  })

  page = await Page.open({ mockedTime: mockedNow })
  await enduserLogin(page, testAdult)
  header = new CitizenHeader(page)
  pedagogicalDocumentsPage = new CitizenPedagogicalDocumentsPage(page)
})

describe('Citizen pedagogical documents', () => {
  describe('Citizen main page pedagogical documents header', () => {
    test('Number of unread pedagogical documents is shown correctly', async () => {
      await page.reload()
      await header.assertUnreadChildrenCount(0)

      const pd = await Fixture.pedagogicalDocument({
        childId: testChild.id,
        description: 'e2e test description'
      }).save()

      const employee = await Fixture.employee({ roles: ['ADMIN'] }).save()
      const attachmentId = await insertPedagogicalDocumentAttachment(
        pd.id,
        employee.id,
        testFileName,
        testFilePath
      )

      await page.reload()
      await header.assertUnreadChildrenCount(1)

      await header.openChildPage(testChild.id)
      const childPage = new CitizenChildPage(page)
      await childPage.openCollapsible('pedagogical-documents')

      await pedagogicalDocumentsPage.downloadAttachment(attachmentId)
      await header.assertUnreadChildrenCount(0)
    })
  })

  describe('Pedagogical documents view', () => {
    test('Existing pedagogical document without attachment is shown', async () => {
      const pd = await Fixture.pedagogicalDocument({
        childId: testChild.id,
        description: 'e2e test description'
      }).save()

      await header.openChildPage(testChild.id)
      const childPage = new CitizenChildPage(page)
      await childPage.openCollapsible('pedagogical-documents')

      await pedagogicalDocumentsPage.assertPedagogicalDocumentExists(
        pd.id,
        LocalDate.todayInSystemTz().format(),
        pd.description
      )
    })
  })
})
