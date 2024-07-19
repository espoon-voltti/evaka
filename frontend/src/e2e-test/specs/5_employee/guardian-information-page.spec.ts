// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

import { startTest } from '../../browser'
import {
  applicationFixture,
  createDaycarePlacementFixture,
  testDaycare,
  testDaycareGroup,
  decisionFixture,
  testChild,
  testChild2,
  testAdult,
  familyWithTwoGuardians,
  Fixture,
  invoiceFixture,
  uuidv4,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createApplications,
  createDaycareGroups,
  createDaycarePlacements,
  createDecisions,
  createInvoices,
  deleteDaycareCostCenter
} from '../../generated/api-clients'
import GuardianInformationPage from '../../pages/employee/guardian-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page

beforeEach(async () => {
  await startTest()
  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2]
  }).save()
  await Fixture.family(familyWithTwoGuardians).save()
  await createDaycareGroups({ body: [testDaycareGroup] })

  const admin = await Fixture.employee().admin().save()

  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    testChild.id,
    testDaycare.id
  )
  const application = applicationFixture(testChild, testAdult)

  const application2 = {
    ...applicationFixture(
      testChild2,
      familyWithTwoGuardians.guardian,
      testAdult
    ),
    id: uuidv4()
  }

  const startDate = LocalDate.of(2021, 8, 16)
  await createDaycarePlacements({ body: [daycarePlacementFixture] })
  await createApplications({ body: [application, application2] })
  await createDecisions({
    body: [
      {
        ...decisionFixture(application.id, startDate, startDate),
        employeeId: admin.id
      },
      {
        ...decisionFixture(application2.id, startDate, startDate),
        employeeId: admin.id,
        id: uuidv4()
      }
    ]
  })

  page = await Page.open()
  await employeeLogin(page, admin)
})

describe('Employee - Guardian Information', () => {
  test('guardian information is shown', async () => {
    const guardianPage = new GuardianInformationPage(page)
    await guardianPage.navigateToGuardian(testAdult.id)

    const personInfoSection = guardianPage.getCollapsible('personInfo')
    await personInfoSection.assertPersonInfo(
      testAdult.lastName,
      testAdult.firstName,
      testAdult.ssn ?? ''
    )

    const expectedChildName = `${testChild.lastName} ${testChild.firstName}`
    const dependantsSection = await guardianPage.openCollapsible('dependants')
    await dependantsSection.assertContainsDependantChild(testChild.id)

    const applicationsSection =
      await guardianPage.openCollapsible('applications')
    await applicationsSection.assertApplicationCount(1)
    await applicationsSection.assertApplicationSummary(
      0,
      expectedChildName,
      testDaycare.name
    )

    const decisionsSection = await guardianPage.openCollapsible('decisions')
    await decisionsSection.assertDecisionCount(2)
    await decisionsSection.assertDecision(
      0,
      expectedChildName,
      testDaycare.name,
      'Odottaa vastausta'
    )

    await decisionsSection.assertDecision(
      1,
      `${testChild2.lastName} ${testChild2.firstName}`,
      testDaycare.name,
      'Odottaa vastausta'
    )
  })

  test('Invoices are listed on the admin UI guardian page', async () => {
    await createInvoices({
      body: [
        invoiceFixture(
          testAdult.id,
          testChild.id,
          testCareArea.id,
          testDaycare.id,
          'DRAFT',
          LocalDate.of(2020, 1, 1),
          LocalDate.of(2020, 1, 31)
        )
      ]
    })

    const guardianPage = new GuardianInformationPage(page)
    await guardianPage.navigateToGuardian(testAdult.id)

    const invoiceSection = await guardianPage.openCollapsible('invoices')
    await invoiceSection.assertInvoiceCount(1)
    await invoiceSection.assertInvoice(0, '01.01.2020', '31.01.2020', 'Luonnos')
  })

  test('Invoice correction can be created and deleted', async () => {
    await Fixture.fridgeChild({
      headOfChild: testAdult.id,
      childId: testChild.id,
      startDate: LocalDate.of(2020, 1, 1),
      endDate: LocalDate.of(2020, 12, 31)
    }).save()
    const guardianPage = new GuardianInformationPage(page)
    await guardianPage.navigateToGuardian(testAdult.id)

    const invoiceCorrectionsSection =
      await guardianPage.openCollapsible('invoiceCorrections')
    const newRow = await invoiceCorrectionsSection.addNewInvoiceCorrection()
    await newRow.productSelect.selectOption('DAYCARE_DISCOUNT')
    await newRow.description.fill('Virheen korjaus')
    await newRow.unitSelect.fillAndSelectFirst(testDaycare.name)
    await newRow.startDate.fill('01.01.2020')
    await newRow.endDate.fill('05.01.2020')
    await newRow.amount.fill('5')
    await newRow.price.fill('12')
    await newRow.totalPrice.assertTextEquals('60 €')
    const noteModal = await newRow.addNote()
    await noteModal.note.fill('Testimuistiinpano')
    await noteModal.submit()
    await invoiceCorrectionsSection.saveButton.click()

    await invoiceCorrectionsSection.invoiceCorrectionRows.assertCount(1)
    const row = invoiceCorrectionsSection.lastRow()
    await row.productSelect.assertTextEquals('Alennus (maksup.)')
    await row.description.assertTextEquals('Virheen korjaus')
    await row.unitSelect.assertTextEquals(testDaycare.name)
    await row.period.assertTextEquals('01.01.2020 - 05.01.2020')
    await row.amount.assertTextEquals('5')
    await row.unitPrice.assertTextEquals('12 €')
    await row.totalPrice.assertTextEquals('60 €')
    await row.status.assertTextEquals('Ei vielä laskulla')
    await row.noteIcon.hover()
    await row.noteTooltip.assertTextEquals('Testimuistiinpano')

    await row.deleteButton.click()
    await invoiceCorrectionsSection.invoiceCorrectionRows.assertCount(0)
  })

  test('Invoice corrections show only units with cost center', async () => {
    await Fixture.fridgeChild({
      headOfChild: testAdult.id,
      childId: testChild.id,
      startDate: LocalDate.of(2020, 1, 1),
      endDate: LocalDate.of(2020, 12, 31)
    }).save()

    const guardianPage = new GuardianInformationPage(page)
    await guardianPage.navigateToGuardian(testAdult.id)

    const invoiceCorrectionsSection =
      await guardianPage.openCollapsible('invoiceCorrections')
    await invoiceCorrectionsSection.invoiceCorrectionRows.assertCount(0)
    let row = await invoiceCorrectionsSection.addNewInvoiceCorrection()
    await row.clickAndAssertUnitVisibility(testDaycare.name, true)

    await deleteDaycareCostCenter({ daycareId: testDaycare.id })
    await guardianPage.navigateToGuardian(testAdult.id)
    await guardianPage.openCollapsible('invoiceCorrections')
    await invoiceCorrectionsSection.invoiceCorrectionRows.assertCount(0)
    row = await invoiceCorrectionsSection.addNewInvoiceCorrection()
    await row.clickAndAssertUnitVisibility(testDaycare.name, false)
  })
})
