// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import LocalDate from 'lib-common/local-date'
import config from '../../config'
import {
  insertFeeDecisionFixtures,
  insertVoucherValueDecisionFixtures,
  resetDatabase
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  daycareFixture,
  enduserChildFixtureKaarina,
  enduserGuardianFixture,
  feeDecisionsFixture,
  Fixture,
  uuidv4,
  voucherValueDecisionsFixture
} from '../../dev-api/fixtures'
import GuardianInformationPage from '../../pages/employee/guardian-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let guardianPage: GuardianInformationPage

beforeEach(async () => {
  await resetDatabase()
  await initializeAreaAndPersonData()
  const financeAdmin = await Fixture.employeeFinanceAdmin().save()

  page = await Page.open({ acceptDownloads: true })
  await employeeLogin(page, financeAdmin.data)

  await page.goto(config.employeeUrl)
  guardianPage = new GuardianInformationPage(page)
})

describe('Person finance decisions', () => {
  test('Fee decisions are sorted by sent date', async () => {
    const sentAtFirst = LocalDate.today().subDays(3)
    const sentAtSecond = sentAtFirst.addDays(1)
    const sentAtThird = sentAtSecond.addDays(1)

    const createFeeDecisionFixture = async (sentAt: LocalDate) => {
      await insertFeeDecisionFixtures([
        feeDecisionsFixture(
          'SENT',
          enduserGuardianFixture,
          enduserChildFixtureKaarina,
          daycareFixture.id,
          null,
          new DateRange(sentAt, sentAt),
          sentAt.toSystemTzDate(),
          uuidv4()
        )
      ])
    }

    await createFeeDecisionFixture(sentAtFirst)
    await createFeeDecisionFixture(sentAtThird)
    await createFeeDecisionFixture(sentAtSecond)

    await guardianPage.navigateToGuardian(enduserGuardianFixture.id)
    const feeDecisions = await guardianPage.openCollapsible('feeDecisions')

    await feeDecisions.checkFeeDecisionSentAt(0, sentAtThird)
    await feeDecisions.checkFeeDecisionSentAt(1, sentAtSecond)
    await feeDecisions.checkFeeDecisionSentAt(2, sentAtFirst)
  })

  test('Voucher value decisions are sorted by sent date', async () => {
    const sentAtFirst = LocalDate.today().subDays(3)
    const sentAtSecond = sentAtFirst.addDays(1)
    const sentAtThird = sentAtSecond.addDays(1)

    const createVoucherValueDecisionFixture = async (sentAt: LocalDate) => {
      await insertVoucherValueDecisionFixtures([
        voucherValueDecisionsFixture(
          uuidv4(),
          enduserGuardianFixture.id,
          enduserChildFixtureKaarina.id,
          daycareFixture.id,
          null,
          'SENT',
          sentAt,
          sentAt,
          sentAt.toSystemTzDate()
        )
      ])
    }

    await createVoucherValueDecisionFixture(sentAtFirst)
    await createVoucherValueDecisionFixture(sentAtThird)
    await createVoucherValueDecisionFixture(sentAtSecond)

    await guardianPage.navigateToGuardian(enduserGuardianFixture.id)
    const voucherValueDecisions = await guardianPage.openCollapsible(
      'voucherValueDecisions'
    )

    await voucherValueDecisions.checkVoucherValueDecisionSentAt(0, sentAtThird)
    await voucherValueDecisions.checkVoucherValueDecisionSentAt(1, sentAtSecond)
    await voucherValueDecisions.checkVoucherValueDecisionSentAt(2, sentAtFirst)
  })
})
