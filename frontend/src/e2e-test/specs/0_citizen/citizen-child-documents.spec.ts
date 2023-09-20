// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { UUID } from 'lib-common/types'

import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertGuardianFixtures,
  resetDatabase
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { PersonDetail } from '../../dev-api/types'
import { CitizenChildPage } from '../../pages/citizen/citizen-children'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let child: PersonDetail
let templateId: UUID
let documentId: UUID
let header: CitizenHeader

const mockedNow = HelsinkiDateTime.of(2022, 7, 31, 13, 0)

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  const unitId = fixtures.daycareFixture.id
  child = fixtures.enduserChildFixtureJari
  await insertGuardianFixtures([
    {
      guardianId: fixtures.enduserGuardianFixture.id,
      childId: child.id
    }
  ])

  await insertDaycarePlacementFixtures([
    createDaycarePlacementFixture(uuidv4(), child.id, unitId)
  ])
  templateId = (await Fixture.documentTemplate().withPublished(true).save())
    .data.id
  documentId = (
    await Fixture.childDocument()
      .withTemplate(templateId)
      .withChild(child.id)
      .withPublishedAt(mockedNow)
      .withPublishedContent({
        answers: [
          {
            questionId: 'q1',
            type: 'TEXT',
            answer: 'test'
          }
        ]
      })
      .save()
  ).data.id

  page = await Page.open({ mockedTime: mockedNow.toSystemTzDate() })
  header = new CitizenHeader(page, 'desktop')
  await enduserLogin(page, fixtures.enduserGuardianFixture.ssn)
})

describe('Citizen child documents listing page', () => {
  test('Published document is in the list', async () => {
    await page.reload()
    await header.openChildPage(child.id)
    const childPage = new CitizenChildPage(page)
    await childPage.openCollapsible('vasu')
    await childPage.childDocumentLink(documentId).click()
    expect(page.url.endsWith(`/child-documents/${documentId}`)).toBeTruthy()
    await page.find('h1').assertTextEquals(Fixture.documentTemplate().data.name)
  })
})
