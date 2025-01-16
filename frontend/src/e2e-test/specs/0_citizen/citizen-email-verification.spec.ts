// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState, runJobs } from '../../generated/api-clients'
import { DevPerson } from '../../generated/api-types'
import CitizenHeader from '../../pages/citizen/citizen-header'
import CitizenPersonalDetailsPage from '../../pages/citizen/citizen-personal-details'
import { getVerificationCodeFromEmail } from '../../utils/email'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

const mockedTime = HelsinkiDateTime.of(2024, 1, 1, 12, 0)
let page: Page

beforeEach(async () => {
  await resetServiceState()
})

describe('Citizen e-mail verification', () => {
  it('works for persons who already have an unverified e-mail set', async () => {
    const citizen = await Fixture.person({
      email: 'test@example.com'
    }).saveAdult({
      updateMockVtjWithDependants: []
    })
    page = await Page.open({ mockedTime })

    const personalDetailsPage = await openPersonalDetailsPage(citizen)
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
  test('if a person has a verified e-mail and they change it, it requires re-verification', async () => {
    const citizen = await Fixture.person({
      email: 'test@example.com',
      verifiedEmail: 'test@example.com'
    }).saveAdult({
      updateMockVtjWithDependants: []
    })
    page = await Page.open({ mockedTime })

    const personalDetailsPage = await openPersonalDetailsPage(citizen)
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
    await section.unverifiedEmailStatus.waitUntilVisible()
  })
})

async function openPersonalDetailsPage(citizen: DevPerson) {
  await enduserLogin(page, citizen)
  const header = new CitizenHeader(page)
  await header.selectTab('personal-details')
  return new CitizenPersonalDetailsPage(page)
}
