// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { Page } from 'playwright'
import config from '../../../e2e-test-common/config'
import { resetDatabase } from '../../../e2e-test-common/dev-api'
import { newBrowserContext } from '../../browser'
import CitizenHeader from '../../pages/citizen/citizen-header'
import IncomeStatementsPage from '../../pages/citizen/citizen-income'
import { waitUntilEqual, waitUntilTrue } from '../../utils'
import { enduserLogin } from '../../utils/user'

let page: Page
let header: CitizenHeader
let incomeStatementsPage: IncomeStatementsPage

beforeEach(async () => {
  await resetDatabase()

  page = await (await newBrowserContext()).newPage()
  await page.goto(config.enduserUrl)
  await enduserLogin(page)
  header = new CitizenHeader(page)
  incomeStatementsPage = new IncomeStatementsPage(page)
})
afterEach(async () => {
  await page.close()
})

async function assertIncomeStatementCreated(
  startDate = LocalDate.today().format()
) {
  await waitUntilEqual(async () => await incomeStatementsPage.rows.count(), 1)
  await waitUntilTrue(async () =>
    (await incomeStatementsPage.rows.innerText()).includes(startDate)
  )
}

async function assertNeededAttachment(attachment: string, present = true) {
  await waitUntilTrue(
    async () =>
      (!present &&
        !(await incomeStatementsPage.requiredAttachments.isVisible())) ||
      (
        await incomeStatementsPage.requiredAttachments.innerText()
      ).includes(attachment) === present
  )
}

describe('Income statements', () => {
  describe('With the bare minimum selected', () => {
    test('Highest fee', async () => {
      const startDate = '24.12.2044'

      await header.selectTab('income')
      await incomeStatementsPage.createNewIncomeStatement()
      await incomeStatementsPage.setValidFromDate(startDate)
      await incomeStatementsPage.selectIncomeStatementType('highest-fee')
      await incomeStatementsPage.checkAssured()
      await incomeStatementsPage.submit()

      await assertIncomeStatementCreated(startDate)
    })

    test('Gross income', async () => {
      await header.selectTab('income')
      await incomeStatementsPage.createNewIncomeStatement()
      await incomeStatementsPage.selectIncomeStatementType('gross-income')
      await incomeStatementsPage.checkIncomesRegisterConsent()
      await incomeStatementsPage.checkAssured()
      await incomeStatementsPage.submit()

      await assertIncomeStatementCreated()
    })
  })

  describe('Entrepreneur income', () => {
    test('Self employed', async () => {
      await header.selectTab('income')
      await incomeStatementsPage.createNewIncomeStatement()
      await incomeStatementsPage.selectIncomeStatementType(
        'entrepreneur-income'
      )
      await incomeStatementsPage.selectEntrepreneurType('part-time')
      await incomeStatementsPage.setEntrepreneurStartDate(
        LocalDate.today().addYears(-5).addWeeks(-7).format()
      )
      await incomeStatementsPage.selectEntrepreneurSpouse('no')

      await incomeStatementsPage.toggleEntrepreneurStartupGrant(true)
      await assertNeededAttachment('Starttirahapäätös')

      await incomeStatementsPage.toggleEntrepreneurCheckupConsent(true)

      await assertNeededAttachment('Tuloslaskelma ja tase', false)
      await incomeStatementsPage.toggleSelfEmployed(true)
      await assertNeededAttachment('Tuloslaskelma ja tase')

      await incomeStatementsPage.toggleSelfEmployedEstimatedIncome(true)
      await assertNeededAttachment('Tuloslaskelma ja tase', false)
      await incomeStatementsPage.toggleSelfEmployedEstimatedIncome(false)

      await incomeStatementsPage.toggleSelfEmployedAttachments(true)
      await assertNeededAttachment('Tuloslaskelma ja tase')

      await incomeStatementsPage.fillAccountant()

      await incomeStatementsPage.checkAssured()
      await incomeStatementsPage.submit()

      await assertIncomeStatementCreated()
    })

    test('Light entrepreneur', async () => {
      await header.selectTab('income')
      await incomeStatementsPage.createNewIncomeStatement()
      await incomeStatementsPage.selectIncomeStatementType(
        'entrepreneur-income'
      )
      await incomeStatementsPage.selectEntrepreneurType('full-time')
      await incomeStatementsPage.setEntrepreneurStartDate(
        LocalDate.today().addMonths(-3).format()
      )
      await incomeStatementsPage.selectEntrepreneurSpouse('no')

      await assertNeededAttachment(
        'Maksutositteet palkoista ja työkorvauksista',
        false
      )
      await incomeStatementsPage.toggleLightEntrepreneur(true)
      await assertNeededAttachment(
        'Maksutositteet palkoista ja työkorvauksista'
      )

      await incomeStatementsPage.toggleStudent(true)
      await assertNeededAttachment('Opiskelutodistus')
      await incomeStatementsPage.toggleAlimonyPayer(true)
      await assertNeededAttachment('Maksutosite elatusmaksuista')

      await incomeStatementsPage.checkAssured()
      await incomeStatementsPage.submit()

      await assertIncomeStatementCreated()
    })

    test('Partnership', async () => {
      await header.selectTab('income')
      await incomeStatementsPage.createNewIncomeStatement()
      await incomeStatementsPage.selectIncomeStatementType(
        'entrepreneur-income'
      )
      await incomeStatementsPage.selectEntrepreneurType('full-time')
      await incomeStatementsPage.setEntrepreneurStartDate(
        LocalDate.today().addMonths(-1).addDays(3).format()
      )
      await incomeStatementsPage.selectEntrepreneurSpouse('yes')

      await incomeStatementsPage.togglePartnership(true)
      await assertNeededAttachment('Tuloslaskelma ja tase')
      await assertNeededAttachment(
        'Kirjanpitäjän selvitys palkasta ja luontoiseduista'
      )
      await incomeStatementsPage.fillAccountant()

      await incomeStatementsPage.checkAssured()
      await incomeStatementsPage.submit()

      await assertIncomeStatementCreated()
    })
  })
})
