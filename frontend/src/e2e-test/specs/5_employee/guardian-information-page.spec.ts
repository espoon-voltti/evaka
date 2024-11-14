// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'

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
  uuidv4,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createApplications,
  createDaycareGroups,
  createDaycarePlacements,
  createDecisions,
  deleteDaycareCostCenter,
  resetServiceState
} from '../../generated/api-clients'
import GuardianInformationPage from '../../pages/employee/guardian-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page

beforeEach(async () => {
  await resetServiceState()
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
    await Fixture.invoice({
      headOfFamilyId: testAdult.id,
      areaId: testCareArea.id,
      periodStart: LocalDate.of(2020, 1, 1),
      periodEnd: LocalDate.of(2020, 1, 31)
    })
      .addRow({
        childId: testChild.id,
        unitId: testDaycare.id
      })
      .save()

    const guardianPage = new GuardianInformationPage(page)
    await guardianPage.navigateToGuardian(testAdult.id)

    const invoiceSection = await guardianPage.openCollapsible('invoices')
    await invoiceSection.assertInvoiceCount(1)
    await invoiceSection.assertInvoice(0, '01/2020', 'Luonnos')
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
    const createModal =
      await invoiceCorrectionsSection.addNewInvoiceCorrection()
    await createModal.productSelect.selectOption('DAYCARE_DISCOUNT')
    await createModal.description.fill('Virheen korjaus')
    await createModal.unitSelect.selectOption(testDaycare.name)
    await createModal.startDate.fill('01.01.2020')
    await createModal.endDate.fill('05.01.2020')
    await createModal.amount.fill('5')
    await createModal.price.fill('12')
    await createModal.totalPrice.assertTextEquals('60 €')
    await createModal.note.fill('Testimuistiinpano')
    await createModal.submit()

    await invoiceCorrectionsSection.invoiceCorrectionRows.assertCount(1)
    const row = invoiceCorrectionsSection.lastRow()
    await row.productSelect.assertTextEquals('Alennus (maksup.)')
    await row.description.assertTextEquals('Virheen korjaus')
    await row.unitSelect.assertTextEquals(testDaycare.name)
    await row.period.assertTextEquals('01.01.2020 - 05.01.2020')
    await row.amount.assertTextEquals('5')
    await row.unitPrice.assertTextEquals('12,00 €')
    await row.totalPrice.assertTextEquals('60,00 €')
    await row.status.assertTextEquals('Ei laskulla')
    await row.noteIcon.hover()
    await row.noteTooltip.assertTextEquals('Testimuistiinpano')

    const noteModal = await row.editNote()
    await noteModal.note.fill('Muokattu muistiinpano')
    await noteModal.submit()

    await row.noteIcon.hover()
    await row.noteTooltip.assertTextEquals('Muokattu muistiinpano')

    await row.deleteRow()
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
    let modal = await invoiceCorrectionsSection.addNewInvoiceCorrection()
    await modal.clickAndAssertUnitVisibility(testDaycare.name, true)

    await deleteDaycareCostCenter({ daycareId: testDaycare.id })
    await guardianPage.navigateToGuardian(testAdult.id)
    await guardianPage.openCollapsible('invoiceCorrections')
    await invoiceCorrectionsSection.invoiceCorrectionRows.assertCount(0)
    modal = await invoiceCorrectionsSection.addNewInvoiceCorrection()
    await modal.clickAndAssertUnitVisibility(testDaycare.name, false)
  })
})
