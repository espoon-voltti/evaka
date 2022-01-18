// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { format } from 'date-fns'
import { UUID } from 'lib-common/types'
import config from '../../config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  resetDatabase
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import ChildInformationPage, {
  PedagogicalDocumentsSection
} from '../../pages/employee/child-information'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let childInformationPage: ChildInformationPage
let childId: UUID

const testfile1Name = 'test_file.png'
const testfile1Path = `src/e2e-test/assets/${testfile1Name}`

const testfile2Name = 'test_file.jpg'
const testfile2Path = `src/e2e-test/assets/${testfile2Name}`

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  const unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id

  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    childId,
    unitId
  )
  await insertDaycarePlacementFixtures([daycarePlacementFixture])

  const admin = await Fixture.employeeAdmin().save()

  page = await Page.open()
  await employeeLogin(page, admin.data)
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  childInformationPage = new ChildInformationPage(page)
})

describe('Child Information - Pedagogical documents', () => {
  let section: PedagogicalDocumentsSection
  beforeEach(async () => {
    section = await childInformationPage.openCollapsible('pedagogicalDocuments')
  })

  test('Can add a new pedagogigcal document', async () => {
    await section.addNew()
    await waitUntilEqual(
      () => section.startDate,
      format(new Date(), 'dd.MM.yyyy')
    )
    await section.setDescription('Test description')
    await section.save()
    await waitUntilEqual(() => section.description, 'Test description')
    await section.addAttachmentAndAssert(page, testfile1Name, testfile1Path)
    await section.addAttachmentAndAssert(page, testfile2Name, testfile2Path)
  })
})
