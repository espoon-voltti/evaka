// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import type { DevPerson } from '../../generated/api-types'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenPersonalDetailsPage, {
  DeletePasskeyModal
} from '../../pages/citizen/citizen-personal-details'
import { test, expect } from '../../playwright'
import type { Page } from '../../utils/page'
import {
  enduserLogin,
  enduserLoginPasskey,
  enduserLoginWeak
} from '../../utils/user'
import { addVirtualAuthenticator } from '../../utils/virtual-authenticator'

const mockedTime = HelsinkiDateTime.of(2024, 1, 1, 12, 0)

test.use({
  evakaOptions: {
    mockedTime
  }
})

const email = 'test@example.com'
const password = 'aifiefaeC3io?dee'

let citizen: DevPerson

test.beforeEach(async () => {
  await resetServiceState()
  citizen = await Fixture.person({
    email,
    verifiedEmail: email
  }).saveAdult({
    updateMockVtjWithDependants: [],
    updateWeakCredentials: { username: email, password }
  })
})

async function registerPasskey(page: Page) {
  await enduserLogin(page, citizen, '/personal-details')
  const personalDetailsPage = new CitizenPersonalDetailsPage(page)
  await personalDetailsPage.passkeysSection.addPasskey.click()
  await expect(personalDetailsPage.passkeysSection.passkeys).toHaveCount(1)
}

test.describe('Citizen passkeys', () => {
  test('a citizen can register a passkey, rename it, log in with it, and delete it', async ({
    evaka
  }) => {
    await addVirtualAuthenticator(evaka)
    const header = new CitizenHeader(evaka)

    // a new passkey gets a default name that the citizen can then edit
    await registerPasskey(evaka)
    const passkeys = new CitizenPersonalDetailsPage(evaka).passkeysSection
    await expect(passkeys.passkeyName(0)).toHaveText('Pääsyavain')
    await passkeys.editPasskeyName(0, 'Oma Passkey')
    await header.logout()

    await enduserLoginPasskey(evaka)

    // the list is visible under the weak session and shows the last use
    await evaka.goto(config.enduserUrl + '/personal-details')
    await expect(passkeys.passkeyName(0)).toHaveText('Oma Passkey')
    await expect(passkeys.passkeyLastUsed(0)).not.toContainText('Ei koskaan')
    await header.logout()

    // deletion requires a strong session
    await enduserLogin(evaka, citizen, '/personal-details')
    await passkeys.deletePasskey(0).click()
    await new DeletePasskeyModal(evaka).ok.click()
    await expect(passkeys.passkeys).toHaveCount(0)
    await header.logout()

    // the deleted passkey no longer works
    await evaka.goto(config.enduserLoginUrl)
    await evaka.findByDataQa('passkey-login').click()
    await expect(evaka.findByDataQa('passkey-login-error')).toBeVisible()
  })

  test('the login page presents the last used method first with a tag', async ({
    evaka
  }) => {
    await addVirtualAuthenticator(evaka)
    const header = new CitizenHeader(evaka)
    const loginButtons = evaka.findAll(
      '[data-qa="weak-login"], [data-qa="passkey-login"]'
    )

    // unknown state: email login first, no tag
    await evaka.goto(config.enduserLoginUrl)
    await expect(loginButtons).toHaveCount(2)
    await expect(loginButtons.first()).toHaveAttribute('data-qa', 'weak-login')
    await expect(evaka.findByDataQa('used-last-tag')).toBeHidden()

    // after an email login the email method is tagged
    await enduserLoginWeak(evaka, { username: email, password })
    await header.logout()
    await evaka.goto(config.enduserLoginUrl)
    await expect(loginButtons.first()).toHaveAttribute('data-qa', 'weak-login')
    await expect(evaka.findByDataQa('used-last-tag')).toBeVisible()

    // after a passkey login the passkey method is first and tagged
    await registerPasskey(evaka)
    await header.logout()
    await enduserLoginPasskey(evaka)
    await header.logout()
    await evaka.goto(config.enduserLoginUrl)
    await expect(loginButtons.first()).toHaveAttribute(
      'data-qa',
      'passkey-login'
    )
    await expect(evaka.findByDataQa('used-last-tag')).toBeVisible()
  })
})
