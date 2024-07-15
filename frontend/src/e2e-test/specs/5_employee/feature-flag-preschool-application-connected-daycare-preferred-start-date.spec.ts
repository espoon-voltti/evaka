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
import CreateApplicationModal from '../../pages/employee/applications/create-application-modal'
import ChildInformationPage from '../../pages/employee/child-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let childInformationPage: ChildInformationPage

let page: Page
let createApplicationModal: CreateApplicationModal
const now = HelsinkiDateTime.of(2023, 3, 15, 12, 0)

beforeEach(async () => {
  await resetServiceState()
  await Fixture.preschoolTerm(preschoolTerm2022).save()
  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family({ guardian: testAdult, children: [testChild] }).save()
  await createDaycareGroups({ body: [testDaycareGroup] })
  const admin = await Fixture.employeeAdmin().save()

  page = await Page.open({
    mockedTime: now,
    employeeCustomizations: {
      featureFlags: {
        preschoolApplication: {
          connectedDaycarePreferredStartDate: false
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
  test('Service worker fills preschool application with connected daycare preferred start date disabled', async () => {
    await createApplicationModal.selectApplicationType('PRESCHOOL')
    const applicationEditPage = await createApplicationModal.submit()

    await applicationEditPage.fillStartDate(now.toLocalDate().format())
    await applicationEditPage.checkConnectedDaycare()
    await applicationEditPage.fillTimes()
    await applicationEditPage.pickUnit(testDaycare.name)
    await applicationEditPage.fillApplicantPhoneAndEmail(
      '123456',
      'email@evaka.test'
    )
    const applicationViewPage = await applicationEditPage.saveApplication()
    await applicationViewPage.waitUntilLoaded()
  })
})
