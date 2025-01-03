// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState, runJobs } from '../../generated/api-clients'
import { DevPerson } from '../../generated/api-types'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenPersonalDetailsPage, {
  WeakCredentialsModal
} from '../../pages/citizen/citizen-personal-details'
import { getVerificationCodeFromEmail } from '../../utils/email'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

const mockedTime = HelsinkiDateTime.of(2024, 1, 1, 12, 0)
let page: Page

beforeEach(async () => {
  await resetServiceState()
})

describe('Citizen weak credentials', () => {
  test('a person with an unverified email cannot activate weak credentials', async () => {
    const citizen = await Fixture.person({
      email: 'test@example.com'
    }).saveAdult({
      updateMockVtjWithDependants: []
    })
    page = await Page.open({ mockedTime })

    const personalDetailsPage = await openPersonalDetailsPage(citizen)
    await personalDetailsPage.personalDetailsSection.unverifiedEmailStatus.waitUntilVisible()
    const section = personalDetailsPage.loginDetailsSection
    await section.weakLoginDisabled.waitUntilVisible()
    await section.activateCredentials.assertDisabled(true)
  })
  test('a person with a verified email can activate weak credentials', async () => {
    const email = 'test@example.com'
    const citizen = await Fixture.person({
      email,
      verifiedEmail: email
    }).saveAdult({
      updateMockVtjWithDependants: []
    })
    page = await Page.open({ mockedTime })
    const validPassword = 'aifiefaeC3io?dee'

    const personalDetailsPage = await openPersonalDetailsPage(citizen)
    await personalDetailsPage.personalDetailsSection.verifiedEmailStatus.waitUntilVisible()
    const section = personalDetailsPage.loginDetailsSection
    await section.weakLoginDisabled.waitUntilVisible()
    await section.activateCredentials.click()

    const modal = new WeakCredentialsModal(page)
    await modal.username.assertAttributeEquals('value', email)
    await modal.password.fill(validPassword)
    await modal.confirmPassword.fill(validPassword)
    // two clicks because there's a separate confirmation page
    await modal.ok.click()
    await modal.ok.click()
    await section.weakLoginEnabled.waitUntilVisible()
    await section.username.assertTextEquals(email)
  })
  test('a person with weak credentials can change their password', async () => {
    const email = 'test@example.com'
    const citizen = await Fixture.person({
      email,
      verifiedEmail: email
    }).saveAdult({
      updateMockVtjWithDependants: [],
      updateWeakCredentials: {
        username: email,
        password: 'aifiefaeC3io?dee'
      }
    })
    page = await Page.open({ mockedTime })
    const newPassword = 'EeyahShoqu+oe7th'

    const personalDetailsPage = await openPersonalDetailsPage(citizen)
    await personalDetailsPage.personalDetailsSection.verifiedEmailStatus.waitUntilVisible()
    const section = personalDetailsPage.loginDetailsSection
    await section.weakLoginEnabled.waitUntilVisible()
    await section.username.assertTextEquals(email)
    await section.updatePassword.click()

    const modal = new WeakCredentialsModal(page)
    await modal.username.assertAttributeEquals('value', email)
    await modal.password.fill(newPassword)
    await modal.confirmPassword.fill(newPassword)
    await modal.ok.click()
    await modal.waitUntilHidden()
  })
  test('a person with a different email can change their username', async () => {
    const oldEmail = 'old@example.com'
    const newEmail = 'new@example.com'
    const citizen = await Fixture.person({
      email: newEmail,
      verifiedEmail: oldEmail
    }).saveAdult({
      updateMockVtjWithDependants: [],
      updateWeakCredentials: {
        username: oldEmail,
        password: 'aifiefaeC3io?dee'
      }
    })
    page = await Page.open({ mockedTime })

    const personalDetailsPage = await openPersonalDetailsPage(citizen)
    const section = personalDetailsPage.personalDetailsSection

    await section.updateUsername.click()
    await section.verificationCodeField.waitUntilVisible()
    await runJobs({ mockedTime })
    const verificationCode = await getVerificationCodeFromEmail()
    expect(verificationCode).toBeTruthy()
    await section.verificationCodeField.fill(verificationCode ?? '')
    await section.verifyEmail.click()

    await section.verifiedEmailStatus.waitUntilVisible()
    await personalDetailsPage.loginDetailsSection.username.assertTextEquals(
      newEmail
    )
  })
})

async function openPersonalDetailsPage(citizen: DevPerson) {
  await enduserLogin(page, citizen)
  const header = new CitizenHeader(page)
  await header.selectTab('personal-details')
  return new CitizenPersonalDetailsPage(page)
}
