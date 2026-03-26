// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { PersonId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { randomId } from 'lib-common/id-type'

import config from '../../config'
import {
  createDaycarePlacementFixture,
  testDaycareGroup,
  Fixture,
  familyWithTwoGuardians,
  testDaycare,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createDaycarePlacements,
  resetServiceState
} from '../../generated/api-clients'
import type { PedagogicalDocumentsSection } from '../../pages/employee/child-information'
import ChildInformationPage from '../../pages/employee/child-information'
import { test, expect } from '../../playwright'
import type { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockNow = HelsinkiDateTime.of(2025, 4, 10, 12, 0, 0)

const testfile1Name = 'test_file.png'
const testfile1Path = `src/e2e-test/assets/${testfile1Name}`

const testfile2Name = 'test_file.jpg'
const testfile2Path = `src/e2e-test/assets/${testfile2Name}`

test.describe('Child Information - Pedagogical documents', () => {
  let page: Page
  let childInformationPage: ChildInformationPage
  let childId: PersonId
  let section: PedagogicalDocumentsSection

  test.use({ evakaOptions: { mockedTime: mockNow } })

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()

    await testCareArea.save()
    await testDaycare.save()
    await familyWithTwoGuardians.save()
    await createDaycareGroups({ body: [testDaycareGroup] })

    const unitId = testDaycare.id
    childId = familyWithTwoGuardians.children[0].id

    const daycarePlacementFixture = createDaycarePlacementFixture(
      randomId(),
      childId,
      unitId
    )
    await createDaycarePlacements({ body: [daycarePlacementFixture] })

    const admin = await Fixture.employee().admin().save()

    page = evaka
    await employeeLogin(page, admin)
    await page.goto(config.employeeUrl + '/child-information/' + childId)
    childInformationPage = new ChildInformationPage(page)
    section = await childInformationPage.openCollapsible('pedagogicalDocuments')
  })

  test('Can add a new pedagogigcal document', async () => {
    await section.addNew()
    await expect(section.startDateElement).toHaveText(
      mockNow.toLocalDate().format()
    )
    await section.setDescription('Test description')
    await section.save()
    await expect(section.descriptionElement).toHaveText('Test description')
    await section.fileUpload.upload(testfile1Path)
    await section.fileUpload.upload(testfile2Path)
  })
})
