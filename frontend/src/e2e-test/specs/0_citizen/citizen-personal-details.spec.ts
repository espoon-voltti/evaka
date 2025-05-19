// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import CitizenHeader from '../../pages/citizen/citizen-header'
import type {
  CitizenNotificationSettingsSection,
  CitizenPersonalDetailsSection
} from '../../pages/citizen/citizen-personal-details'
import CitizenPersonalDetailsPage from '../../pages/citizen/citizen-personal-details'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let header: CitizenHeader
let personalDetailsPage: CitizenPersonalDetailsPage
let page: Page

const citizenFixture = Fixture.person({
  firstName: 'Johannes Olavi Antero Tapio',
  lastName: 'Karhula',
  preferredName: '',
  phone: '',
  backupPhone: '',
  email: null
})

beforeEach(async () => {
  await resetServiceState()
  await citizenFixture.saveAdult({
    updateMockVtjWithDependants: []
  })
  page = await Page.open()
})

describe('Citizen personal details', () => {
  let section: CitizenPersonalDetailsSection

  beforeEach(async () => {
    await enduserLogin(page, citizenFixture)
    header = new CitizenHeader(page)

    personalDetailsPage = new CitizenPersonalDetailsPage(page)
    section = personalDetailsPage.personalDetailsSection
  })

  test('Citizen sees indications of missing email and phone', async () => {
    await header.checkPersonalDetailsAttentionIndicatorsAreShown()
    await header.selectTab('personal-details')
    await section.checkMissingEmailWarningIsShown()
    await section.checkMissingPhoneWarningIsShown()
  })

  test('Citizen fills successfully personal data without email by selecting I have no email -option', async () => {
    const data = {
      preferredName: citizenFixture.firstName.split(' ')[1],
      phone: '123123',
      backupPhone: '456456',
      email: null
    }

    await header.selectTab('personal-details')
    await section.editPersonalData(data, true)
    await section.checkPersonalData(data)
    await section.assertAlertIsNotShown()
  })

  test('Citizen fills in personal data but cannot save without phone', async () => {
    const data = {
      preferredName: citizenFixture.firstName.split(' ')[1],
      phone: null,
      backupPhone: '456456',
      email: 'a@b.com'
    }

    await header.selectTab('personal-details')
    await section.editPersonalData(data, false)
    await section.assertSaveIsDisabled()
  })

  test('Citizen fills in personal data correctly and saves', async () => {
    const data = {
      preferredName: citizenFixture.firstName.split(' ')[1],
      phone: '123456789',
      backupPhone: '456456',
      email: 'a@b.com'
    }

    await header.selectTab('personal-details')
    await section.editPersonalData(data, true)
    await section.checkPersonalData(data)
    await section.assertAlertIsNotShown()
  })
})

describe('Citizen notification settings', () => {
  let section: CitizenNotificationSettingsSection

  beforeEach(async () => {
    await enduserLogin(page, citizenFixture)
    header = new CitizenHeader(page)

    await header.selectTab('personal-details')
    personalDetailsPage = new CitizenPersonalDetailsPage(page)
    section = personalDetailsPage.notificationSettingsSectiong
  })

  test('Edit and cancel work', async () => {
    await section.assertEditable(false)
    await section.startEditing.click()
    await section.assertEditable(true)
    await section.cancel.click()
    await section.assertEditable(false)
  })

  test('Settings can be changed', async () => {
    await section.assertAllChecked(true)
    await section.startEditing.click()
    await section.checkboxes.message.uncheck()
    await section.checkboxes.income.uncheck()
    await section.checkboxes.decision.uncheck()
    await section.checkboxes.informalDocument.uncheck()
    await section.save.click()
    await section.assertEditable(false)

    await section.checkboxes.message.waitUntilChecked(false)
    await section.checkboxes.bulletin.waitUntilChecked(true)
    await section.checkboxes.income.waitUntilChecked(false)
    await section.checkboxes.calendarEvent.waitUntilChecked(true)
    await section.checkboxes.decision.waitUntilChecked(false)
    await section.checkboxes.document.waitUntilChecked(true)
    await section.checkboxes.informalDocument.waitUntilChecked(false)
    await section.checkboxes.attendanceReservation.waitUntilChecked(true)
    await section.checkboxes.discussionTime.waitUntilChecked(true)
  })
})
