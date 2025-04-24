// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { DocumentContent } from 'lib-common/generated/api-types/document'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { evakaUserId } from 'lib-common/id-type'

import config from '../../config'
import {
  Fixture,
  testAdult,
  testCareArea,
  testChild2,
  testDaycare
} from '../../dev-api/fixtures'
import {
  getSentEmails,
  resetServiceState,
  runJobs
} from '../../generated/api-clients'
import { DevEmployee } from '../../generated/api-types'
import ChildInformationPage from '../../pages/employee/child-information'
import { ChildDocumentPage } from '../../pages/employee/documents/child-document'
import {
  DocumentTemplateEditorPage,
  DocumentTemplatesListPage
} from '../../pages/employee/documents/document-templates'
import EmployeeNav from '../../pages/employee/employee-nav'
import { ChildDocumentsReport } from '../../pages/employee/reports'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let admin: DevEmployee
let unitSupervisor: DevEmployee
let page: Page

const now = HelsinkiDateTime.of(2023, 2, 1, 12, 10, 0)

beforeEach(async () => {
  await resetServiceState()

  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.person(testAdult).saveAdult()
  await Fixture.person(testChild2).saveChild()
  await Fixture.guardian(testChild2, testAdult).save()
  admin = await Fixture.employee().admin().save()
  unitSupervisor = await Fixture.employee()
    .unitSupervisor(testDaycare.id)
    .save()

  await Fixture.placement({
    childId: testChild2.id,
    unitId: testDaycare.id,
    startDate: now.toLocalDate().subYears(1),
    endDate: now.toLocalDate().addYears(1),
    type: 'PRESCHOOL'
  }).save()
})

