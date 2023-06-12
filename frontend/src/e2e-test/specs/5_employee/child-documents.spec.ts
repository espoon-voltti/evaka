// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import config from '../../config'
import { resetDatabase } from '../../dev-api'
import {
  AreaAndPersonFixtures,
  initializeAreaAndPersonData
} from '../../dev-api/data-init'
import { Fixture } from '../../dev-api/fixtures'
import { EmployeeDetail, PersonDetail } from '../../dev-api/types'
import ChildInformationPage from '../../pages/employee/child-information'
import { ChildDocumentPage } from '../../pages/employee/documents/child-document'
import {
  DocumentTemplateEditorPage,
  DocumentTemplatesListPage
} from '../../pages/employee/documents/document-templates'
import EmployeeNav from '../../pages/employee/employee-nav'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let fixtures: AreaAndPersonFixtures
let childFixture: PersonDetail
let admin: EmployeeDetail
let unitSupervisor: EmployeeDetail
let page: Page

const now = HelsinkiDateTime.of(2023, 2, 1, 12, 10, 0)

beforeEach(async () => {
  await resetDatabase()

  fixtures = await initializeAreaAndPersonData()
  childFixture = fixtures.enduserChildFixtureKaarina
  admin = (await Fixture.employeeAdmin().save()).data
  unitSupervisor = (
    await Fixture.employeeUnitSupervisor(fixtures.daycareFixture.id).save()
  ).data

  await Fixture.placement()
    .with({
      childId: childFixture.id,
      unitId: fixtures.daycareFixture.id,
      startDate: now.toLocalDate().subYears(1),
      endDate: now.toLocalDate().addYears(1)
    })
    .save()
})

describe('Employee - Child documents', () => {
  test('Full basic workflow smoke test', async () => {
    // Admin creates a template

    page = await Page.open({ mockedTime: now.toSystemTzDate() })
    await employeeLogin(page, admin)
    await page.goto(config.employeeUrl)
    const nav = new EmployeeNav(page)
    await nav.openAndClickDropdownMenuItem('document-templates')

    const documentTemplatesPage = new DocumentTemplatesListPage(page)
    await documentTemplatesPage.createNewButton.click()
    const documentName = 'Pedagoginen arvio 2022-2023'
    await documentTemplatesPage.nameInput.fill(documentName)
    await documentTemplatesPage.validityStartInput.fill('01.08.2022')
    await documentTemplatesPage.confirmCreateButton.click()
    await documentTemplatesPage.openTemplate(documentName)

    const templateEditor = new DocumentTemplateEditorPage(page)
    await templateEditor.createNewSectionButton.click()
    const sectionName = 'Eka osio'
    await templateEditor.sectionNameInput.fill(sectionName)
    await templateEditor.confirmCreateSectionButton.click()

    const section = templateEditor.getSection(sectionName)
    await section.element.hover()
    await section.createNewQuestionButton.click()
    const questionName = 'Eka kysymys'
    await templateEditor.questionLabelInput.fill(questionName)
    await templateEditor.confirmCreateQuestionButton.click()

    await templateEditor.publishCheckbox.check()
    await templateEditor.saveButton.click()
    await templateEditor.saveButton.waitUntilHidden()
    await page.close()
    // End of admin creates a template

    // Unit supervisor creates a child document

    page = await Page.open({ mockedTime: now.toSystemTzDate() })
    await employeeLogin(page, unitSupervisor)
    await page.goto(
      `${config.employeeUrl}/child-information/${childFixture.id}`
    )
    const childInformationPage = new ChildInformationPage(page)
    let childDocumentsSection = await childInformationPage.openCollapsible(
      'childDocuments'
    )
    await childDocumentsSection.createPedagogicalReportButton.assertDisabled(
      true
    )
    await childDocumentsSection.createPedagogicalAssessmentButton.assertDisabled(
      false
    )
    await childDocumentsSection.createPedagogicalAssessmentButton.click()

    const childDocument = new ChildDocumentPage(page)
    const answer = 'Jonkin sortin vastaus'
    let question = childDocument.getTextQuestion(sectionName, questionName)
    await question.fill(answer)
    await childDocument.savingIndicator.waitUntilHidden()
    await childDocument.previewButton.click()
    await childDocument.returnButton.click()

    childDocumentsSection = await childInformationPage.openCollapsible(
      'childDocuments'
    )
    await waitUntilEqual(childDocumentsSection.childDocumentsCount, 1)
    const row = childDocumentsSection.childDocuments(0)
    await row.status.assertTextEquals('Luonnos')
    await row.openLink.click()

    question = childDocument.getTextQuestion(sectionName, questionName)
    await question.assertValueEquals(answer)
  })
})
