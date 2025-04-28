// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import HelsinkiDateTime from 'lib-common/helsinki-date-time'

import { testAdult } from '../../dev-api/fixtures'
import { resetServiceState } from '../../generated/api-clients'
import CitizenHeader from '../../pages/citizen/citizen-header'
import IncomeStatementsPage from '../../pages/citizen/citizen-income'
import { waitUntilEqual } from '../../utils'
import { envs, EnvType, Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let header: CitizenHeader
let incomeStatementsPage: IncomeStatementsPage

const now = HelsinkiDateTime.of(2024, 11, 25, 12)

async function assertIncomeStatementCreated(
  startDate: string,
  sent: HelsinkiDateTime | null,
  env: EnvType
) {
  await waitUntilEqual(async () => await incomeStatementsPage.rows.count(), 1)
  const row = incomeStatementsPage.rows.only()
  await row.assertText((text) => text.includes(startDate))
  await row.assertText((text) =>
    text.includes(
      sent
        ? sent.toLocalDate().format()
        : env === 'desktop'
          ? 'Ei lähetetty'
          : 'Lähetetty: -'
    )
  )
}

describe.each(envs)('Income statements', (env) => {
  const startDate = '24.12.2044'
  const endDate = '24.12.2044'

  beforeEach(async () => {
    await resetServiceState()

    await testAdult.saveAdult({
      updateMockVtjWithDependants: []
    })

    const viewport =
      env === 'mobile'
        ? { width: 375, height: 812 }
        : { width: 1920, height: 1080 }

    page = await Page.open({
      viewport,
      screen: viewport,
      mockedTime: now
    })
    await enduserLogin(page, testAdult)
    header = new CitizenHeader(page, env)
    incomeStatementsPage = new IncomeStatementsPage(page, env)
  })

  describe('With the bare minimum selected', () => {
    test('Highest fee', async () => {
      await header.selectTab('income')
      await incomeStatementsPage.createNewIncomeStatement()
      await incomeStatementsPage.selectIncomeStatementType('highest-fee')
      await incomeStatementsPage.setValidFromDate(startDate)
      await incomeStatementsPage.checkAssured()
      await incomeStatementsPage.submit()

      await assertIncomeStatementCreated(startDate, now, env)
    })

    test('Gross income', async () => {
      await header.selectTab('income')

      await incomeStatementsPage.createNewIncomeStatement()

      await incomeStatementsPage.selectIncomeStatementType('gross-income')

      // Start date can be max 1y from now so an error is shown
      await incomeStatementsPage.setValidFromDate(
        now.toLocalDate().subMonths(12).subDays(1).format('d.M.yyyy')
      )
      await incomeStatementsPage.incomeStartDateInfo.waitUntilVisible()

      await incomeStatementsPage.setValidFromDate(startDate)
      await incomeStatementsPage.incomeStartDateInfo.waitUntilHidden()

      await incomeStatementsPage.incomeEndDateInfo.waitUntilVisible()

      await incomeStatementsPage.checkIncomesRegisterConsent()
      await incomeStatementsPage.checkAssured()
      await incomeStatementsPage.setGrossIncomeEstimate(1500)
      await incomeStatementsPage.setEntrepreneur(false)

      // End date can be max 1y from start date so a warning is shown
      await incomeStatementsPage.setValidToDate('25.12.2045')
      await incomeStatementsPage.incomeEndDateInfo.assertTextEquals(
        'Valitse aikaisempi päivä'
      )

      await incomeStatementsPage.setValidToDate(endDate)
      await incomeStatementsPage.incomeEndDateInfo.waitUntilHidden()
      await incomeStatementsPage.submit()
      await assertIncomeStatementCreated(startDate, now, env)
    })
  })

  describe('Entrepreneur income', () => {
    test('Limited liability company', async () => {
      await header.selectTab('income')
      await incomeStatementsPage.createNewIncomeStatement()
      await incomeStatementsPage.selectIncomeStatementType('gross-income')
      await incomeStatementsPage.setValidFromDate(startDate)
      await incomeStatementsPage.setValidToDate(endDate)
      await incomeStatementsPage.checkIncomesRegisterConsent()
      await incomeStatementsPage.setGrossIncomeEstimate(1500)
      await incomeStatementsPage.setEntrepreneur(true)

      await incomeStatementsPage.setEntrepreneurStartDate(
        now.toLocalDate().addYears(-10).format()
      )
      await incomeStatementsPage.selectEntrepreneurSpouse('no')

      await incomeStatementsPage.toggleEntrepreneurStartupGrant(false)
      await incomeStatementsPage.toggleEntrepreneurCheckupConsent(false)

      await incomeStatementsPage.toggleLimitedLiabilityCompany(true)
      await incomeStatementsPage.assertMissingAttachment(
        'ACCOUNTANT_REPORT_LLC'
      )

      await incomeStatementsPage.toggleLlcType('attachments')
      await incomeStatementsPage.assertMissingAttachment('PAYSLIP_LLC')
      await incomeStatementsPage.toggleLlcType('incomes-register')

      await incomeStatementsPage.fillAccountant()

      await incomeStatementsPage.checkAssured()

      // Try to submit without attachments
      await incomeStatementsPage.submit()
      await incomeStatementsPage.invalidForm.waitUntilVisible()

      // Add the missing attachment
      await incomeStatementsPage
        .attachmentInput('ACCOUNTANT_REPORT_LLC')
        .uploadTestFile()

      await incomeStatementsPage.submit()

      await assertIncomeStatementCreated(startDate, now, env)
    })
    test('Self employed', async () => {
      await header.selectTab('income')
      await incomeStatementsPage.createNewIncomeStatement()
      await incomeStatementsPage.selectIncomeStatementType('gross-income')
      await incomeStatementsPage.setValidFromDate(startDate)
      await incomeStatementsPage.setValidToDate(endDate)
      await incomeStatementsPage.checkIncomesRegisterConsent()
      await incomeStatementsPage.setGrossIncomeEstimate(1500)
      await incomeStatementsPage.setEntrepreneur(true)

      await incomeStatementsPage.setEntrepreneurStartDate(
        now.toLocalDate().addYears(-5).addWeeks(-7).format()
      )
      await incomeStatementsPage.selectEntrepreneurSpouse('no')

      await incomeStatementsPage.toggleEntrepreneurStartupGrant(true)
      await incomeStatementsPage.assertMissingAttachment('STARTUP_GRANT')

      await incomeStatementsPage.toggleEntrepreneurCheckupConsent(true)

      await incomeStatementsPage.toggleSelfEmployed(true)
      await incomeStatementsPage.toggleSelfEmployedAttachments(true)
      await incomeStatementsPage.assertMissingAttachment(
        'PROFIT_AND_LOSS_STATEMENT_SELF_EMPLOYED'
      )

      await incomeStatementsPage.fillAccountant()

      await incomeStatementsPage
        .attachmentInput('STARTUP_GRANT')
        .uploadTestFile()
      await incomeStatementsPage
        .attachmentInput('PROFIT_AND_LOSS_STATEMENT_SELF_EMPLOYED')
        .uploadTestFile()

      await incomeStatementsPage.checkAssured()
      await incomeStatementsPage.submit()

      await assertIncomeStatementCreated(startDate, now, env)
    })

    test('Light entrepreneur', async () => {
      await header.selectTab('income')
      await incomeStatementsPage.createNewIncomeStatement()
      await incomeStatementsPage.selectIncomeStatementType('gross-income')
      await incomeStatementsPage.setValidFromDate(startDate)
      await incomeStatementsPage.setValidToDate(endDate)
      await incomeStatementsPage.checkIncomesRegisterConsent()
      await incomeStatementsPage.setGrossIncomeEstimate(1500)
      await incomeStatementsPage.setEntrepreneur(true)

      await incomeStatementsPage.setEntrepreneurStartDate(
        now.toLocalDate().addMonths(-3).format()
      )
      await incomeStatementsPage.selectEntrepreneurSpouse('no')

      await incomeStatementsPage.toggleLightEntrepreneur(true)
      await incomeStatementsPage.assertMissingAttachment('SALARY')

      await incomeStatementsPage.toggleStudent(true)
      await incomeStatementsPage.assertMissingAttachment('PROOF_OF_STUDIES')
      await incomeStatementsPage.toggleAlimonyPayer(true)
      await incomeStatementsPage.assertMissingAttachment('ALIMONY_PAYOUT')

      await incomeStatementsPage.attachmentInput('SALARY').uploadTestFile()
      await incomeStatementsPage
        .attachmentInput('PROOF_OF_STUDIES')
        .uploadTestFile()
      await incomeStatementsPage
        .attachmentInput('ALIMONY_PAYOUT')
        .uploadTestFile()

      await incomeStatementsPage.checkAssured()
      await incomeStatementsPage.submit()

      await assertIncomeStatementCreated(startDate, now, env)
    })

    test('Partnership', async () => {
      await header.selectTab('income')
      await incomeStatementsPage.createNewIncomeStatement()
      await incomeStatementsPage.selectIncomeStatementType('gross-income')
      await incomeStatementsPage.setValidFromDate(startDate)
      await incomeStatementsPage.setValidToDate(endDate)
      await incomeStatementsPage.checkIncomesRegisterConsent()
      await incomeStatementsPage.setGrossIncomeEstimate(1500)
      await incomeStatementsPage.setEntrepreneur(true)

      await incomeStatementsPage.setEntrepreneurStartDate(
        now.toLocalDate().addMonths(-1).addDays(3).format()
      )
      await incomeStatementsPage.selectEntrepreneurSpouse('yes')

      await incomeStatementsPage.togglePartnership(true)
      await incomeStatementsPage.assertMissingAttachment(
        'PROFIT_AND_LOSS_STATEMENT_PARTNERSHIP'
      )
      await incomeStatementsPage.assertMissingAttachment(
        'ACCOUNTANT_REPORT_PARTNERSHIP'
      )
      await incomeStatementsPage.fillAccountant()

      await incomeStatementsPage
        .attachmentInput('PROFIT_AND_LOSS_STATEMENT_PARTNERSHIP')
        .uploadTestFile()
      await incomeStatementsPage
        .attachmentInput('ACCOUNTANT_REPORT_PARTNERSHIP')
        .uploadTestFile()

      await incomeStatementsPage.checkAssured()
      await incomeStatementsPage.submit()

      await assertIncomeStatementCreated(startDate, now, env)
    })
  })

  describe('Saving as draft', () => {
    test('No need to check assured', async () => {
      await header.selectTab('income')
      await incomeStatementsPage.createNewIncomeStatement()
      await incomeStatementsPage.selectIncomeStatementType('highest-fee')
      await incomeStatementsPage.setValidFromDate(startDate)
      await incomeStatementsPage.saveDraft()

      await assertIncomeStatementCreated(startDate, null, env)

      // update and send
      const startDate2 = '24.12.2044'
      await incomeStatementsPage.editIncomeStatement(0)
      await incomeStatementsPage.setValidFromDate(startDate2)
      await incomeStatementsPage.checkAssured()
      await incomeStatementsPage.submit()

      await assertIncomeStatementCreated(startDate2, now, env)
    })
  })
})
