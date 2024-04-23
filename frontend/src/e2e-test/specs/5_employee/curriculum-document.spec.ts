// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import config from '../../config'
import { insertVasuTemplateFixture } from '../../dev-api'
import { EmployeeBuilder, Fixture, PersonBuilder } from '../../dev-api/fixtures'
import {
  createVasuDocument,
  resetServiceState
} from '../../generated/api-clients'
import ChildInformationPage from '../../pages/employee/child-information'
import { VasuEditPage, VasuPage } from '../../pages/employee/vasu/vasu'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedTime = HelsinkiDateTime.of(2023, 9, 27, 10, 31)
const mockedDate = mockedTime.toLocalDate()

beforeEach(async (): Promise<void> => resetServiceState())

describe('curriculum document with person duplicate', () => {
  let admin: EmployeeBuilder
  let daycareSupervisor: EmployeeBuilder
  let preschoolSupervisor: EmployeeBuilder
  let child: PersonBuilder
  let duplicate: PersonBuilder

  beforeEach(async () => {
    admin = await Fixture.employeeAdmin().save()
    const area = await Fixture.careArea().save()
    const daycare = await Fixture.daycare()
      .with({
        areaId: area.data.id,
        type: ['CENTRE'],
        enabledPilotFeatures: ['VASU_AND_PEDADOC']
      })
      .save()
    daycareSupervisor = await Fixture.employeeUnitSupervisor(
      daycare.data.id
    ).save()
    const preschool = await Fixture.daycare()
      .with({
        areaId: area.data.id,
        type: ['PRESCHOOL'],
        enabledPilotFeatures: ['VASU_AND_PEDADOC']
      })
      .save()
    preschoolSupervisor = await Fixture.employeeUnitSupervisor(
      preschool.data.id
    ).save()

    child = await Fixture.person().save()
    await Fixture.child(child.data.id).save()
    await Fixture.placement()
      .with({
        childId: child.data.id,
        unitId: daycare.data.id,
        type: 'DAYCARE_PART_TIME',
        startDate: mockedDate,
        endDate: mockedDate
      })
      .save()

    duplicate = await Fixture.person()
      .with({
        ssn: undefined,
        duplicateOf: child.data.id
      })
      .save()
    await Fixture.child(duplicate.data.id).save()
    await Fixture.placement()
      .with({
        childId: duplicate.data.id,
        unitId: preschool.data.id,
        type: 'PRESCHOOL',
        startDate: mockedDate,
        endDate: mockedDate
      })
      .save()
  })

  it('unit supervisor sees preschool document from duplicate', async () => {
    const templateId = await insertVasuTemplateFixture({
      type: 'PRESCHOOL',
      valid: new FiniteDateRange(mockedDate, mockedDate)
    })
    const documentId = await createVasuDocument({
      body: { childId: duplicate.data.id, templateId }
    })

    const page = await Page.open({ mockedTime })
    await employeeLogin(page, daycareSupervisor.data)
    await page.goto(`${config.employeeUrl}/vasu/${documentId}`)
    const vasuPage = new VasuPage(page)
    await vasuPage.assertDocumentVisible()
  })

  it('unit supervisor sees preschool documents from duplicate', async () => {
    const daycareTemplateId = await insertVasuTemplateFixture({
      type: 'DAYCARE',
      valid: new FiniteDateRange(mockedDate, mockedDate)
    })
    await createVasuDocument({
      body: { childId: duplicate.data.id, templateId: daycareTemplateId }
    })
    const preschoolTemplateId = await insertVasuTemplateFixture({
      type: 'PRESCHOOL',
      valid: new FiniteDateRange(mockedDate, mockedDate)
    })
    const preschoolDocumentId = await createVasuDocument({
      body: {
        childId: duplicate.data.id,
        templateId: preschoolTemplateId
      }
    })

    const page = await Page.open({ mockedTime })
    await employeeLogin(page, daycareSupervisor.data)
    await page.goto(`${config.employeeUrl}/child-information/${child.data.id}`)
    const childInformationPage = new ChildInformationPage(page)
    const childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await childDocumentsSection.assertCurriculumDocuments([
      { id: preschoolDocumentId }
    ])
    const curriculumDocumentPage =
      await childDocumentsSection.openCurriculumDocument(preschoolDocumentId)
    await curriculumDocumentPage.assertDocumentVisible()
    await curriculumDocumentPage.back()
    await childInformationPage.waitUntilLoaded()
  })

  it('unit supervisor sees daycare document from duplicate of', async () => {
    const templateId = await insertVasuTemplateFixture({
      type: 'DAYCARE',
      valid: new FiniteDateRange(mockedDate, mockedDate)
    })
    const documentId = await createVasuDocument({
      body: { childId: child.data.id, templateId }
    })

    const page = await Page.open({ mockedTime })
    await employeeLogin(page, preschoolSupervisor.data)
    await page.goto(`${config.employeeUrl}/vasu/${documentId}`)
    const vasuPage = new VasuPage(page)
    await vasuPage.assertDocumentVisible()
  })

  it('unit supervisor sees daycare documents from duplicate of', async () => {
    const daycareTemplateId = await insertVasuTemplateFixture({
      type: 'DAYCARE',
      valid: new FiniteDateRange(mockedDate, mockedDate)
    })
    const daycareDocumentId = await createVasuDocument({
      body: {
        childId: child.data.id,
        templateId: daycareTemplateId
      }
    })
    const preschoolTemplateId = await insertVasuTemplateFixture({
      type: 'PRESCHOOL',
      valid: new FiniteDateRange(mockedDate, mockedDate)
    })
    await createVasuDocument({
      body: { childId: child.data.id, templateId: preschoolTemplateId }
    })

    const page = await Page.open({ mockedTime })
    await employeeLogin(page, preschoolSupervisor.data)
    await page.goto(
      `${config.employeeUrl}/child-information/${duplicate.data.id}`
    )
    const childInformationPage = new ChildInformationPage(page)
    const childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await childDocumentsSection.assertCurriculumDocuments([
      { id: daycareDocumentId }
    ])
    const curriculumDocumentPage =
      await childDocumentsSection.openCurriculumDocument(daycareDocumentId)
    await curriculumDocumentPage.assertDocumentVisible()
    await curriculumDocumentPage.back()
    await childInformationPage.waitUntilLoaded()
  })

  it('admin can see all documents from duplicate and edit', async () => {
    const daycareTemplateId = await insertVasuTemplateFixture({
      type: 'DAYCARE',
      valid: new FiniteDateRange(mockedDate, mockedDate)
    })
    const daycareDocumentId = await createVasuDocument({
      body: {
        childId: duplicate.data.id,
        templateId: daycareTemplateId
      }
    })
    const preschoolTemplateId = await insertVasuTemplateFixture({
      type: 'PRESCHOOL',
      valid: new FiniteDateRange(mockedDate, mockedDate)
    })
    const preschoolDocumentId = await createVasuDocument({
      body: {
        childId: duplicate.data.id,
        templateId: preschoolTemplateId
      }
    })

    const page = await Page.open({ mockedTime })
    await employeeLogin(page, admin.data)
    await page.goto(`${config.employeeUrl}/child-information/${child.data.id}`)
    const childInformationPage = new ChildInformationPage(page)
    const childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await childDocumentsSection.assertCurriculumDocuments([
      { id: preschoolDocumentId },
      { id: daycareDocumentId }
    ])
    const curriculumDocumentPage =
      await childDocumentsSection.openCurriculumDocument(preschoolDocumentId)
    await curriculumDocumentPage.assertDocumentVisible()
    await curriculumDocumentPage.edit()
    const curriculumDocumentEditPage = new VasuEditPage(page)
    await curriculumDocumentEditPage.waitUntilSaved()
    await curriculumDocumentEditPage.previewBtn.click()
    await curriculumDocumentPage.assertDocumentVisible()
    await curriculumDocumentPage.back()
    await childInformationPage.waitUntilLoaded()
    await childInformationPage.assertName(
      child.data.lastName,
      child.data.firstName
    )
  })
})
