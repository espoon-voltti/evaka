// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { PartnershipId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'

import { Fixture } from '../../dev-api/fixtures'
import {
  createFridgePartner,
  resetServiceState
} from '../../generated/api-clients'
import CitizenHeader from '../../pages/citizen/citizen-header'
import type {
  CitizenNotificationSettingsSection,
  ContactDetailsSection,
  FamilySizeSection,
  PersonDetailsSection
} from '../../pages/citizen/citizen-personal-details'
import CitizenPersonalDetailsPage from '../../pages/citizen/citizen-personal-details'
import { expect, test } from '../../playwright'
import type { Page } from '../../utils/page'
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

test.describe('Citizen personal details', () => {
  let personSection: PersonDetailsSection
  let contactSection: ContactDetailsSection

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    await citizenFixture.saveAdult({
      updateMockVtjWithDependants: []
    })
    page = evaka

    await enduserLogin(page, citizenFixture, '/personal-details')
    header = new CitizenHeader(page)

    personalDetailsPage = new CitizenPersonalDetailsPage(page)
    personSection = personalDetailsPage.personDetailsSection
    contactSection = personalDetailsPage.contactDetailsSection
  })

  test('Citizen sees indications of missing email and phone', async () => {
    await header.checkPersonalDetailsAttentionIndicatorsAreShown()
    await expect(personalDetailsPage.addEmailTask).toBeVisible()
    await expect(personalDetailsPage.verifyEmailTask).toBeHidden()
    await expect(personalDetailsPage.addPhoneTask).toBeVisible()
  })

  test('Citizen fills successfully personal data without email', async () => {
    const preferredName = citizenFixture.firstName.split(' ')[1]
    const contactData = {
      phone: '123123',
      backupPhone: '456456',
      email: null
    }

    await personSection.editPreferredName(preferredName)
    await contactSection.editContactDetails(contactData, true)

    await personSection.assertPreferredName(preferredName)
    await contactSection.checkContactDetails(contactData)
    await expect(personalDetailsPage.addPhoneTask).toBeHidden()
  })

  test('Citizen fills in contact details but cannot save without phone', async () => {
    await contactSection.editContactDetails(
      {
        phone: null,
        backupPhone: '456456',
        email: 'a@b.com'
      },
      false
    )
    await contactSection.assertSaveIsDisabled()
  })

  test('Citizen fills in personal data correctly and saves', async () => {
    const preferredName = citizenFixture.firstName.split(' ')[1]
    const contactData = {
      phone: '123456789',
      backupPhone: '456456',
      email: 'a@b.com'
    }

    await personSection.editPreferredName(preferredName)
    await contactSection.editContactDetails(contactData, true)

    await personSection.assertPreferredName(preferredName)
    await contactSection.checkContactDetails(contactData)
    await expect(personalDetailsPage.addEmailTask).toBeHidden()
    await expect(personalDetailsPage.verifyEmailTask).toBeVisible()
    await expect(personalDetailsPage.addPhoneTask).toBeHidden()
    await header.checkPersonalDetailsAttentionIndicatorsAreShown()
  })
})

test.describe('Citizen personal details tasks', () => {
  const email = 'test@example.com'
  const verifiedCitizen = Fixture.person({
    email,
    verifiedEmail: email,
    phone: '123456789'
  })

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    page = evaka
  })

  test('Citizen with verified email but no weak login sees only the weak login task', async () => {
    const citizen = await verifiedCitizen.saveAdult({
      updateMockVtjWithDependants: []
    })
    await enduserLogin(page, citizen, '/personal-details')
    header = new CitizenHeader(page)
    personalDetailsPage = new CitizenPersonalDetailsPage(page)

    await expect(personalDetailsPage.addWeakLoginTask).toBeVisible()
    await expect(personalDetailsPage.addEmailTask).toBeHidden()
    await expect(personalDetailsPage.verifyEmailTask).toBeHidden()
    await expect(personalDetailsPage.addPhoneTask).toBeHidden()
    await header.checkPersonalDetailsAttentionIndicatorsAreShown()
  })

  test('Citizen with all tasks done sees no tasks or attention indicators', async () => {
    const citizen = await verifiedCitizen.saveAdult({
      updateMockVtjWithDependants: [],
      updateWeakCredentials: { username: email, password: 'aifiefaeC3io?dee' }
    })
    await enduserLogin(page, citizen, '/personal-details')
    header = new CitizenHeader(page)
    personalDetailsPage = new CitizenPersonalDetailsPage(page)

    await expect(
      personalDetailsPage.contactDetailsSection.verifiedEmailStatus
    ).toBeVisible()
    await expect(personalDetailsPage.addEmailTask).toBeHidden()
    await expect(personalDetailsPage.verifyEmailTask).toBeHidden()
    await expect(personalDetailsPage.addPhoneTask).toBeHidden()
    await expect(personalDetailsPage.addWeakLoginTask).toBeHidden()
    await header.checkPersonalDetailsAttentionIndicatorsAreHidden()
  })
})

