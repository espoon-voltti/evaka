// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import { Fixture } from '../../dev-api/fixtures'
import {
  resetServiceState,
  runJobs,
  upsertPasswordBlacklist
} from '../../generated/api-clients'
import type { DevPerson } from '../../generated/api-types'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenPersonalDetailsPage, {
  WeakCredentialsModal
} from '../../pages/citizen/citizen-personal-details'
import { test, expect } from '../../playwright'
import { getVerificationCodeFromEmail } from '../../utils/email'
import type { Page } from '../../utils/page'
import { enduserLogin, enduserLoginWeak } from '../../utils/user'

const mockedTime = HelsinkiDateTime.of(2024, 1, 1, 12, 0)

test.use({
  evakaOptions: {
    mockedTime
  }
})

test.beforeEach(async () => {
  await resetServiceState()
})

async function openPersonalDetailsPage(page: Page, citizen: DevPerson) {
  await enduserLogin(page, citizen)
  const header = new CitizenHeader(page)
  await header.selectTab('personal-details')
  return new CitizenPersonalDetailsPage(page)
}

test.describe('Citizen weak credentials', () => {
  test('a person with an unverified email cannot activate weak credentials', async ({
    evaka
  }) => {
    const citizen = await Fixture.person({
      email: 'test@example.com'
    }).saveAdult({
      updateMockVtjWithDependants: []
    })

    const personalDetailsPage = await openPersonalDetailsPage(evaka, citizen)
    await expect(
      personalDetailsPage.personalDetailsSection.unverifiedEmailStatus
    ).toBeVisible()
    const section = personalDetailsPage.loginDetailsSection
    await expect(section.weakLoginDisabled).toBeVisible()
    await section.activateCredentials.assertDisabled(true)
  })

  test('a person with a verified email can activate weak credentials', async ({
    evaka
  }) => {
    const email = 'test@example.com'
    const citizen = await Fixture.person({
      email,
      verifiedEmail: email
    }).saveAdult({
      updateMockVtjWithDependants: []
    })
    const validPassword = 'aifiefaeC3io?dee'

    const personalDetailsPage = await openPersonalDetailsPage(evaka, citizen)
    await expect(
      personalDetailsPage.personalDetailsSection.verifiedEmailStatus
    ).toBeVisible()
    const section = personalDetailsPage.loginDetailsSection
    await expect(section.weakLoginDisabled).toBeVisible()
    await section.activateCredentials.click()

    const modal = new WeakCredentialsModal(evaka)
    await modal.username.assertAttributeEquals('value', email)
    await modal.password.fill(validPassword)
    await modal.confirmPassword.fill(validPassword)
    // two clicks because there's a separate confirmation page
    await modal.ok.click()
    await modal.ok.click()
    await expect(section.weakLoginEnabled).toBeVisible()
    await section.username.assertTextEquals(email)
  })

  test('a person with weak credentials can change their password', async ({
    evaka
  }) => {
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
    const newPassword = 'EeyahShoqu+oe7th'

    const personalDetailsPage = await openPersonalDetailsPage(evaka, citizen)
    await expect(
      personalDetailsPage.personalDetailsSection.verifiedEmailStatus
    ).toBeVisible()
    const section = personalDetailsPage.loginDetailsSection
    await expect(section.weakLoginEnabled).toBeVisible()
    await section.username.assertTextEquals(email)
    await section.updatePassword.click()

    const modal = new WeakCredentialsModal(evaka)
    await modal.username.assertAttributeEquals('value', email)
    await modal.password.fill(newPassword)
    await modal.confirmPassword.fill(newPassword)
    await modal.ok.click()
    await expect(modal).toBeHidden()
  })

  test('a person with a different email can change their username - email updated on the same page', async ({
    evaka
  }) => {
    const oldEmail = 'old@example.com'
    const newEmail = 'new@example.com'
    const citizen = await Fixture.person({
      email: oldEmail,
      verifiedEmail: oldEmail
    }).saveAdult({
      updateMockVtjWithDependants: [],
      updateWeakCredentials: {
        username: oldEmail,
        password: 'aifiefaeC3io?dee'
      }
    })

    const personalDetailsPage = await openPersonalDetailsPage(evaka, citizen)
    const section = personalDetailsPage.personalDetailsSection

    await section.editPersonalData(
      {
        preferredName: citizen.firstName.split(' ')[1],
        email: newEmail,
        phone: citizen.phone,
        backupPhone: citizen.backupPhone
      },
      true
    )

    await expect(section.verificationCodeField).toBeVisible()
    await runJobs({ mockedTime })
    const verificationCode = await getVerificationCodeFromEmail()
    expect(verificationCode).toBeTruthy()
    await section.verificationCodeField.fill(verificationCode ?? '')
    await section.verifyEmail.click()

    await expect(section.verifiedEmailStatus).toBeVisible()
    await personalDetailsPage.loginDetailsSection.username.assertTextEquals(
      newEmail
    )
  })

  test('a person with a different email can change their username - email updated elsewhere', async ({
    evaka
  }) => {
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

    const personalDetailsPage = await openPersonalDetailsPage(evaka, citizen)
    const section = personalDetailsPage.personalDetailsSection

    await section.updateUsername.click()
    await expect(section.verificationCodeField).toBeVisible()
    await runJobs({ mockedTime })
    const verificationCode = await getVerificationCodeFromEmail()
    expect(verificationCode).toBeTruthy()
    await section.verificationCodeField.fill(verificationCode ?? '')
    await section.verifyEmail.click()

    await expect(section.verifiedEmailStatus).toBeVisible()
    await personalDetailsPage.loginDetailsSection.username.assertTextEquals(
      newEmail
    )
  })

  test('a new password must be valid and acceptable', async ({ evaka }) => {
    const email = 'test@example.com'
    const validPassword = 'aifiefaeC3io?dee'
    const invalidPassword = 'wrong2short'
    const unacceptablePassword = 'TestPassword123!'
    const citizen = await Fixture.person({
      email,
      verifiedEmail: email
    }).saveAdult({
      updateMockVtjWithDependants: [],
      updateWeakCredentials: {
        username: email,
        password: validPassword
      }
    })
    await upsertPasswordBlacklist({ body: [unacceptablePassword] })

    const personalDetailsPage = await openPersonalDetailsPage(evaka, citizen)
    await expect(
      personalDetailsPage.personalDetailsSection.verifiedEmailStatus
    ).toBeVisible()
    const section = personalDetailsPage.loginDetailsSection
    await expect(section.weakLoginEnabled).toBeVisible()
    await section.updatePassword.click()

    const modal = new WeakCredentialsModal(evaka)
    await modal.password.fill(invalidPassword)
    await modal.password.blur()
    await modal.passwordInfo.assertTextEquals('Salasana ei täytä vaatimuksia')
    await modal.ok.assertDisabled(true)
    await modal.password.fill(unacceptablePassword)
    await modal.confirmPassword.fill(invalidPassword)
    await modal.confirmPassword.blur()
    await modal.confirmPasswordInfo.assertTextEquals('Salasanat eivät täsmää')
    await modal.ok.assertDisabled(true)
    await modal.confirmPassword.fill(unacceptablePassword)
    await modal.ok.click()
    await expect(modal.unacceptablePasswordAlert).toBeVisible()

    await modal.password.fill(validPassword)
    await modal.confirmPassword.fill(validPassword)
    await modal.confirmPassword.blur()
    await expect(modal.passwordInfo).toBeHidden()
    await expect(modal.confirmPasswordInfo).toBeHidden()
    await expect(modal.unacceptablePasswordAlert).toBeHidden()
    await modal.ok.click()
    await expect(modal).toBeHidden()
  })

  test('credentials change invalidates existing weak credential sessions', async ({
    newEvakaPage
  }) => {
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
    const strongSession = await newEvakaPage()
    const weakSession = await newEvakaPage()
    const newPassword = 'EeyahShoqu+oe7th'

    await enduserLogin(strongSession, citizen)
    await enduserLoginWeak(weakSession, {
      username: email,
      password: 'aifiefaeC3io?dee'
    })
    const header = new CitizenHeader(strongSession)
    await header.selectTab('personal-details')
    const personalDetailsPage = new CitizenPersonalDetailsPage(strongSession)
    const section = personalDetailsPage.loginDetailsSection
    await expect(section.weakLoginEnabled).toBeVisible()
    await section.username.assertTextEquals(email)
    await section.updatePassword.click()

    const modal = new WeakCredentialsModal(strongSession)
    await modal.username.assertAttributeEquals('value', email)
    await modal.password.fill(newPassword)
    await modal.confirmPassword.fill(newPassword)
    await modal.ok.click()
    await expect(modal).toBeHidden()

    // Weak session should be logged out
    await weakSession.findByDataQa('desktop-nav').click()
    await expect(
      weakSession.findByDataQa('session-expired-modal')
    ).toBeVisible()
  })
})
