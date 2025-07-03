// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import type {
  DecisionIncome,
  FeeDecision
} from 'lib-common/generated/api-types/invoicing'
import type { FeeDecisionId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'

import config from '../../config'
import {
  testDaycare,
  feeDecisionsFixture,
  voucherValueDecisionsFixture,
  Fixture,
  testAdultRestricted,
  testAdult,
  testChild,
  testCareArea
} from '../../dev-api/fixtures'
import {
  createFeeDecisions,
  createVoucherValueDecisions,
  resetServiceState
} from '../../generated/api-clients'
import type { DevPerson, VoucherValueDecision } from '../../generated/api-types'
import CitizenDecisionsPage from '../../pages/citizen/citizen-decisions'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page
let header: CitizenHeader
let citizenDecisionsPage: CitizenDecisionsPage
const now = HelsinkiDateTime.of(2023, 3, 15, 12, 0)

let feeDecision: FeeDecision
let voucherValueDecision: VoucherValueDecision
let headOfFamily: DevPerson
let partner: DevPerson
let child: DevPerson

const feeDecisionValidDuring = new FiniteDateRange(
  LocalDate.of(2023, 1, 1),
  LocalDate.of(2023, 12, 31)
)
const voucherValueDecisionValidDuring = new DateRange(
  LocalDate.of(2023, 2, 1),
  null
)

beforeEach(async () => {
  await resetServiceState()
  await testCareArea.save()
  await testDaycare.save()
  await Fixture.family({ guardian: testAdult, children: [testChild] }).save()
  headOfFamily = testAdult
  partner = await testAdultRestricted.saveAdult({
    updateMockVtjWithDependants: []
  })
  child = testChild

  feeDecision = feeDecisionsFixture(
    'SENT',
    headOfFamily,
    child,
    testDaycare.id,
    partner,
    feeDecisionValidDuring,
    now,
    randomId<FeeDecisionId>(),
    'test-fd-key'
  )
  await insertFeeDecision(feeDecision)
  voucherValueDecision = voucherValueDecisionsFixture(
    randomId(),
    headOfFamily.id,
    child.id,
    testDaycare.id,
    null,
    'SENT',
    voucherValueDecisionValidDuring.start,
    voucherValueDecisionValidDuring.end ?? undefined,
    now.subHours(24),
    'test-vvd-key'
  )
  await insertVoucherValueDecision(voucherValueDecision)
})

const parsePersonNames = (persons: DevPerson[]) =>
  persons.map((person) => `${person.firstName} ${person.lastName}`)

describe('Citizen finance decisions', () => {
  test('Head of family sees their decisions with strong auth', async () => {
    page = await Page.open({ mockedTime: now })
    header = new CitizenHeader(page)
    citizenDecisionsPage = new CitizenDecisionsPage(page)
    await enduserLogin(page, testAdult)
    await page.goto(config.enduserUrl)

    await header.selectTab('decisions')

    await citizenDecisionsPage.assertFinanceDecisionShown(
      feeDecision.id,
      `Maksupäätös ${feeDecisionValidDuring.start.format()} -`,
      `${feeDecision.sentAt?.format()}`,
      parsePersonNames([headOfFamily, partner])
    )
    await citizenDecisionsPage.assertFinanceDecisionShown(
      voucherValueDecision.id,
      `Arvopäätös ${voucherValueDecisionValidDuring.start.format()} -`,
      `${voucherValueDecision.sentAt?.format()}`,
      parsePersonNames([headOfFamily]),
      parsePersonNames([child])
    )
  })

  test('Restricted partner sees their decisions with strong auth', async () => {
    page = await Page.open({ mockedTime: now })
    header = new CitizenHeader(page)
    citizenDecisionsPage = new CitizenDecisionsPage(page)
    await enduserLogin(page, partner)
    await page.goto(config.enduserUrl)

    await header.selectTab('decisions')

    await citizenDecisionsPage.assertFinanceDecisionShown(
      feeDecision.id,
      `Maksupäätös ${feeDecisionValidDuring.start.format()} -`,
      `${feeDecision.sentAt?.format()}`,
      parsePersonNames([headOfFamily, partner])
    )
    await citizenDecisionsPage.assertFinanceDecisionNotShown(
      voucherValueDecision.id
    )
  })

  test('Head of family sees their decision metadata with strong auth', async () => {
    page = await Page.open({ mockedTime: now })
    header = new CitizenHeader(page)
    citizenDecisionsPage = new CitizenDecisionsPage(page)
    await enduserLogin(page, testAdult)
    await page.goto(config.enduserUrl)

    await header.selectTab('decisions')

    await citizenDecisionsPage.assertMetadataForFinanceDecisionShown(
      feeDecision.id
    )
    await citizenDecisionsPage.assertMetadataForFinanceDecisionShown(
      voucherValueDecision.id
    )
  })

  test('Restricted partner sees their decision metadata with strong auth', async () => {
    page = await Page.open({ mockedTime: now })
    header = new CitizenHeader(page)
    citizenDecisionsPage = new CitizenDecisionsPage(page)
    await enduserLogin(page, partner)
    await page.goto(config.enduserUrl)

    await header.selectTab('decisions')

    await citizenDecisionsPage.assertMetadataForFinanceDecisionShown(
      feeDecision.id
    )
    await citizenDecisionsPage.assertMetadataForFinanceDecisionNotShown(
      voucherValueDecision.id
    )
  })
})

async function insertFeeDecision(
  fixture: FeeDecision,
  childIncome: DecisionIncome | null = null
) {
  await createFeeDecisions({
    body: [
      {
        ...fixture,
        children: fixture.children.map((child) => ({
          ...child,
          childIncome
        }))
      }
    ],
    insertCaseProcess: true
  })
}

async function insertVoucherValueDecision(fixture: VoucherValueDecision) {
  await createVoucherValueDecisions({
    body: [fixture],
    insertCaseProcess: true
  })
}
