// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import libqp from 'libqp'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import {
  DummySuomiFiConfirmPage,
  DummySuomiFiLoginPage
} from '../../pages/citizen/citizen-dummy-suomifi'
import {
  ConfirmPage,
  ForgotPasswordPage,
  KeycloakLoginPage,
  UpdatePasswordPage
} from '../../pages/citizen/citizen-keycloak'
import { waitUntilEqual } from '../../utils'
import {
  createSuomiFiUser,
  deleteAllSuomiFiUsers
} from '../../utils/dummy-suomifi'
import { KeycloakRealmClient } from '../../utils/keycloak'
import {
  deleteCapturedEmails,
  getCapturedEmails,
  Messages
} from '../../utils/mailhog'
import { Page } from '../../utils/page'

let keycloak: KeycloakRealmClient

beforeEach(async () => {
  await resetServiceState()
  await deleteCapturedEmails()
  await deleteAllSuomiFiUsers()
  keycloak = await KeycloakRealmClient.createCitizenClient()
  await keycloak.deleteAllUsers()
})

test('Forgot my password', async () => {
  const user = {
    username: 'test@example.com',
    enabled: true,
    firstName: 'Seppo',
    lastName: 'Sorsa',
    email: 'test@example.com',
    socialSecurityNumber: '070644-937X',
    password: 'test123'
  }
  await Fixture.person({
    ssn: user.socialSecurityNumber,
    firstName: user.firstName,
    lastName: user.lastName
  }).saveAdult({ updateMockVtjWithDependants: [] })
  await keycloak.createUser(user)

  const page = await Page.open()
  await page.goto(config.enduserUrl)
  await page.findByDataQa('weak-login').click()

  const loginPage = new KeycloakLoginPage(page)
  await loginPage.forgotPasswordLink.click()

  const forgotPasswordPage = new ForgotPasswordPage(page)
  await forgotPasswordPage.backToLoginLink.click()
  await loginPage.forgotPasswordLink.click()
  await forgotPasswordPage.email.fill(user.email)
  await forgotPasswordPage.changePasswordButton.click()

  const emails = await getCapturedEmails()
  const resetPasswordLink = extractResetLink(emails, user.email)

  await page.goto(resetPasswordLink)
  const newPassword = 'eurie30ofeec'
  const updatePasswordPage = new UpdatePasswordPage(page)
  await updatePasswordPage.newPassword.fill(newPassword)
  await updatePasswordPage.confirmPassword.fill(newPassword)
  await updatePasswordPage.changePasswordButton.click()

  await page.findByDataQa('header-city-logo').waitUntilVisible()
})

function extractResetLink(emails: Messages, toAddress: string) {
  expect(emails.count).toStrictEqual(1)
  const email = emails.items[0]
  expect(email.Raw.To).toEqual([toAddress])
  const textPart = email.MIME.Parts.find((part) =>
    part.Headers['Content-Type']?.some((headerValue) =>
      headerValue.startsWith('text/plain')
    )
  )
  if (!textPart) {
    throw new Error('No text/plain MIME part in e-mail')
  }
  const needsDecoding = textPart.Headers['Content-Transfer-Encoding'].some(
    (headerValue) => headerValue === 'quoted-printable'
  )
  const emailBody = needsDecoding
    ? libqp.decode(textPart.Body).toString()
    : textPart.Body

  const linkRegex = /(https?:\/\/\S+)/g
  const linkMatches = linkRegex.exec(emailBody)
  if (!linkMatches) {
    throw new Error('Failed to find link in e-mail')
  }
  expect(linkMatches.length).toStrictEqual(2)
  return linkMatches[1]
}

test('Registration via suomi.fi', async () => {
  const user = {
    ssn: '010106A9953',
    commonName: 'Eemeli Esimerkki',
    givenName: 'Eemeli',
    surname: 'Esimerkki'
  }
  await Fixture.person({
    ssn: user.ssn,
    firstName: user.givenName,
    lastName: user.surname
  }).saveAdult({ updateMockVtjWithDependants: [] })
  const email = 'test@example.com'
  await createSuomiFiUser(user)

  const page = await Page.open()
  await page.goto(config.enduserUrl)
  await page.findByDataQa('weak-login').click()

  const loginPage = new KeycloakLoginPage(page)
  await loginPage.registerLink.click()

  const suomiFiLoginPage = new DummySuomiFiLoginPage(page)
  await suomiFiLoginPage.userRadio(user.commonName).click()
  await suomiFiLoginPage.loginButton.click()

  const suomiFiConfirmPage = new DummySuomiFiConfirmPage(page)
  await suomiFiConfirmPage.proceedButton.click()

  const confirmPage = new ConfirmPage(page)
  await confirmPage.heading.assertTextEquals('Luo uusi eVaka-tunnus')
  // double-check we don't have any extra fields that shouldn't be there
  await waitUntilEqual(
    () => confirmPage.allLabels.allTexts(),
    ['Sähköpostiosoite']
  )
  await confirmPage.email.fill(email)
  await confirmPage.sendButton.click()

  await page.findByDataQa('header-city-logo').waitUntilVisible()
})
