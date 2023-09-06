// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { BrowserContextOptions } from 'playwright'

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import { EvakaBrowserContextOptions } from '../../browser'
import config from '../../config'
import {
  insertDaycareGroupFixtures,
  insertDefaultServiceNeedOptions,
  resetDatabase
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import { daycareGroupFixture, Fixture } from '../../dev-api/fixtures'
import { EmployeeDetail } from '../../dev-api/types'
import ChildInformationPage, {
  AssistanceSection
} from '../../pages/employee/child-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedTime = LocalDate.of(2021, 8, 16)

let page: Page
let childInformationPage: ChildInformationPage
let assistance: AssistanceSection
let childId: UUID

let admin: EmployeeDetail

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  await insertDefaultServiceNeedOptions()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  childId = fixtures.familyWithTwoGuardians.children[0].id
})

const openPageWithFeatureFlags = async (
  featureFlagOptions?: BrowserContextOptions & EvakaBrowserContextOptions
) => {
  page = await Page.open(featureFlagOptions)
  admin = (await Fixture.employeeAdmin().save()).data
}

const logUserIn = async (user: EmployeeDetail) => {
  await employeeLogin(page, user)
  await page.goto(config.employeeUrl + '/child-information/' + childId)
  childInformationPage = new ChildInformationPage(page)
  assistance = await childInformationPage.openCollapsible('assistance')
}

describe('Child Information assistance functionality for employees', () => {
  it('should show GENERAL_SUPPORT for daycare assistance level based if featureflag disableGeneralSupportDaycareAssistanceLevel is false', async () => {
    await openPageWithFeatureFlags({
      mockedTime: mockedTime.toSystemTzDate(),
      employeeCustomizations: {
        featureFlags: {
          hideGeneralSupportDaycareAssistanceLevel: false
        }
      }
    })
    await logUserIn(admin)
    await assistance.createDaycareAssistanceButton.click()
    await assistance.daycareAssistanceForm.assertValues((values) =>
      values.includes('Yleinen tuki, ei päätöstä')
    )
  })

  it('should hide GENERAL_SUPPORT for daycare assistance level based if featureflag disableGeneralSupportDaycareAssistanceLevel is true', async () => {
    await openPageWithFeatureFlags({
      mockedTime: mockedTime.toSystemTzDate(),
      employeeCustomizations: {
        featureFlags: {
          hideGeneralSupportDaycareAssistanceLevel: true
        }
      }
    })
    await logUserIn(admin)
    await assistance.createDaycareAssistanceButton.click()
    await assistance.daycareAssistanceForm.assertValues(
      (values) => !values.includes('Yleinen tuki, ei päätöstä')
    )
  })
})
