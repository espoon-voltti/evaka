// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState, runJobs } from '../../generated/api-clients'
import type { DevPerson } from '../../generated/api-types'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenPersonalDetailsPage from '../../pages/citizen/citizen-personal-details'
import { test, expect } from '../../playwright'
import { getVerificationCodeFromEmail } from '../../utils/email'
import type { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

const mockedTime = HelsinkiDateTime.of(2024, 1, 1, 12, 0)

test.describe('Citizen e-mail verification', () => {
  let page: Page

  test.use({ evakaOptions: { mockedTime } })

  test.beforeEach(async () => {
    await resetServiceState()
  })

  async function openPersonalDetailsPage(evaka: Page, citizen: DevPerson) {
    page = evaka
    await enduserLogin(page, citizen)
    const header = new CitizenHeader(page)
    await header.selectTab('personal-details')
    return new CitizenPersonalDetailsPage(page)
  }

  test('works for persons who already have an unverified e-mail set', async ({
    evaka
  }) => {
    const citizen = await Fixture.person({
      email: 'test@example.com'
    }).saveAdult({
      updateMockVtjWithDependants: []
    })

    const personalDetailsPage = await openPersonalDetailsPage(evaka, citizen)
    const section = personalDetailsPage.personalDetailsSection
    await section.unverifiedEmailStatus.waitUntilVisible()

    await section.sendVerificationCode.click()
    await section.verificationCodeField.waitUntilVisible()
    await runJobs({ mockedTime })

    const verificationCode = await getVerificationCodeFromEmail()
    expect(verificationCode).toBeTruthy()
    await section.verificationCodeField.fill(verificationCode ?? '')
    await section.verifyEmail.click()

    await section.verifiedEmailStatus.waitUntilVisible()
  })

  test('if a person has a verified e-mail and they change it, it requires re-verification', async ({
    evaka
  }) => {
    const citizen = await Fixture.person({
      email: 'test@example.com',
      verifiedEmail: 'test@example.com'
    }).saveAdult({
      updateMockVtjWithDependants: []
    })

    const personalDetailsPage = await openPersonalDetailsPage(evaka, citizen)
    const section = personalDetailsPage.personalDetailsSection
    await section.verifiedEmailStatus.waitUntilVisible()

    await section.editPersonalData(
      {
        preferredName: citizen.firstName.split(' ')[1],
        email: 'unverified@example.com',
        phone: citizen.phone,
        backupPhone: citizen.backupPhone
      },
      true
    )
    await section.sendVerificationCode.waitUntilVisible()
  })
})
