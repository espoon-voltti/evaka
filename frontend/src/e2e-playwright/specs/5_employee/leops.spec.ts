// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import ChildInformationPage, {
  VasuAndLeopsSection
} from 'e2e-playwright/pages/employee/child-information'
import { employeeLogin } from 'e2e-playwright/utils/user'
import config from 'e2e-test-common/config'
import {
  insertDaycarePlacementFixtures,
  insertDefaultServiceNeedOptions,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  Fixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { EmployeeDetail } from 'e2e-test-common/dev-api/types'
import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'
import EmployeeNav from '../../pages/employee/employee-nav'
import {
  VasuEditPage,
  VasuPage,
  VasuPreviewPage
} from '../../pages/employee/vasu/vasu'
import { VasuTemplateEditPage } from '../../pages/employee/vasu/vasu-template-edit'
import { VasuTemplatesListPage } from '../../pages/employee/vasu/vasu-templates-list'
import { Page } from '../../utils/page'

let page: Page
let admin: EmployeeDetail
let childInformationPage: ChildInformationPage
let childId: UUID

beforeAll(async () => {
  await resetDatabase()

  admin = (await Fixture.employeeAdmin().save()).data

  const fixtures = await initializeAreaAndPersonData()
  await insertDefaultServiceNeedOptions()

  const unitId = fixtures.preschoolFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id

  const preschooldPlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    childId,
    unitId,
    LocalDate.today().formatIso(),
    LocalDate.today().addYears(1).formatIso(),
    'PRESCHOOL'
  )

  await insertDaycarePlacementFixtures([preschooldPlacementFixture])
})

describe('Child Information - Leops documents section', () => {
  let section: VasuAndLeopsSection
  beforeEach(async () => {
    page = await Page.open()
    await employeeLogin(page, admin)
    await page.goto(`${config.employeeUrl}/child-information/${childId}`)
    childInformationPage = new ChildInformationPage(page)
  })

  const createLeopsTemplate = async (templateName: string) => {
    await new EmployeeNav(page).openAndClickDropdownMenuItem('vasu-templates')
    const vasuTemplatesList = new VasuTemplatesListPage(page)
    await vasuTemplatesList.addTemplateButton.click()
    await vasuTemplatesList.nameInput.fill(templateName)
    await vasuTemplatesList.selectType.selectOption('PRESCHOOL')
    await vasuTemplatesList.okButton.click()
    await new VasuTemplateEditPage(page).saveButton.click()
  }

  test('Can add a new vasu document', async () => {
    const templateName = 'Test template'
    await createLeopsTemplate(templateName)
    await page.goto(`${config.employeeUrl}/child-information/${childId}`)
    section = await childInformationPage.openCollapsible('vasuAndLeops')

    await section.addNew()
    const vasuPage = new VasuPage(page)

    await vasuPage.assertTemplateName(templateName)

    await vasuPage.edit()
    const vasuEditPage = new VasuEditPage(page)

    await vasuEditPage.clickMultiSelectQuestionOption('tiedonsaajataho_muualle')
    await vasuEditPage.setMultiSelectQuestionOptionText(
      'tiedonsaajataho_muualle',
      'test text'
    )

    await vasuEditPage.previewBtn.click()
    const vasuPreviewPage = new VasuPreviewPage(page)

    await vasuPreviewPage.assertMultiSelectContains('9.1', 'test text')
  })
})
