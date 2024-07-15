// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../config'
import {
  createDaycarePlacementFixture,
  testDaycareGroup,
  Fixture,
  uuidv4,
  familyWithTwoGuardians,
  testDaycare,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createDaycarePlacements,
  resetServiceState
} from '../../generated/api-clients'
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
  await resetServiceState()

  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family(familyWithTwoGuardians).save()
  await createDaycareGroups({ body: [testDaycareGroup] })

  const unitId = testDaycare.id
  childId = familyWithTwoGuardians.children[0].id

  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    childId,
    unitId
  )
  await createDaycarePlacements({ body: [daycarePlacementFixture] })

  const admin = await Fixture.employeeAdmin().save()

  page = await Page.open()
  await employeeLogin(page, admin)
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
      LocalDate.todayInSystemTz().format()
    )
    await section.setDescription('Test description')
    await section.save()
    await waitUntilEqual(() => section.description, 'Test description')
    await section.addAttachmentAndAssert(page, testfile1Name, testfile1Path)
    await section.addAttachmentAndAssert(page, testfile2Name, testfile2Path)
  })
})
