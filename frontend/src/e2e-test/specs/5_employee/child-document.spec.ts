// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import { DevEmployee, DevPerson } from '../../generated/api-types'
import ChildInformationPage from '../../pages/employee/child-information'
import { ChildDocumentPage } from '../../pages/employee/documents/child-document'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedTime = HelsinkiDateTime.of(2023, 9, 27, 10, 31)
const mockedDate = mockedTime.toLocalDate()

beforeEach(async (): Promise<void> => resetServiceState())

describe('child document with person duplicate', () => {
  let admin: DevEmployee
  let daycareSupervisor: DevEmployee
  let child: DevPerson
  let duplicate: DevPerson

  beforeEach(async () => {
    admin = await Fixture.employeeAdmin().save()
    const area = await Fixture.careArea().save()
    const daycare = await Fixture.daycare({
      areaId: area.id,
      type: ['CENTRE'],
      enabledPilotFeatures: ['VASU_AND_PEDADOC']
    }).save()
    daycareSupervisor = await Fixture.employeeUnitSupervisor(daycare.id).save()
    const preschool = await Fixture.daycare({
      areaId: area.id,
      type: ['PRESCHOOL'],
      enabledPilotFeatures: ['VASU_AND_PEDADOC']
    }).save()

    child = await Fixture.person().saveChild()
    await Fixture.placement({
      childId: child.id,
      unitId: daycare.id,
      type: 'DAYCARE_PART_TIME',
      startDate: mockedDate,
      endDate: mockedDate
    }).save()

    duplicate = await Fixture.person()
      .with({
        ssn: null,
        duplicateOf: child.id
      })
      .saveChild()
    await Fixture.placement({
      childId: duplicate.id,
      unitId: preschool.id,
      type: 'PRESCHOOL',
      startDate: mockedDate,
      endDate: mockedDate
    }).save()
  })

  it('unit supervisor sees hojks document from duplicate', async () => {
    const template = await Fixture.documentTemplate()
      .with({
        type: 'HOJKS',
        validity: new DateRange(mockedDate, mockedDate)
      })
      .save()
    const document = await Fixture.childDocument({
      childId: duplicate.id,
      templateId: template.id
    }).save()

    const page = await Page.open({ mockedTime })
    await employeeLogin(page, daycareSupervisor)
    await page.goto(`${config.employeeUrl}/child-documents/${document.id}`)
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
    await Fixture.childDocument({
      childId: duplicate.id,
      templateId: pedagogicalAssessmentTemplate.id
    }).save()
    const pedagogicalReportTemplate = await Fixture.documentTemplate()
      .with({
        type: 'PEDAGOGICAL_REPORT',
        validity: new DateRange(mockedDate, mockedDate)
      })
      .save()
    await Fixture.childDocument({
      childId: duplicate.id,
      templateId: pedagogicalReportTemplate.id
    }).save()
    const hojksTemplate = await Fixture.documentTemplate()
      .with({
        type: 'HOJKS',
        validity: new DateRange(mockedDate, mockedDate)
      })
      .save()
    const hojksDocument = await Fixture.childDocument({
      childId: duplicate.id,
      templateId: hojksTemplate.id
    }).save()

    const page = await Page.open({ mockedTime })
    await employeeLogin(page, daycareSupervisor)
    await page.goto(`${config.employeeUrl}/child-information/${child.id}`)
    const childInformationPage = new ChildInformationPage(page)
    const childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await childDocumentsSection.assertChildDocuments([{ id: hojksDocument.id }])
  })

  it('admin can see all documents from duplicate and edit', async () => {
    const pedagogicalAssessmentTemplate = await Fixture.documentTemplate()
      .with({
        type: 'PEDAGOGICAL_ASSESSMENT',
        name: 'Pedagoginen arvio',
        validity: new DateRange(mockedDate, mockedDate)
      })
      .save()
    const pedagogicalAssessmentDocument = await Fixture.childDocument({
      childId: duplicate.id,
      templateId: pedagogicalAssessmentTemplate.id
    }).save()
    const pedagogicalReportTemplate = await Fixture.documentTemplate()
      .with({
        type: 'PEDAGOGICAL_REPORT',
        name: 'Pedagoginen selvitys',
        validity: new DateRange(mockedDate, mockedDate)
      })
      .save()
    const pedagogicalReportDocument = await Fixture.childDocument({
      childId: duplicate.id,
      templateId: pedagogicalReportTemplate.id
    }).save()
    const hojksTemplate = await Fixture.documentTemplate()
      .with({
        type: 'HOJKS',
        name: 'HOJKS',
        validity: new DateRange(mockedDate, mockedDate)
      })
      .save()
    const hojksDocument = await Fixture.childDocument({
      childId: duplicate.id,
      templateId: hojksTemplate.id
    }).save()

    const page = await Page.open({ mockedTime })
    await employeeLogin(page, admin)
    await page.goto(`${config.employeeUrl}/child-information/${child.id}`)
    const childInformationPage = new ChildInformationPage(page)
    const childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await childDocumentsSection.assertChildDocuments([
      { id: hojksDocument.id },
      { id: pedagogicalReportDocument.id },
      { id: pedagogicalAssessmentDocument.id }
    ])
    const childDocumentPage = await childDocumentsSection.openChildDocument(
      hojksDocument.id
    )
    await childDocumentPage.status.waitUntilVisible()
    await childDocumentPage.returnButton.click()
    await childInformationPage.waitUntilLoaded()
    await childInformationPage.assertName(child.lastName, child.firstName)
  })
})
