// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import config from '../../config'
import {
  createDaycarePlacementFixture,
  testDaycare,
  testDaycarePrivateVoucher,
  testChild2,
  testAdult,
  feeDecisionsFixture,
  Fixture,
  uuidv4,
  voucherValueDecisionsFixture,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createDaycarePlacements,
  createDefaultServiceNeedOptions,
  createFeeDecisions,
  createVoucherValueDecisions,
  createVoucherValues,
  resetServiceState
} from '../../generated/api-clients'
import GuardianInformationPage from '../../pages/employee/guardian-information'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let guardianPage: GuardianInformationPage

beforeEach(async () => {
  await resetServiceState()
  await createDefaultServiceNeedOptions()
  await Fixture.careArea().with(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.daycare(testDaycarePrivateVoucher).save()
  const financeAdmin = await Fixture.employeeFinanceAdmin().save()

  page = await Page.open({ acceptDownloads: true })
  await employeeLogin(page, financeAdmin)

  await page.goto(config.employeeUrl)
  guardianPage = new GuardianInformationPage(page)
})

describe('Person finance decisions', () => {
  test('Fee decisions are sorted by sent date', async () => {
    await Fixture.family({ guardian: testAdult, children: [testChild2] }).save()

    const sentAtFirst = LocalDate.todayInSystemTz().subDays(3)
    const sentAtSecond = sentAtFirst.addDays(1)
    const sentAtThird = sentAtSecond.addDays(1)

    const createFeeDecisionFixture = async (sentAt: LocalDate) => {
      await createFeeDecisions({
        body: [
          feeDecisionsFixture(
            'SENT',
            testAdult,
            testChild2,
            testDaycare.id,
            null,
            new DateRange(sentAt, sentAt),
            sentAt.toHelsinkiDateTime(LocalTime.of(12, 0)),
            uuidv4()
          )
        ]
      })
    }

    await createFeeDecisionFixture(sentAtFirst)
    await createFeeDecisionFixture(sentAtThird)
    await createFeeDecisionFixture(sentAtSecond)

    await guardianPage.navigateToGuardian(testAdult.id)
    const feeDecisions = await guardianPage.openCollapsible('feeDecisions')

    await feeDecisions.checkFeeDecisionSentAt(0, sentAtThird)
    await feeDecisions.checkFeeDecisionSentAt(1, sentAtSecond)
    await feeDecisions.checkFeeDecisionSentAt(2, sentAtFirst)
  })

  test('Voucher value decisions are sorted by sent date', async () => {
    await Fixture.family({ guardian: testAdult, children: [testChild2] }).save()

    const sentAtFirst = LocalDate.todayInSystemTz().subDays(3)
    const sentAtSecond = sentAtFirst.addDays(1)
    const sentAtThird = sentAtSecond.addDays(1)

    const createVoucherValueDecisionFixture = async (sentAt: LocalDate) => {
      await createVoucherValueDecisions({
        body: [
          voucherValueDecisionsFixture(
            uuidv4(),
            testAdult.id,
            testChild2.id,
            testDaycare.id,
            null,
            'SENT',
            sentAt,
            sentAt,
            sentAt.toHelsinkiDateTime(LocalTime.of(12, 0))
          )
        ]
      })
    }

    await createVoucherValueDecisionFixture(sentAtFirst)
    await createVoucherValueDecisionFixture(sentAtThird)
    await createVoucherValueDecisionFixture(sentAtSecond)

    await guardianPage.navigateToGuardian(testAdult.id)
    const voucherValueDecisions = await guardianPage.openCollapsible(
      'voucherValueDecisions'
    )

    await voucherValueDecisions.checkVoucherValueDecisionSentAt(0, sentAtThird)
    await voucherValueDecisions.checkVoucherValueDecisionSentAt(1, sentAtSecond)
    await voucherValueDecisions.checkVoucherValueDecisionSentAt(2, sentAtFirst)
  })

  test('Retroactive voucher value decisions can be generated on demand', async () => {
    const from = LocalDate.todayInSystemTz().subMonths(2)

    await Fixture.feeThresholds().save()
    await createVoucherValues()

    await Fixture.family({ guardian: testAdult, children: [testChild2] }).save()
    await Fixture.parentship({
      childId: testChild2.id,
      headOfChildId: testAdult.id,
      startDate: testChild2.dateOfBirth,
      endDate: LocalDate.of(2099, 1, 1)
    }).save()

    await createDaycarePlacements({
      body: [
        createDaycarePlacementFixture(
          uuidv4(),
          testChild2.id,
          testDaycarePrivateVoucher.id,
          from,
          LocalDate.todayInSystemTz()
        )
      ]
    })

    const adminUser = await Fixture.employeeAdmin().save()
    page = await Page.open({ acceptDownloads: true })
    await employeeLogin(page, adminUser)
    await page.goto(config.employeeUrl)
    guardianPage = new GuardianInformationPage(page)
    await guardianPage.navigateToGuardian(testAdult.id)

    const voucherValueDecisions = await guardianPage.openCollapsible(
      'voucherValueDecisions'
    )

    await voucherValueDecisions.checkVoucherValueDecisionCount(0)
    await voucherValueDecisions.createRetroactiveDecisions(from)
    await voucherValueDecisions.checkVoucherValueDecisionCount(1)
  })
})
