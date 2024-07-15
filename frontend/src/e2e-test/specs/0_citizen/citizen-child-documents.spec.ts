// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { UUID } from 'lib-common/types'

import {
  createDaycarePlacementFixture,
  testDaycareGroup,
  Fixture,
  uuidv4,
  testAdult,
  testChild,
  testCareArea,
  testDaycare
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createDaycarePlacements,
  insertGuardians,
  resetServiceState
} from '../../generated/api-clients'
import { DevPerson } from '../../generated/api-types'
import { CitizenChildPage } from '../../pages/citizen/citizen-children'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let child: DevPerson
let templateIdHojks: UUID
let documentIdHojks: UUID
let templateIdPed: UUID
let documentIdPed: UUID
let header: CitizenHeader

const mockedNow = HelsinkiDateTime.of(2022, 7, 31, 13, 0)

beforeEach(async () => {
  await resetServiceState()

  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family({ guardian: testAdult, children: [testChild] }).save()
  await createDaycareGroups({ body: [testDaycareGroup] })

  const unitId = testDaycare.id
  child = testChild
  await insertGuardians({
    body: [
      {
        guardianId: testAdult.id,
        childId: child.id
      }
    ]
  })

  await createDaycarePlacements({
    body: [createDaycarePlacementFixture(uuidv4(), child.id, unitId)]
  })

  templateIdHojks = (
    await Fixture.documentTemplate()
      .with({
        type: 'HOJKS',
        name: 'HOJKS 2023-2024'
      })
      .withPublished(true)
      .save()
  ).id
  documentIdHojks = (
    await Fixture.childDocument({
      childId: child.id,
      templateId: templateIdHojks
    })
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
  ).id

  templateIdPed = (
    await Fixture.documentTemplate()
      .with({
        type: 'PEDAGOGICAL_REPORT',
        name: 'Pedagoginen selvitys'
      })
      .withPublished(true)
      .save()
  ).id
  documentIdPed = (
    await Fixture.childDocument({
      templateId: templateIdPed,
      childId: child.id
    })
      .withModifiedAt(mockedNow)
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
  ).id

  page = await Page.open({ mockedTime: mockedNow })
  header = new CitizenHeader(page, 'desktop')
  await enduserLogin(page, testAdult)
})

describe('Citizen child documents listing page', () => {
  test('Published hojks is in the list', async () => {
    await header.openChildPage(child.id)
    const childPage = new CitizenChildPage(page)
    await childPage.openCollapsible('vasu')
    await childPage.childDocumentLink(documentIdHojks).click()
    expect(
      page.url.endsWith(`/child-documents/${documentIdHojks}`)
    ).toBeTruthy()
    await page.find('h1').assertTextEquals('HOJKS 2023-2024')
  })

  test('Published pedagogical report is in the list', async () => {
    await header.openChildPage(child.id)
    const childPage = new CitizenChildPage(page)
    await childPage.openCollapsible('vasu')
    await childPage.childDocumentLink(documentIdPed).click()
    expect(page.url.endsWith(`/child-documents/${documentIdPed}`)).toBeTruthy()
    await page.find('h1').assertTextEquals('Pedagoginen selvitys')
  })
})
