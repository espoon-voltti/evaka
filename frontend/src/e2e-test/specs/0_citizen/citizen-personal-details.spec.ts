// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { resetDatabase } from '../../dev-api'
import { enduserGuardianFixture, Fixture } from '../../dev-api/fixtures'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenPersonalDetailsPage from '../../pages/citizen/citizen-personal-details'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

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
  test('Citizen sees indications of missing email and phone', async () => {
    await header.checkPersonalDetailsAttentionIndicatorsAreShown()
    await header.navigateToPersonalDetailsPage()
    await personalDetailsPage.checkMissingEmailWarningIsShown()
    await personalDetailsPage.checkMissingPhoneWarningIsShown()
  })

  test('Citizen fills successfully personal data without email by selecting I have no email -option', async () => {
    const data = {
      preferredName: enduserGuardianFixture.firstName.split(' ')[1],
      phone: '123123',
      backupPhone: '456456',
      email: null
    }

    await header.navigateToPersonalDetailsPage()
    await personalDetailsPage.editPersonalData(data, true)
    await personalDetailsPage.checkPersonalData(data)
    await personalDetailsPage.assertAlertIsNotShown()
  })

  test('Citizen fills in personal data but cannot save without phone', async () => {
    const data = {
      preferredName: enduserGuardianFixture.firstName.split(' ')[1],
      phone: null,
      backupPhone: '456456',
      email: 'a@b.com'
    }

    await header.navigateToPersonalDetailsPage()
    await personalDetailsPage.editPersonalData(data, false)
    await personalDetailsPage.assertSaveIsDisabled()
  })

  test('Citizen fills in personal data correctly and saves', async () => {
    const data = {
      preferredName: enduserGuardianFixture.firstName.split(' ')[1],
      phone: '123456789',
      backupPhone: '456456',
      email: 'a@b.com'
    }

    await header.navigateToPersonalDetailsPage()
    await personalDetailsPage.editPersonalData(data, true)
    await personalDetailsPage.checkPersonalData(data)
    await personalDetailsPage.assertAlertIsNotShown()
  })
})