test.describe('Citizen notification settings', () => {
  let section: CitizenNotificationSettingsSection

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    await citizenFixture.saveAdult({
      updateMockVtjWithDependants: []
    })
    page = evaka

    await enduserLogin(page, citizenFixture, '/personal-details')
    header = new CitizenHeader(page)

    personalDetailsPage = new CitizenPersonalDetailsPage(page)
    section = personalDetailsPage.notificationSettingsSection
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

test.describe('Citizen family size', () => {
  let familySection: FamilySizeSection

  const head = Fixture.person({
    firstName: 'Elina Maria Leena',
    lastName: 'Mäkinen',
    ssn: '150385-9987'
  })
  const partner = Fixture.person({
    firstName: 'Jukka Tapio',
    lastName: 'Mäkinen',
    ssn: '010280-9994'
  })
  const child1 = Fixture.person({
    firstName: 'Linnea Aino Ursula',
    lastName: 'Mäkinen',
    ssn: '120618A999E',
    dateOfBirth: LocalDate.of(2018, 6, 12)
  })
  const child2 = Fixture.person({
    firstName: 'Robin Tenho Kalevi',
    lastName: 'Mäkinen',
    ssn: '050721A999H',
    dateOfBirth: LocalDate.of(2021, 7, 5)
  })

  test.beforeEach(async ({ evaka }) => {
    await resetServiceState()
    await child1.saveChild({ updateMockVtj: true })
    await child2.saveChild({ updateMockVtj: true })
    await head.saveAdult({ updateMockVtjWithDependants: [child1, child2] })
    await partner.saveAdult({ updateMockVtjWithDependants: [] })

    const partnershipId = randomId<PartnershipId>()
    const start = LocalDate.of(2020, 1, 1)
    const end = LocalDate.of(2030, 1, 1)
    await createFridgePartner({
      body: [
        {
          partnershipId,
          indx: 1,
          otherIndx: 2,
          personId: head.id,
          startDate: start,
          endDate: end,
          conflict: false,
          createdAt: HelsinkiDateTime.now()
        },
        {
          partnershipId,
          indx: 2,
          otherIndx: 1,
          personId: partner.id,
          startDate: start,
          endDate: end,
          conflict: false,
          createdAt: HelsinkiDateTime.now()
        }
      ]
    })
    for (const child of [child1, child2]) {
      await Fixture.fridgeChild({
        headOfChild: head.id,
        childId: child.id,
        startDate: start,
        endDate: end
      }).save()
    }

    page = evaka
    await enduserLogin(page, head, '/personal-details')
    personalDetailsPage = new CitizenPersonalDetailsPage(page)
    familySection = personalDetailsPage.familySizeSection
  })

  test('Citizen sees adults and children with the self marker', async () => {
    await familySection.assertAdultCount(2)
    await familySection.assertChildCount(2)
    await familySection.assertMember(head.id, 'Elina Maria Leena Mäkinen', true)
    await familySection.assertMember(partner.id, 'Jukka Tapio Mäkinen')
    await familySection.assertMember(child1.id, 'Linnea Aino Ursula Mäkinen')
    await familySection.assertMember(child2.id, 'Robin Tenho Kalevi Mäkinen')
  })
})
