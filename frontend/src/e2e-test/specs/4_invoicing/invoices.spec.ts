// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import 'testcafe'
import InvoicingPage from '../../pages/invoicing'
import {
  initializeAreaAndPersonData,
  AreaAndPersonFixtures
} from 'e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  feeDecisionsFixture,
  invoiceFixture,
  adultFixtureWihtoutSSN,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import { logConsoleMessages } from '../../utils/fixture'
import EmployeeHome from '../../pages/employee/home'
import GuardianPage from '../../pages/employee/guardian-page'
import {
  cleanUpInvoicingDatabase,
  deletePersonFixture,
  insertDaycarePlacementFixtures,
  insertFeeDecisionFixtures,
  insertInvoiceFixtures,
  insertParentshipFixtures,
  insertPersonFixture
} from 'e2e-test-common/dev-api'
import { seppoAdminRole } from '../../config/users'
import LocalDate from 'lib-common/local-date'

const page = new InvoicingPage()

let fixtures: AreaAndPersonFixtures
let cleanUp: () => Promise<void>
let adultWithoutSSN: string

fixture('Invoicing - invoices')
  .meta({ type: 'regression', subType: 'invoices' })
  .page(page.url)
  .before(async () => {
    ;[fixtures, cleanUp] = await initializeAreaAndPersonData()
    adultWithoutSSN = await insertPersonFixture(adultFixtureWihtoutSSN)
    await insertParentshipFixtures([
      {
        childId: fixtures.enduserChildFixtureKaarina.id,
        headOfChildId: fixtures.enduserGuardianFixture.id,
        startDate: fixtures.enduserChildFixtureKaarina.dateOfBirth,
        endDate: '2099-01-01'
      }
    ])
  })
  .beforeEach(async (t) => {
    await cleanUpInvoicingDatabase()
    const feeDecision = feeDecisionsFixture(
      'SENT',
      fixtures.enduserGuardianFixture,
      fixtures.enduserChildFixtureKaarina,
      fixtures.daycareFixture.id
    )
    await insertFeeDecisionFixtures([feeDecision])
    await insertDaycarePlacementFixtures([
      createDaycarePlacementFixture(
        uuidv4(),
        fixtures.enduserChildFixtureKaarina.id,
        fixtures.daycareFixture.id,
        feeDecision.validDuring.start.formatIso(),
        feeDecision.validDuring.end.formatIso()
      )
    ])
    await t.useRole(seppoAdminRole)
    await page.navigateToInvoices(t)
    await t.expect(page.loaderSpinner.exists).notOk()
  })
  .afterEach(logConsoleMessages)
  .after(async () => {
    await cleanUpInvoicingDatabase()
    await deletePersonFixture(adultWithoutSSN)
    await cleanUp()
  })

test('List of invoice drafts is empty intially and after creating new drafts the list has one invoice', async (t) => {
  await t.expect(page.invoiceTable.visible).ok()
  await t.expect(page.loaderSpinner.exists).notOk()
  await t.expect(page.invoiceRows.count).eql(0)
  await t.click(page.createInvoices)
  await t.expect(page.invoiceRows.count).eql(1)
})

test('Navigate to and from invoice page', async (t) => {
  await t.expect(page.loaderSpinner.exists).notOk()
  await t.expect(page.createInvoices.hasAttribute('disabled')).notOk()
  await t.click(page.createInvoices)

  await page.openFirstInvoice(t)
  await t.expect(page.invoiceDetailsPage.exists).ok()

  await t.expect(page.navigateBack.exists).ok()
  await t.click(page.navigateBack)
  await t.expect(page.invoicesPage.exists).ok()
})

test('Add a new invoice row, modify its amount and price and persist the changes', async (t) => {
  await t.expect(page.loaderSpinner.exists).notOk()
  await t.expect(page.createInvoices.hasAttribute('disabled')).notOk()
  await t.click(page.createInvoices)
  await page.openFirstInvoice(t)

  await t.expect(page.invoiceRow.count).eql(1)
  await t.click(page.addRowButton)
  await t.expect(page.invoiceRow.count).eql(2)

  await t.typeText(page.costCenterInput.nth(1), '12345')
  await t
    .selectText(page.amountInput.nth(1))
    .typeText(page.amountInput.nth(1), '10')
  await t
    .selectText(page.priceInput.nth(1))
    .typeText(page.priceInput.nth(1), '100')
  await t.click(page.saveInvoiceChangesButton)

  await t.click(page.navigateBack)
  await t.expect(page.invoicesPage.exists).ok()
  await page.openFirstInvoice(t)
  await t.expect(page.invoiceRow.count).eql(2)

  const rowNum: number =
    (await page.costCenterInput.nth(1).value) === '12345' ? 1 : 0
  await t.expect(page.costCenterInput.nth(rowNum).value).eql('12345')
  await t.expect(page.amountInput.nth(rowNum).value).eql('10')
  await t.expect(page.priceInput.nth(rowNum).value).eql('100')
})

