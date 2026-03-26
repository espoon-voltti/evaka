// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type { DevEmployee, DevPerson } from '../../generated/api-types'
import ChildInformationPage from '../../pages/employee/child-information'
import { ChildDocumentPage } from '../../pages/employee/documents/child-document'
import { test, expect } from '../../playwright'
import { employeeLogin } from '../../utils/user'

const mockedTime = HelsinkiDateTime.of(2023, 9, 27, 10, 31)
const mockedDate = mockedTime.toLocalDate()

test.describe('child document with person duplicate', () => {
  test.use({ evakaOptions: { mockedTime } })

  let admin: DevEmployee
  let daycareSupervisor: DevEmployee
  let child: DevPerson
  let duplicate: DevPerson

  test.beforeEach(async () => {
    await resetServiceState()
    admin = await Fixture.employee().admin().save()
    const area = await Fixture.careArea().save()
    const daycare = await Fixture.daycare({
      areaId: area.id,
      type: ['CENTRE'],
      enabledPilotFeatures: ['VASU_AND_PEDADOC', 'OTHER_DECISION']
    }).save()
    daycareSupervisor = await Fixture.employee()
      .unitSupervisor(daycare.id)
      .save()
    const preschool = await Fixture.daycare({
      areaId: area.id,
      type: ['PRESCHOOL'],
      enabledPilotFeatures: ['VASU_AND_PEDADOC', 'OTHER_DECISION']
    }).save()

    child = await Fixture.person().saveChild({ updateMockVtj: true })
    await Fixture.placement({
      childId: child.id,
      unitId: daycare.id,
      type: 'DAYCARE_PART_TIME',
      startDate: mockedDate,
      endDate: mockedDate
    }).save()

    duplicate = await Fixture.person({
      ssn: null,
      duplicateOf: child.id
    }).saveChild()
    await Fixture.placement({
      childId: duplicate.id,
      unitId: preschool.id,
      type: 'PRESCHOOL',
      startDate: mockedDate,
      endDate: mockedDate
    }).save()
  })

  test('unit supervisor sees hojks document from duplicate', async ({
    evaka
  }) => {
    const template = await Fixture.documentTemplate({
      type: 'HOJKS',
      validity: new DateRange(mockedDate, mockedDate)
    }).save()
    const document = await Fixture.childDocument({
      childId: duplicate.id,
      templateId: template.id
    }).save()

    await employeeLogin(evaka, daycareSupervisor)
    await evaka.goto(`${config.employeeUrl}/child-documents/${document.id}`)
    const childDocumentPage = new ChildDocumentPage(evaka)
    await expect(childDocumentPage.status).toBeVisible()
  })

  test('unit supervisor sees hojks documents from duplicate', async ({
    evaka
  }) => {
    const pedagogicalAssessmentTemplate = await Fixture.documentTemplate({
      type: 'PEDAGOGICAL_ASSESSMENT',
      validity: new DateRange(mockedDate, mockedDate)
    }).save()
    await Fixture.childDocument({
      childId: duplicate.id,
      templateId: pedagogicalAssessmentTemplate.id
    }).save()
    const pedagogicalReportTemplate = await Fixture.documentTemplate({
      type: 'PEDAGOGICAL_REPORT',
      validity: new DateRange(mockedDate, mockedDate)
    }).save()
    await Fixture.childDocument({
      childId: duplicate.id,
      templateId: pedagogicalReportTemplate.id
    }).save()
    const hojksTemplate = await Fixture.documentTemplate({
      type: 'HOJKS',
      validity: new DateRange(mockedDate, mockedDate)
    }).save()
    const hojksDocument = await Fixture.childDocument({
      childId: duplicate.id,
      templateId: hojksTemplate.id
    }).save()

    await employeeLogin(evaka, daycareSupervisor)
    await evaka.goto(`${config.employeeUrl}/child-information/${child.id}`)
    const childInformationPage = new ChildInformationPage(evaka)
    const childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await childDocumentsSection.assertInternalChildDocuments([
      { id: hojksDocument.id }
    ])
  })

  test('admin can see all documents from duplicate and edit', async ({
    evaka
  }) => {
    const pedagogicalAssessmentTemplate = await Fixture.documentTemplate({
      type: 'PEDAGOGICAL_ASSESSMENT',
      name: 'Pedagoginen arvio',
      validity: new DateRange(mockedDate, mockedDate)
    }).save()
    const pedagogicalAssessmentDocument = await Fixture.childDocument({
      childId: duplicate.id,
      templateId: pedagogicalAssessmentTemplate.id
    }).save()
    const pedagogicalReportTemplate = await Fixture.documentTemplate({
      type: 'PEDAGOGICAL_REPORT',
      name: 'Pedagoginen selvitys',
      validity: new DateRange(mockedDate, mockedDate)
    }).save()
    const pedagogicalReportDocument = await Fixture.childDocument({
      childId: duplicate.id,
      templateId: pedagogicalReportTemplate.id
    }).save()
    const hojksTemplate = await Fixture.documentTemplate({
      type: 'HOJKS',
      name: 'HOJKS',
      validity: new DateRange(mockedDate, mockedDate)
    }).save()
    const hojksDocument = await Fixture.childDocument({
      childId: duplicate.id,
      templateId: hojksTemplate.id
    }).save()

    await employeeLogin(evaka, admin)
    await evaka.goto(`${config.employeeUrl}/child-information/${child.id}`)
    const childInformationPage = new ChildInformationPage(evaka)
    const childDocumentsSection =
      await childInformationPage.openCollapsible('childDocuments')
    await childDocumentsSection.assertInternalChildDocuments([
      { id: hojksDocument.id },
      { id: pedagogicalReportDocument.id },
      { id: pedagogicalAssessmentDocument.id }
    ])
    const childDocumentPage = await childDocumentsSection.openChildDocument(
      hojksDocument.id
    )
    await expect(childDocumentPage.status).toBeVisible()
    await childDocumentPage.returnButton.click()
    await childInformationPage.waitUntilLoaded()
    await childInformationPage.assertName(child.lastName, child.firstName)
  })
})
