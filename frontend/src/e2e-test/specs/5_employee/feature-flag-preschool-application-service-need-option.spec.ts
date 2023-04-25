// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import { insertDaycareGroupFixtures, resetDatabase } from '../../dev-api'
import type { AreaAndPersonFixtures } from '../../dev-api/data-init'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import { daycareGroupFixture, Fixture } from '../../dev-api/fixtures'
import type CreateApplicationModal from '../../pages/employee/applications/create-application-modal'
import ChildInformationPage from '../../pages/employee/child-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let childInformationPage: ChildInformationPage

let fixtures: AreaAndPersonFixtures
let page: Page
let createApplicationModal: CreateApplicationModal

beforeEach(async () => {
  await resetDatabase()
  fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])
  await Fixture.serviceNeedOption()
    .with({ validPlacementType: 'PRESCHOOL_DAYCARE', nameFi: 'vaka' })
    .save()
  await Fixture.serviceNeedOption()
    .with({ validPlacementType: 'PRESCHOOL_CLUB', nameFi: 'kerho' })
    .save()
  const admin = await Fixture.employeeAdmin().save()

  page = await Page.open({
    employeeCustomizations: {
      featureFlags: {
        preschoolApplication: {
          serviceNeedOption: true
        }
      }
    }
  })
  await employeeLogin(page, admin.data)

  childInformationPage = new ChildInformationPage(page)
  await childInformationPage.navigateToChild(
    fixtures.enduserChildFixtureJari.id
  )

  const applications = await childInformationPage.openCollapsible(
    'applications'
  )
  createApplicationModal = await applications.openCreateApplicationModal()
})

describe('Employee - paper application', () => {
  test('Service worker fills preschool application with service need option enabled', async () => {
    await createApplicationModal.selectApplicationType('PRESCHOOL')
    const applicationEditPage = await createApplicationModal.submit()

    await applicationEditPage.fillStartDate(
      LocalDate.todayInSystemTz().format()
    )
    await applicationEditPage.checkConnectedDaycare()
    await applicationEditPage.fillConnectedDaycarePreferredStartDate(
      LocalDate.todayInSystemTz().format()
    )
    await applicationEditPage.selectPreschoolPlacementType('PRESCHOOL_DAYCARE')
    await applicationEditPage.selectPreschoolServiceNeedOption('vaka')
    await applicationEditPage.pickUnit(fixtures.daycareFixture.name)
    await applicationEditPage.fillApplicantPhoneAndEmail(
      '123456',
      'email@evaka.test'
    )
    const applicationViewPage = await applicationEditPage.saveApplication()
    await applicationViewPage.waitUntilLoaded()
  })
})
