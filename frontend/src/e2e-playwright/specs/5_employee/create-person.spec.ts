// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Page } from 'playwright'
import config from 'e2e-test-common/config'
import { resetDatabase } from 'e2e-test-common/dev-api'
import { newBrowserContext } from 'e2e-playwright/browser'
import { employeeLogin } from 'e2e-playwright/utils/user'
import PersonSearchPage from 'e2e-playwright/pages/employee/person-search'
import LocalDate from 'lib-common/local-date'

let page: Page
let personSearchPage: PersonSearchPage

beforeEach(async () => {
  await resetDatabase()

  page = await (await newBrowserContext()).newPage()
  await employeeLogin(page, 'ADMIN')
  await page.goto(`${config.employeeUrl}/search`)
  personSearchPage = new PersonSearchPage(page)
})

describe('Create person', () => {
  test('Create person without a SSN', async () => {
    const person = {
      firstName: 'Etunimi',
      lastName: 'Sukunimi',
      dateOfBirth: LocalDate.today().subDays(30),
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
      dateOfBirth: LocalDate.today().subDays(30),
      streetAddress: 'Osoite 1',
      postalCode: '02100',
      postOffice: 'Espoo'
    }
    await personSearchPage.createPerson(person)
    await personSearchPage.findPerson(person.firstName)
    await personSearchPage.assertPersonData(person)

    // data from mock-vtj-data.json
    const personWithSsn = {
      firstName: 'Ville',
      lastName: 'Vilkas',
      dateOfBirth: LocalDate.parseIso('1999-12-31'),
      streetAddress: 'Toistie 33',
      postalCode: '02230',
      postOffice: 'Espoo',
      ssn: '311299-999E'
    }
    await personSearchPage.addSsn(personWithSsn.ssn)
    await personSearchPage.assertPersonData(personWithSsn)
  })
})
