// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { newBrowserContext } from 'e2e-playwright/browser'
import EmployeeNav from 'e2e-playwright/pages/employee/employee-nav'
import config from 'e2e-test-common/config'
import { insertEmployeeFixture, resetDatabase } from 'e2e-test-common/dev-api'
import { Page } from 'playwright'
import UnitsPage from 'e2e-playwright/pages/employee/units/units'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import { UnitPage, UnitEditor } from 'e2e-playwright/pages/employee/units/unit'

import { employeeLogin } from 'e2e-playwright/utils/user'

let page: Page
let nav: EmployeeNav
let unitPage: UnitPage
let unitEditorPage: UnitEditor

beforeEach(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  await insertEmployeeFixture({
    externalId: `espoo-ad:${config.adminExternalId}`,
    email: 'teppo.testaaja@example.com',
    firstName: 'Teppo',
    lastName: 'Testaaja',
    roles: []
  })

  page = await (await newBrowserContext()).newPage()
  await employeeLogin(page, 'ADMIN')
  await page.goto(config.employeeUrl)
  nav = new EmployeeNav(page)
  await nav.openTab('units')
  const units = new UnitsPage(page)
  await units.navigateToUnit(fixtures.daycareFixture.id)
  unitPage = new UnitPage(page)
  await unitPage.openUnitInformation()
  await unitPage.openUnitDetails()
  unitEditorPage = await unitPage.clickEditUnit()
})

describe('Employee - unit editor validations and warnings', () => {
  test('Unit closing date warning is shown when needed', async () => {
    await unitEditorPage.assertWarningIsNotVisible('closing-date-warning')
    await unitEditorPage.selectSomeClosingDate()
    await unitEditorPage.assertWarningIsVisible('closing-date-warning')
    await unitEditorPage.clearClosingDate()
    await unitEditorPage.assertWarningIsNotVisible('closing-date-warning')
  })

  test('Varda unit warning is shown for non varda units', async () => {
    await unitEditorPage.selectProviderType('MUNICIPAL')
    await unitEditorPage.assertWarningIsNotVisible('send-to-varda-warning')
    await unitEditorPage.selectProviderType('PRIVATE')
    await unitEditorPage.assertWarningIsVisible('send-to-varda-warning')
  })

  test('Municipal, service voucher and purchased units shows warning if handler address is missing', async () => {
    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'MUNICIPAL',
      '',
      true
    )
    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'MUNICIPAL',
      'An address',
      false
    )

    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'MUNICIPAL_SCHOOL',
      '',
      true
    )
    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'MUNICIPAL_SCHOOL',
      'An address',
      false
    )

    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'PURCHASED',
      '',
      true
    )
    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'PURCHASED',
      'An address',
      false
    )

    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'PRIVATE_SERVICE_VOUCHER',
      '',
      true
    )
    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'PRIVATE_SERVICE_VOUCHER',
      'An address',
      false
    )

    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'PRIVATE',
      '',
      false
    )
    await unitEditorPage.assertUnitHandlerAddressVisibility(
      'EXTERNAL_PURCHASED',
      '',
      false
    )
  })

  test('Invoicing related fields are only shown if unit is invoiced by municipality', async () => {
    await unitEditorPage.assertInvoicingFieldsVisibility(true)
    await unitEditorPage.clickInvoicedByMunicipality()
    await unitEditorPage.assertInvoicingFieldsVisibility(false)
  })
})
