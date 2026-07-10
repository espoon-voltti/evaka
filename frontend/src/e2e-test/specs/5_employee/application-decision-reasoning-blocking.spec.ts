// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

import { execSimpleApplicationActions } from '../../dev-api'
import {
  applicationFixture,
  Fixture,
  testAdult,
  testCareArea,
  testChild,
  testDaycare
} from '../../dev-api/fixtures'
import {
  createApplications,
  createDefaultServiceNeedOptions,
  resetServiceState
} from '../../generated/api-clients'
import ApplicationListView from '../../pages/employee/applications/application-list-view'
import { test, expect } from '../../playwright'
import type { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

const mockedDate = LocalDate.of(2021, 8, 16)

let page: Page
let applicationId: ApplicationId

test.beforeEach(async ({ evaka }) => {
  await resetServiceState()
  await Fixture.decisionReasoningGeneric({
    collectionType: 'DAYCARE',
    validFrom: LocalDate.of(2000, 1, 1),
    textFi: 'Keskeneräinen perustelu',
    textSv: 'Ofärdig motivering',
    ready: false
  }).save()
  await testCareArea.save()
  await testDaycare.save()
  await Fixture.family({ guardian: testAdult, children: [testChild] }).save()
  const serviceWorker = await Fixture.employee().serviceWorker().save()
  await createDefaultServiceNeedOptions()

  const fixture = applicationFixture(
    testChild,
    testAdult,
    undefined,
    'DAYCARE',
    null,
    [testDaycare.id],
    true,
    'SENT',
    mockedDate
  )
  applicationId = fixture.id
  await createApplications({ body: [fixture] })
  await execSimpleApplicationActions(
    applicationId,
    ['MOVE_TO_WAITING_PLACEMENT', 'CREATE_DEFAULT_PLACEMENT_PLAN'],
    HelsinkiDateTime.fromLocal(mockedDate, LocalTime.of(13, 0))
  )

  page = evaka
  await employeeLogin(page, serviceWorker)
})

test('sending decisions is blocked with an error dialog when the generic reasoning is not ready', async () => {
  const applicationListView = new ApplicationListView(page)
  await page.goto(ApplicationListView.url)
  await applicationListView.filterByApplicationStatus('WAITING_DECISION')
  await applicationListView.searchButton.click()

  await applicationListView.applicationRow(applicationId).checkbox.check()
  await applicationListView.actionBar.sendDecisionsWithoutProposal.click()

  const modal = page.findByDataQa('decision-reasoning-blocked-modal')
  await expect(modal).toBeVisible()
  await expect(modal).toContainText('perustelutekstit eivät ole valmiita')
  await page.findByDataQa('modal-okBtn').click()
  await expect(modal).toBeHidden()

  await expect(applicationListView.applicationRow(applicationId)).toBeVisible()
})
