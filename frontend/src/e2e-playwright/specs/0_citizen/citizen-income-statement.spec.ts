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

describe('Income statements', () => {
  test('Simple income statement can be created', async () => {
    const startDate = '24.12.2044'

    await header.incomeTab.click()
    await incomeStatementsPage.createNewIncomeStatement()
    await incomeStatementsPage.setValidFromDate(startDate)
    await incomeStatementsPage.selectIncomeStatementType('highest-fee')
    await incomeStatementsPage.checkAssured()
    await incomeStatementsPage.submit()

    await waitUntilEqual(async () => await incomeStatementsPage.rows.count(), 1)
    await waitUntilTrue(async () =>
      (await incomeStatementsPage.rows.innerText()).includes(startDate)
    )
  })
})
