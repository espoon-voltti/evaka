// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { enduserLogin } from 'e2e-playwright/utils/user'
import { resetDatabase } from 'e2e-test-common/dev-api'
import {
  enduserGuardianFixture,
  Fixture
} from 'e2e-test-common/dev-api/fixtures'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenPersonalDetailsPage from '../../pages/citizen/citizen-personal-details'
import { Page } from '../../utils/page'

let page: Page
let header: CitizenHeader
let personalDetailsPage: CitizenPersonalDetailsPage

const citizenFixture = {
  ...enduserGuardianFixture,
  preferredName: '',
  phone: '',
  backupPhone: '',
  email: null
}

beforeEach(async () => {
  await resetDatabase()
  await Fixture.person().with(citizenFixture).save()

  page = await Page.open()
  await enduserLogin(page)
  header = new CitizenHeader(page)
  personalDetailsPage = new CitizenPersonalDetailsPage(page)
})

describe('Citizen personal details', () => {
  test('Citizen sees indications of missing email', async () => {
    await header.checkPersonalDetailsAttentionIndicatorsAreShown()
    await header.navigateToPersonalDetailsPage()
    await personalDetailsPage.checkMissingEmailWarningIsShown()
  })

  test('Citizen fills in personal data', async () => {
    const data = {
      preferredName: enduserGuardianFixture.firstName.split(' ')[1],
      phone: '123123',
      backupPhone: '456456',
      email: null
    }

    await header.navigateToPersonalDetailsPage()
    await personalDetailsPage.editPersonalData(data)
    await personalDetailsPage.checkPersonalData(data)
  })
})
