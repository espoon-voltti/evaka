// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../config'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import { Fixture, uuidv4 } from '../../dev-api/fixtures'
import {
  createDefaultServiceNeedOptions,
  deleteVasuTemplates,
  resetServiceState
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import ChildInformationPage, {
  ChildDocumentsSection
} from '../../pages/employee/child-information'
import EmployeeNav from '../../pages/employee/employee-nav'
import {
  VasuEditPage,
  VasuPage,
  VasuPreviewPage
} from '../../pages/employee/vasu/vasu'
import { VasuTemplateEditPage } from '../../pages/employee/vasu/vasu-template-edit'
import { VasuTemplatesListPage } from '../../pages/employee/vasu/vasu-templates-list'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let admin: DevEmployee
let childInformationPage: ChildInformationPage
let childId: UUID

beforeAll(async () => {
  await resetServiceState()

  admin = await Fixture.employeeAdmin().save()

  const fixtures = await initializeAreaAndPersonData()
  await createDefaultServiceNeedOptions()

  const unitId = fixtures.preschoolFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id

  await Fixture.placement()
    .with({
      id: uuidv4(),
      childId,
      unitId,
      startDate: LocalDate.todayInSystemTz(),
      endDate: LocalDate.todayInSystemTz().addYears(1),
      type: 'PRESCHOOL'
    })
    .save()
})

describe('Child Information - Leops documents section', () => {
  let section: ChildDocumentsSection
  let vasuTemplatesList: VasuTemplatesListPage

  beforeEach(async () => {
    page = await Page.open()
    await deleteVasuTemplates()
    await employeeLogin(page, admin)
    await page.goto(`${config.employeeUrl}/child-information/${childId}`)
    childInformationPage = new ChildInformationPage(page)
  })

  const createLeopsTemplate = async (templateName: string) => {
    await new EmployeeNav(page).openAndClickDropdownMenuItem('vasu-templates')
    vasuTemplatesList = new VasuTemplatesListPage(page)
    await vasuTemplatesList.addTemplateButton.click()
    await vasuTemplatesList.nameInput.fill(templateName)
    await vasuTemplatesList.selectType.selectOption('PRESCHOOL')
    await vasuTemplatesList.okButton.click()
    await new VasuTemplateEditPage(page).saveButton.click()
    await vasuTemplatesList.templateTable.waitUntilVisible()
  }

  test('Can create a leops template', async () => {
    const templateName = 'Test template'
    await createLeopsTemplate(templateName)
    await vasuTemplatesList.assertTemplateRowCount(1)
    await vasuTemplatesList.assertTemplate(0, templateName)
  })

  test('Can fill a leops questionnaire', async () => {
    const templateName = 'Test template'
    await createLeopsTemplate(templateName)
    await page.goto(`${config.employeeUrl}/child-information/${childId}`)
    section = await childInformationPage.openCollapsible('childDocuments')

    await section.addNewVasu()
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
