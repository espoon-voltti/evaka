// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import {
  resetServiceState,
  upsertVtjDataset
} from '../../generated/api-clients'
import PersonSearchPage from '../../pages/employee/person-search'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let personSearchPage: PersonSearchPage

beforeEach(async () => {
  await resetServiceState()
  const admin = await Fixture.employeeAdmin().save()

  page = await Page.open()
  await employeeLogin(page, admin)
  await page.goto(`${config.employeeUrl}/search`)
  personSearchPage = new PersonSearchPage(page)
})

describe('Create person', () => {
  test('Create person without a SSN', async () => {
    const person = {
      firstName: 'Etunimi',
      lastName: 'Sukunimi',
      dateOfBirth: LocalDate.todayInSystemTz().subDays(30),
      streetAddress: 'Osoite 1',
      postalCode: '02100',
      postOffice: 'Espoo'
    }
    await personSearchPage.createPerson(person)
    await personSearchPage.findPerson(person.firstName)
    await personSearchPage.assertPersonData(person)
  })

  test('Create person without a SSN and then add a SSN', async () => {
    const person = {
      firstName: 'Etunimi',
      lastName: 'Sukunimi',
      dateOfBirth: LocalDate.todayInSystemTz().subDays(30),
      streetAddress: 'Osoite 1',
      postalCode: '02100',
      postOffice: 'Espoo'
    }
    await personSearchPage.createPerson(person)
    await personSearchPage.findPerson(person.firstName)
    await personSearchPage.assertPersonData(person)

    // data from addLegacyVtjMocks()
    const personWithSsn = {
      firstName: 'Ville',
      lastName: 'Vilkas',
      dateOfBirth: LocalDate.parseIso('1999-12-31'),
      streetAddress: 'Toistie 33',
      postalCode: '02230',
      postOffice: 'Espoo',
      ssn: '311299-999E'
    }
    await upsertVtjDataset({
      body: {
        persons: [
          {
            socialSecurityNumber: personWithSsn.ssn,
            lastName: personWithSsn.lastName,
            firstNames: personWithSsn.firstName,
            address: {
              streetAddress: personWithSsn.streetAddress,
              postalCode: personWithSsn.postalCode,
              postOffice: personWithSsn.postOffice,
              streetAddressSe: null,
              postOfficeSe: null
            },
            dateOfDeath: null,
            nationalities: [],
            nativeLanguage: null,
            residenceCode: null,
            restrictedDetails: null
          }
        ],
        guardianDependants: {}
      }
    })
    await personSearchPage.addSsn(personWithSsn.ssn)
    await personSearchPage.assertPersonData(personWithSsn)
  })
})
