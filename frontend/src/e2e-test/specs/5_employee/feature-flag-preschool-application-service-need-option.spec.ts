// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import {
  testDaycareGroup,
  Fixture,
  testAdult,
  testChild,
  testCareArea,
  testDaycare,
  preschoolTerm2022
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  resetServiceState
} from '../../generated/api-clients'
import type CreateApplicationModal from '../../pages/employee/applications/create-application-modal'
import ChildInformationPage from '../../pages/employee/child-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let childInformationPage: ChildInformationPage

let page: Page
let createApplicationModal: CreateApplicationModal
const now = HelsinkiDateTime.of(2023, 3, 15, 12, 0)

beforeEach(async () => {
  await resetServiceState()
  await preschoolTerm2022.save()
  await testCareArea.save()
  await testDaycare.save()
  await Fixture.family({ guardian: testAdult, children: [testChild] }).save()
  await createDaycareGroups({ body: [testDaycareGroup] })
  await Fixture.serviceNeedOption({
    validPlacementType: 'PRESCHOOL_DAYCARE',
    nameFi: 'vaka'
  }).save()
  await Fixture.serviceNeedOption({
    validPlacementType: 'PRESCHOOL_CLUB',
    nameFi: 'kerho'
  }).save()
  const admin = await Fixture.employee().admin().save()

  page = await Page.open({
    mockedTime: now,
    employeeCustomizations: {
      featureFlags: {
        preschoolApplication: {
          serviceNeedOption: true
        }
      }
    }
  })
  await employeeLogin(page, admin)

  childInformationPage = new ChildInformationPage(page)
  await childInformationPage.navigateToChild(testChild.id)

  const applications =
    await childInformationPage.openCollapsible('applications')
  createApplicationModal = await applications.openCreateApplicationModal()
})

describe('Employee - paper application', () => {
  test('Service worker fills preschool application with service need option enabled', async () => {
    await createApplicationModal.selectApplicationType('PRESCHOOL')
    const applicationEditPage = await createApplicationModal.submit()

    await applicationEditPage.fillStartDate(now.toLocalDate())
    await applicationEditPage.checkConnectedDaycare()
    await applicationEditPage.fillConnectedDaycarePreferredStartDate(
      now.toLocalDate().format()
    )
    await applicationEditPage.selectPreschoolPlacementType('PRESCHOOL_DAYCARE')
    await applicationEditPage.selectPreschoolServiceNeedOption('vaka')
    await applicationEditPage.pickUnit(testDaycare.name)
    await applicationEditPage.fillApplicantPhoneAndEmail(
      '123456',
      'email@evaka.test'
    )
    const applicationViewPage = await applicationEditPage.saveApplication()
    await applicationViewPage.waitUntilLoaded()
  })
})
