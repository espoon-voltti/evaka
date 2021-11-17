// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { employeeLogin } from 'e2e-playwright/utils/user'
import config from 'e2e-test-common/config'
import {
  insertEmployeeFixture,
  insertFeeDecisionFixtures,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { Page } from 'playwright'
import {
  daycareFixture,
  enduserChildFixtureKaarina,
  enduserGuardianFixture,
  feeDecisionsFixture,
  uuidv4
} from '../../../e2e-test-common/dev-api/fixtures'
import { newBrowserContext } from '../../browser'
import { PersonProfilePage } from '../../pages/employee/person-profile'
import { initializeAreaAndPersonData } from '../../../e2e-test-common/dev-api/data-init'
import DateRange from 'lib-common/date-range'
import LocalDate from 'lib-common/local-date'

let page: Page
let personProfilePage: PersonProfilePage

beforeEach(async () => {
  await resetDatabase()
  await initializeAreaAndPersonData()

  page = await (await newBrowserContext({ acceptDownloads: true })).newPage()

  await insertEmployeeFixture({
    id: config.financeAdminAad,
    externalId: `espoo-ad:${config.financeAdminAad}`,
    email: 'lasse.laskuttaja@evaka.test',
    firstName: 'Lasse',
    lastName: 'Laskuttaja',
    roles: ['FINANCE_ADMIN']
  })
  await employeeLogin(page, 'FINANCE_ADMIN')

  await page.goto(config.employeeUrl)
  personProfilePage = new PersonProfilePage(page)
})

describe('Head of family fee decisions', () => {
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

    await personProfilePage.openPersonPage(enduserGuardianFixture.id)
    await personProfilePage.openCollapsible('person-fee-decisions')

    await personProfilePage.checkFeeDecisionSentAt(0, sentAtThird)
    await personProfilePage.checkFeeDecisionSentAt(1, sentAtSecond)
    await personProfilePage.checkFeeDecisionSentAt(2, sentAtFirst)
  })
})
