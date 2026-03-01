// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import config from '../../config'
import { Fixture } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import PersonSearchPage from '../../pages/employee/person-search'
import { test } from '../../playwright'
import type { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

test.describe('SSN disabling', () => {
  let adminPage: Page
  let adminPersonSearchPage: PersonSearchPage
  let serviceWorkerPage: Page
  let serviceWorkerPersonSearchPage: PersonSearchPage

  test.beforeEach(async ({ newEvakaPage }) => {
    await resetServiceState()

    const admin = await Fixture.employee().admin().save()
    const serviceWorker = await Fixture.employee().serviceWorker().save()

    adminPage = await newEvakaPage()
    await employeeLogin(adminPage, admin)
    await adminPage.goto(`${config.employeeUrl}/search`)
    adminPersonSearchPage = new PersonSearchPage(adminPage)

    serviceWorkerPage = await newEvakaPage()
    await employeeLogin(serviceWorkerPage, serviceWorker)
    await serviceWorkerPage.goto(`${config.employeeUrl}/search`)
    serviceWorkerPersonSearchPage = new PersonSearchPage(serviceWorkerPage)
  })

  test('SSN adding can be disabled for a newly created person', async () => {
    const person = {
      firstName: 'Etunimi',
      lastName: 'Sukunimi',
      dateOfBirth: LocalDate.todayInSystemTz().subDays(30),
      streetAddress: 'Osoite 1',
      postalCode: '02100',
      postOffice: 'Espoo'
    }
    await adminPersonSearchPage.createPerson(person)
    await adminPersonSearchPage.findPerson(person.firstName)
    await adminPersonSearchPage.disableSsnAdding(true)

    await serviceWorkerPersonSearchPage.findPerson(person.firstName)
    await serviceWorkerPersonSearchPage.checkAddSsnButtonVisibility(false)

    await adminPage.reload()
    await adminPersonSearchPage.checkAddSsnButtonVisibility(true)
    await adminPersonSearchPage.disableSsnAdding(false)

    await serviceWorkerPage.reload()
    await serviceWorkerPersonSearchPage.checkAddSsnButtonVisibility(true)
  })
})