test('Add a new invoice row, delete it, and delete the parent row', async (t) => {
  await t.expect(page.loaderSpinner.exists).notOk()
  await t.expect(page.createInvoices.hasAttribute('disabled')).notOk()
  await t.click(page.createInvoices)
  await page.openFirstInvoice(t)

  await t.expect(page.invoiceRow.count).eql(1)
  await t.click(page.addRowButton)
  await t.expect(page.invoiceRow.count).eql(2)

  await t.click(page.deleteRowButton.nth(1))
  await t.expect(page.invoiceRow.count).eql(1)
  await t.click(page.deleteRowButton)
  await t.expect(page.invoiceRow.count).eql(0)
  await t.click(page.saveInvoiceChangesButton)

  await t.click(page.navigateBack)
  await t.expect(page.invoicesPage.exists).ok()
  await page.openFirstInvoice(t)
  await t.expect(page.invoiceRow.count).eql(0)
})

test('Invoices are toggled and sent', async (t) => {
  await t.expect(page.loaderSpinner.exists).notOk()
  await t.expect(page.createInvoices.hasAttribute('disabled')).notOk()
  await t.click(page.createInvoices)

  await t.expect(page.toggleAllInvoices.exists).ok()
  await t.expect(page.toggleFirstInvoice.exists).ok()
  await t.expect(page.toggleAllInvoices.checked).eql(false)
  await t.expect(page.toggleFirstInvoice.checked).eql(false)

  await page.toggleAllInvoices.click()
  await t.expect(page.toggleAllInvoices.checked).eql(true)
  await t.expect(page.toggleFirstInvoice.checked).eql(true)

  await page.toggleAllInvoices.click()
  await t.expect(page.toggleAllInvoices.checked).eql(false)
  await t.expect(page.toggleFirstInvoice.checked).eql(false)

  await page.toggleFirstInvoice.click()
  await t.expect(page.toggleFirstInvoice.checked).eql(true)

  await page.toggleFirstInvoice.click()
  await t.expect(page.toggleFirstInvoice.checked).eql(false)

  await page.toggleFirstInvoice.click()
  await t.click(page.openInvoiceModal)
  await t.click(page.sendInvoices)
  await t.expect(page.invoiceRows.count).eql(0)

  await page.invoicesStatusFilterSent.click()
  await t.expect(page.invoiceRows.count).eql(1)
})

test('Sending an invoice with a recipient without a SSN', async (t) => {
  await insertInvoiceFixtures([
    invoiceFixture(
      adultWithoutSSN,
      fixtures.enduserChildFixtureJari.id,
      fixtures.daycareFixture.id,
      'DRAFT'
    )
  ])

  await t.typeText(page.freeTextSearchInput, adultFixtureWihtoutSSN.firstName)

  await page.toggleFirstInvoice.click()
  await t.click(page.openInvoiceModal)
  await t.click(page.sendInvoices)
  await t.expect(page.invoiceRows.count).eql(0)

  await page.invoicesStatusFilterWaitingForSending.click()
  await t.expect(page.invoiceRows.count).eql(1)

  await page.openFirstInvoice(t)
  await t.click(page.markInvoiceSentButton)

  await t.click(page.navigateBack)
  await t.expect(page.invoiceRows.count).eql(0)

  await page.invoicesStatusFilterSent.click()
  await t.expect(page.invoiceRows.count).eql(1)
})

test('Invoices are listed on the admin UI guardian page', async (t) => {
  await insertInvoiceFixtures([
    invoiceFixture(
      fixtures.enduserGuardianFixture.id,
      fixtures.enduserChildFixtureJari.id,
      fixtures.daycareFixture.id,
      'DRAFT',
      '2020-01-01',
      '2020-02-01'
    )
  ])

  const employeeHome = new EmployeeHome()

  await employeeHome.navigateToPersonSearch()
  await employeeHome.personSearch.filterByName(
    fixtures.enduserGuardianFixture.firstName
  )
  await t.expect(employeeHome.personSearch.searchResults.count).gte(1)
  await employeeHome.personSearch.navigateToNthPerson(0)

  const guardianPage = new GuardianPage()
  await guardianPage.containsInvoice('01.01.2020', '01.02.2020', 'Luonnos')
})

test('Invoices are listed on the admin UI guardian page', async (t) => {
  await t.click(page.createInvoices)

  const today = LocalDate.today().format()

  await t.expect(page.invoiceCreatedAt.innerText).eql(today)
})