describe('Employee - Child documents', () => {
  test('Full basic workflow for hojks', async () => {
    // Admin creates a template

    page = await Page.open({ mockedTime: now })
    await employeeLogin(page, admin)
    await page.goto(config.employeeUrl)
    const nav = new EmployeeNav(page)
    await nav.openAndClickDropdownMenuItem('document-templates')

    const documentTemplatesPage = new DocumentTemplatesListPage(page)
    const modal = await documentTemplatesPage.openCreateModal()
    const documentName = 'HOJKS 2022-2023'
    await modal.nameInput.fill(documentName)
    await modal.typeSelect.selectOption('HOJKS')
    await modal.placementTypesSelect.fillAndSelectFirst('Esiopetus')
    await modal.validityStartInput.fill('01.08.2022')
    await modal.confidentialityDurationYearsInput.fill('100')
    await modal.confidentialityBasisInput.fill('Joku laki §300')
    await modal.confirmCreateButton.click()
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
    page = await Page.open({ mockedTime: now })
    await employeeLogin(page, unitSupervisor)
    await page.goto(`${config.employeeUrl}/child-information/${testChild2.id}`)
    let childInformationPage = new ChildInformationPage(page)
    let childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await childDocumentsSection.createInternalDocumentButton.click()
    await childDocumentsSection.createModalTemplateSelect.assertTextEquals(
      'HOJKS 2022-2023'
    )
    await childDocumentsSection.modalOk.click()

    // Fill an answer and return
    let childDocument = new ChildDocumentPage(page)
    await childDocument.editButton.click()
    await childDocument.status.assertTextEquals('Luonnos')
    const answer = 'Jonkin sortin vastaus'
    let question = childDocument.getTextQuestion(sectionName, questionName)
    await question.fill(answer)
    await childDocument.savingIndicator.waitUntilHidden()
    await childDocument.previewButton.click()
    await childDocument.returnButton.click()

    // Assert status draft and unpublished, open the document again
    childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await waitUntilEqual(childDocumentsSection.internalChildDocumentsCount, 1)
    let row = childDocumentsSection.internalChildDocuments(0)
    await row.status.assertTextEquals('Luonnos')
    await row.published.assertTextEquals('-')
    await row.openLink.click()
    const documentUrl = page.url

    // Assert answer was saved
    await childDocument.editButton.click()
    question = childDocument.getTextQuestion(sectionName, questionName)
    await question.assertValueEquals(answer)

    // Publish without changing status
    await childDocument.previewButton.click()
    await childDocument.publish()

    // Assert status and publish time
    await childDocument.returnButton.click()
    childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await waitUntilEqual(childDocumentsSection.internalChildDocumentsCount, 1)
    row = childDocumentsSection.internalChildDocuments(0)
    await row.status.assertTextEquals('Luonnos')
    await row.published.assertTextEquals(now.format())
    await page.close()

    // go to next status twice
    const later = now.addHours(1)
    page = await Page.open({ mockedTime: later })
    await employeeLogin(page, unitSupervisor)
    await page.goto(documentUrl)
    childDocument = new ChildDocumentPage(page)
    await childDocument.goToNextStatus()
    await childDocument.status.assertTextEquals('Laadittu')
    await childDocument.goToNextStatus()
    await childDocument.status.assertTextEquals('Valmis')

    // Assert status and new publish time
    await childDocument.returnButton.click()
    childInformationPage = new ChildInformationPage(page)
    childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await waitUntilEqual(childDocumentsSection.internalChildDocumentsCount, 1)
    row = childDocumentsSection.internalChildDocuments(0)
    await row.status.assertTextEquals('Valmis')
    await row.published.assertTextEquals(later.format())
  })

  test('Pedagogical report only has two states', async () => {
    await Fixture.documentTemplate({
      type: 'PEDAGOGICAL_REPORT',
      published: true
    }).save()

    // Unit supervisor creates a child document
    page = await Page.open({ mockedTime: now })
    await employeeLogin(page, unitSupervisor)
    await page.goto(`${config.employeeUrl}/child-information/${testChild2.id}`)
    const childInformationPage = new ChildInformationPage(page)
    const childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await childDocumentsSection.createInternalDocumentButton.click()
    await childDocumentsSection.modalOk.click()

    // go to next status
    const childDocument = new ChildDocumentPage(page)
    await childDocument.status.assertTextEquals('Luonnos')
    await childDocument.goToNextStatus()
    await childDocument.status.assertTextEquals('Valmis')
  })

  test('Edit mode cannot be entered for 15 minutes after another use has edited the document content', async () => {
    await Fixture.documentTemplate({
      type: 'PEDAGOGICAL_REPORT',
      published: true
    }).save()

    // Unit supervisor creates a child document
    page = await Page.open({ mockedTime: now })
    await employeeLogin(page, unitSupervisor)
    await page.goto(`${config.employeeUrl}/child-information/${testChild2.id}`)
    let childInformationPage = new ChildInformationPage(page)
    let childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await childDocumentsSection.createInternalDocumentButton.click()
    await childDocumentsSection.modalOk.click()
    let childDocument = new ChildDocumentPage(page)
    await childDocument.editButton.click()
    await childDocument.status.assertTextEquals('Luonnos')
    await childDocument.savingIndicator.waitUntilHidden()
    await page.close()

    // Admin tries to open the document in edit mode too soon
    page = await Page.open({ mockedTime: now.addMinutes(3) })
    await employeeLogin(page, admin)
    await page.goto(`${config.employeeUrl}/child-information/${testChild2.id}`)
    childInformationPage = new ChildInformationPage(page)
    childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await waitUntilEqual(childDocumentsSection.internalChildDocumentsCount, 1)
    await childDocumentsSection.internalChildDocuments(0).openLink.click()
    childDocument = new ChildDocumentPage(page)
    await childDocument.editButton.click()
    await childDocument.closeConcurrentEditErrorModal()
    await childDocument.editButton.waitUntilVisible() // back in read mode
    await page.close()

    // Admin opens the document in edit mode after lock expires
    page = await Page.open({ mockedTime: now.addMinutes(6) })
    await employeeLogin(page, admin)
    await page.goto(`${config.employeeUrl}/child-information/${testChild2.id}`)
    childInformationPage = new ChildInformationPage(page)
    childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await waitUntilEqual(childDocumentsSection.internalChildDocumentsCount, 1)
    await childDocumentsSection.internalChildDocuments(0).openLink.click()
    childDocument = new ChildDocumentPage(page)
    await childDocument.editButton.click()
    await childDocument.previewButton.click()
  })

  test('Child documents report shows document status', async () => {
    const daycareGroup = await Fixture.daycareGroup({
      daycareId: testDaycare.id
    }).save()
    const template = await Fixture.documentTemplate({
      type: 'VASU',
      published: true,
      name: 'Vasu 2022-2023',
      placementTypes: ['DAYCARE']
    }).save()

    const child1 = await Fixture.person().saveChild()
    const placement1 = await Fixture.placement({
      childId: child1.id,
      unitId: testDaycare.id,
      type: 'DAYCARE',
      startDate: now.toLocalDate().subYears(1),
      endDate: now.toLocalDate().addYears(1)
    }).save()
    await Fixture.groupPlacement({
      daycareGroupId: daycareGroup.id,
      daycarePlacementId: placement1.id,
      startDate: now.toLocalDate().subYears(1),
      endDate: now.toLocalDate().addYears(1)
    }).save()

    const child2 = await Fixture.person({
      ssn: null
    }).saveChild()
    const placement2 = await Fixture.placement({
      childId: child2.id,
      unitId: testDaycare.id,
      type: 'DAYCARE',
      startDate: now.toLocalDate().subYears(1),
      endDate: now.toLocalDate().addYears(1)
    }).save()
    await Fixture.groupPlacement({
      daycareGroupId: daycareGroup.id,
      daycarePlacementId: placement2.id,
      startDate: now.toLocalDate().subYears(1),
      endDate: now.toLocalDate().addYears(1)
    }).save()

    await Fixture.childDocument({
      childId: child1.id,
      templateId: template.id,
      status: 'DRAFT'
    }).save()

    page = await Page.open({ mockedTime: now })
    await employeeLogin(page, unitSupervisor)
    await page.goto(`${config.employeeUrl}/reports/child-documents`)

    const report = new ChildDocumentsReport(page)
    await report.unitSelector.fillAndSelectFirst(testDaycare.name)
    await report.unitSelector.close()
    await report.templateSelector.open()
    await report.templateSelector.expandAll()
    await report.templateSelector.option(template.id).check()
    await report.templateSelector.close()

    const unitRow = report.getUnitRow(testDaycare.id)
    await unitRow.name.assertTextEquals(testDaycare.name)
    await unitRow.drafts.assertTextEquals('1')
    await unitRow.prepared.assertTextEquals('0')
    await unitRow.completed.assertTextEquals('0')
    await unitRow.noDocuments.assertTextEquals('1')
    await unitRow.total.assertTextEquals('2')

    await report.toggleUnitRowGroups(testDaycare.id)
    const groupRow = report.getGroupRow(daycareGroup.id)
    await groupRow.name.assertTextEquals(daycareGroup.name)
    await groupRow.drafts.assertTextEquals('1')
    await groupRow.prepared.assertTextEquals('0')
    await groupRow.completed.assertTextEquals('0')
    await groupRow.noDocuments.assertTextEquals('1')
    await groupRow.total.assertTextEquals('2')
  })
  test('Document archiving', async () => {
    // Admin creates a template

    page = await Page.open({ mockedTime: now })
    await employeeLogin(page, admin)
    await page.goto(config.employeeUrl)
    const nav = new EmployeeNav(page)
    await nav.openAndClickDropdownMenuItem('document-templates')

    const documentTemplatesPage = new DocumentTemplatesListPage(page)
    const modal = await documentTemplatesPage.openCreateModal()
    const documentName = 'VASU 2022-2023'
    await modal.nameInput.fill(documentName)
    await modal.typeSelect.selectOption('VASU')
    await modal.placementTypesSelect.fillAndSelectFirst('Esiopetus')
    await modal.validityStartInput.fill('01.08.2022')
    await modal.confidentialityDurationYearsInput.fill('100')
    await modal.confidentialityBasisInput.fill('Joku laki §300')
    await modal.processDefinitionNumberInput.fill('12.06.01')
    await modal.archiveDurationMonthsInput.fill('1320')
    await modal.confirmCreateButton.click()
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
    page = await Page.open({ mockedTime: now })
    await employeeLogin(page, unitSupervisor)
    await page.goto(`${config.employeeUrl}/child-information/${testChild2.id}`)
    const childInformationPage = new ChildInformationPage(page)
    const childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await childDocumentsSection.createInternalDocumentButton.click()
    await childDocumentsSection.createModalTemplateSelect.assertTextEquals(
      'VASU 2022-2023'
    )
    await childDocumentsSection.modalOk.click()

    // Fill an answer and return
    const childDocument = new ChildDocumentPage(page)
    await childDocument.editButton.click()
    await childDocument.status.assertTextEquals('Luonnos')
    const answer = 'Jonkin sortin vastaus'
    const question = childDocument.getTextQuestion(sectionName, questionName)
    await question.fill(answer)
    await childDocument.savingIndicator.waitUntilHidden()
    await childDocument.previewButton.click()
    await childDocument.publish()
    await childDocument.goToNextStatus()
    await childDocument.goToNextStatus()

    // PDF-generation should be triggered by publishing
    await runJobs({ mockedTime: now })

    page = await Page.open({ mockedTime: now })
    await employeeLogin(page, admin)
    await page.goto(`${config.employeeUrl}/child-information/${testChild2.id}`)
    const childInformationPage2 = new ChildInformationPage(page)
    const childDocumentsSection2 =
      await childInformationPage2.openCollapsible('childDocuments')
    const row2 = childDocumentsSection2.internalChildDocuments(0)
    await row2.openLink.click()
    const childDocument2 = new ChildDocumentPage(page)
    await childDocument2.archiveButton.click()
    await childDocument2.archiveButton.waitUntilIdle()
    await runJobs({ mockedTime: now })

    await page.reload()
    const childDocument3 = new ChildDocumentPage(page)
    await childDocument3.archiveButton.hover()
    await childDocument3.archiveTooltip.assertText((text) =>
      text.includes('Asiakirja on arkistoitu ')
    )
  })

  test('Citizen basic can be sent without filling the form', async () => {
    // create document template
    page = await Page.open({
      mockedTime: now,
      employeeCustomizations: {
        featureFlags: { citizenChildDocumentTypes: true }
      }
    })
    await employeeLogin(page, admin)
    await page.goto(config.employeeUrl)
    const nav = new EmployeeNav(page)
    await nav.openAndClickDropdownMenuItem('document-templates')

    const documentTemplatesPage = new DocumentTemplatesListPage(page)
    const modal = await documentTemplatesPage.openCreateModal()
    const documentName = 'Lomake kuntalaiselle'
    await modal.nameInput.fill(documentName)
    await modal.typeSelect.selectOption('Kuntalaisen lomake - perus')
    await modal.placementTypesSelect.fillAndSelectFirst('Esiopetus')
    await modal.validityStartInput.fill('01.08.2022')
    await modal.confidentialityDurationYearsInput.fill('100')
    await modal.confidentialityBasisInput.fill('Joku laki §300')
    await modal.confirmCreateButton.click()
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

    // create child document and send to citizen
    page = await Page.open({
      mockedTime: now,
      employeeCustomizations: {
        featureFlags: { citizenChildDocumentTypes: true }
      }
    })
    await employeeLogin(page, unitSupervisor)
    await page.goto(`${config.employeeUrl}/child-information/${testChild2.id}`)
    const childInformationPage = new ChildInformationPage(page)
    const childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await childDocumentsSection.createExternalDocumentButton.click()
    await childDocumentsSection.createModalTemplateSelect.assertTextEquals(
      documentName
    )
    await childDocumentsSection.modalOk.click()
    const childDocument = new ChildDocumentPage(page)
    await childDocument.status.assertTextEquals('Luonnos')
    await childDocument.goToNextStatus()
    await childDocument.status.assertTextEquals('Täytettävänä huoltajalla')
    await childDocument.returnButton.click()
    await childInformationPage.openCollapsible('childDocuments')
    await waitUntilEqual(childDocumentsSection.internalChildDocumentsCount, 0)
    await waitUntilEqual(childDocumentsSection.externalChildDocumentsCount, 1)
    const row = childDocumentsSection.externalChildDocuments(0)
    await row.sent.assertTextEquals(now.toLocalDate().format())
    await row.answered.assertTextEquals('Ei vastattu')
    await row.status.assertTextEquals('Täytettävänä huoltajalla')
    await runJobs({ mockedTime: now })
    const emails = await getSentEmails()
    expect(
      emails.map((email) => ({
        from: email.fromAddress,
        to: email.toAddress,
        subject: email.content.subject
      }))
    ).toEqual([
      {
        from: 'Espoon Varhaiskasvatus <no-reply.evaka@espoo.fi>',
        to: 'johannes.karhula@evaka.test',
        subject:
          'Uusi dokumentti eVakassa / Nytt dokument i eVaka / New document in eVaka'
      }
    ])
  })

  test('Citizen answered at is shown', async () => {
    const template = await Fixture.documentTemplate({
      type: 'CITIZEN_BASIC',
      name: 'Lomake kuntalaiselle',
      published: true
    }).save()
    const content: DocumentContent = {
      answers: [
        {
          questionId: 'q1',
          type: 'TEXT',
          answer: 'test'
        }
      ]
    }
    const publishedAt = now.addHours(1)
    const answeredAt = now.addHours(2)
    const guardian = await Fixture.person({ ssn: null }).saveAdult()
    await Fixture.childDocument({
      templateId: template.id,
      childId: testChild2.id,
      status: 'COMPLETED',
      content,
      publishedAt,
      publishedContent: content,
      answeredAt,
      answeredBy: evakaUserId(guardian.id)
    }).save()

    page = await Page.open({
      mockedTime: now,
      employeeCustomizations: {
        featureFlags: { citizenChildDocumentTypes: true }
      }
    })
    await employeeLogin(page, unitSupervisor)
    await page.goto(`${config.employeeUrl}/child-information/${testChild2.id}`)
    const childInformationPage = new ChildInformationPage(page)
    const childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await waitUntilEqual(childDocumentsSection.externalChildDocumentsCount, 1)
    const row = childDocumentsSection.externalChildDocuments(0)
    await row.sent.assertTextEquals(publishedAt.toLocalDate().format())
    await row.answered.assertTextEquals(
      `${answeredAt.toLocalDate().format()}, ${guardian.lastName} ${guardian.firstName}`
    )
    await row.status.assertTextEquals('Valmis')
  })

  test('Citizen basic can be filled by employee', async () => {
    // create document template
    page = await Page.open({
      mockedTime: now,
      employeeCustomizations: {
        featureFlags: { citizenChildDocumentTypes: true }
      }
    })
    await employeeLogin(page, admin)
    await page.goto(config.employeeUrl)
    const nav = new EmployeeNav(page)
    await nav.openAndClickDropdownMenuItem('document-templates')

    const documentTemplatesPage = new DocumentTemplatesListPage(page)
    const modal = await documentTemplatesPage.openCreateModal()
    const documentName = 'Lomake kuntalaiselle'
    await modal.nameInput.fill(documentName)
    await modal.typeSelect.selectOption('Kuntalaisen lomake - perus')
    await modal.placementTypesSelect.fillAndSelectFirst('Esiopetus')
    await modal.validityStartInput.fill('01.08.2022')
    await modal.confidentialityDurationYearsInput.fill('100')
    await modal.confidentialityBasisInput.fill('Joku laki §300')
    await modal.confirmCreateButton.click()
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

    // create child document and send to citizen
    page = await Page.open({
      mockedTime: now,
      employeeCustomizations: {
        featureFlags: { citizenChildDocumentTypes: true }
      }
    })
    await employeeLogin(page, unitSupervisor)
    await page.goto(`${config.employeeUrl}/child-information/${testChild2.id}`)
    const childInformationPage = new ChildInformationPage(page)
    const childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await childDocumentsSection.createExternalDocumentButton.click()
    await childDocumentsSection.createModalTemplateSelect.assertTextEquals(
      documentName
    )
    await childDocumentsSection.modalOk.click()
    const childDocument = new ChildDocumentPage(page)
    await childDocument.status.assertTextEquals('Luonnos')
    await childDocument.editButton.click()
    const answer = 'Jonkin sortin vastaus'
    const question = childDocument.getTextQuestion(sectionName, questionName)
    await question.fill(answer)
    await childDocument.savingIndicator.waitUntilHidden()
    await childDocument.previewButton.click()
    await childDocument.goToNextStatus()
    await childDocument.status.assertTextEquals('Täytettävänä huoltajalla')
    await childDocument.goToNextStatus()
    await childDocument.status.assertTextEquals('Valmis')
    await childDocument.returnButton.click()
    await childInformationPage.openCollapsible('childDocuments')
    await waitUntilEqual(childDocumentsSection.internalChildDocumentsCount, 0)
    await waitUntilEqual(childDocumentsSection.externalChildDocumentsCount, 1)
    const row = childDocumentsSection.externalChildDocuments(0)
    await row.sent.assertTextEquals(now.toLocalDate().format())
    await row.answered.assertTextEquals(
      `${now.toLocalDate().format()}, ${unitSupervisor.lastName} ${unitSupervisor.firstName}`
    )
    await row.status.assertTextEquals('Valmis')
    await runJobs({ mockedTime: now })
    const emails = await getSentEmails()
    expect(
      emails.map((email) => ({
        from: email.fromAddress,
        to: email.toAddress,
        subject: email.content.subject
      }))
    ).toEqual([
      {
        from: 'Espoon Varhaiskasvatus <no-reply.evaka@espoo.fi>',
        to: 'johannes.karhula@evaka.test',
        subject:
          'Uusi dokumentti eVakassa / Nytt dokument i eVaka / New document in eVaka'
      }
    ])
  })

  test('checkbox group answers are ordered correctly', async () => {
    const child = await Fixture.person().saveChild()
    const template = await Fixture.documentTemplate({
      content: {
        sections: [
          {
            id: 's1',
            label: 'osio 1',
            infoText: '',
            questions: [
              {
                id: 'q1',
                type: 'CHECKBOX_GROUP',
                label: 'kysymys 1',
                infoText: '',
                options: [
                  { id: 'o1', label: 'vaihtoehto 1', withText: true },
                  { id: 'o2', label: 'vaihtoehto 2', withText: true },
                  { id: 'o3', label: 'vaihtoehto 3', withText: false },
                  { id: 'o4', label: 'vaihtoehto 4', withText: false }
                ]
              }
            ]
          }
        ]
      }
    }).save()
    const document = await Fixture.childDocument({
      templateId: template.id,
      childId: child.id,
      content: {
        answers: [
          {
            questionId: 'q1',
            type: 'CHECKBOX_GROUP',
            answer: [
              { optionId: 'o3', extra: '' },
              { optionId: 'o1', extra: 'lisäinfoa' },
              { optionId: 'o2', extra: '' },
              { optionId: 'o5', extra: 'tuntematon vaihtoehto' }
            ]
          }
        ]
      }
    }).save()

    const page = await Page.open({ mockedTime: now })
    await employeeLogin(page, admin)
    await page.goto(`${config.employeeUrl}/child-information/${child.id}`)
    const childInformationPage = new ChildInformationPage(page)
    const childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    const childDocumentPage = await childDocumentsSection.openChildDocument(
      document.id
    )
    const answer1 = childDocumentPage.getCheckboxGroupAnswer(
      'osio 1',
      'kysymys 1'
    )
    await answer1.assertTextEquals(
      'vaihtoehto 1 : lisäinfoa\nvaihtoehto 2 :\nvaihtoehto 3'
    )
  })
})
