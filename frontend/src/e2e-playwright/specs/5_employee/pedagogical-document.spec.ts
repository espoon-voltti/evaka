// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { format } from 'date-fns'
import ChildInformationPage, {
  PedagogicalDocumentsSection
} from 'e2e-playwright/pages/employee/child-information'
import { waitUntilEqual } from 'e2e-playwright/utils'
import { employeeLogin } from 'e2e-playwright/utils/user'
import config from 'e2e-test-common/config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { UUID } from 'lib-common/types'
import { Page } from '../../utils/page'

let page: Page
let childInformationPage: ChildInformationPage
let childId: UUID

const testfile1Name = 'test_file.png'
const testfile1Path = `src/e2e-playwright/assets/${testfile1Name}`

const testfile2Name = 'test_file.jpg'
const testfile2Path = `src/e2e-playwright/assets/${testfile2Name}`

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
