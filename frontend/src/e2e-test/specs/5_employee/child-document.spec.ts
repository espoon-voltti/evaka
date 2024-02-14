// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import config from '../../config'
import { EmployeeBuilder, Fixture, PersonBuilder } from '../../dev-api/fixtures'
import { resetDatabase } from '../../generated/api-clients'
import ChildInformationPage from '../../pages/employee/child-information'
import { ChildDocumentPage } from '../../pages/employee/documents/child-document'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedTime = HelsinkiDateTime.of(2023, 9, 27, 10, 31)
const mockedDate = mockedTime.toLocalDate()

beforeEach(async (): Promise<void> => resetDatabase())

describe('child document with person duplicate', () => {
  let admin: EmployeeBuilder
  let daycareSupervisor: EmployeeBuilder
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

  it('unit supervisor sees hojks document from duplicate', async () => {
    const template = await Fixture.documentTemplate()
      .with({
        type: 'HOJKS',
        validity: new DateRange(mockedDate, mockedDate)
      })
      .save()
    const document = await Fixture.childDocument()
      .with({
        childId: duplicate.data.id,
        templateId: template.data.id
      })
      .save()

    const page = await Page.open({ mockedTime })
    await employeeLogin(page, daycareSupervisor.data)
    await page.goto(`${config.employeeUrl}/child-documents/${document.data.id}`)
    const childDocumentPage = new ChildDocumentPage(page)
    await childDocumentPage.status.waitUntilVisible()
  })

  it('unit supervisor sees hojks documents from duplicate', async () => {
    const pedagogicalAssessmentTemplate = await Fixture.documentTemplate()
      .with({
        type: 'PEDAGOGICAL_ASSESSMENT',
        validity: new DateRange(mockedDate, mockedDate)
      })
      .save()
    await Fixture.childDocument()
      .with({
        childId: duplicate.data.id,
        templateId: pedagogicalAssessmentTemplate.data.id
      })
      .save()
    const pedagogicalReportTemplate = await Fixture.documentTemplate()
      .with({
        type: 'PEDAGOGICAL_REPORT',
        validity: new DateRange(mockedDate, mockedDate)
      })
      .save()
    await Fixture.childDocument()
      .with({
        childId: duplicate.data.id,
        templateId: pedagogicalReportTemplate.data.id
      })
      .save()
    const hojksTemplate = await Fixture.documentTemplate()
      .with({
        type: 'HOJKS',
        validity: new DateRange(mockedDate, mockedDate)
      })
      .save()
    const hojksDocument = await Fixture.childDocument()
      .with({
        childId: duplicate.data.id,
        templateId: hojksTemplate.data.id
      })
      .save()

    const page = await Page.open({ mockedTime })
    await employeeLogin(page, daycareSupervisor.data)
    await page.goto(`${config.employeeUrl}/child-information/${child.data.id}`)
    const childInformationPage = new ChildInformationPage(page)
    const childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await childDocumentsSection.assertChildDocuments([
      { id: hojksDocument.data.id }
    ])
  })

  it('admin can see all documents from duplicate and edit', async () => {
    const pedagogicalAssessmentTemplate = await Fixture.documentTemplate()
      .with({
        type: 'PEDAGOGICAL_ASSESSMENT',
        name: 'Pedagoginen arvio',
        validity: new DateRange(mockedDate, mockedDate)
      })
      .save()
    const pedagogicalAssessmentDocument = await Fixture.childDocument()
      .with({
        childId: duplicate.data.id,
        templateId: pedagogicalAssessmentTemplate.data.id
      })
      .save()
    const pedagogicalReportTemplate = await Fixture.documentTemplate()
      .with({
        type: 'PEDAGOGICAL_REPORT',
        name: 'Pedagoginen selvitys',
        validity: new DateRange(mockedDate, mockedDate)
      })
      .save()
    const pedagogicalReportDocument = await Fixture.childDocument()
      .with({
        childId: duplicate.data.id,
        templateId: pedagogicalReportTemplate.data.id
      })
      .save()
    const hojksTemplate = await Fixture.documentTemplate()
      .with({
        type: 'HOJKS',
        name: 'HOJKS',
        validity: new DateRange(mockedDate, mockedDate)
      })
      .save()
    const hojksDocument = await Fixture.childDocument()
      .with({
        childId: duplicate.data.id,
        templateId: hojksTemplate.data.id
      })
      .save()

    const page = await Page.open({ mockedTime })
    await employeeLogin(page, admin.data)
    await page.goto(`${config.employeeUrl}/child-information/${child.data.id}`)
    const childInformationPage = new ChildInformationPage(page)
    const childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await childDocumentsSection.assertChildDocuments([
      { id: hojksDocument.data.id },
      { id: pedagogicalReportDocument.data.id },
      { id: pedagogicalAssessmentDocument.data.id }
    ])
    const childDocumentPage = await childDocumentsSection.openChildDocument(
      hojksDocument.data.id
    )
    await childDocumentPage.status.waitUntilVisible()
    await childDocumentPage.returnButton.click()
    await childInformationPage.waitUntilLoaded()
    await childInformationPage.assertName(
      child.data.lastName,
      child.data.firstName
    )
  })
})
