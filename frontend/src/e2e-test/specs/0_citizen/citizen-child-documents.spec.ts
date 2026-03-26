// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import type {
  DocumentContent,
  DocumentTemplateContent
} from 'lib-common/generated/api-types/document'
import type { DocumentTemplateId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { evakaUserId, randomId } from 'lib-common/id-type'
import type { UUID } from 'lib-common/types'

import {
  createDaycarePlacementFixture,
  testDaycareGroup,
  Fixture,
  testAdult,
  testChild,
  testCareArea,
  testDaycare
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createDaycarePlacements,
  insertGuardians,
  resetServiceState,
  upsertWeakCredentials
} from '../../generated/api-clients'
import type {
  DevDocumentTemplate,
  DevEmployee,
  DevPerson
} from '../../generated/api-types'
import { CitizenChildPage } from '../../pages/citizen/citizen-children'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { MockStrongAuthPage } from '../../pages/citizen/citizen-strong-auth'
import { ChildDocumentPage } from '../../pages/employee/documents/child-document'
import { expect, test } from '../../playwright'
import type { Page } from '../../utils/page'
import { enduserLogin, enduserLoginWeak } from '../../utils/user'

let child: DevPerson
let decisionMaker: DevEmployee
let templateIdVasu: DocumentTemplateId
let documentIdVasu: UUID
let templateIdHojks: DocumentTemplateId
let documentIdHojks: UUID
let templateIdPed: DocumentTemplateId
let documentIdPed: UUID
let templateIdDecision: DocumentTemplateId
let documentIdDecision: UUID

const mockedNow = HelsinkiDateTime.of(2022, 7, 31, 13, 0)

test.beforeEach(async () => {
  await resetServiceState()

  await testCareArea.save()
  await testDaycare.save()
  await Fixture.family({ guardian: testAdult, children: [testChild] }).save()
  await createDaycareGroups({ body: [testDaycareGroup] })

  decisionMaker = await Fixture.employee().director().save()

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
    body: [createDaycarePlacementFixture(randomId(), child.id, unitId)]
  })

  templateIdVasu = (
    await Fixture.documentTemplate({
      type: 'VASU',
      name: 'VASU 2023-2024'
    })
      .withPublished(true)
      .save()
  ).id
  documentIdVasu = (
    await Fixture.childDocument({
      childId: child.id,
      templateId: templateIdVasu
    })
      .withPublishedVersion({
        versionNumber: 1,
        createdAt: mockedNow,
        createdBy: evakaUserId(decisionMaker.id),
        publishedContent: {
          answers: [
            {
              questionId: 'q1',
              type: 'TEXT',
              answer: 'test'
            }
          ]
        }
      })
      .save()
  ).id

  templateIdHojks = (
    await Fixture.documentTemplate({
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
      .withPublishedVersion({
        versionNumber: 1,
        createdAt: mockedNow,
        createdBy: evakaUserId(decisionMaker.id),
        publishedContent: {
          answers: [
            {
              questionId: 'q1',
              type: 'TEXT',
              answer: 'test'
            }
          ]
        }
      })
      .save()
  ).id

  templateIdPed = (
    await Fixture.documentTemplate({
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
      .withPublishedVersion({
        versionNumber: 1,
        createdAt: mockedNow,
        createdBy: evakaUserId(decisionMaker.id),
        publishedContent: {
          answers: [
            {
              questionId: 'q1',
              type: 'TEXT',
              answer: 'test'
            }
          ]
        }
      })
      .save()
  ).id

  templateIdDecision = (
    await Fixture.documentTemplate({
      type: 'OTHER_DECISION',
      name: 'Tuenpäätös',
      endDecisionWhenUnitChanges: true
    })
      .withPublished(true)
      .save()
  ).id
  documentIdDecision = (
    await Fixture.childDocument({
      templateId: templateIdDecision,
      childId: child.id,
      status: 'COMPLETED',
      decisionMaker: decisionMaker.id
    })
      .withModifiedAt(mockedNow)
      .withPublishedVersion({
        versionNumber: 1,
        createdAt: mockedNow,
        createdBy: evakaUserId(decisionMaker.id),
        publishedContent: {
          answers: [
            {
              questionId: 'q1',
              type: 'TEXT',
              answer: 'test'
            }
          ]
        }
      })
      .withDecision({
        status: 'ACCEPTED',
        validity: new DateRange(
          mockedNow.toLocalDate().addDays(3),
          mockedNow.toLocalDate().addDays(14)
        ),
        createdBy: decisionMaker.id,
        modifiedBy: decisionMaker.id
      })
      .save()
  ).id
})

test.describe('Citizen child documents listing page', () => {
  test.use({ evakaOptions: { mockedTime: mockedNow } })

  test.beforeEach(async ({ evaka }) => {
    await enduserLogin(evaka, testAdult)
  })

  test('Published vasu is in the list', async ({ evaka }) => {
    const header = new CitizenHeader(evaka, 'desktop')
    await header.openChildPage(child.id)
    const childPage = new CitizenChildPage(evaka)
    await childPage.openCollapsible('child-documents')
    await childPage.childDocumentLink(documentIdVasu).click()
    expect(
      evaka.url.endsWith(`/child-documents/${documentIdVasu}`)
    ).toBeTruthy()
    await expect(evaka.find('h1')).toHaveText('VASU 2023-2024', {
      useInnerText: true
    })
  })

  test('Published hojks is in the list', async ({ evaka }) => {
    const header = new CitizenHeader(evaka, 'desktop')
    await header.openChildPage(child.id)
    const childPage = new CitizenChildPage(evaka)
    await childPage.openCollapsible('child-documents')
    await childPage.childDocumentLink(documentIdHojks).click()
    expect(
      evaka.url.endsWith(`/child-documents/${documentIdHojks}`)
    ).toBeTruthy()
    await expect(evaka.find('h1')).toHaveText('HOJKS 2023-2024', {
      useInnerText: true
    })
  })

  test('Published pedagogical report is in the list', async ({ evaka }) => {
    const header = new CitizenHeader(evaka, 'desktop')
    await header.openChildPage(child.id)
    const childPage = new CitizenChildPage(evaka)
    await childPage.openCollapsible('child-documents')
    await childPage.childDocumentLink(documentIdPed).click()
    expect(evaka.url.endsWith(`/child-documents/${documentIdPed}`)).toBeTruthy()
    await expect(evaka.find('h1')).toHaveText('Pedagoginen selvitys', {
      useInnerText: true
    })
  })

  test('Published decision is in the list', async ({ evaka }) => {
    const header = new CitizenHeader(evaka, 'desktop')
    await header.openChildPage(child.id)
    const childPage = new CitizenChildPage(evaka)
    await childPage.openCollapsible('child-documents')
    await childPage.childDocumentLink(documentIdDecision).click()
    expect(
      evaka.url.endsWith(`/child-documents/${documentIdDecision}`)
    ).toBeTruthy()
    await expect(evaka.find('h1')).toHaveText('Tuenpäätös', {
      useInnerText: true
    })
  })

  test('Answered by employee does not show name', async ({ evaka }) => {
    const templateContent: DocumentTemplateContent = {
      sections: [
        {
          id: randomId(),
          infoText: '',
          label: 'Testi',
          questions: [
            {
              id: randomId(),
              type: 'TEXT',
              infoText: '',
              label: 'Kysymys 1',
              multiline: false
            },
            {
              id: randomId(),
              type: 'TEXT',
              infoText: '',
              label: 'Kysymys 2',
              multiline: false
            }
          ]
        }
      ]
    }
    const template = await Fixture.documentTemplate({
      type: 'CITIZEN_BASIC',
      name: 'Lomake kuntalaiselle',
      content: templateContent,
      published: true
    }).save()
    const documentContent: DocumentContent = {
      answers: []
    }
    const unitSupervisor = await Fixture.employee()
      .unitSupervisor(testDaycare.id)
      .save()
    const document = await Fixture.childDocument({
      templateId: template.id,
      childId: child.id,
      status: 'COMPLETED',
      content: documentContent,
      answeredAt: mockedNow,
      answeredBy: evakaUserId(unitSupervisor.id)
    })
      .withPublishedVersion({
        versionNumber: 1,
        createdAt: mockedNow,
        createdBy: evakaUserId(unitSupervisor.id),
        publishedContent: documentContent
      })
      .save()

    const header = new CitizenHeader(evaka, 'desktop')
    await header.openChildPage(child.id)
    const childPage = new CitizenChildPage(evaka)
    await childPage.openCollapsible('child-documents')
    const row = childPage.childDocumentRow(document.id)
    await expect(row).toHaveText(
      `${mockedNow.toLocalDate().format()}\tLomake kuntalaiselle\tVastattu, ${mockedNow.toLocalDate().format()}, Henkilökunta\tValmis`,
      { useInnerText: true }
    )
  })
})

test.describe('Citizen child documents editor page', () => {
  test.use({ evakaOptions: { mockedTime: mockedNow } })

  const templateContent: DocumentTemplateContent = {
    sections: [
      {
        id: randomId(),
        infoText: '',
        label: 'Testi',
        questions: [
          {
            id: randomId(),
            type: 'TEXT',
            infoText: '',
            label: 'Kysymys 1',
            multiline: false
          },
          {
            id: randomId(),
            type: 'TEXT',
            infoText: '',
            label: 'Kysymys 2',
            multiline: false
          }
        ]
      }
    ]
  }
  const documentTemplate: Partial<DevDocumentTemplate> = {
    type: 'CITIZEN_BASIC',
    name: 'Lomake kuntalaiselle',
    content: templateContent,
    published: true
  }

  test('guardian can fill document and send', async ({ evaka }) => {
    const template = await Fixture.documentTemplate(documentTemplate).save()
    const documentContent: DocumentContent = {
      answers: []
    }
    const document = await Fixture.childDocument({
      templateId: template.id,
      childId: child.id,
      status: 'CITIZEN_DRAFT',
      content: documentContent
    })
      .withPublishedVersion({
        versionNumber: 1,
        createdAt: mockedNow,
        createdBy: evakaUserId(decisionMaker.id),
        publishedContent: documentContent
      })
      .save()

    await enduserLogin(evaka, testAdult)
    const header = new CitizenHeader(evaka, 'desktop')
    await header.assertUnreadChildrenCount(5)
    await header.openChildPage(child.id)
    const childPage = new CitizenChildPage(evaka)
    await childPage.openCollapsible('child-documents')
    const row = childPage.childDocumentRow(document.id)
    await expect(row).toHaveText(
      `${mockedNow.toLocalDate().format()}\tLomake kuntalaiselle\tEi vastattu\tTäytettävänä huoltajalla`,
      { useInnerText: true }
    )
    await childPage.childDocumentLink(document.id).click()
    await header.assertUnreadChildrenCount(4)
    const childDocumentPage = new ChildDocumentPage(evaka)
    await childDocumentPage.editButton.click()
    await expect(childDocumentPage.status).toHaveText(
      'Täytettävänä huoltajalla',
      { useInnerText: true }
    )
    const question1 = childDocumentPage.getTextQuestion('Testi', 'Kysymys 1')
    await question1.fill('Jonkin sortin vastaus 1')
    const question2 = childDocumentPage.getTextQuestion('Testi', 'Kysymys 2')
    await question2.fill('Jonkin sortin vastaus 2')
    await childDocumentPage.previewButton.click()
    const answer1 = childDocumentPage.getTextAnswer('Testi', 'Kysymys 1')
    await expect(answer1).toHaveText('Jonkin sortin vastaus 1\n', {
      useInnerText: true
    })
    const answer2 = childDocumentPage.getTextAnswer('Testi', 'Kysymys 2')
    await expect(answer2).toHaveText('Jonkin sortin vastaus 2\n', {
      useInnerText: true
    })
    await childDocumentPage.sendButton.click()
    await childDocumentPage.sendingConfirmationModal.submit()
    await expect(childDocumentPage.status).toHaveText('Valmis', {
      useInnerText: true
    })
    await childDocumentPage.returnButton.click()
    await childPage.openCollapsible('child-documents')
    await expect(row).toHaveText(
      `${mockedNow.toLocalDate().format()}\tLomake kuntalaiselle\tVastattu, ${mockedNow.toLocalDate().format()}, ${testAdult.lastName} ${testAdult.firstName}\tValmis`,
      { useInnerText: true }
    )
  })

  test('strong auth guardian can navigate via toast', async ({ evaka }) => {
    const template = await Fixture.documentTemplate(documentTemplate).save()
    const documentContent: DocumentContent = {
      answers: []
    }
    const document = await Fixture.childDocument({
      templateId: template.id,
      childId: child.id,
      status: 'CITIZEN_DRAFT',
      content: documentContent
    })
      .withPublishedVersion({
        versionNumber: 1,
        createdAt: mockedNow,
        createdBy: evakaUserId(decisionMaker.id),
        publishedContent: documentContent
      })
      .save()

    await enduserLogin(evaka, testAdult)
    const toast1 = evaka.findByDataQa(`toast-child-document-${document.id}`)
    await expect(toast1).toHaveText(
      'Henkilökunta on pyytänyt sinua täyttämään asiakirjan, joka koskee lastasi: Jari-Petteri Karhula\nTäytä asiakirja',
      { useInnerText: true }
    )
    await toast1.click()
    const childDocumentPage = new ChildDocumentPage(evaka)
    await expect(childDocumentPage.status).toHaveText(
      'Täytettävänä huoltajalla',
      { useInnerText: true }
    )
    const question1 = childDocumentPage.getTextQuestion('Testi', 'Kysymys 1')
    await question1.fill('Jonkin sortin vastaus 1')
    const question2 = childDocumentPage.getTextQuestion('Testi', 'Kysymys 2')
    await question2.fill('Jonkin sortin vastaus 2')
    await childDocumentPage.previewButton.click()
    const answer1 = childDocumentPage.getTextAnswer('Testi', 'Kysymys 1')
    await expect(answer1).toHaveText('Jonkin sortin vastaus 1\n', {
      useInnerText: true
    })
    const answer2 = childDocumentPage.getTextAnswer('Testi', 'Kysymys 2')
    await expect(answer2).toHaveText('Jonkin sortin vastaus 2\n', {
      useInnerText: true
    })
    await childDocumentPage.sendButton.click()
    await childDocumentPage.sendingConfirmationModal.submit()
    await expect(toast1).toBeHidden()
    const toast2 = evaka.findByDataQa(
      `toast-child-document-${document.id}-success`
    )
    await expect(toast2).toHaveText('Lomake lähetetty', { useInnerText: true })
  })

  test('weak auth guardian can navigate via toast, document is in edit mode', async ({
    evaka
  }) => {
    const credentials = {
      username: 'test@example.com',
      password: 'TestPassword456!'
    }
    await upsertWeakCredentials({
      id: testAdult.id,
      body: credentials
    })
    const template = await Fixture.documentTemplate(documentTemplate).save()
    const documentContent: DocumentContent = {
      answers: []
    }
    const document = await Fixture.childDocument({
      templateId: template.id,
      childId: child.id,
      status: 'CITIZEN_DRAFT',
      content: documentContent
    })
      .withPublishedVersion({
        versionNumber: 1,
        createdAt: mockedNow,
        createdBy: evakaUserId(decisionMaker.id),
        publishedContent: documentContent
      })
      .save()

    await enduserLoginWeak(evaka, credentials)
    const toast1 = evaka.findByDataQa(`toast-child-document-${document.id}`)
    await expect(toast1).toHaveText(
      'Henkilökunta on pyytänyt sinua täyttämään asiakirjan, joka koskee lastasi: Jari-Petteri Karhula\nTäytä asiakirja\nAsiakirjan täyttäminen vaatii vahvan tunnistautumisen.',
      { useInnerText: true }
    )
    await toast1.click()
    const strongAuthPage = new MockStrongAuthPage(evaka)
    const childDocumentPage = await strongAuthPage.login(
      testAdult.ssn!,
      (page: Page) => new ChildDocumentPage(page)
    )
    expect(
      evaka.url.endsWith(
        `/child-documents/${document.id}?returnTo=calendar&readOnly=false`
      )
    ).toBeTruthy()
    await expect(childDocumentPage.status).toHaveText(
      'Täytettävänä huoltajalla',
      { useInnerText: true }
    )
    const question1 = childDocumentPage.getTextQuestion('Testi', 'Kysymys 1')
    await question1.fill('Jonkin sortin vastaus 1')
    const question2 = childDocumentPage.getTextQuestion('Testi', 'Kysymys 2')
    await question2.fill('Jonkin sortin vastaus 2')
    await childDocumentPage.previewButton.click()
    const answer1 = childDocumentPage.getTextAnswer('Testi', 'Kysymys 1')
    await expect(answer1).toHaveText('Jonkin sortin vastaus 1\n', {
      useInnerText: true
    })
    const answer2 = childDocumentPage.getTextAnswer('Testi', 'Kysymys 2')
    await expect(answer2).toHaveText('Jonkin sortin vastaus 2\n', {
      useInnerText: true
    })
    await childDocumentPage.sendButton.click()
    await childDocumentPage.sendingConfirmationModal.submit()
    await expect(toast1).toBeHidden()
    const toast2 = evaka.findByDataQa(
      `toast-child-document-${document.id}-success`
    )
    await expect(toast2).toHaveText('Lomake lähetetty', { useInnerText: true })
  })
})
